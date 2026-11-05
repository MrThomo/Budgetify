package com.budgetify.app.notifications

import android.util.Log
import com.budgetify.app.BudgetifyApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives Firebase Cloud Messaging pushes. The API sends DATA messages (not ready-made
 * notifications), so this service always runs and builds the notification itself in the
 * user's chosen language.
 *
 * Payload examples:
 *   { "type": "budget_alert", "categoryId": "3", "categoryName": "Groceries", "percent": "80" }
 *   { "type": "test" }
 */
class BudgetifyMessagingService : FirebaseMessagingService() {

    private val tag = "BudgetifyFcm"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.i(tag, "New FCM token issued")
        val container = (application as BudgetifyApp).container
        scope.launch { container.pushTokens.onNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as BudgetifyApp).container
        val data = message.data
        Log.i(tag, "Push received: type=${data["type"]}")
        when (data["type"]) {
            "budget_alert" -> NotificationHelper.showBudgetAlert(
                context = this,
                prefs = container.prefs,
                categoryId = data["categoryId"]?.toIntOrNull() ?: 0,
                categoryName = data["categoryName"].orEmpty(),
                percent = data["percent"]?.toIntOrNull() ?: 100
            )
            "test" -> NotificationHelper.showTest(this, container.prefs)
            else -> Log.w(tag, "Unknown push type ignored")
        }
    }
}
