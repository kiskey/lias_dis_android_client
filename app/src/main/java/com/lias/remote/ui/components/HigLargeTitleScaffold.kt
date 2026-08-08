// ====================================================================
// File: HigLargeTitleScaffold.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Two-tier large title nav bar. Adaptive landscape layout.
//          Sticky search field with scroll-aware collapse.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.github.robinpcrd.cupertino.CupertinoSearchTextField

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
    floatingActionButton: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Collapse large title on scroll
    val isCollapsed by remember(scrollState) {
        derivedStateOf {
            scrollState?.firstVisibleItemIndex != 0 || (scrollState?.firstVisibleItemScrollOffset ?: 0) > 50
        }
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { floatingActionButton?.invoke() },
        bottomBar = { bottomBar?.invoke() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tier 1: Navigation Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
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
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    navTrailing?.invoke()
                }
            }

            // Tier 2: Large Title & Search
            if (!isCollapsed) {
                if (isLandscape) {
                    // Landscape: Side-by-side Title and Search to save vertical space
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HigSpec.SpacingM, vertical = HigSpec.SpacingS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
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
                    // Portrait: Stacked Title and Search
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HigSpec.SpacingM, vertical = HigSpec.SpacingS)
                    )
                    if (searchPlaceholder.isNotEmpty()) {
                        HigSearchField(
                            query = searchQuery,
                            onQueryChanged = onSearchQueryChanged,
                            placeholder = searchPlaceholder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = HigSpec.SpacingM, vertical = HigSpec.SpacingS)
                        )
                    }
                }
            }

            // Main Content Area
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
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}
