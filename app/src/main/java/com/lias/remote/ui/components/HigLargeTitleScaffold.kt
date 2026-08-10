// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigLargeTitleScaffold.kt
// Version: 29.0.0
//
// Purpose:
//   Apple-inspired collapsible large-title application scaffold.
//
// Batch 21:
//   - Large titles may wrap under large font scale.
//   - Landscape layout remains compact.
//   - Search controls use >=48dp interaction height.
//   - Title is exposed as a semantic heading.
//   - Collapsed navbar stays compact and single-line.
//   - Removes assumptions that large text always fits one line.
//   - Search preserves cursor selection and includes a clear affordance.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun HigLargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    scrollState: LazyListState? = null,
    navLeading:
        (@Composable () -> Unit)? =
        null,
    navTrailing:
        (@Composable () -> Unit)? =
        null,
    searchPlaceholder: String = "",
    searchQuery: String = "",
    onSearchQueryChanged:
        (String) -> Unit = {},
    bottomBar:
        (@Composable () -> Unit)? =
        null,
    content:
        @Composable (PaddingValues) ->
            Unit
) {

    val configuration =
        LocalConfiguration.current

    val isLandscape =
        configuration.screenWidthDp >
            configuration.screenHeightDp

    val isCollapsed by
        remember(
            scrollState
        ) {

            derivedStateOf {

                scrollState
                    ?.firstVisibleItemIndex !=
                    0 ||
                    (
                        scrollState
                            ?.firstVisibleItemScrollOffset
                            ?: 0
                        ) >
                    50
            }
        }

    CupertinoScaffold(
        modifier =
            modifier.fillMaxSize(),
        bottomBar = {
            bottomBar
                ?.invoke()
        }
    ) {
        innerPadding ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        LiasThemeColors.background
                    )
                    .padding(
                        innerPadding
                    )
        ) {

            if (
                isCollapsed
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min =
                                    44.dp
                            )
                            .background(
                                LiasThemeColors.background
                            )
                            .padding(
                                horizontal =
                                    HigSpec.SpacingM
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        contentAlignment =
                            Alignment.CenterStart
                    ) {
                        navLeading
                            ?.invoke()
                    }

                    CupertinoText(
                        text =
                            title,
                        style =
                            HigTypography.headline,
                        color =
                            LiasThemeColors.label,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.weight(
                                if (
                                    isLandscape
                                ) {
                                    1f
                                } else {
                                    2f
                                }
                            )
                    )

                    Box(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        contentAlignment =
                            Alignment.CenterEnd
                    ) {
                        navTrailing
                            ?.invoke()
                    }
                }

            } else {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start =
                                    HigSpec.SpacingM,
                                end =
                                    HigSpec.SpacingM,
                                top =
                                    HigSpec.SpacingS,
                                bottom =
                                    HigSpec.SpacingXS
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            HigSpec.SpacingS
                        )
                ) {

                    navLeading
                        ?.invoke()

                    CupertinoText(
                        text =
                            title,
                        style =
                            HigTypography.largeTitle,
                        color =
                            LiasThemeColors.label,
                        maxLines =
                            if (
                                isLandscape
                            ) {
                                2
                            } else {
                                3
                            },
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .semantics {
                                    heading()
                                }
                    )

                    if (
                        isLandscape &&
                        searchPlaceholder
                            .isNotEmpty()
                    ) {

                        HigSearchField(
                            query =
                                searchQuery,
                            onQueryChanged =
                                onSearchQueryChanged,
                            placeholder =
                                searchPlaceholder,
                            modifier =
                                Modifier.weight(
                                    1.35f
                                )
                        )
                    }

                    navTrailing
                        ?.invoke()
                }

                if (
                    !isLandscape &&
                    searchPlaceholder
                        .isNotEmpty()
                ) {

                    HigSearchField(
                        query =
                            searchQuery,
                        onQueryChanged =
                            onSearchQueryChanged,
                        placeholder =
                            searchPlaceholder,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        HigSpec.SpacingM,
                                    vertical =
                                        HigSpec.SpacingXS
                                )
                    )
                }
            }

            Box(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                content(
                    PaddingValues(
                        bottom =
                            HigSpec.SpacingL
                    )
                )
            }
        }
    }
}

@Composable
fun HigSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val clearInteractionSource =
        remember {
            MutableInteractionSource()
        }
    val searchTint = LiasThemeColors.secondaryLabel
    val clearMark = LiasThemeColors.secondaryBackground

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        48.dp
                )
                .clip(RoundedCornerShape(10.dp))
                .background(LiasThemeColors.fill2)
                .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(17.dp)) {
            val strokeWidth = 1.7.dp.toPx()
            val radius = size.minDimension * 0.32f
            val center = Offset(size.width * 0.42f, size.height * 0.42f)

            drawCircle(
                color = searchTint,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            drawLine(
                color = searchTint,
                start =
                    Offset(
                        center.x + radius * 0.72f,
                        center.y + radius * 0.72f
                    ),
                end = Offset(size.width * 0.90f, size.height * 0.90f),
                strokeWidth = strokeWidth
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        CursorSafeTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = {
                CupertinoText(
                    text = placeholder,
                    style = HigTypography.body,
                    color = LiasThemeColors.tertiaryLabel
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
        )

        if (query.isNotEmpty()) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .semantics {
                            contentDescription = "Clear search"
                        }
                        .clickable(
                            interactionSource = clearInteractionSource,
                            indication = null,
                            onClick = { onQueryChanged("") }
                        ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    val markInset = size.minDimension * 0.33f
                    val markWidth = 1.5.dp.toPx()

                    drawCircle(color = searchTint)
                    drawLine(
                        color = clearMark,
                        start = Offset(markInset, markInset),
                        end =
                            Offset(
                                size.width - markInset,
                                size.height - markInset
                            ),
                        strokeWidth = markWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = clearMark,
                        start = Offset(size.width - markInset, markInset),
                        end = Offset(markInset, size.height - markInset),
                        strokeWidth = markWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}
