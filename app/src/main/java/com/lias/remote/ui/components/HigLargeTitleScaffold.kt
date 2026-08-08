package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoSearchTextField
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun HigLargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    scrollState: LazyListState? = null,
    navLeading: (@Composable () -> Unit)? = null,
    navTrailing: (@Composable () -> Unit)? = null,
    searchPlaceholder: String = "",
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val isCollapsed by remember(scrollState) {
        derivedStateOf {
            scrollState?.firstVisibleItemIndex != 0 || (scrollState?.firstVisibleItemScrollOffset ?: 0) > 50
        }
    }

    CupertinoScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { bottomBar?.invoke() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LiasThemeColors.background)
                .padding(innerPadding)
        ) {
            // Tier 1: Sticky Navigation Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(LiasThemeColors.background)
                    .padding(horizontal = HigSpec.SpacingS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    navLeading?.invoke()
                }
                Box(
                    modifier = Modifier
                        .weight(if (isLandscape) 1f else 2f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCollapsed) {
                        CupertinoText(
                            text = title,
                            style = HigTypography.headline,
                            color = LiasThemeColors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    navTrailing?.invoke()
                }
            }

            // Tier 2: Collapsible Large Title & Integrated Search Bar
            if (!isCollapsed) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HigSpec.SpacingM, vertical = HigSpec.SpacingXS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CupertinoText(
                            text = title,
                            style = HigTypography.largeTitle,
                            color = LiasThemeColors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchPlaceholder.isNotEmpty()) {
                            HigSearchField(
                                query = searchQuery,
                                onQueryChanged = onSearchQueryChanged,
                                placeholder = searchPlaceholder,
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                } else {
                    CupertinoText(
                        text = title,
                        style = HigTypography.largeTitle,
                        color = LiasThemeColors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HigSpec.SpacingM, vertical = HigSpec.SpacingXS)
                    )
                    if (searchPlaceholder.isNotEmpty()) {
                        HigSearchField(
                            query = searchQuery,
                            onQueryChanged = onSearchQueryChanged,
                            placeholder = searchPlaceholder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = HigSpec.SpacingM, vertical = HigSpec.SpacingXS)
                        )
                    }
                }
            }

            // Main Content Stream Container
            Box(modifier = Modifier.fillMaxSize()) {
                content(PaddingValues(bottom = HigSpec.BottomNavPadding))
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
    CupertinoSearchTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            CupertinoText(
                text = placeholder,
                style = HigTypography.body,
                color = LiasThemeColors.tertiaryLabel
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}
