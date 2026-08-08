package com.lias.remote.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Native Apple HIG Layout & Geometry Token Specifications.
 * Optimized for Pixel 6a (429 dpi) & Modern Devices.
 */
object HigSpec {
    val SpacingXS = 4.dp
    val SpacingS = 8.dp
    val SpacingM = 16.dp
    val SpacingL = 24.dp
    val SpacingXL = 32.dp

    // Inset Grouped Section
    val GroupedCardCorner = 12.dp
    val RowMinHeight = 48.dp
    val RowHorizontalPadding = 16.dp
    val RowVerticalPadding = 12.dp

    // Modal Sheet
    val SheetCorner = 14.dp
    val SheetHandleWidth = 36.dp
    val SheetHandleHeight = 5.dp

    // Tab Bar (Optimized for 6.1" Pixel 6a)
    val TabBarHeight = 60.dp
    val BottomNavPadding = 94.dp

    // Control Elements
    val IconSizeS = 16.dp
    val IconSizeM = 26.dp
    val IconSizeL = 30.dp
    val IconBubbleSize = 32.dp
    val IconBubbleCorner = 8.dp

    val ButtonCorner = 10.dp
    val ButtonHeight = 46.dp
    val ButtonHeightLarge = 52.dp

    val SegmentedControlHeight = 34.dp
    val SegmentedControlCorner = 9.dp

    val SectionHeaderPadding = PaddingValues(start = 32.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
}
