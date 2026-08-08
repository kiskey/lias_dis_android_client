// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigSwipeRow.kt
// Version: 1.3.0
// Audit Fixes:
//   1. Removed side-effects from confirmValueChange to prevent drag-calculation toast spam.
//   2. Held revealed state open with opposite-end Cupertino 'X' cancel button and auto-reset on tap.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import kotlinx.coroutines.launch

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
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> leadingAction != null
                SwipeToDismissBoxValue.EndToStart -> trailingAction != null
                SwipeToDismissBoxValue.Settled -> true
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
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leading Action Button (Left side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    leadingAction.onTrigger()
                                    coroutineScope.launch { state.reset() }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = leadingAction.icon,
                                contentDescription = leadingAction.label,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = leadingAction.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Cupertino Cancel 'X' Button (Opposite End / Right side)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    coroutineScope.launch { state.reset() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else if (direction == SwipeToDismissBoxValue.EndToStart && trailingAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(trailingAction.color)
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cupertino Cancel 'X' Button (Opposite End / Left side)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    coroutineScope.launch { state.reset() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Trailing Action Button (Right side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    trailingAction.onTrigger()
                                    coroutineScope.launch { state.reset() }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = trailingAction.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
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
        }
    ) {
        content()
    }
}
