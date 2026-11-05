package com.budgetify.app.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.budgetify.app.BudgetifyApp
import com.budgetify.app.data.repository.SyncResult

/** Background job that pushes offline expenses once the phone has a connection. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BudgetifyApp).container
        return when (val result = container.budgetRepository.syncNow()) {
            is SyncResult.Success -> {
                Log.i("SyncWorker", "Background sync succeeded")
                Result.success()
            }
            is SyncResult.NotLoggedIn -> Result.success()
            is SyncResult.Failed -> {
                Log.w("SyncWorker", "Background sync failed (${result.reason}); will retry")
                Result.retry()
            }
        }
    }
}
