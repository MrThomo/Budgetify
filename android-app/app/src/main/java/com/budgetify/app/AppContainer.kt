package com.budgetify.app

import android.content.Context
import com.budgetify.app.data.api.ApiFactory
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.AppDatabase
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.local.SessionManager
import com.budgetify.app.data.repository.*
import com.budgetify.app.notifications.PushTokenRegistrar
import com.budgetify.app.sync.NetworkMonitor
import com.budgetify.app.sync.SyncScheduler

/**
 * Hand-rolled dependency container (a full DI framework would be overkill here).
 * Created once in [BudgetifyApp]; ViewModels, the sync worker and the FCM service all
 * pull their dependencies from the same instances.
 */
class AppContainer(val context: Context) {

    val prefs = AppPreferences(context)
    val session = SessionManager(context)
    val database: AppDatabase = AppDatabase.getInstance(context)

    val api: ApiService = ApiFactory.create(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = { session.getToken() },
        debug = BuildConfig.DEBUG,
        onUnauthorized = { session.expire() },
        baseUrlOverride = { prefs.apiUrlOverride }
    )

    val cloud = CloudRepository(context)
    val networkMonitor = NetworkMonitor(context)
    val pushTokens = PushTokenRegistrar(api, session, prefs)

    val authRepository = AuthRepository(api, session, cloud)
    val settingsRepository = SettingsRepository(api, session, prefs)
    val budgetRepository = BudgetRepository(context, api, database, session, prefs, cloud, settingsRepository)

    /** Work that should happen right after any successful login / registration / SSO. */
    suspend fun afterLogin() {
        settingsRepository.pullAndApply()
        pushTokens.registerCurrentToken()
        budgetRepository.refreshCategories()
        SyncScheduler.requestSync(context)
    }
}
