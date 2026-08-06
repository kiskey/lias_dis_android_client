// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SwipeRow.kt
// Version: 2.0.0
// Purpose: Apple HIG SwipeToDismiss row component with full-bleed action backgrounds.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionRow(
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeRight?.invoke()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeLeft?.invoke()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        backgroundContent = {
            val direction = state.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd && onSwipeRight != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp)
                        .wrapContentSize(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White
                    )
                }
            } else if (direction == SwipeToDismissBoxValue.EndToStart && onSwipeLeft != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 20.dp)
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        content()
    }
}
