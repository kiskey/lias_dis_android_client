// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/HigSpec.kt
// Version: 1.1.0
// Purpose: Codifies Apple HIG layout, radius, dimension, and spacing
//          constants optimized for tall 20:9 displays (Pixel 6a).
// Audit Fixes:
//   1. Increased min row height to 48dp and tab bar height to 84dp for tall screen density.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object HigSpec {
    val ScreenBgCornerNone = 0.dp
    val GroupedListCorner = 16.dp      // Spec .glist border-radius: 16px
    val RowMinHeight = 48.dp           // Spec .grow min-height: 48px (generous touch targets)
    val RowHorizontalPadding = 16.dp   // Spec .grow padding: 12px 16px
    val RowVerticalPadding = 12.dp
    val SheetCorner = 22.dp            // Spec sheet border-radius: 22px 22px 0 0
    val CardCorner = 18.dp             // Spec .hero-card / .card-lite
    val IconBubbleSize = 28.dp         // Spec .ibubble 28x28, radius 8
    val IconBubbleCorner = 8.dp
    val StatusDotSize = 10.dp          // Spec .dot 10x10
    val SegmentedControlHeight = 40.dp // Spec .segctrl height: 40px
    val FabSize = 56.dp                // Spec .fab width/height: 56px
    val FabOffsetFromTabBar = 16.dp    // Spec bottom offset
    val TabBarHeight = 84.dp           // Spec .tabbar height: 84px
    val SectionLabelPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
}
