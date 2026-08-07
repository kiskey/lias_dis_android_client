// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SkeletonRow.kt
// Version: 1.0.0
// Purpose: Animated linear gradient shimmer skeleton loader for grouped list
//          placeholders during asynchronous state hydration.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec

@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HigSpec.RowMinHeight)
            .padding(horizontal = HigSpec.RowHorizontalPadding, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}

@Composable
fun SkeletonGroupedList(
    count: Int = 4,
    modifier: Modifier = Modifier
) {
    GroupedListCard(modifier = modifier) {
        repeat(count) { index ->
            SkeletonRow()
            if (index < count - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(start = 16.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
        }
    }
}
