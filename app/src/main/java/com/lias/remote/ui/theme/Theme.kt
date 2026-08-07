// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Theme.kt
// Version: 3.0.0
// Purpose: Material 3 Theme mapping iOS HIG colors and system bar insets.
//          Updated to wrap with CupertinoTheme for Cupertino library components.
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
import io.github.robinpcrd.cupertino.CupertinoTheme
import io.github.robinpcrd.cupertino.CupertinoColorScheme

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    // Wrap with CupertinoTheme for Cupertino components while keeping MaterialTheme for interop
    CupertinoTheme(
        darkTheme = darkTheme,
        content = content
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LiasTypography,
            content = content
        )
    }
}
