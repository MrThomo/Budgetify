package com.budgetify.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.budgetify.app.core.*
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.*
import com.budgetify.app.data.model.CategoryRequest
import com.budgetify.app.data.model.PendingTransactionDto
import com.budgetify.app.data.model.SyncRequest
import com.budgetify.app.data.model.TransactionDto
import com.budgetify.app.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

sealed class SyncResult {
    object Success : SyncResult()
    object NotLoggedIn : SyncResult()
    data class Failed(val reason: String) : SyncResult()
}

enum class SaveOutcome { SYNCED, OFFLINE }

/** Everything the Home / Budgets / Rewards screens need, derived from the local database. */
data class BudgetSnapshot(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val statuses: List<CategoryStatus> = emptyList(),
    val monthSpent: Double = 0.0,
    val game: GameState = GameState(),
    val pendingCount: Int = 0
)

/**
 * Offline-first repository. The Room database is the single source of truth for the UI;
 * [syncNow] reconciles it with the REST API in both directions.
 */
class BudgetRepository(
    private val appContext: Context,
    private val api: ApiService,
    private val db: AppDatabase,
    private val session: SessionManager,
    private val prefs: AppPreferences,
    private val cloud: CloudRepository,
    private val settingsRepository: SettingsRepository
) {
    private val tag = "BudgetRepository"
    private val syncMutex = Mutex()

    // ---------------------------------------------------------------- reading

    fun observeSnapshot(): Flow<BudgetSnapshot> {
        val uid = session.getUserId()
        return combine(
            db.transactionDao().observeAll(uid),
            db.categoryDao().observeAll(uid)
        ) { txs, cats -> buildSnapshot(txs, cats) }
    }

    private fun buildSnapshot(txs: List<TransactionEntity>, cats: List<CategoryEntity>): BudgetSnapshot {
        val today = LocalDate.now()
        val yearMonth = BudgetMath.yearMonth(today.toString())
        val expenses = txs.map {
            Expense(it.categoryId, it.amount, it.transactionDate, it.receiptPath != null || it.receiptUrl != null)
        }
        val statuses = BudgetMath.categoryStatuses(
            cats.map { CategoryInfo(it.id, it.name, it.monthlyLimit) }, expenses, yearMonth
        )
        return BudgetSnapshot(
            transactions = txs,
            categories = cats,
            statuses = statuses,
            monthSpent = BudgetMath.spentInMonth(expenses, yearMonth),
            game = GamificationEngine.evaluate(expenses, statuses, today),
            pendingCount = txs.count { !it.isSynced }
        )
    }

    // ---------------------------------------------------------------- categories (online)

    suspend fun refreshCategories(): Boolean {
        val uid = session.getUserId()
        if (uid < 0) return false
        return try {
            val response = api.getCategories()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                db.categoryDao().replaceAll(uid, body.map { CategoryEntity(it.id, uid, it.name, it.monthlyLimit) })
                Log.i(tag, "Loaded ${body.size} categories")
                true
            } else false
        } catch (e: Exception) {
            Log.w(tag, "Could not refresh categories: ${e.message}")
            false
        }
    }

    suspend fun addCategory(name: String, limit: Double): Boolean = try {
        val ok = api.createCategory(CategoryRequest(name.trim(), limit)).isSuccessful
        if (ok) refreshCategories()
        ok
    } catch (e: Exception) {
        Log.w(tag, "Add category failed: ${e.message}")
        false
    }

    suspend fun updateCategoryLimit(category: CategoryEntity, limit: Double): Boolean = try {
        val ok = api.updateCategory(category.id, CategoryRequest(category.name, limit)).isSuccessful
        if (ok) refreshCategories()
        ok
    } catch (e: Exception) {
        Log.w(tag, "Update category failed: ${e.message}")
        false
    }

    // ---------------------------------------------------------------- adding an expense (works offline)

    suspend fun addExpense(
        amount: Double,
        categoryId: Int,
        date: String,
        note: String,
        receiptUri: Uri?
    ): SaveOutcome {
        val uid = session.getUserId()
        val receiptPath = receiptUri?.let { copyReceiptToAppStorage(it) }

        db.transactionDao().insert(
            TransactionEntity(
                clientId = UUID.randomUUID().toString(),
                userId = uid,
                amount = amount,
                categoryId = categoryId,
                transactionDate = date,
                note = note.trim(),
                receiptPath = receiptPath
            )
        )
        Log.i(tag, "Expense saved locally (offline-first): $amount in category $categoryId")

        maybeShowLocalBudgetAlert(uid, categoryId, amount, date)

        val result = syncNow()
        return if (result is SyncResult.Success) SaveOutcome.SYNCED else SaveOutcome.OFFLINE
    }

    /** Immediate on-device alert (works offline). The server's FCM push uses the same tag, so they replace each other. */
    private suspend fun maybeShowLocalBudgetAlert(uid: Int, categoryId: Int, amount: Double, date: String) {
        if (!prefs.notificationsEnabled) return
        val category = db.categoryDao().getAll(uid).firstOrNull { it.id == categoryId } ?: return
        val yearMonth = BudgetMath.yearMonth(date)
        val after = db.transactionDao().getAll(uid)
            .filter { it.categoryId == categoryId && it.transactionDate.startsWith(yearMonth) }
            .sumOf { it.amount }
        val crossed = BudgetMath.crossedThreshold(after - amount, after, category.monthlyLimit)
        if (crossed != null) {
            NotificationHelper.showBudgetAlert(appContext, prefs, category.id, category.name, crossed)
        }
    }

    private suspend fun copyReceiptToAppStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(appContext.filesDir, "receipts").apply { mkdirs() }
            val out = File(dir, "${UUID.randomUUID()}.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            if (out.length() > 0) out.absolutePath else null
        } catch (e: Exception) {
            Log.e(tag, "Could not copy receipt image", e)
            null
        }
    }

    // ---------------------------------------------------------------- sync

    /**
     * Push-then-pull sync:
     *  1. upload any receipt photos still on the phone to Firebase Storage,
     *  2. POST unsynced expenses to /api/transactions/sync,
     *  3. mark acknowledged rows as synced and merge anything newer from the server.
     */
    suspend fun syncNow(): SyncResult = syncMutex.withLock {
        val uid = session.getUserId()
        if (uid < 0 || !session.isLoggedIn()) return SyncResult.NotLoggedIn

        try {
            settingsRepository.pushIfDirty()

            val pending = db.transactionDao().getUnsynced(uid).map { tx ->
                if (tx.receiptUrl == null && tx.receiptPath != null) {
                    val url = cloud.uploadReceipt(uid, File(tx.receiptPath))
                    if (url != null) {
                        val updated = tx.copy(receiptUrl = url)
                        db.transactionDao().update(updated)
                        updated
                    } else tx
                } else tx
            }

            val request = SyncRequest(
                lastSyncTimestamp = prefs.lastSync(uid),
                pendingTransactions = pending.map {
                    PendingTransactionDto(
                        localId = it.clientId,
                        amount = it.amount,
                        categoryId = it.categoryId,
                        transactionDate = "${it.transactionDate}T00:00:00",
                        note = it.note,
                        receiptUrl = it.receiptUrl
                    )
                }
            )

            val response = api.sync(request)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                Log.w(tag, "Sync failed with HTTP ${response.code()}")
                return SyncResult.Failed("HTTP ${response.code()}")
            }

            body.syncedLocalIds.forEach { clientId ->
                db.transactionDao().findByClientId(clientId)?.let {
                    db.transactionDao().update(it.copy(isSynced = true))
                }
            }
            // The server refused these (e.g. a category that no longer exists); drop them so they don't retry forever.
            body.rejectedLocalIds.orEmpty().forEach { db.transactionDao().deleteByClientId(it) }
            body.serverUpdates.forEach { mergeServerTransaction(uid, it) }

            prefs.setLastSync(uid, body.syncTimestamp)
            refreshCategories()
            publishAchievements()
            Log.i(tag, "Sync complete: pushed ${body.syncedLocalIds.size}, pulled ${body.serverUpdates.size}")
            SyncResult.Success
        } catch (e: Exception) {
            Log.w(tag, "Sync failed (probably offline): ${e.message}")
            SyncResult.Failed(e.message ?: "error")
        }
    }

    private suspend fun mergeServerTransaction(uid: Int, s: TransactionDto) {
        val dao = db.transactionDao()
        val existing = s.localId?.let { dao.findByClientId(it) } ?: dao.findByServerId(uid, s.id)
        if (existing != null) {
            dao.update(
                existing.copy(
                    serverId = s.id,
                    isSynced = true,
                    receiptUrl = s.receiptUrl ?: existing.receiptUrl
                )
            )
        } else {
            dao.insert(
                TransactionEntity(
                    clientId = s.localId ?: "srv-${s.id}",
                    serverId = s.id,
                    userId = uid,
                    amount = s.amount,
                    categoryId = s.categoryId,
                    transactionDate = s.transactionDate.take(10),
                    note = s.note.orEmpty(),
                    receiptUrl = s.receiptUrl,
                    isSynced = true
                )
            )
        }
    }

    // ---------------------------------------------------------------- gamification -> Firestore

    suspend fun publishAchievements() {
        val uid = session.getUserId()
        if (uid < 0) return
        val snapshot = buildSnapshot(db.transactionDao().getAll(uid), db.categoryDao().getAll(uid))
        val alreadyPushed = prefs.pushedBadges(uid)
        val fresh = snapshot.game.unlocked.map { it.key }.toSet() - alreadyPushed
        val issued = cloud.publishGameState(uid, snapshot.game, fresh.mapNotNull { Badge.fromKey(it) }.toSet())
        if (issued) prefs.setPushedBadges(uid, alreadyPushed + fresh)
    }

    suspend fun loadCloudBadges(): Set<Badge> = cloud.loadUnlockedBadges(session.getUserId())
}
