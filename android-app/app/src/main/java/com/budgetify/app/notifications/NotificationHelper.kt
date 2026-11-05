package com.budgetify.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.budgetify.app.MainActivity
import com.budgetify.app.R
import com.budgetify.app.core.CategoryNames
import com.budgetify.app.core.LocaleHelper
import com.budgetify.app.data.local.AppPreferences

/**
 * Builds and shows the app's notifications. Used both for LOCAL alerts (after the user logs an
 * expense, even offline) and for REMOTE alerts delivered by Firebase Cloud Messaging.
 * Text is resolved in the user's chosen language, so alerts are translated too.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "budget_alerts"

    fun createChannel(context: Context, prefs: AppPreferences) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ctx = LocaleHelper.localized(context, prefs.language)
        val channel = NotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = ctx.getString(R.string.channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** percent is 80 (warning) or 100 (over budget). */
    fun showBudgetAlert(context: Context, prefs: AppPreferences, categoryId: Int, categoryName: String, percent: Int) {
        if (!prefs.notificationsEnabled) return
        val ctx = LocaleHelper.localized(context, prefs.language)
        val label = CategoryNames.resFor(categoryName)?.let { ctx.getString(it) } ?: categoryName
        val body = if (percent >= 100) {
            ctx.getString(R.string.notif_budget_over_body, label)
        } else {
            ctx.getString(R.string.notif_budget_warn_body, label, percent)
        }
        // Same tag per category => a local alert and the server push for the same event replace each other.
        post(context, "budget_$categoryId", ctx.getString(R.string.notif_budget_title), body)
    }

    fun showTest(context: Context, prefs: AppPreferences) {
        val ctx = LocaleHelper.localized(context, prefs.language)
        post(context, "test", ctx.getString(R.string.notif_budget_title), ctx.getString(R.string.notif_test_body))
    }

    @SuppressLint("MissingPermission") // permission is checked explicitly just below
    private fun post(context: Context, tag: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping '$tag'")
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(tag, 1, notification)
        Log.i(TAG, "Notification shown: $tag")
    }
}
