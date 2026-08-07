// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/Type.kt
// Version: 2.2.0
// Purpose: Material 3 & Cupertino Typography scaled for Pixel 6a 20:9 display.
// Audit Fixes:
//   1. Increased font scale (32sp Large Title, 16sp Body) to fill Pixel 6a tall screen without clipping.
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
    // Large Title / 32 · 800 (letterSpacing -0.02em, lineHeight 38sp) - Optimized for 20:9 display
    headlineLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.02).em
    ),
    // Title 2 / 26 · 800 (letterSpacing -0.01em, lineHeight 30sp)
    headlineSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.01).em
    ),
    // Title 3 / 22 · 700 (lineHeight 27sp)
    headlineMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 27.sp
    ),
    // Subhead Title / 18 · 600 (lineHeight 23sp)
    titleMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 23.sp
    ),
    // Headline / Primary Row Text / 16 · 700 (lineHeight 22sp)
    titleLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Body / 16 · 400 (lineHeight 22sp)
    bodyLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Subbody / Secondary Text / 14.5 · 400 (lineHeight 19sp)
    bodyMedium = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.5.sp,
        lineHeight = 19.sp
    ),
    // Subhead Label / Button / 14 · 600 (lineHeight 19sp)
    labelLarge = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    // Footnote / Section Header / 12.5 · 700 (lineHeight 17sp, letterSpacing 0.06em)
    labelSmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.06.em
    ),
    // Micro Caption / 11.5 · 600
    bodySmall = TextStyle(
        fontFamily = LiasFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.5.sp,
        lineHeight = 15.sp
    )
)
