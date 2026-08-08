// ====================================================================
// File: SettingsScreen.kt
// Version: 3.2.0 (Cupertino Refactor)
// Purpose: Removed duplicate ConnectionSettingsScreen function to
//          resolve overload ambiguity.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigSpec
import io.github.alexzhirkevich.cupertino.CupertinoSwitch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateToConnection: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    var showFlushDialog by remember { mutableStateOf(false) }

    HigLargeTitleScaffold(title = "Settings", scrollState = scrollState) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { ListSectionHeader("Server") }
            item {
                GroupedListCard {
                    GroupedListRow(
                        primaryText = "Connection",
                        secondaryText = if (uiState.serverUrl.isNotBlank()) "${uiState.serverUrl} · Connected" else "Not configured",
                        leadingContent = { Box(modifier = Modifier.size(HigSpec.IconBubbleSize), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary) } },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onNavigateToConnection
                    )
                }
            }

            item { ListSectionHeader("Appearance") }
            item {
                GroupedListCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Auto switches between Light and Dark based on system setting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SegmentedControl(
                            options = listOf("System", "Light", "Dark"),
                            selectedOption = uiState.themeMode.replaceFirstChar { it.uppercase() },
                            onOptionSelected = { mode -> viewModel.updateThemeMode(mode.lowercase()) },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            item { ListSectionHeader("Controls") }
            item {
                GroupedListCard {
                    GroupedListRow(
                        primaryText = "Vacation Mode",
                        secondaryText = "Block all non-infrastructure devices",
                        leadingContent = { Box(modifier = Modifier.size(HigSpec.IconBubbleSize), contentAlignment = Alignment.Center) { Icon(Icons.Filled.FlightTakeoff, null, tint = MaterialTheme.colorScheme.error) } },
                        trailingContent = { CupertinoSwitch(checked = uiState.vacationMode, onCheckedChange = { viewModel.toggleVacationMode(it) }) }
                    )
                }
            }

            item { ListSectionHeader("Danger Zone") }
            item {
                GroupedListCard {
                    GroupedListRow(
                        primaryText = "Flush Nftables Table",
                        secondaryText = "Rebuilds automatically on next sync",
                        isDestructive = true,
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { showFlushDialog = true }
                    )
                }
            }
        }
    }

    if (showFlushDialog) {
        HigAlertDialog(
            onDismissRequest = { showFlushDialog = false },
            title = "Flush nftables?",
            message = "This will temporarily disable all LIAS enforcement. Internet access will be unrestricted until LIAS rebuilds the table.",
            confirmText = "Flush",
            onConfirm = { viewModel.flushNftables() },
            isDestructive = true
        )
    }
}
