package com.lias.remote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.section.CupertinoSection

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
            .padding(HigSpec.SectionHeaderPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CupertinoText(
            text = text.uppercase(),
            style = HigTypography.subheadline,
            color = LiasThemeColors.secondaryLabel,
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
    CupertinoSection(modifier = modifier) {
        content()
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
    val headlineColor = if (isDestructive) LiasThemeColors.red else LiasThemeColors.label
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
                .padding(horizontal = HigSpec.RowHorizontalPadding, vertical = HigSpec.RowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.let { leading ->
                leading()
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                CupertinoText(
                    text = primaryText,
                    style = HigTypography.body,
                    color = headlineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                secondaryText?.let { secondary ->
                    CupertinoText(
                        text = secondary,
                        style = HigTypography.subheadline,
                        color = LiasThemeColors.tertiaryLabel,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .padding(start = 16.dp)
                    .background(LiasThemeColors.separator)
            )
        }
    }
}
