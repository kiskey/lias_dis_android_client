// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Theme.kt
// Version: 1.0.0
// Purpose: Material 3 Theme configuration. Maps LiasColors to M3 ColorScheme.
//          Handles system dark mode and edge-to-edge window insets.
// ====================================================================

package com.lias.remote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LiasAccentLight,
    onPrimary = LiasBgSecondaryLight,
    background = LiasBgPrimaryLight,
    onBackground = LiasTextPrimaryLight,
    surface = LiasBgSecondaryLight,
    onSurface = LiasTextPrimaryLight,
    surfaceVariant = LiasBgTertiaryLight,
    onSurfaceVariant = LiasTextSecondaryLight,
    error = LiasDangerLight,
    onError = LiasBgSecondaryLight,
    outline = LiasSeparator
)

private val DarkColorScheme = darkColorScheme(
    primary = LiasAccentDark,
    onPrimary = LiasBgSecondaryDark,
    background = LiasBgPrimaryDark,
    onBackground = LiasTextPrimaryDark,
    surface = LiasBgSecondaryDark,
    onSurface = LiasTextPrimaryDark,
    surfaceVariant = LiasBgTertiaryDark,
    onSurfaceVariant = LiasTextSecondaryDark,
    error = LiasDangerDark,
    onError = LiasBgSecondaryDark,
    outline = LiasSeparatorDark
)

@Composable
fun LiasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar icon color to match theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LiasTypography,
        content = content
    )
}
