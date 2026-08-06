// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/GroupedList.kt
// Version: 2.1.0
// Audit Fixes:
//   1. Enforced 14dp grouped container border-radius (`HigSpec.GroupedListCorner`).
//   2. Replaced individual row clipping with continuous inset group section surfaces.
//   3. Inserted 0.5dp separators between section items with flush outer corners.
//   4. Guaranteed minimum row height of 44dp (`HigSpec.RowMinHeight`).
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec

@Composable
fun GroupedList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding
    ) {
        content()
    }
}

@Composable
fun ListSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(HigSpec.SectionLabelPadding)
    )
}

@Composable
fun GroupedListCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(HigSpec.GroupedListCorner),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        color = MaterialTheme.colorScheme.surface,
        shape = shape
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
    onClick: () -> Unit = {},
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        headlineColor = MaterialTheme.colorScheme.onSurface,
        supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            supportingContent = secondaryText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            colors = colors,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = HigSpec.RowMinHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp)
        )
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
