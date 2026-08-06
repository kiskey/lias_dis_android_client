// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Type.kt
// Version: 2.0.0
// Purpose: Material 3 Typography aligned with Apple HIG font scale.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val LiasFontFamily = FontFamily.Default

val LiasTypography = Typography(
    // Large Title (29sp, w800, letterSpacing -0.02em)
    headlineLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 29.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).em
    ),
    // Title 3 (20sp, w700)
    headlineMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    // Headline / Primary Row Text (15sp, w700)
    titleLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // Secondary Title (17sp, w600)
    titleMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    // Body (15sp, w400)
    bodyLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // Subbody / Secondary Text (14sp, w400)
    bodyMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    // Subhead Label (13sp, w600)
    labelLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Footnote / Section Header (12sp, w600)
    labelSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
