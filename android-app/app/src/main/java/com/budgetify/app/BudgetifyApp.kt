package com.budgetify.app

import android.app.Application
import android.util.Log
import com.budgetify.app.notifications.NotificationHelper
import com.budgetify.app.sync.SyncScheduler

class BudgetifyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this, container.prefs)
        SyncScheduler.schedulePeriodic(this)
        Log.i("BudgetifyApp", "Started. API base URL = ${BuildConfig.API_BASE_URL}")
    }
}
