// ====================================================================
// File: SchedulesScreen.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Schedule list with FAB. Strict HIG layout. Preserves
//          Schedule API and conflict validation logic.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.lias.remote.core.models.Schedule
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.theme.HigSpec

@Composable
fun SchedulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    
    var showEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    HigLargeTitleScaffold(
        title = "Schedules",
        scrollState = scrollState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Schedule", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item { ListSectionHeader("Configured Schedules (${state.schedules.size})") }
            
            if (state.schedules.isEmpty()) {
                item {
                    GroupedListCard {
                        GroupedListRow(
                            primaryText = "No schedules configured",
                            secondaryText = "Tap + to create a time window."
                        )
                    }
                }
            } else {
                items(state.schedules, key = { it.id }) { schedule ->
                    GroupedListCard {
                        HigSwipeRow(
                            leadingAction = SwipeAction(
                                icon = Icons.Filled.Edit,
                                color = MaterialTheme.colorScheme.primary,
                                onTrigger = {
                                    editingSchedule = schedule
                                    showEditor = true
                                }
                            ),
                            trailingAction = SwipeAction(
                                icon = Icons.Filled.Delete,
                                color = MaterialTheme.colorScheme.error,
                                onTrigger = { scheduleToDelete = schedule }
                            )
                        ) {
                            GroupedListRow(
                                primaryText = schedule.name,
                                secondaryText = "${schedule.mode.uppercase()} · ${schedule.timezone}",
                                trailingContent = {
                                    StatusPill(
                                        text = schedule.mode,
                                        tone = if (schedule.mode == "downtime") PillTone.BLOCKED else PillTone.ALLOWED
                                    )
                                }
                            )
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
        HigAlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = "Delete Schedule",
            message = "Are you sure you want to delete the schedule '${schedule.name}'? Policies using this schedule will default to ALLOW ALL.",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteSchedule(schedule.id) },
            isDestructive = true
        )
    }
}
