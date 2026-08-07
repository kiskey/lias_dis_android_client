// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Color.kt
// Version: 2.2.0
// Purpose: HIG system palette and theme-resolved semantic colors.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Mode HIG System Colors
val SystemBlueLight = Color(0xFF0A84FF)
val SystemGreenLight = Color(0xFF30D158)
val SystemRedLight = Color(0xFFFF3B30)
val SystemOrangeLight = Color(0xFFFF9500)
val SystemIndigoLight = Color(0xFF5856D6)
val SystemTealLight = Color(0xFF00C7BE)
val SystemYellowLight = Color(0xFFFFCC00)
val SystemPinkLight = Color(0xFFFF2D55)

val SystemBackgroundLight = Color(0xFFF2F2F7)
val SystemSecondaryBackgroundLight = Color(0xFFFFFFFF)
val SystemTertiaryBackgroundLight = Color(0xFFE5E5EA)

val SystemLabelLight = Color(0xFF1C1C1E)
val SystemSecondaryLabelLight = Color(0xFF8A8A8E)
val SystemSeparatorLight = Color(0xFFC7C7CC)

// Light Mode Semantic Pill Backgrounds
val PillGreenBgLight  = Color(0xFFE6F9EA)
val PillRedBgLight    = Color(0xFFFFE9E7)
val PillOrangeBgLight = Color(0xFFFFF3E0)
val PillBlueBgLight   = Color(0xFFEAF3FF)

// Dark Mode HIG System Colors
val SystemBlueDark = Color(0xFF0A84FF)
val SystemGreenDark = Color(0xFF30D158)
val SystemRedDark = Color(0xFFFF453A)
val SystemOrangeDark = Color(0xFFFF9F0A)
val SystemIndigoDark = Color(0xFF5E5CE6)
val SystemTealDark = Color(0xFF64D2FF)
val SystemYellowDark = Color(0xFFFFD60A)
val SystemPinkDark = Color(0xFFFF375F)

val SystemBackgroundDark = Color(0xFF000000)
val SystemSecondaryBackgroundDark = Color(0xFF1C1C1E)
val SystemTertiaryBackgroundDark = Color(0xFF2C2C2E)

val SystemLabelDark = Color(0xFFFFFFFF)
val SystemSecondaryLabelDark = Color(0xFF8E8E93)
val SystemSeparatorDark = Color(0x38383A38)

// Dark Mode Semantic Pill Backgrounds
val PillGreenBgDark  = Color(0x2930D158)
val PillRedBgDark    = Color(0x29FF453A)
val PillOrangeBgDark = Color(0x29FF9F0A)
val PillBlueBgDark   = Color(0x290A84FF)

// Search Pill & Input Fill Tokens
val LiasFillLight = Color(0x1F767680)
val LiasFillDark  = Color(0x3D767680)

// Backward Compatibility Preset Aliases
val LiasBgPrimaryLight = SystemBackgroundLight
val LiasBgSecondaryLight = SystemSecondaryBackgroundLight
val LiasBgTertiaryLight = SystemTertiaryBackgroundLight
val LiasTextPrimaryLight = SystemLabelLight
val LiasTextSecondaryLight = SystemSecondaryLabelLight
val LiasAccentLight = SystemBlueLight
val LiasDangerLight = SystemRedLight

val LiasBgPrimaryDark = SystemBackgroundDark
val LiasBgSecondaryDark = SystemSecondaryBackgroundDark
val LiasBgTertiaryDark = SystemTertiaryBackgroundDark
val LiasTextPrimaryDark = SystemLabelDark
val LiasTextSecondaryDark = SystemSecondaryLabelDark
val LiasAccentDark = SystemBlueDark
val LiasDangerDark = SystemRedDark

// Theme Token Accessors
object LiasThemeColors {
    val fill: Color
        @Composable
        get() = if (isSystemInDarkTheme()) LiasFillDark else LiasFillLight

    val pillGreenBg: Color
        @Composable
        get() = if (isSystemInDarkTheme()) PillGreenBgDark else PillGreenBgLight

    val pillRedBg: Color
        @Composable
        get() = if (isSystemInDarkTheme()) PillRedBgDark else PillRedBgLight

    val pillOrangeBg: Color
        @Composable
        get() = if (isSystemInDarkTheme()) PillOrangeBgDark else PillOrangeBgLight

    val pillBlueBg: Color
        @Composable
        get() = if (isSystemInDarkTheme()) PillBlueBgDark else PillBlueBgLight
}
