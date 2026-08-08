package com.lias.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalLiasDarkTheme = staticCompositionLocalOf { false }

// Apple HIG Light Palette
val SystemBlueLight = Color(0xFF007AFF)
val SystemGreenLight = Color(0xFF34C759)
val SystemIndigoLight = Color(0xFF5856D6)
val SystemOrangeLight = Color(0xFFFF9500)
val SystemPinkLight = Color(0xFFFF2D55)
val SystemPurpleLight = Color(0xFFAF52DE)
val SystemRedLight = Color(0xFFFF3B30)
val SystemTealLight = Color(0xFF5AC8FA)
val SystemYellowLight = Color(0xFFFFCC00)

val SystemBackgroundLight = Color(0xFFF2F2F7)
val SystemSecondaryBackgroundLight = Color(0xFFFFFFFF)
val SystemTertiaryBackgroundLight = Color(0xFFE5E5EA)

val SystemLabelLight = Color(0xFF1C1C1E)
val SystemSecondaryLabelLight = Color(0xFF3C3C43)
val SystemTertiaryLabelLight = Color(0xFF8E8E93)

val SystemSeparatorLight = Color(0x2E3C3C43)
val FillLight = Color(0x1F767680)
val FillLight2 = Color(0x29767680)

// Apple HIG Dark Palette
val SystemBlueDark = Color(0xFF0A84FF)
val SystemGreenDark = Color(0xFF30D158)
val SystemIndigoDark = Color(0xFF5E5CE6)
val SystemOrangeDark = Color(0xFFFF9F0A)
val SystemPinkDark = Color(0xFFFF375F)
val SystemPurpleDark = Color(0xFFBF5AF2)
val SystemRedDark = Color(0xFFFF453A)
val SystemTealDark = Color(0xFF64D2FF)
val SystemYellowDark = Color(0xFFFFD60A)

val SystemBackgroundDark = Color(0xFF000000)
val SystemSecondaryBackgroundDark = Color(0xFF1C1C1E)
val SystemTertiaryBackgroundDark = Color(0xFF2C2C2E)

val SystemLabelDark = Color(0xFFFFFFFF)
val SystemSecondaryLabelDark = Color(0xEBEBF5)
val SystemTertiaryLabelDark = Color(0xFF8E8E93)

val SystemSeparatorDark = Color(0x99545458)
val FillDark = Color(0x3D767680)
val FillDark2 = Color(0x52767680)

object LiasThemeColors {
    val isDark: Boolean
        @Composable get() = LocalLiasDarkTheme.current

    val background: Color
        @Composable get() = if (isDark) SystemBackgroundDark else SystemBackgroundLight

    val secondaryBackground: Color
        @Composable get() = if (isDark) SystemSecondaryBackgroundDark else SystemSecondaryBackgroundLight

    val tertiaryBackground: Color
        @Composable get() = if (isDark) SystemTertiaryBackgroundDark else SystemTertiaryBackgroundLight

    val label: Color
        @Composable get() = if (isDark) SystemLabelDark else SystemLabelLight

    val secondaryLabel: Color
        @Composable get() = if (isDark) SystemSecondaryLabelDark else SystemSecondaryLabelLight

    val tertiaryLabel: Color
        @Composable get() = if (isDark) SystemTertiaryLabelDark else SystemTertiaryLabelLight

    val separator: Color
        @Composable get() = if (isDark) SystemSeparatorDark else SystemSeparatorLight

    val fill: Color
        @Composable get() = if (isDark) FillDark else FillLight

    val fill2: Color
        @Composable get() = if (isDark) FillDark2 else FillLight2

    val blue: Color
        @Composable get() = if (isDark) SystemBlueDark else SystemBlueLight

    val green: Color
        @Composable get() = if (isDark) SystemGreenDark else SystemGreenLight

    val red: Color
        @Composable get() = if (isDark) SystemRedDark else SystemRedLight

    val orange: Color
        @Composable get() = if (isDark) SystemOrangeDark else SystemOrangeLight

    val indigo: Color
        @Composable get() = if (isDark) SystemIndigoDark else SystemIndigoLight
}
