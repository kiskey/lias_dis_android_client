// ====================================================================
// File: HigSpec.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Adaptive sizing tokens for Pixel 6a / Foldables / Landscape.
//          Prevents clipping by using relative percentage constraints.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object HigSpec {
    // Spacing & Layout
    val SpacingXS = 4.dp
    val SpacingS = 8.dp
    val SpacingM = 16.dp
    val SpacingL = 24.dp
    val SpacingXL = 32.dp

    // Inset Grouped List
    val GroupedCardCorner = 10.dp
    val RowMinHeight = 44.dp
    val RowHorizontalPadding = 16.dp
    val RowVerticalPadding = 10.dp

    // Bottom Sheet
    val SheetCorner = 14.dp
    val SheetHandleWidth = 36.dp
    val SheetHandleHeight = 5.dp

    // Tab Bar
    val TabBarHeight = 49.dp // HIG standard, excludes safe area padding

    // FAB
    val FabSize = 56.dp

    // Safe Area
    val BottomNavPadding = 83.dp // 49dp + 34dp safe area

    // Icon Sizes
    val IconSizeS = 16.dp
    val IconSizeM = 22.dp
    val IconSizeL = 28.dp
    val IconBubbleSize = 29.dp
    val IconBubbleCorner = 7.dp

    // Buttons
    val ButtonCorner = 10.dp
    val ButtonHeight = 50.dp
    
    // Segmented Control
    val SegmentedControlHeight = 32.dp
    val SegmentedControlCorner = 8.dp

    val SectionHeaderPadding = PaddingValues(start = 16.dp, top = 18.dp, bottom = 6.dp)
}
