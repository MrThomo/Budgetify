package com.budgetify.app.data.repository

import android.util.Log
import com.budgetify.app.core.LocaleHelper
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.local.SessionManager
import com.budgetify.app.data.model.UpdateSettingsRequest

enum class SaveResult { SYNCED, SAVED_LOCALLY }

/**
 * User settings (currency, theme, language, notifications). They are always applied and
 * stored on the phone first, then pushed to GET/PUT /api/settings. If the push fails
 * (offline) the settings are flagged "dirty" and pushed on the next successful sync.
 */
class SettingsRepository(
    private val api: ApiService,
    private val session: SessionManager,
    private val prefs: AppPreferences
) {
    private val tag = "SettingsRepository"

    private fun toRequest() = UpdateSettingsRequest(
        currency = prefs.currency,
        theme = if (prefs.darkTheme.value) "dark" else "light",
        language = prefs.language,
        notificationsEnabled = prefs.notificationsEnabled
    )

    /** Pushes local settings to the server if an earlier push failed. */
    suspend fun pushIfDirty() {
        if (!prefs.settingsDirty || !session.isLoggedIn()) return
        try {
            val response = api.updateSettings(toRequest())
            if (response.isSuccessful) {
                prefs.settingsDirty = false
                Log.i(tag, "Pushed offline settings changes")
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not push dirty settings yet: ${e.message}")
        }
    }

    /** Pulls the server copy (so settings follow the user to a new phone) and applies it. */
    suspend fun pullAndApply(): Boolean {
        if (!session.isLoggedIn()) return false
        pushIfDirty()
        if (prefs.settingsDirty) return false
        return try {
            val response = api.getSettings()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                prefs.setCurrency(body.currency)
                prefs.setDarkTheme(body.theme == "dark")
                prefs.notificationsEnabled = body.notificationsEnabled
                prefs.language = body.language
                LocaleHelper.apply(body.language)
                Log.i(tag, "Settings pulled from server")
                true
            } else false
        } catch (e: Exception) {
            Log.w(tag, "Could not pull settings: ${e.message}")
            false
        }
    }

    suspend fun save(
        currency: String,
        dark: Boolean,
        language: String,
        notifications: Boolean
    ): SaveResult {
        prefs.setCurrency(currency)
        prefs.setDarkTheme(dark)
        prefs.notificationsEnabled = notifications
        prefs.language = language
        prefs.settingsDirty = true

        val result = try {
            val response = api.updateSettings(toRequest())
            if (response.isSuccessful) {
                prefs.settingsDirty = false
                SaveResult.SYNCED
            } else SaveResult.SAVED_LOCALLY
        } catch (e: Exception) {
            Log.w(tag, "Settings saved locally only: ${e.message}")
            SaveResult.SAVED_LOCALLY
        }
        // Applying the language recreates the Activity, so do it last.
        LocaleHelper.apply(language)
        return result
    }
}
