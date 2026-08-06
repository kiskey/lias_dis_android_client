// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/theme/HigSpec.kt
// Version: 1.0.0
// Purpose: Codifies Apple HIG layout, radius, dimension, and spacing
//          constants to guarantee consistency across all screens.
// ====================================================================

package com.lias.remote.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object HigSpec {
    val ScreenBgCornerNone = 0.dp
    val GroupedListCorner = 14.dp      // Spec .glist border-radius: 14px
    val RowMinHeight = 44.dp           // Spec .grow min-height: 44px
    val RowHorizontalPadding = 14.dp   // Spec .grow padding: 10px 14px
    val RowVerticalPadding = 10.dp
    val SheetCorner = 22.dp            // Spec sheet border-radius: 22px 22px 0 0
    val CardCorner = 16.dp             // Spec .hero-card / .card-lite
    val IconBubbleSize = 26.dp         // Spec .ibubble 26x26, radius 7
    val IconBubbleCorner = 7.dp
    val StatusDotSize = 9.dp           // Spec .dot 9x9
    val SegmentedControlHeight = 38.dp // Spec .segctrl height: 38px
    val FabSize = 52.dp                // Spec .fab width/height: 52px
    val FabOffsetFromTabBar = 16.dp    // Spec bottom: 96px inside 80px bar
    val TabBarHeight = 80.dp           // Spec .tabbar height: 80px
    val SectionLabelPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
}
