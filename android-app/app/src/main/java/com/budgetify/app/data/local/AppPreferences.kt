package com.budgetify.app.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Non-sensitive local settings (theme, currency, language, notification opt-in) plus small
 * bookkeeping values for sync. Theme and currency are exposed as StateFlows so the UI
 * updates instantly when the user changes them in Settings.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("budgetify_prefs", Context.MODE_PRIVATE)

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK, false))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, "ZAR") ?: "ZAR")
    val currencyFlow: StateFlow<String> = _currency.asStateFlow()

    val currency: String get() = _currency.value

    fun setCurrency(value: String) {
        prefs.edit().putString(KEY_CURRENCY, value).apply()
        _currency.value = value
    }

    fun setDarkTheme(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, value).apply()
        _darkTheme.value = value
    }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value).apply() }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply() }

    /** True when settings were changed while offline and still need pushing to the API. */
    var settingsDirty: Boolean
        get() = prefs.getBoolean(KEY_SETTINGS_DIRTY, false)
        set(value) { prefs.edit().putBoolean(KEY_SETTINGS_DIRTY, value).apply() }

    /** Server address typed in Login -> Server settings (overrides the build-time URL). */
    var apiUrlOverride: String?
        get() = prefs.getString(KEY_API_URL, null)
        set(value) { prefs.edit().putString(KEY_API_URL, value).apply() }

    var pendingFcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_FCM_TOKEN, value).apply() }

    fun lastSync(userId: Int): String? = prefs.getString("last_sync_$userId", null)
    fun setLastSync(userId: Int, value: String?) {
        prefs.edit().putString("last_sync_$userId", value).apply()
    }

    fun pushedBadges(userId: Int): Set<String> =
        prefs.getStringSet("pushed_badges_$userId", emptySet()) ?: emptySet()

    fun setPushedBadges(userId: Int, value: Set<String>) {
        prefs.edit().putStringSet("pushed_badges_$userId", value).apply()
    }

    companion object {
        private const val KEY_DARK = "dark_theme"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_SETTINGS_DIRTY = "settings_dirty"
        private const val KEY_API_URL = "api_url_override"
        private const val KEY_FCM_TOKEN = "pending_fcm_token"
    }
}
