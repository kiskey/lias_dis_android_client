// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Theme.kt
// Version: 30.0.0
//
// Purpose:
//   LIAS-owned theme adapter.
//
// Compose Cupertino migration Plan 3.0 / Batch 1:
//   - Replaces the old Cupertino package namespace with the maintained
//     fork namespace.
//   - Preserves LIAS color tokens, dark/light behavior, system-bar
//     behavior, and the public LiasTheme API.
// ====================================================================

package com.lias.remote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.slapps.cupertino.theme.CupertinoTheme
import com.slapps.cupertino.theme.darkColorScheme
import com.slapps.cupertino.theme.lightColorScheme

@Composable
fun LiasTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark =
        isSystemInDarkTheme()

    val darkTheme =
        when (themeMode) {
            "light" ->
                false

            "dark" ->
                true

            else ->
                systemDark
        }

    val cupertinoColorScheme =
        if (darkTheme) {
            darkColorScheme(
                accent =
                    SystemBlueDark,
                systemBackground =
                    SystemBackgroundDark,
                secondarySystemBackground =
                    SystemSecondaryBackgroundDark,
                tertiarySystemBackground =
                    SystemTertiaryBackgroundDark,
                label =
                    SystemLabelDark,
                secondaryLabel =
                    SystemSecondaryLabelDark,
                tertiaryLabel =
                    SystemTertiaryLabelDark
            )
        } else {
            lightColorScheme(
                accent =
                    SystemBlueLight,
                systemBackground =
                    SystemBackgroundLight,
                secondarySystemBackground =
                    SystemSecondaryBackgroundLight,
                tertiarySystemBackground =
                    SystemTertiaryBackgroundLight,
                label =
                    SystemLabelLight,
                secondaryLabel =
                    SystemSecondaryLabelLight,
                tertiaryLabel =
                    SystemTertiaryLabelLight
            )
        }

    val view =
        LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window =
                (view.context as Activity)
                    .window

            WindowCompat
                .getInsetsController(
                    window,
                    view
                )
                .isAppearanceLightStatusBars =
                !darkTheme

            val bgColor =
                if (darkTheme) {
                    SystemBackgroundDark
                } else {
                    SystemBackgroundLight
                }

            window.statusBarColor =
                bgColor.toArgb()

            window.navigationBarColor =
                bgColor.toArgb()
        }
    }

    CompositionLocalProvider(
        LocalLiasDarkTheme provides darkTheme
    ) {
        CupertinoTheme(
            colorScheme =
                cupertinoColorScheme,
            content =
                content
        )
    }
}
