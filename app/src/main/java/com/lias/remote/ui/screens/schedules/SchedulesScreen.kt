// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulesScreen.kt
// Version: 2.0.1
// Audit Fixes:
//   1. Added `import androidx.compose.foundation.lazy.items` to resolve model parameter list overloading.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SwipeActionRow
import com.lias.remote.ui.components.WeeklyTimeline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedules", style = MaterialTheme.typography.headlineLarge) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Schedule")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.schedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No schedules yet. Tap + to create one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                GroupedList {
                    item { ListSectionHeader("Configured Schedules (${state.schedules.size})") }

                    items(state.schedules, key = { it.id }) { schedule ->
                        SwipeActionRow(
                            onSwipeRight = {
                                editingSchedule = schedule
                                showEditor = true
                            },
                            onSwipeLeft = {
                                scheduleToDelete = schedule
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
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
                                Spacer(modifier = Modifier.height(4.dp))
                                WeeklyTimeline(schedules = listOf(schedule))
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
        
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Confirm Delete") },
            text = {
                if (impactedPolicies.isNotEmpty()) {
                    Column {
                        Text("⚠️ Warning: This schedule is attached to the following policies:")
                        impactedPolicies.forEach { p ->
                            Text("• ${p.name}", fontWeight = FontWeight.Bold)
                        }
                        Text("These policies will fail closed (BLOCK).")
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
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
