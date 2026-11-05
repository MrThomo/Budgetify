package com.budgetify.app.ui.settings

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.R
import com.budgetify.app.core.LocaleHelper
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.local.SessionManager
import com.budgetify.app.data.repository.AuthRepository
import com.budgetify.app.data.repository.SaveResult
import com.budgetify.app.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
    private val api: ApiService,
    private val prefs: AppPreferences,
    session: SessionManager
) : ViewModel() {

    private val tag = "SettingsViewModel"

    val accountName: String = session.getFullName().orEmpty()
    val accountEmail: String = session.getEmail().orEmpty()

    var currency by mutableStateOf(prefs.currency); private set
    var isDarkTheme by mutableStateOf(prefs.darkTheme.value); private set
    var language by mutableStateOf(LocaleHelper.currentCode()); private set
    var notificationsEnabled by mutableStateOf(prefs.notificationsEnabled); private set
    var messageRes by mutableStateOf<Int?>(null); private set

    init {
        // Pull the server copy so settings follow the user across phones.
        viewModelScope.launch {
            if (settings.pullAndApply()) {
                currency = prefs.currency
                isDarkTheme = prefs.darkTheme.value
                notificationsEnabled = prefs.notificationsEnabled
                language = LocaleHelper.currentCode()
            }
        }
    }

    fun onCurrencyChange(value: String) { currency = value }
    fun onThemeToggle(dark: Boolean) { isDarkTheme = dark }
    fun onLanguageChange(value: String) { language = value }
    fun onNotificationsToggle(enabled: Boolean) { notificationsEnabled = enabled }
    fun onNotificationPermissionDenied() {
        notificationsEnabled = false
        messageRes = R.string.notif_permission_denied
    }

    fun save() {
        viewModelScope.launch {
            val result = settings.save(currency, isDarkTheme, language, notificationsEnabled)
            messageRes = if (result == SaveResult.SYNCED) R.string.settings_saved else R.string.settings_saved_offline
            Log.i(tag, "Settings saved: $result")
        }
    }

    /** Asks the API to push a test notification to this phone via Firebase Cloud Messaging. */
    fun sendTestPush() {
        viewModelScope.launch {
            messageRes = try {
                if (api.sendTestNotification().isSuccessful) R.string.test_push_sent else R.string.test_push_failed
            } catch (e: Exception) {
                Log.w(tag, "Test push failed: ${e.message}")
                R.string.test_push_failed
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        auth.logout()
        onDone()
    }
}
