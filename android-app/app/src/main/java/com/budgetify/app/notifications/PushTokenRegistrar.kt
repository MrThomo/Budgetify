package com.budgetify.app.notifications

import android.util.Log
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.local.SessionManager
import com.budgetify.app.data.model.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Keeps the API informed of this phone's FCM token so the server can push to it. */
class PushTokenRegistrar(
    private val api: ApiService,
    private val session: SessionManager,
    private val prefs: AppPreferences
) {
    private val tag = "PushTokenRegistrar"

    suspend fun registerCurrentToken() {
        if (!session.isLoggedIn()) return
        var token: String? = null
        try {
            token = FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(tag, "FCM token unavailable (Firebase not configured or offline): ${e.message}")
        }
        val toSend = token ?: prefs.pendingFcmToken ?: return
        send(toSend)
    }

    /** Called by the FCM service when Firebase rotates the token. */
    suspend fun onNewToken(token: String) {
        prefs.pendingFcmToken = token
        if (session.isLoggedIn()) send(token)
    }

    private suspend fun send(token: String) {
        try {
            val response = api.registerDeviceToken(DeviceTokenRequest(token))
            if (response.isSuccessful) {
                prefs.pendingFcmToken = null
                Log.i(tag, "FCM token registered with the API")
            } else {
                prefs.pendingFcmToken = token
                Log.w(tag, "FCM token registration failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            prefs.pendingFcmToken = token
            Log.w(tag, "FCM token registration failed: ${e.message}")
        }
    }
}
