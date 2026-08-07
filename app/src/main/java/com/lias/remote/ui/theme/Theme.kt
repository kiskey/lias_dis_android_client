// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Theme.kt
// Version: 3.4.0
// Purpose: Material 3 & Cupertino Theme integration mapping iOS HIG colors.
// Audit Fixes:
//   1. Added time-of-day automatic switching (6 AM - 6 PM light, 6 PM - 6 AM dark).
//   2. Supported manual "system", "light", "dark" theme mode overrides.
// ====================================================================

package com.lias.remote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.robinpcrd.cupertino.theme.CupertinoTheme
import io.github.robinpcrd.cupertino.theme.darkColorScheme as cupertinoDarkColorScheme
import io.github.robinpcrd.cupertino.theme.lightColorScheme as cupertinoLightColorScheme
import java.util.Calendar

private val LightColorScheme = lightColorScheme(
    primary = SystemBlueLight,
    onPrimary = SystemSecondaryBackgroundLight,
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
    themeMode: String = "system", // "system", "light", "dark"
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    
    val isNightTime = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour < 6 || hour >= 18 // 6 PM to 6 AM is night
    }

    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark || isNightTime // Auto time-of-day or system setting
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
