package com.budgetify.app.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Schedules WorkManager sync jobs. Both require a network connection, so a job queued
 * while offline simply waits and runs automatically the moment the phone reconnects.
 */
object SyncScheduler {

    private const val ONE_TIME = "budgetify_sync_now"
    private const val PERIODIC = "budgetify_sync_periodic"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun requestSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
