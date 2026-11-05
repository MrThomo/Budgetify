package com.budgetify.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Emerald = Color(0xFF0F9D58)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B7A44),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F0E0),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF3E6B57),
    background = Color(0xFFF6FBF8),
    surface = Color(0xFFF6FBF8),
    surfaceVariant = Color(0xFFDDE5DF)
)

private val DarkColors = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF00210F),
    primaryContainer = Color(0xFF0B5A33),
    onPrimaryContainer = Color(0xFFD3F0E0),
    secondary = Color(0xFFA5CFBA),
    background = Color(0xFF0E1512),
    surface = Color(0xFF0E1512),
    surfaceVariant = Color(0xFF3F4943)
)

@Composable
fun BudgetifyTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The in-app theme can differ from the system theme, so set status/nav bar icon colours ourselves.
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
