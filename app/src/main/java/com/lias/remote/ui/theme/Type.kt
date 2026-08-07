// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Type.kt
// Version: 2.1.0
// Purpose: Material 3 & Cupertino Typography aligned with Apple HIG & HTML Spec.
// Audit Fixes:
//   1. Explicitly mapped 1:1 HIG scale for Pixel 6a adaptive density (29sp, 20sp, 17sp, 15sp, 14sp, 13sp, 12sp).
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
    // Large Title / 29 · 800 (letterSpacing -0.02em, lineHeight 34sp)
    headlineLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 29.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).em
    ),
    // Title 2 / 24 · 800 (letterSpacing -0.01em)
    headlineSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em
    ),
    // Title 3 / 20 · 700 (lineHeight 25sp)
    headlineMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    // Title 2 Regular / Subhead Title / 17 · 600 (lineHeight 22sp)
    titleMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    // Headline / Primary Row Text / 15 · 700 (lineHeight 20sp)
    titleLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // Body / 15 · 400 (lineHeight 20sp)
    bodyLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // Subbody / Secondary Text / 14 · 400 (lineHeight 18sp)
    bodyMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    // Subhead Label / Button / 13 · 600 (lineHeight 18sp)
    labelLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Footnote / Section Header / 12 · 700 (lineHeight 16sp, letterSpacing 0.06em)
    labelSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.em
    ),
    // Micro Caption / 11 · 600
    bodySmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
