// ====================================================================
// File: SchedulesScreen.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Schedule list with FAB. Integrated ScheduleEditorSheet.
// ====================================================================

package com.lias.remote.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SegmentedControl
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
                onClick = { editingSchedule = null; showEditor = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) { Icon(Icons.Filled.Add, "New Schedule", tint = Color.White) }
        }
    ) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { ListSectionHeader("Configured Schedules (${state.schedules.size})") }
            
            if (state.schedules.isEmpty()) {
                item { GroupedListCard { GroupedListRow(primaryText = "No schedules configured", secondaryText = "Tap + to create a time window.") } }
            } else {
                items(state.schedules, key = { it.id }) { schedule ->
                    GroupedListCard {
                        HigSwipeRow(
                            leadingAction = SwipeAction(Icons.Filled.Edit, MaterialTheme.colorScheme.primary, { editingSchedule = schedule; showEditor = true }),
                            trailingAction = SwipeAction(Icons.Filled.Delete, MaterialTheme.colorScheme.error, { scheduleToDelete = schedule })
                        ) {
                            GroupedListRow(
                                primaryText = schedule.name,
                                secondaryText = "${schedule.mode.uppercase()} · ${schedule.timezone}",
                                trailingContent = { StatusPill(text = schedule.mode, tone = if (schedule.mode == "downtime") PillTone.BLOCKED else PillTone.ALLOWED) }
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
            onSave = { viewModel.saveSchedule(it); showEditor = false }
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

@Composable
fun ScheduleEditorSheet(initialSchedule: Schedule?, onDismiss: () -> Unit, onSave: (Schedule) -> Unit) {
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var mode by remember { mutableStateOf(initialSchedule?.mode ?: "downtime") }
    var timezone by remember { mutableStateOf(initialSchedule?.timezone ?: "UTC") }
    val rules = remember { mutableStateOf<List<ScheduleRule>>(initialSchedule?.rules ?: listOf(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))) }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(
                title = if (initialSchedule == null) "New Schedule" else "Edit Schedule",
                onCancel = onDismiss,
                trailingAction = {
                    HigButton(
                        text = "Save",
                        onClick = { onSave(Schedule(id = initialSchedule?.id ?: "sched_${System.currentTimeMillis()}", name = name, mode = mode, timezone = timezone, rules = rules.value)) },
                        style = HigButtonStyle.Primary
                    )
                }
            )

            HigField(value = name, onValueChange = { name = it }, label = "Schedule Name", placeholder = "e.g. Bedtime")

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentedControl(
                    options = listOf("Downtime", "Whitelist"),
                    selectedOption = if (mode == "downtime") "Downtime" else "Whitelist",
                    onOptionSelected = { mode = it.lowercase() },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            HigField(value = timezone, onValueChange = { timezone = it }, label = "Timezone", placeholder = "America/New_York")
        }
    }
}
