package com.lias.remote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.darkColorScheme
import io.github.alexzhirkevich.cupertino.theme.lightColorScheme

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

    val cupertinoColorScheme = if (darkTheme) {
        darkColorScheme(
            accent = SystemBlueDark,
            systemBackground = SystemBackgroundDark,
            secondarySystemBackground = SystemSecondaryBackgroundDark,
            tertiarySystemBackground = SystemTertiaryBackgroundDark,
            label = SystemLabelDark,
            secondaryLabel = SystemSecondaryLabelDark,
            tertiaryLabel = SystemTertiaryLabelDark
        )
    } else {
        lightColorScheme(
            accent = SystemBlueLight,
            systemBackground = SystemBackgroundLight,
            secondarySystemBackground = SystemSecondaryBackgroundLight,
            tertiarySystemBackground = SystemTertiaryBackgroundLight,
            label = SystemLabelLight,
            secondaryLabel = SystemSecondaryLabelLight,
            tertiaryLabel = SystemTertiaryLabelLight
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            val bgColor = if (darkTheme) SystemBackgroundDark else SystemBackgroundLight
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
        }
    }

    CupertinoTheme(
        colorScheme = cupertinoColorScheme,
        content = content
    )
}
