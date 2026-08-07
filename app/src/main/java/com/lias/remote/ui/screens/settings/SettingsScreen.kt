// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt
// Version: 3.0.0
// Purpose: Native iOS Settings Screen with CupertinoSwitch controls.
// Audit Fixes:
//   1. Migrated toggle switch to CupertinoSwitch.
//   2. Preserved HigLargeTitleScaffold and Flush Nftables confirmation dialog.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.SystemOrangeDark
import io.github.robinpcrd.cupertino.CupertinoSwitch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToConnection: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFlushDialog by remember { mutableStateOf(false) }

    HigLargeTitleScaffold(
        title = "Settings"
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GroupedList {
                // Section 1 - Server
                item { ListSectionHeader("Server") }
                item {
                    GroupedListCard {
                        GroupedListRow(
                            primaryText = "Connection",
                            secondaryText = if (uiState.serverUrl.isNotBlank()) "${uiState.serverUrl} · Configured" else "Not configured",
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(HigSpec.IconBubbleSize)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(HigSpec.IconBubbleCorner)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            },
                            trailingContent = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = onNavigateToConnection
                        )
                    }
                }

                // Section 2 - Controls
                item { ListSectionHeader("Controls") }
                item {
                    GroupedListCard {
                        GroupedListRow(
                            primaryText = "Vacation Mode",
                            secondaryText = "Block all non-infrastructure devices",
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(HigSpec.IconBubbleSize)
                                        .background(SystemOrangeDark, RoundedCornerShape(HigSpec.IconBubbleCorner)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            },
                            trailingContent = {
                                CupertinoSwitch(
                                    checked = uiState.vacationMode,
                                    onCheckedChange = { viewModel.toggleVacationMode(it) }
                                )
                            }
                        )
                    }
                }

                // Section 3 - Danger Zone
                item { ListSectionHeader("Danger Zone") }
                item {
                    GroupedListCard {
                        GroupedListRow(
                            primaryText = "Flush Nftables Table",
                            secondaryText = "Rebuilds automatically on next sync",
                            trailingContent = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = { showFlushDialog = true },
                            colors = androidx.compose.material3.ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                headlineColor = MaterialTheme.colorScheme.error,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }

    if (showFlushDialog) {
        HigAlertDialog(
            onDismissRequest = { showFlushDialog = false },
            title = { Text("Confirm Flush") },
            text = { Text("Are you sure you want to flush all nftables rules? Internet access will be temporarily unrestricted until LIAS rebuilds the table.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.flushNftables()
                        showFlushDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Flush") }
            },
            dismissButton = {
                TextButton(onClick = { showFlushDialog = false }) { Text("Cancel") }
            }
        )
    }
}
