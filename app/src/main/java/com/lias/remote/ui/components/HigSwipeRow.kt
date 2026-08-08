package com.lias.remote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import kotlin.math.roundToInt

data class SwipeAction(
    val icon: ImageVector,
    val color: Color,
    val onTrigger: () -> Unit
)

@Composable
fun HigSwipeRow(
    modifier: Modifier = Modifier,
    leadingAction: SwipeAction? = null,
    trailingAction: SwipeAction? = null,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "swipeRowOffset"
    )

    val draggableState = rememberDraggableState { delta ->
        val newOffset = offsetX + delta
        if ((delta > 0 && leadingAction != null) || (delta < 0 && trailingAction != null)) {
            offsetX = newOffset.coerceIn(-180f, 180f)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(HigSpec.GroupedCardCorner))
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (offsetX > 100f && leadingAction != null) {
                        leadingAction.onTrigger()
                    } else if (offsetX < -100f && trailingAction != null) {
                        trailingAction.onTrigger()
                    }
                    offsetX = 0f
                }
            )
    ) {
        // Swipe Background Actions
        if (animatedOffset > 0 && leadingAction != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(leadingAction.color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                CupertinoIcon(imageVector = leadingAction.icon, contentDescription = null, tint = Color.White)
            }
        } else if (animatedOffset < 0 && trailingAction != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(trailingAction.color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                CupertinoIcon(imageVector = trailingAction.icon, contentDescription = null, tint = Color.White)
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier.offset { IntOffset(animatedOffset.roundToInt(), 0) }
        ) {
            content()
        }
    }
}
