// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 1.2.0
// Audit Fixes: 
//   1. Replaced unsafe OutlinedTextField time inputs with a native Material 3 
//      TimePicker dialog to guarantee strict HH:MM format compliance (Gap 3.4).
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var mode by remember { mutableStateOf(initialSchedule?.mode ?: "downtime") }
    var timezone by remember { mutableStateOf(initialSchedule?.timezone ?: "UTC") }
    
    val rules = remember {
        mutableStateListOf<ScheduleRule>().apply {
            if (initialSchedule != null) addAll(initialSchedule.rules)
            else add(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Schedule Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "downtime",
                    onClick = { mode = "downtime" },
                    label = { Text("Downtime") }
                )
                FilterChip(
                    selected = mode == "whitelist",
                    onClick = { mode = "whitelist" },
                    label = { Text("Whitelist") }
                )
            }

            OutlinedTextField(
                value = timezone,
                onValueChange = { timezone = it },
                label = { Text("Timezone (e.g. America/New_York)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Rules", style = MaterialTheme.typography.titleMedium)
            
            rules.forEachIndexed { index, rule ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").forEach { day ->
                                FilterChip(
                                    selected = day in rule.days,
                                    onClick = {
                                        val newDays = rule.days.toMutableList()
                                        if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
                                        rules[index] = rule.copy(days = newDays)
                                    },
                                    label = { Text(day.take(3).uppercase()) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.size(12.dp))
                        
                        // FIX 3.4: Native TimePicker implementation
                        var showStartPicker by remember { mutableStateOf(false) }
                        var showEndPicker by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).clickable { showStartPicker = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Start", style = MaterialTheme.typography.labelSmall)
                                    Text(rule.startTime, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Text("to", style = MaterialTheme.typography.bodyMedium)
                            Card(
                                modifier = Modifier.weight(1f).clickable { showEndPicker = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("End", style = MaterialTheme.typography.labelSmall)
                                    Text(rule.endTime, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        if (showStartPicker) {
                            val parts = rule.startTime.split(":")
                            val timeState = rememberTimePickerState(
                                initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                                initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                                is24Hour = true
                            )
                            TimePickerDialog(
                                onConfirm = {
                                    val h = timeState.hour.toString().padStart(2, '0')
                                    val m = timeState.minute.toString().padStart(2, '0')
                                    rules[index] = rule.copy(startTime = "$h:$m")
                                    showStartPicker = false
                                },
                                onDismiss = { showStartPicker = false }
                            ) {
                                TimePicker(state = timeState)
                            }
                        }

                        if (showEndPicker) {
                            val parts = rule.endTime.split(":")
                            val timeState = rememberTimePickerState(
                                initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                                initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                                is24Hour = true
                            )
                            TimePickerDialog(
                                onConfirm = {
                                    val h = timeState.hour.toString().padStart(2, '0')
                                    val m = timeState.minute.toString().padStart(2, '0')
                                    rules[index] = rule.copy(endTime = "$h:$m")
                                    showEndPicker = false
                                },
                                onDismiss = { showEndPicker = false }
                            ) {
                                TimePicker(state = timeState)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(Schedule(
                        id = initialSchedule?.id ?: "sched_${System.currentTimeMillis()}",
                        name = name,
                        mode = mode,
                        timezone = timezone,
                        rules = rules.toList()
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Schedule")
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                content()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
