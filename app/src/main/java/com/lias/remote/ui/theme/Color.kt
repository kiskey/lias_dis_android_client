// ====================================================================
// File: Color.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Strict Apple HIG system color palette with dynamic light/dark.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// HIG System Colors (Light)
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

val SystemLabelLight = Color(0xFF000000)
val SystemSecondaryLabelLight = Color(0xFF3C3C43).copy(alpha = 0.6f)
val SystemTertiaryLabelLight = Color(0xFF3C3C43).copy(alpha = 0.3f)
val SystemQuaternaryLabelLight = Color(0xFF3C3C43).copy(alpha = 0.18f)

val SystemSeparatorLight = Color(0xFF3C3C43).copy(alpha = 0.29f)
val SystemOpaqueSeparatorLight = Color(0xFFC6C6C8)

// HIG System Colors (Dark)
val SystemBlueDark = Color(0xFF0A84FF)
val SystemGreenDark = Color(0xFF30D158)
val SystemIndigoDark = Color(0xFF5E5CE6)
val SystemOrangeDark = Color(0xFFFF9F0A)
val SystemPinkDark = Color(0xFFFF375F)
val SystemPurpleDark = Color(0xFFBF5AF2)
val SystemRedDark = Color(0xFFFF453A)
val SystemTealDark = Color(0xFF40C8E0)
val SystemYellowDark = Color(0xFFFFD60A)

val SystemBackgroundDark = Color(0xFF000000)
val SystemSecondaryBackgroundDark = Color(0xFF1C1C1E)
val SystemTertiaryBackgroundDark = Color(0xFF2C2C2E)

val SystemLabelDark = Color(0xFFFFFFFF)
val SystemSecondaryLabelDark = Color(0xFFEBEBF5).copy(alpha = 0.6f)
val SystemTertiaryLabelDark = Color(0xFFEBEBF5).copy(alpha = 0.3f)
val SystemQuaternaryLabelDark = Color(0xFFEBEBF5).copy(alpha = 0.16f)

val SystemSeparatorDark = Color(0xFF545458).copy(alpha = 0.65f)
val SystemOpaqueSeparatorDark = Color(0xFF38383A)

val FillLight = Color(0xFF7C7C80).copy(alpha = 0.12f)
val FillDark = Color(0xFF767680).copy(alpha = 0.24f)

object LiasThemeColors {
    val fill: Color
        @Composable get() = if (isSystemInDarkTheme()) FillDark else FillLight
}
