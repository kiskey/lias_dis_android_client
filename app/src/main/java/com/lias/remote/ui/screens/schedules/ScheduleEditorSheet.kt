// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 3.4.0
// Purpose: Modal bottom sheet for creating/editing multi-rule time schedules.
// Audit Fixes:
//   1. Ensured day mode SegmentedControl fills 100% width matching mode switch above.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.LiasThemeColors
import java.util.Calendar

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
                options = listOf("Block", "Allow"),
                modifier = Modifier.fillMaxWidth()
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

                        // Full-width 48dp Segmented Control with Adaptive Font Scaling (14sp-16sp)
                        val dayModeOptions = listOf("Day Range", "Specific Days", "Dates")
                        val selectedDayModeLabel = when (dayMode) {
                            RuleDayMode.RANGE -> "Day Range"
                            RuleDayMode.SPECIFIC -> "Specific Days"
                            RuleDayMode.CALENDAR -> "Dates"
                        }

                        SegmentedControl(
                            selected = selectedDayModeLabel,
                            onSelected = { label ->
                                dayMode = when (label) {
                                    "day range" -> RuleDayMode.RANGE
                                    "specific days" -> RuleDayMode.SPECIFIC
                                    "dates" -> RuleDayMode.CALENDAR
                                    else -> RuleDayMode.RANGE
                                }
                            },
                            options = dayModeOptions,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.size(12.dp))

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

                                var showStartDatePicker by remember { mutableStateOf(false) }
                                var showEndDatePicker by remember { mutableStateOf(false) }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GroupedListRow(
                                        primaryText = startDate.ifBlank { "Select Start Date" },
                                        secondaryText = "Start Date",
                                        trailingContent = {
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { showStartDatePicker = true }
                                    )
                                    GroupedListRow(
                                        primaryText = endDate.ifBlank { "Select End Date" },
                                        secondaryText = "End Date",
                                        trailingContent = {
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { showEndDatePicker = true }
                                    )
                                }

                                if (showStartDatePicker) {
                                    CupertinoDatePickerSheet(
                                        title = "Start Date",
                                        initialDateIso = startDate,
                                        onDismiss = { showStartDatePicker = false },
                                        onConfirm = { dateIso ->
                                            startDate = dateIso
                                            rules[index] = rule.copy(startDate = startDate, endDate = endDate)
                                            showStartDatePicker = false
                                        }
                                    )
                                }

                                if (showEndDatePicker) {
                                    CupertinoDatePickerSheet(
                                        title = "End Date",
                                        initialDateIso = endDate,
                                        onDismiss = { showEndDatePicker = false },
                                        onConfirm = { dateIso ->
                                            endDate = dateIso
                                            rules[index] = rule.copy(startDate = startDate, endDate = endDate)
                                            showEndDatePicker = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.size(12.dp))

                        var showStartTimePicker by remember { mutableStateOf(false) }
                        var showEndTimePicker by remember { mutableStateOf(false) }
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
                                    onClick = { showStartTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("to", style = MaterialTheme.typography.bodyMedium)
                                GroupedListRow(
                                    primaryText = rule.endTime,
                                    secondaryText = "End Time",
                                    onClick = { showEndTimePicker = true },
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

                        if (showStartTimePicker) {
                            CupertinoTimePickerSheet(
                                title = "Start Time",
                                initialTime = rule.startTime,
                                onDismiss = { showStartTimePicker = false },
                                onConfirm = { selectedTime ->
                                    rules[index] = rule.copy(startTime = selectedTime)
                                    showStartTimePicker = false
                                }
                            )
                        }

                        if (showEndTimePicker) {
                            CupertinoTimePickerSheet(
                                title = "End Time",
                                initialTime = rule.endTime,
                                onDismiss = { showEndTimePicker = false },
                                onConfirm = { selectedTime ->
                                    rules[index] = rule.copy(endTime = selectedTime)
                                    showEndTimePicker = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CupertinoDatePickerSheet(
    title: String,
    initialDateIso: String,
    onDismiss: () -> Unit,
    onConfirm: (dateIso: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember { (currentYear..currentYear + 5).toList() }
    val days = remember { (1..31).toList() }

    var parsedYear by remember { mutableIntStateOf(currentYear) }
    var parsedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var parsedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    LaunchedEffect(initialDateIso) {
        if (initialDateIso.isNotBlank()) {
            val parts = initialDateIso.split("-")
            if (parts.size == 3) {
                parsedYear = parts[0].toIntOrNull() ?: currentYear
                parsedMonth = (parts[1].toIntOrNull() ?: 1).coerceIn(1, 12)
                parsedDay = (parts[2].toIntOrNull() ?: 1).coerceIn(1, 31)
            }
        }
    }

    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = (parsedMonth - 1).coerceIn(0, 11))
    val dayListState = rememberLazyListState(initialFirstVisibleItemIndex = (parsedDay - 1).coerceIn(0, 30))
    val yearListState = rememberLazyListState(initialFirstVisibleItemIndex = (years.indexOf(parsedYear)).coerceAtLeast(0))

    val monthFling = rememberSnapFlingBehavior(lazyListState = monthListState)
    val dayFling = rememberSnapFlingBehavior(lazyListState = dayListState)
    val yearFling = rememberSnapFlingBehavior(lazyListState = yearListState)

    val centerMonthIdx by remember { derivedStateOf { (monthListState.firstVisibleItemIndex + if (monthListState.firstVisibleItemScrollOffset > 100) 1 else 0).coerceIn(0, 11) } }
    val centerDayIdx by remember { derivedStateOf { (dayListState.firstVisibleItemIndex + if (dayListState.firstVisibleItemScrollOffset > 100) 1 else 0).coerceIn(0, 30) } }
    val centerYearIdx by remember { derivedStateOf { (yearListState.firstVisibleItemIndex + if (yearListState.firstVisibleItemScrollOffset > 100) 1 else 0).coerceIn(0, years.size - 1) } }

    LaunchedEffect(centerMonthIdx, centerDayIdx, centerYearIdx) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        val selYear = years[centerYearIdx]
                        val selMonth = centerMonthIdx + 1
                        val selDay = centerDayIdx + 1
                        val formattedIso = String.format("%04d-%02d-%02d", selYear, selMonth, selDay)
                        onConfirm(formattedIso)
                    }
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // 3-Column iOS Wheel Date Picker (Month | Day | Year)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(LiasThemeColors.fill, RoundedCornerShape(10.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Month Column
                    LazyColumn(
                        state = monthListState,
                        flingBehavior = monthFling,
                        contentPadding = PaddingValues(vertical = 68.dp),
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(months) { idx, monthName ->
                            val isCenter = idx == centerMonthIdx
                            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = monthName,
                                    style = if (isCenter) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCenter) FontWeight.W800 else FontWeight.Normal,
                                    color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Day Column
                    LazyColumn(
                        state = dayListState,
                        flingBehavior = dayFling,
                        contentPadding = PaddingValues(vertical = 68.dp),
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(days) { idx, dayVal ->
                            val isCenter = idx == centerDayIdx
                            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayVal.toString(),
                                    style = if (isCenter) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCenter) FontWeight.W800 else FontWeight.Normal,
                                    color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Year Column
                    LazyColumn(
                        state = yearListState,
                        flingBehavior = yearFling,
                        contentPadding = PaddingValues(vertical = 68.dp),
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(years) { idx, yearVal ->
                            val isCenter = idx == centerYearIdx
                            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = yearVal.toString(),
                                    style = if (isCenter) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCenter) FontWeight.W800 else FontWeight.Normal,
                                    color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HigButton(
                text = "Set Date",
                onClick = {
                    val selYear = years[centerYearIdx]
                    val selMonth = centerMonthIdx + 1
                    val selDay = centerDayIdx + 1
                    val formattedIso = String.format("%04d-%02d-%02d", selYear, selMonth, selDay)
                    onConfirm(formattedIso)
                },
                style = HigButtonStyle.Primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CupertinoTimePickerSheet(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (time: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    var initialHourIdx = 0
    var initialMinIdx = 0
    if (initialTime.isNotBlank()) {
        val parts = initialTime.split(":")
        if (parts.size == 2) {
            initialHourIdx = (parts[0].toIntOrNull() ?: 0).coerceIn(0, 23)
            initialMinIdx = (parts[1].toIntOrNull() ?: 0).coerceIn(0, 59)
        }
    }

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = initialHourIdx)
    val minListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinIdx)

    val hourFling = rememberSnapFlingBehavior(lazyListState = hourListState)
    val minFling = rememberSnapFlingBehavior(lazyListState = minListState)

    val centerHourIdx by remember { derivedStateOf { (hourListState.firstVisibleItemIndex + if (hourListState.firstVisibleItemScrollOffset > 100) 1 else 0).coerceIn(0, 23) } }
    val centerMinIdx by remember { derivedStateOf { (minListState.firstVisibleItemIndex + if (minListState.firstVisibleItemScrollOffset > 100) 1 else 0).coerceIn(0, 59) } }

    LaunchedEffect(centerHourIdx, centerMinIdx) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        val selHour = hours[centerHourIdx]
                        val selMin = minutes[centerMinIdx]
                        onConfirm("$selHour:$selMin")
                    }
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // 2-Column iOS Wheel Time Picker (Hour | Minute)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(LiasThemeColors.fill, RoundedCornerShape(10.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Hour Column
                    LazyColumn(
                        state = hourListState,
                        flingBehavior = hourFling,
                        contentPadding = PaddingValues(vertical = 68.dp),
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(hours) { idx, hourVal ->
                            val isCenter = idx == centerHourIdx
                            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = hourVal,
                                    style = if (isCenter) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCenter) FontWeight.W800 else FontWeight.Normal,
                                    color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Separator
                    Box(modifier = Modifier.height(180.dp), contentAlignment = Alignment.Center) {
                        Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }

                    // Minute Column
                    LazyColumn(
                        state = minListState,
                        flingBehavior = minFling,
                        contentPadding = PaddingValues(vertical = 68.dp),
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(minutes) { idx, minVal ->
                            val isCenter = idx == centerMinIdx
                            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = minVal,
                                    style = if (isCenter) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCenter) FontWeight.W800 else FontWeight.Normal,
                                    color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HigButton(
                text = "Set Time",
                onClick = {
                    val selHour = hours[centerHourIdx]
                    val selMin = minutes[centerMinIdx]
                    onConfirm("$selHour:$selMin")
                },
                style = HigButtonStyle.Primary
            )
        }
    }
}
