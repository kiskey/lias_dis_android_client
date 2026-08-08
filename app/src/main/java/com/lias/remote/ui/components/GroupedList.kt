// ====================================================================
// File: GroupedList.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Native iOS Inset Grouped List card and row items.
//          10dp corners, 0.5px inset dividers, strict 44dp min height.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec

@Composable
fun GroupedList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding
    ) {
        content()
    }
}

@Composable
fun ListSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        trailingAction?.invoke()
    }
}

@Composable
fun GroupedListCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(HigSpec.GroupedCardCorner)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun GroupedListRow(
    primaryText: String,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val headlineColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.55f else 1.0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
        label = "iosRowPressAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = pressAlpha }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HigSpec.RowMinHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = onClick != null,
                    onClick = { onClick?.invoke() }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.let { leading ->
                leading()
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = headlineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                secondaryText?.let { secondary ->
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            trailingContent?.let { trailing ->
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
