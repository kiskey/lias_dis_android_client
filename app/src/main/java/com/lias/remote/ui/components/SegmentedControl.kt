// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 3.5.0
// Purpose: iOS segmented control with native Apple spring-sliding thumb physics.
// Audit Fixes:
//   1. Built self-contained iOS segmented control with spring physics (dampingRatio 0.82f)
//      to eliminate unresolved CupertinoSegmentedControl references.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.LiasThemeColors

@Composable
fun SegmentedControl(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("Allow", "Schedule", "Block")
) {
    val selectedIndex = options.indexOfFirst { it.equals(selected, ignoreCase = true) }.coerceAtLeast(0)

    val maxOptionLength = options.maxOfOrNull { it.length } ?: 0
    val textStyle = when {
        options.size >= 3 && maxOptionLength > 10 -> MaterialTheme.typography.labelLarge // 14sp w600
        options.size >= 3 -> MaterialTheme.typography.bodyLarge // 15sp w600
        else -> MaterialTheme.typography.titleLarge // 16sp w700
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(HigSpec.SegmentedControlHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(LiasThemeColors.fill)
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val animatedOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
            label = "iosSegmentThumbOffset"
        )

        // Animated iOS White Thumb
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
        )

        // Option Labels Row
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelected(label.lowercase()) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = when {
                            isSelected && label.equals("Block", ignoreCase = true) -> MaterialTheme.colorScheme.error
                            isSelected && label.equals("Allow", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                            isSelected -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = textStyle,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
