package com.budgetify.app.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * RoomDB tables used for OFFLINE MODE. Expenses are always written here first
 * (isSynced = false) so the app works with no connection; a WorkManager job pushes
 * them to POST /api/transactions/sync as soon as the phone is online again.
 */
@Entity(tableName = "transactions", indices = [Index(value = ["clientId"], unique = true)])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    /** UUID generated on the phone; lets the server acknowledge a row it has never seen. */
    val clientId: String,
    val serverId: Int? = null,
    val userId: Int,
    val amount: Double,
    val categoryId: Int,
    /** ISO date yyyy-MM-dd */
    val transactionDate: String,
    val note: String = "",
    /** Private app-storage copy of the receipt photo (uploaded to Firebase Storage on sync). */
    val receiptPath: String? = null,
    /** Download URL of the receipt in Firebase Storage (blob storage). */
    val receiptUrl: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val name: String,
    val monthlyLimit: Double
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY transactionDate DESC, localId DESC")
    fun observeAll(userId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId")
    suspend fun getAll(userId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsynced(userId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE clientId = :clientId LIMIT 1")
    suspend fun findByClientId(clientId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId AND serverId = :serverId LIMIT 1")
    suspend fun findByServerId(userId: Int, serverId: Int): TransactionEntity?

    @Query("DELETE FROM transactions WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)
}

@Dao
abstract class CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name")
    abstract fun observeAll(userId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name")
    abstract suspend fun getAll(userId: Int): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE userId = :userId")
    abstract suspend fun deleteForUser(userId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<CategoryEntity>)

    @Transaction
    open suspend fun replaceAll(userId: Int, items: List<CategoryEntity>) {
        deleteForUser(userId)
        insertAll(items)
    }
}

@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budgetify_local.db"
                )
                    // The prototype (v1) schema is replaced; the server is the source of truth.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
