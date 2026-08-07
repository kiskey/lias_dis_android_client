// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulesScreen.kt
// Version: 3.0.0
// Purpose: Native iOS Schedules Screen with inset card items and MiniWeekStrips.
// Audit Fixes:
//   1. Replaced Material 3 AlertDialog with Cupertino-styled HigAlertDialog.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ContextMenuItem
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigContextMenu
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MiniWeekStrip
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.theme.HigSpec

@Composable
fun SchedulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    HigLargeTitleScaffold(
        title = "Schedules",
        navTrailing = {
            IconButton(
                onClick = {
                    editingSchedule = null
                    showEditor = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Schedule", tint = MaterialTheme.colorScheme.primary)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Schedule", tint = Color.White)
            }
        }
    ) {
        if (state.schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Create your first schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Automate internet downtime or allowed access windows across devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HigButton(
                        text = "Create Schedule",
                        onClick = {
                            editingSchedule = null
                            showEditor = true
                        },
                        style = HigButtonStyle.Primary
                    )
                }
            }
        } else {
            GroupedList {
                item { ListSectionHeader("Configured Schedules (${state.schedules.size})") }

                items(state.schedules, key = { it.id }) { schedule ->
                    GroupedListCard {
                        val contextMenuItems = listOf(
                            ContextMenuItem(
                                label = "Edit",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    editingSchedule = schedule
                                    showEditor = true
                                }
                            ),
                            ContextMenuItem(
                                label = "Duplicate",
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    val cloned = schedule.copy(
                                        id = "sched_${System.currentTimeMillis()}",
                                        name = "${schedule.name} Copy"
                                    )
                                    viewModel.saveSchedule(cloned)
                                }
                            ),
                            ContextMenuItem(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                isDestructive = true,
                                onClick = { scheduleToDelete = schedule }
                            )
                        )

                        HigContextMenu(
                            items = contextMenuItems,
                            onClick = {
                                editingSchedule = schedule
                                showEditor = true
                            }
                        ) {
                            HigSwipeRow(
                                leadingAction = SwipeAction(
                                    label = "Edit",
                                    icon = Icons.Default.Edit,
                                    color = MaterialTheme.colorScheme.primary,
                                    onTrigger = {
                                        editingSchedule = schedule
                                        showEditor = true
                                    }
                                ),
                                trailingAction = SwipeAction(
                                    label = "Delete",
                                    icon = Icons.Default.Delete,
                                    color = MaterialTheme.colorScheme.error,
                                    onTrigger = { scheduleToDelete = schedule }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    GroupedListRow(
                                        primaryText = schedule.name,
                                        secondaryText = "${schedule.mode.uppercase()} · ${schedule.timezone}",
                                        trailingContent = {
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = {
                                            editingSchedule = schedule
                                            showEditor = true
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MiniWeekStrip(schedules = listOf(schedule))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ScheduleEditorSheet(
            initialSchedule = editingSchedule,
            onDismiss = { showEditor = false },
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                showEditor = false
            }
        )
    }

    scheduleToDelete?.let { schedule ->
        val impactedPolicies = state.policies.filter { p ->
            p.resolveScheduleIDs().contains(schedule.id)
        }

        HigAlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Confirm Delete") },
            text = {
                if (impactedPolicies.isNotEmpty()) {
                    Column {
                        Text("⚠️ Warning: This schedule is attached to the following policies:")
                        impactedPolicies.forEach { p ->
                            Text("• ${p.name}", fontWeight = FontWeight.Bold)
                        }
                        Text("These policies will default to ALLOW ALL.")
                    }
                } else {
                    Text("Are you sure you want to delete this schedule?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(schedule.id)
                        scheduleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
