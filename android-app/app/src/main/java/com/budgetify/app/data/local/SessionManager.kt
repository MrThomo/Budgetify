package com.budgetify.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the logged-in user's JWT (and basic profile info) in EncryptedSharedPreferences
 * so the token is never stored in plain text on disk. This is what keeps the user logged
 * in between app launches. The token is also cached in memory because the HTTP interceptor
 * reads it on every request.
 */
class SessionManager(context: Context) {

    private val tag = "SessionManager"

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "budgetify_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Volatile
    private var cachedToken: String? = prefs.getString(KEY_TOKEN, null)

    private val _expired = MutableStateFlow(false)

    /** Becomes true when the API rejects our token (expired); the UI then sends the user to Login. */
    val expired: StateFlow<Boolean> = _expired.asStateFlow()

    fun expire() {
        if (cachedToken == null) return
        Log.w(tag, "Token rejected by API - ending session")
        clearSession()
        _expired.value = true
    }

    fun acknowledgeExpired() { _expired.value = false }

    fun saveSession(userId: Int, fullName: String, email: String, token: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_TOKEN, token)
            .apply()
        cachedToken = token
        Log.d(tag, "Session saved for user $userId")
    }

    fun getToken(): String? = cachedToken
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun isLoggedIn(): Boolean = cachedToken != null

    fun clearSession() {
        prefs.edit().clear().apply()
        cachedToken = null
        Log.d(tag, "Session cleared (logout)")
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_TOKEN = "auth_token"
    }
}
