package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.schedule.ScheduleRuleDraft
import com.lias.remote.core.schedule.ScheduleRuleScope
import com.lias.remote.core.schedule.ScheduleSemantics
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

private enum class TimeTarget { START, END }
private enum class DateTarget { START, END }

@Composable
fun ScheduleRuleEditorCard(
    index: Int,
    rule: ScheduleRuleDraft,
    scheduleMode: String,
    canDelete: Boolean,
    onChange: (ScheduleRuleDraft) -> Unit,
    onDelete: () -> Unit
) {
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }

    var previousStartTime by remember(index) {
        mutableStateOf(if (rule.isAllDayPreset) "22:00" else rule.startTime)
    }
    var previousEndTime by remember(index) {
        mutableStateOf(if (rule.isAllDayPreset) "06:00" else rule.endTime)
    }
    var previousDays by remember(index) {
        mutableStateOf(
            rule.days.takeIf { it.isNotEmpty() && it.size < 7 }
                ?: setOf("mon", "tue", "wed", "thu", "fri")
        )
    }

    GroupedListCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    CupertinoText(
                        text = "Window ${index + 1}",
                        style = HigTypography.headline,
                        color = LiasThemeColors.label
                    )
                    CupertinoText(
                        text = "${ScheduleSemantics.windowAction(scheduleMode)} during this window",
                        style = HigTypography.caption,
                        color = if (ScheduleSemantics.normalizeMode(scheduleMode) == "downtime") {
                            LiasThemeColors.red
                        } else {
                            LiasThemeColors.green
                        }
                    )
                }
                if (canDelete) {
                    HigTextButton(text = "Remove", onClick = onDelete, isDestructive = true)
                }
            }

            SegmentedControl(
                options = listOf("Repeats", "Dates"),
                selectedOption = if (rule.scope == ScheduleRuleScope.CALENDAR) "Dates" else "Repeats",
                onOptionSelected = { selected ->
                    onChange(
                        rule.copy(
                            scope = if (selected == "Dates") {
                                ScheduleRuleScope.CALENDAR
                            } else {
                                ScheduleRuleScope.RECURRING
                            }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            when (rule.scope) {
                ScheduleRuleScope.RECURRING -> {
                    CupertinoText(
                        text = "DAYS",
                        style = HigTypography.caption,
                        color = LiasThemeColors.tertiaryLabel
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScheduleSemantics.orderedDayKeys.forEach { day ->
                            val selected = day in rule.days
                            DayToggle(day = day, selected = selected) {
                                val updated = rule.days.toMutableSet()
                                if (!updated.add(day)) updated.remove(day)
                                if (updated.isNotEmpty() && updated.size < 7) {
                                    previousDays = updated.toSet()
                                }
                                onChange(rule.copy(days = updated))
                            }
                        }
                    }
                    CupertinoText(
                        text = ScheduleSemantics.orderedDays(rule.days)
                            .joinToString(", ") { ScheduleSemantics.dayLabel(it) }
                            .ifBlank { "Choose at least one day." },
                        style = HigTypography.caption,
                        color = if (rule.days.isEmpty()) LiasThemeColors.red else LiasThemeColors.secondaryLabel
                    )
                }

                ScheduleRuleScope.CALENDAR -> {
                    CupertinoText(
                        text = "DATE RANGE",
                        style = HigTypography.caption,
                        color = LiasThemeColors.tertiaryLabel
                    )
                    PickerValueRow("Start Date", rule.startDate.ifBlank { "Choose" }) {
                        dateTarget = DateTarget.START
                    }
                    PickerValueRow("End Date", rule.endDate.ifBlank { "Choose" }) {
                        dateTarget = DateTarget.END
                    }
                    CupertinoText(
                        text = "The end date is inclusive in LIAS.",
                        style = HigTypography.caption,
                        color = LiasThemeColors.secondaryLabel
                    )
                }
            }

            CupertinoText(
                text = "TIME",
                style = HigTypography.caption,
                color = LiasThemeColors.tertiaryLabel
            )
            PickerValueRow("Starts", rule.startTime) { timeTarget = TimeTarget.START }
            PickerValueRow("Ends", rule.endTime) { timeTarget = TimeTarget.END }

            if (rule.isOvernight) {
                CupertinoText(
                    text = "Continues past midnight · ${rule.endTime} is on the following day.",
                    style = HigTypography.caption,
                    color = LiasThemeColors.blue
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HigButton(
                    text = if (rule.isAllDayPreset) "All Day ✓" else "All Day",
                    onClick = {
                        if (rule.isAllDayPreset) {
                            onChange(rule.copy(startTime = previousStartTime, endTime = previousEndTime))
                        } else {
                            previousStartTime = rule.startTime
                            previousEndTime = rule.endTime
                            onChange(rule.copy(startTime = "00:00", endTime = "23:59"))
                        }
                    },
                    style = if (rule.isAllDayPreset) HigButtonStyle.Primary else HigButtonStyle.Gray,
                    modifier = Modifier.weight(1f)
                )

                val everyDay = rule.scope == ScheduleRuleScope.RECURRING && rule.days.size == 7
                HigButton(
                    text = if (everyDay) "Every Day ✓" else "Every Day",
                    onClick = {
                        if (everyDay) {
                            onChange(rule.copy(days = previousDays))
                        } else {
                            if (rule.scope == ScheduleRuleScope.RECURRING && rule.days.isNotEmpty() && rule.days.size < 7) {
                                previousDays = rule.days
                            }
                            onChange(
                                rule.copy(
                                    scope = ScheduleRuleScope.RECURRING,
                                    days = ScheduleSemantics.orderedDayKeys.toSet()
                                )
                            )
                        }
                    },
                    style = if (everyDay) HigButtonStyle.Primary else HigButtonStyle.Gray,
                    modifier = Modifier.weight(1f)
                )
            }

            CupertinoText(
                text = ScheduleSemantics.ruleSummary(rule),
                style = HigTypography.caption,
                color = LiasThemeColors.tertiaryLabel
            )
        }
    }

    timeTarget?.let { target ->
        ScheduleTimePickerSheet(
            title = if (target == TimeTarget.START) "Start Time" else "End Time",
            initialValue = if (target == TimeTarget.START) rule.startTime else rule.endTime,
            onDismiss = { timeTarget = null },
            onConfirm = { value ->
                val updated = if (target == TimeTarget.START) {
                    rule.copy(startTime = value)
                } else {
                    rule.copy(endTime = value)
                }
                if (!updated.isAllDayPreset) {
                    previousStartTime = updated.startTime
                    previousEndTime = updated.endTime
                }
                onChange(updated)
                timeTarget = null
            }
        )
    }

    dateTarget?.let { target ->
        ScheduleDatePickerSheet(
            title = if (target == DateTarget.START) "Start Date" else "End Date",
            initialValue = if (target == DateTarget.START) {
                rule.startDate
            } else {
                rule.endDate.ifBlank { rule.startDate }
            },
            onDismiss = { dateTarget = null },
            onConfirm = { value ->
                onChange(
                    if (target == DateTarget.START) {
                        rule.copy(startDate = value)
                    } else {
                        rule.copy(endDate = value)
                    }
                )
                dateTarget = null
            }
        )
    }
}

@Composable
private fun DayToggle(day: String, selected: Boolean, onClick: () -> Unit) {
    val label = ScheduleSemantics.dayLabel(day).take(2)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) LiasThemeColors.blue else LiasThemeColors.fill2)
            .semantics {
                role = Role.Checkbox
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CupertinoText(
            text = label,
            style = HigTypography.caption,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else LiasThemeColors.label
        )
    }
}

@Composable
private fun PickerValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CupertinoText(
            text = label,
            style = HigTypography.body,
            color = LiasThemeColors.label,
            modifier = Modifier.weight(1f)
        )
        CupertinoText(text = value, style = HigTypography.body, color = LiasThemeColors.blue)
        CupertinoText(text = "  ›", style = HigTypography.headline, color = LiasThemeColors.tertiaryLabel)
    }
}
