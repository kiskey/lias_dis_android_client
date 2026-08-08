// ====================================================================
// File: Theme.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Cupertino + Material3 bridge. Strict system theme adherence.
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.robinpcrd.cupertino.theme.CupertinoTheme
import io.github.robinpcrd.cupertino.theme.darkColorScheme as cupertinoDarkColorScheme
import io.github.robinpcrd.cupertino.theme.lightColorScheme as cupertinoLightColorScheme

private val LightColorScheme = lightColorScheme(
    primary = SystemBlueLight,
    onPrimary = SystemSecondaryBackgroundLight,
    primaryContainer = SystemBlueLight.copy(alpha = 0.1f),
    onPrimaryContainer = SystemBlueLight,
    background = SystemBackgroundLight,
    onBackground = SystemLabelLight,
    surface = SystemSecondaryBackgroundLight,
    onSurface = SystemLabelLight,
    surfaceVariant = SystemTertiaryBackgroundLight,
    onSurfaceVariant = SystemSecondaryLabelLight,
    error = SystemRedLight,
    onError = SystemSecondaryBackgroundLight,
    outline = SystemSeparatorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SystemBlueDark,
    onPrimary = SystemSecondaryBackgroundDark,
    primaryContainer = SystemBlueDark.copy(alpha = 0.16f),
    onPrimaryContainer = SystemBlueDark,
    background = SystemBackgroundDark,
    onBackground = SystemLabelDark,
    surface = SystemSecondaryBackgroundDark,
    onSurface = SystemLabelDark,
    surfaceVariant = SystemTertiaryBackgroundDark,
    onSurfaceVariant = SystemSecondaryLabelDark,
    error = SystemRedDark,
    onError = SystemSecondaryBackgroundDark,
    outline = SystemSeparatorDark
)

@Composable
fun LiasTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val cupertinoColorScheme = if (darkTheme) cupertinoDarkColorScheme() else cupertinoLightColorScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    CupertinoTheme(
        colorScheme = cupertinoColorScheme,
        content = {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = LiasTypography,
                content = content
            )
        }
    )
}
