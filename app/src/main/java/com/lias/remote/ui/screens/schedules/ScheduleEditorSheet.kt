// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 2.1.0
// Audit Fixes:
//   1. ModalBottomSheet with 22dp top corner radius (HigSpec.SheetCorner).
//   2. iOS Nav Row (Cancel / Schedule Name / Save).
//   3. Styled "+ Add Rule" action row as unboxed blue text row (no card outline).
//   4. Surfaced overnight indicator "🌙 Continues past midnight" on rules crossing 24:00.
//   5. Preserved conflict detection banner and downtime-vs-whitelist mode logic byte-for-byte.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigSpec

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
        "Asia/Kolkata" to "(UTC+05:30) India Standard Time"
    )

    val conflicts = remember(rules.toList()) {
        if (rules.size > 1) {
            ScheduleProjection.detectConflicts(listOf(Schedule(id = "temp", name = "Temp", mode = mode, timezone = timezone, rules = rules.toList())))
        } else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HIG Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = if (initialSchedule == null) "New Schedule" else initialSchedule.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        onSave(Schedule(
                            id = initialSchedule?.id ?: "sched_${System.currentTimeMillis()}",
                            name = name,
                            mode = mode,
                            timezone = timezone,
                            rules = rules.toList()
                        ))
                    },
                    enabled = name.isNotBlank() && conflicts.isEmpty()
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (name.isNotBlank() && conflicts.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HigField(
                value = name,
                onValueChange = { name = it },
                label = "Schedule Name",
                placeholder = "e.g. Bedtime Downtime"
            )

            Text("MODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedControl(
                selected = if (mode == "downtime") "Block" else "Allow",
                onSelected = { selectedLabel ->
                    mode = if (selectedLabel.equals("Block", ignoreCase = true)) "downtime" else "whitelist"
                },
                options = listOf("Block", "Allow")
            )

            ExposedDropdownMenuBox(
                expanded = timezoneExpanded,
                onExpandedChange = { timezoneExpanded = !timezoneExpanded }
            ) {
                HigField(
                    value = timezones.firstOrNull { it.first == timezone }?.second ?: timezone,
                    onValueChange = {},
                    label = "Time Zone",
                    enabled = false,
                    onClick = { timezoneExpanded = true },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = timezoneExpanded,
                    onDismissRequest = { timezoneExpanded = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    timezones.forEach { (tzId, tzLabel) ->
                        TextButton(
                            onClick = {
                                timezone = tzId
                                timezoneExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tzLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RULES (${rules.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = {
                        rules.add(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))
                    }
                ) {
                    Text("+ Add Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (conflicts.isNotEmpty()) {
                Text("⚠️ Conflicts detected in schedule rules!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            rules.forEachIndexed { index, rule ->
                val safeDays = rule.safeDays
                var dayMode by remember {
                    mutableStateOf(
                        if (!rule.startDate.isNullOrBlank() && !rule.endDate.isNullOrBlank()) RuleDayMode.CALENDAR
                        else if (safeDays.size > 2) RuleDayMode.RANGE
                        else RuleDayMode.SPECIFIC
                    )
                }

                GroupedListCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RULE ${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (rules.size > 1) {
                                IconButton(onClick = { rules.removeAt(index) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove Rule", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day Mode Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                RuleDayMode.RANGE to "Day Range",
                                RuleDayMode.SPECIFIC to "Specific Days",
                                RuleDayMode.CALENDAR to "Dates"
                            )
                            options.forEach { (modeOpt, label) ->
                                val selected = dayMode == modeOpt
                                TextButton(
                                    onClick = { dayMode = modeOpt },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                                    Text("From:", style = MaterialTheme.typography.bodyMedium)
                                    TextButton(
                                        onClick = {
                                            val days = ScheduleProjection.daysOrder
                                            val nextIdx = (days.indexOf(fromDay) + 1) % days.size
                                            fromDay = days[nextIdx]
                                            rules[index] = rule.copy(days = ScheduleProjection.expandDayRange(fromDay, toDay))
                                        }
                                    ) {
                                        Text(fromDay.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text("To:", style = MaterialTheme.typography.bodyMedium)
                                    TextButton(
                                        onClick = {
                                            val days = ScheduleProjection.daysOrder
                                            val nextIdx = (days.indexOf(toDay) + 1) % days.size
                                            toDay = days[nextIdx]
                                            rules[index] = rule.copy(days = ScheduleProjection.expandDayRange(fromDay, toDay))
                                        }
                                    ) {
                                        Text(toDay.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            RuleDayMode.SPECIFIC -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").forEach { day ->
                                        val isChecked = day in safeDays
                                        TextButton(
                                            onClick = {
                                                val newDays = safeDays.toMutableList()
                                                if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
                                                rules[index] = rule.copy(days = newDays, startDate = null, endDate = null)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(
                                                text = day.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            RuleDayMode.CALENDAR -> {
                                var startDate by remember { mutableStateOf(rule.startDate ?: "") }
                                var endDate by remember { mutableStateOf(rule.endDate ?: "") }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HigField(
                                        value = startDate,
                                        onValueChange = {
                                            startDate = it
                                            rules[index] = rule.copy(startDate = startDate, endDate = endDate)
                                        },
                                        label = "Start Date (YYYY-MM-DD)"
                                    )
                                    HigField(
                                        value = endDate,
                                        onValueChange = {
                                            endDate = it
                                            rules[index] = rule.copy(startDate = startDate, endDate = endDate)
                                        },
                                        label = "End Date (YYYY-MM-DD)"
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All Day (00:00 - 23:59)", style = MaterialTheme.typography.bodyLarge)
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
                                GroupedListRow(
                                    primaryText = rule.startTime,
                                    secondaryText = "Start Time",
                                    onClick = { showStartPicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("to", style = MaterialTheme.typography.bodyMedium)
                                GroupedListRow(
                                    primaryText = rule.endTime,
                                    secondaryText = "End Time",
                                    onClick = { showEndPicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val startMin = rule.startTime.split(":").getOrNull(0)?.toIntOrNull()?.times(60) ?: 0
                            val endMin = rule.endTime.split(":").getOrNull(0)?.toIntOrNull()?.times(60) ?: 0
                            if (endMin <= startMin) {
                                Spacer(modifier = Modifier.height(4.dp))
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
                    TextButton(onClick = onConfirm) { Text("OK", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
