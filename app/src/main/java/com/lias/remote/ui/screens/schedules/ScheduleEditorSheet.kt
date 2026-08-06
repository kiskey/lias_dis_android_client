// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 1.7.0
// Audit Fixes: 
//   1. Full Rule Mode parity with Web Dashboard: Continuous Day Range, Specific Days, Calendar Dates.
//   2. Automatic mode inference when viewing saved schedules so stored rules display accurately.
//   3. Smoothly scrollable ModalBottomSheet body.
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lias.remote.core.util.ScheduleProjection

enum class RuleDayMode { RANGE, SPECIFIC, CALENDAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var mode by remember { mutableStateOf(initialSchedule?.mode ?: "downtime") }
    var timezone by remember { mutableStateOf(initialSchedule?.timezone ?: "UTC") }
    var timezoneExpanded by remember { mutableStateOf(false) }
    
    val rules = remember {
        mutableStateListOf<ScheduleRule>().apply {
            if (initialSchedule != null && initialSchedule.safeRules.isNotEmpty()) {
                addAll(initialSchedule.safeRules)
            } else {
                add(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))
            }
        }
    }

    val timezones = listOf(
        "America/Los_Angeles" to "(UTC-08:00) Pacific Time",
        "America/Denver" to "(UTC-07:00) Mountain Time",
        "America/Chicago" to "(UTC-06:00) Central Time",
        "America/New_York" to "(UTC-05:00) Eastern Time",
        "UTC" to "(UTC+00:00) Coordinated Universal Time",
        "Europe/London" to "(UTC+00:00) London",
        "Europe/Paris" to "(UTC+01:00) Paris",
        "Asia/Kolkata" to "(UTC+05:30) India Standard Time",
        "Asia/Tokyo" to "(UTC+09:00) Tokyo"
    )

    val conflicts = remember(rules.toList()) {
        if (rules.size > 1) {
            ScheduleProjection.detectConflicts(listOf(Schedule(id = "temp", name = "Temp", mode = mode, timezone = timezone, rules = rules.toList())))
        } else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Schedule Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "downtime",
                    onClick = { mode = "downtime" },
                    label = { Text("Downtime (Block rules)") }
                )
                FilterChip(
                    selected = mode == "whitelist",
                    onClick = { mode = "whitelist" },
                    label = { Text("Whitelist (Allow rules)") }
                )
            }

            ExposedDropdownMenuBox(
                expanded = timezoneExpanded,
                onExpandedChange = { timezoneExpanded = !timezoneExpanded }
            ) {
                OutlinedTextField(
                    value = timezones.firstOrNull { it.first == timezone }?.second ?: timezone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Timezone") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = timezoneExpanded,
                    onDismissRequest = { timezoneExpanded = false }
                ) {
                    timezones.forEach { (tzId, tzLabel) ->
                        TextButton(
                            onClick = {
                                timezone = tzId
                                timezoneExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tzLabel)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rules (${rules.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { 
                    rules.add(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block")) 
                }) {
                    Text("+ Add Rule")
                }
            }

            if (conflicts.isNotEmpty()) {
                Text("⚠️ Conflicts detected in rules!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            rules.forEachIndexed { index, rule ->
                val safeDays = rule.safeDays
                var dayMode by remember {
                    mutableStateOf(
                        if (!rule.startDate.isNullOrBlank() && !rule.endDate.isNullOrBlank()) {
                            RuleDayMode.CALENDAR
                        } else if (safeDays.size > 2) {
                            RuleDayMode.RANGE
                        } else {
                            RuleDayMode.SPECIFIC
                        }
                    )
                }

                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rule ${index + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { rules.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove Rule")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = dayMode == RuleDayMode.RANGE,
                                onClick = { dayMode = RuleDayMode.RANGE },
                                label = { Text("Day Range") }
                            )
                            FilterChip(
                                selected = dayMode == RuleDayMode.SPECIFIC,
                                onClick = { dayMode = RuleDayMode.SPECIFIC },
                                label = { Text("Specific Days") }
                            )
                            FilterChip(
                                selected = dayMode == RuleDayMode.CALENDAR,
                                onClick = { dayMode = RuleDayMode.CALENDAR },
                                label = { Text("Calendar Dates") }
                            )
                        }
                        
                        Spacer(modifier = Modifier.size(8.dp))
                        
                        when (dayMode) {
                            RuleDayMode.RANGE -> {
                                var fromDay by remember { mutableStateOf(safeDays.firstOrNull() ?: "mon") }
                                var toDay by remember { mutableStateOf(safeDays.lastOrNull() ?: "fri") }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("From:", style = MaterialTheme.typography.labelMedium)
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            val days = ScheduleProjection.daysOrder
                                            val nextIdx = (days.indexOf(fromDay) + 1) % days.size
                                            fromDay = days[nextIdx]
                                            rules[index] = rule.copy(days = ScheduleProjection.expandDayRange(fromDay, toDay))
                                        },
                                        label = { Text(fromDay.uppercase()) }
                                    )
                                    Text("To:", style = MaterialTheme.typography.labelMedium)
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            val days = ScheduleProjection.daysOrder
                                            val nextIdx = (days.indexOf(toDay) + 1) % days.size
                                            toDay = days[nextIdx]
                                            rules[index] = rule.copy(days = ScheduleProjection.expandDayRange(fromDay, toDay))
                                        },
                                        label = { Text(toDay.uppercase()) }
                                    )
                                }
                            }
                            RuleDayMode.SPECIFIC -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").forEach { day ->
                                        FilterChip(
                                            selected = day in safeDays,
                                            onClick = {
                                                val newDays = safeDays.toMutableList()
                                                if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
                                                rules[index] = rule.copy(days = newDays, startDate = null, endDate = null)
                                            },
                                            label = { Text(day.take(3).uppercase()) }
                                        )
                                    }
                                }
                            }
                            RuleDayMode.CALENDAR -> {
                                var startDate by remember { mutableStateOf(rule.startDate ?: "") }
                                var endDate by remember { mutableStateOf(rule.endDate ?: "") }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = startDate,
                                        onValueChange = { 
                                            startDate = it
                                            rules[index] = rule.copy(startDate = startDate, endDate = endDate)
                                        },
                                        label = { Text("Start Date (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = endDate,
                                        onValueChange = { 
                                            endDate = it
                                            rules[index] = rule.copy(startDate = startDate, endDate = endDate)
                                        },
                                        label = { Text("End Date (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.size(12.dp))
                        
                        var showStartPicker by remember { mutableStateOf(false) }
                        var showEndPicker by remember { mutableStateOf(false) }
                        var isAllDay by remember { mutableStateOf(rule.startTime == "00:00" && rule.endTime == "23:59") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = rule.action == "block",
                                onClick = { rules[index] = rule.copy(action = "block") },
                                label = { Text("Block") }
                            )
                            FilterChip(
                                selected = rule.action == "allow",
                                onClick = { rules[index] = rule.copy(action = "allow") },
                                label = { Text("Allow") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All Day")
                            Checkbox(
                                checked = isAllDay,
                                onCheckedChange = {
                                    isAllDay = it
                                    if (it) {
                                        rules[index] = rule.copy(startTime = "00:00", endTime = "23:59")
                                    }
                                }
                            )
                        }

                        if (!isAllDay) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f).clickable { showStartPicker = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Start", style = MaterialTheme.typography.labelSmall)
                                        Text(rule.startTime, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Text("to", style = MaterialTheme.typography.bodyMedium)
                                Card(
                                    modifier = Modifier.weight(1f).clickable { showEndPicker = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("End", style = MaterialTheme.typography.labelSmall)
                                        Text(rule.endTime, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }

                            val startMin = rule.startTime.split(":").getOrNull(0)?.toIntOrNull()?.times(60) ?: 0
                            val endMin = rule.endTime.split(":").getOrNull(0)?.toIntOrNull()?.times(60) ?: 0
                            if (endMin <= startMin) {
                                Text("🌙 Continues past midnight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
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
                enabled = name.isNotBlank() && conflicts.isEmpty(),
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
