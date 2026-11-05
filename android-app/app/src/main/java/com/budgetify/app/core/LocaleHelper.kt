package com.budgetify.app.core

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Multi-language support: English (en), isiZulu (zu) and Sesotho (st).
 * Uses AppCompat's per-app language API, which recreates the Activity so every
 * stringResource() call re-resolves in the new language.
 */
object LocaleHelper {

    private const val TAG = "LocaleHelper"
    val SUPPORTED = listOf("en", "zu", "st")

    /** The language the app is currently showing. */
    fun currentCode(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val lang = if (tags.isEmpty()) Locale.getDefault().language else tags.substringBefore('-')
        return if (lang in SUPPORTED) lang else "en"
    }

    /** Switches the whole app to [code]. Safe to call from any thread; no-op if unchanged. */
    fun apply(code: String) {
        if (code !in SUPPORTED || code == currentCode()) return
        Log.i(TAG, "Switching app language to $code")
        Handler(Looper.getMainLooper()).post {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        }
    }

    /**
     * A Context whose resources use [code]. Needed for notifications built by the FCM service,
     * because a Service does not follow the per-app language on Android < 13.
     */
    fun localized(context: Context, code: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(code))
        return context.createConfigurationContext(config)
    }
}
