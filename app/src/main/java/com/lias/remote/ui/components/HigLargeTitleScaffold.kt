// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigLargeTitleScaffold.kt
// Version: 2.0.0
// Purpose: Two-tier iOS large title navigation bar scaffold and CupertinoSearchTextField.
// Audit Fixes:
//   1. Replaced search pill with native CupertinoSearchTextField.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import io.github.robinpcrd.cupertino.CupertinoSearchTextField

@Composable
fun HigLargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navLeading: (@Composable () -> Unit)? = null,
    navTrailing: (@Composable () -> Unit)? = null,
    searchField: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
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
            // Tier 1: Secondary Navigation Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HigSpec.RowMinHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    navLeading?.invoke()
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    navTrailing?.invoke()
                }
            }

            // Tier 2: Large Title
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HigSpec.RowHorizontalPadding, vertical = 2.dp)
                )
            }

            // Optional Search Field Slot
            searchField?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HigSpec.RowHorizontalPadding, vertical = 4.dp)
                ) {
                    it()
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                content(PaddingValues(0.dp))
            }
        }
    }
}

@Composable
fun HigSearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search devices"
) {
    CupertinoSearchTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
    )
}
