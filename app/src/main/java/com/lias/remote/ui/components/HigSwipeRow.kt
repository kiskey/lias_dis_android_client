// ====================================================================
// File: HigSwipeRow.kt
// Version: 3.0.1 (HIG Redesign Fix)
// Purpose: Fixed GroupedListCorner reference.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec

data class SwipeAction(
    val icon: ImageVector,
    val color: Color,
    val onTrigger: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HigSwipeRow(
    modifier: Modifier = Modifier,
    leadingAction: SwipeAction? = null,
    trailingAction: SwipeAction? = null,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    leadingAction?.onTrigger?.invoke()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    trailingAction?.onTrigger?.invoke()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(HigSpec.GroupedCardCorner)),
        backgroundContent = {
            val direction = state.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            val action = if (direction == SwipeToDismissBoxValue.StartToEnd) leadingAction else trailingAction
            
            if (action != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(action.color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Icon(imageVector = action.icon, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) {
        content()
    }
}
