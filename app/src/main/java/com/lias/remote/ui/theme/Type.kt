// ====================================================================
// File: Type.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Strict 6-level HIG typography scale. Eliminates 10-level
//          fragmentation. Optimized for 20:9 displays without clipping.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val LiasFontFamily = FontFamily.SansSerif

val LiasTypography = Typography(
    // Large Title (34pt) - Used for Screen Headers
    headlineLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp
    ),
    // Title 1 (28pt) - Used for Modal Sheet Headers & Hero Text
    headlineMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp
    ),
    // Title 2 (22pt) - Used for Primary Card Headers
    headlineSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    ),
    // Title 3 (20pt) - Used for Row Headers
    titleLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp
    ),
    // Headline (17pt) - Used for Row Primary Text & Buttons
    titleMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.43).sp
    ),
    // Body (17pt) - Used for Row Primary Text (Regular weight)
    bodyLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.43).sp
    ),
    // Subheadline (15pt) - Used for Row Secondary Text
    bodyMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    // Footnote (13pt) - Used for Badges, Pills, Metadata
    labelMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    // Caption (12pt) - Used for Micro UI Elements
    labelSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
