// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Color.kt
// Version: 1.0.0
// Purpose: Material 3 Color definitions. Extracted directly from the
//          LIAS web dashboard CSS variables to ensure strict visual
//          parity across light and dark modes.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors (Extracted from web :root)
val LiasBgPrimaryLight = Color(0xFFF5F5F7)
val LiasBgSecondaryLight = Color(0xFFFFFFFF)
val LiasBgTertiaryLight = Color(0xFFE8E8ED)
val LiasTextPrimaryLight = Color(0xFF1D1D1F)
val LiasTextSecondaryLight = Color(0xFF86868B)
val LiasAccentLight = Color(0xFF0071E3)
val LiasSuccessLight = Color(0xFF34C759)
val LiasWarningLight = Color(0xFFFF9500)
val LiasDangerLight = Color(0xFFFF3B30)

// Dark Theme Colors (Extracted from web @media (prefers-color-scheme: dark))
val LiasBgPrimaryDark = Color(0xFF000000)
val LiasBgSecondaryDark = Color(0xFF1C1C1E)
val LiasBgTertiaryDark = Color(0xFF2C2C2E)
val LiasTextPrimaryDark = Color(0xFFFFFFFF)
val LiasTextSecondaryDark = Color(0xFF8E8E93)
val LiasAccentDark = Color(0xFF0A84FF)
val LiasSuccessDark = Color(0xFF32D74B)
val LiasWarningDark = Color(0xFFFF9F0A)
val LiasDangerDark = Color(0xFFFF453A)

// Common
val LiasSeparator = Color(0x1A000000) // rgba(0,0,0,0.1)
val LiasSeparatorDark = Color(0x1AFFFFFF) // rgba(255,255,255,0.15)
