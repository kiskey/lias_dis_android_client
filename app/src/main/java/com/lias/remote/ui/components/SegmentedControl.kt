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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val selectedIndex = options.indexOfFirst { it.equals(selectedOption, ignoreCase = true) }.coerceAtLeast(0)
    val interactionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(HigSpec.SegmentedControlHeight)
            .clip(RoundedCornerShape(HigSpec.SegmentedControlCorner))
            .background(LiasThemeColors.fill2)
            .padding(2.dp)
    ) {
        val segmentWidth = maxWidth / options.size

        val animatedOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
            label = "segmentThumb"
        )

        // Sliding Spring Thumb
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDestructive && selectedIndex == options.lastIndex) 2.dp else 1.dp,
                    shape = RoundedCornerShape(HigSpec.SegmentedControlCorner - 2.dp)
                )
                .background(
                    color = if (isDestructive && selectedIndex == options.lastIndex) LiasThemeColors.red else LiasThemeColors.secondaryBackground,
                    shape = RoundedCornerShape(HigSpec.SegmentedControlCorner - 2.dp)
                )
        )

        // Segment Options Text Row
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val textColor = when {
                    isDestructive && isSelected && index == options.lastIndex -> Color.White
                    isSelected -> LiasThemeColors.label
                    else -> LiasThemeColors.secondaryLabel
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    CupertinoText(
                        text = option,
                        color = textColor,
                        style = HigTypography.subheadline,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
