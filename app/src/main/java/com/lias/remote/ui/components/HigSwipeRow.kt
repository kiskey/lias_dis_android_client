// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigSwipeRow.kt
// Version: 1.0.0
// Purpose: Full-bleed swipe action row parameterized with label, icon,
//          and custom action color; auto-commits on full-swipe threshold.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
    val label: String,
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
    fullSwipeCommits: Boolean = true,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    leadingAction?.onTrigger?.invoke()
                    fullSwipeCommits
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    trailingAction?.onTrigger?.invoke()
                    fullSwipeCommits
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(RoundedCornerShape(HigSpec.GroupedListCorner)),
        backgroundContent = {
            val direction = state.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd && leadingAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(leadingAction.color)
                        .padding(horizontal = 20.dp)
                        .wrapContentSize(Alignment.CenterStart)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = leadingAction.icon,
                            contentDescription = leadingAction.label,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = leadingAction.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else if (direction == SwipeToDismissBoxValue.EndToStart && trailingAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(trailingAction.color)
                        .padding(horizontal = 20.dp)
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trailingAction.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = trailingAction.icon,
                            contentDescription = trailingAction.label,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) {
        content()
    }
}
