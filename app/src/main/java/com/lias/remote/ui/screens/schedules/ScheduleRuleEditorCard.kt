// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleRuleEditorCard.kt
// Version: 18.0.0
//
// Purpose:
//   One independently editable schedule window.
//
// Supports:
//   - recurring weekdays
//   - calendar date ranges
//   - overnight windows
//   - "all day" convenience preset
//   - per-window deletion
//
// The Action is intentionally NOT editable:
//   Downtime  -> BLOCK
//   Whitelist -> ALLOW
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.core.schedule.ScheduleRuleDraft
import com.lias.remote.core.schedule.ScheduleRuleScope
import com.lias.remote.core.schedule.ScheduleSemantics
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun ScheduleRuleEditorCard(
    index: Int,
    rule: ScheduleRuleDraft,
    scheduleMode: String,
    canDelete: Boolean,
    onChange: (ScheduleRuleDraft) -> Unit,
    onDelete: () -> Unit
) {

    GroupedListCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    CupertinoText(
                        text =
                            "Window ${index + 1}",
                        style =
                            HigTypography.headline,
                        color =
                            LiasThemeColors.label
                    )

                    CupertinoText(
                        text =
                            "${
                                ScheduleSemantics
                                    .windowAction(
                                        scheduleMode
                                    )
                            } during this window",
                        style =
                            HigTypography.caption,
                        color =
                            if (
                                ScheduleSemantics
                                    .normalizeMode(
                                        scheduleMode
                                    ) ==
                                "downtime"
                            ) {
                                LiasThemeColors.red
                            } else {
                                LiasThemeColors.green
                            }
                    )
                }

                if (
                    canDelete
                ) {

                    HigTextButton(
                        text =
                            "Remove",
                        onClick =
                            onDelete,
                        isDestructive =
                            true
                    )
                }
            }

            CupertinoText(
                text =
                    "WHEN",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            SegmentedControl(
                options =
                    listOf(
                        "Repeats",
                        "Dates"
                    ),
                selectedOption =
                    if (
                        rule.scope ==
                        ScheduleRuleScope.CALENDAR
                    ) {
                        "Dates"
                    } else {
                        "Repeats"
                    },
                onOptionSelected = {
                    selected ->

                    onChange(
                        rule.copy(
                            scope =
                                if (
                                    selected ==
                                    "Dates"
                                ) {
                                    ScheduleRuleScope.CALENDAR
                                } else {
                                    ScheduleRuleScope.RECURRING
                                }
                        )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            when (
                rule.scope
            ) {

                ScheduleRuleScope.RECURRING -> {

                    CupertinoText(
                        text =
                            "DAYS",
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {

                        ScheduleSemantics
                            .orderedDayKeys
                            .forEach {
                                    day ->

                                val selected =
                                    day in
                                        rule.days

                                HigButton(
                                    text =
                                        ScheduleSemantics
                                            .dayLabel(
                                                day
                                            )
                                            .take(
                                                1
                                            ),
                                    onClick = {

                                        val updated =
                                            rule.days
                                                .toMutableSet()

                                        if (
                                            !updated.add(
                                                day
                                            )
                                        ) {
                                            updated.remove(
                                                day
                                            )
                                        }

                                        onChange(
                                            rule.copy(
                                                days =
                                                    updated
                                            )
                                        )
                                    },
                                    style =
                                        if (
                                            selected
                                        ) {
                                            HigButtonStyle.Primary
                                        } else {
                                            HigButtonStyle.Gray
                                        },
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                )
                            }
                    }

                    CupertinoText(
                        text =
                            ScheduleSemantics
                                .orderedDays(
                                    rule.days
                                )
                                .joinToString(
                                    ", "
                                ) {
                                    ScheduleSemantics
                                        .dayLabel(
                                            it
                                        )
                                }
                                .ifBlank {
                                    "Choose at least one day."
                                },
                        style =
                            HigTypography.caption,
                        color =
                            if (
                                rule.days
                                    .isEmpty()
                            ) {
                                LiasThemeColors.red
                            } else {
                                LiasThemeColors.secondaryLabel
                            }
                    )
                }

                ScheduleRuleScope.CALENDAR -> {

                    CupertinoText(
                        text =
                            "DATE RANGE",
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )

                    HigField(
                        value =
                            rule.startDate,
                        onValueChange = {

                            onChange(
                                rule.copy(
                                    startDate =
                                        it
                                            .filter {
                                                    character ->
                                                character.isDigit() ||
                                                    character ==
                                                    '-'
                                            }
                                            .take(
                                                10
                                            )
                                )
                            )
                        },
                        label =
                            "Start Date",
                        placeholder =
                            "YYYY-MM-DD"
                    )

                    HigField(
                        value =
                            rule.endDate,
                        onValueChange = {

                            onChange(
                                rule.copy(
                                    endDate =
                                        it
                                            .filter {
                                                    character ->
                                                character.isDigit() ||
                                                    character ==
                                                    '-'
                                            }
                                            .take(
                                                10
                                            )
                                )
                            )
                        },
                        label =
                            "End Date",
                        placeholder =
                            "YYYY-MM-DD"
                    )

                    CupertinoText(
                        text =
                            "The end date is inclusive in LIAS.",
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.secondaryLabel
                    )
                }
            }

            CupertinoText(
                text =
                    "TIME",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                HigField(
                    value =
                        rule.startTime,
                    onValueChange = {

                        onChange(
                            rule.copy(
                                startTime =
                                    normalizeTimeInput(
                                        it
                                    )
                            )
                        )
                    },
                    label =
                        "Starts",
                    placeholder =
                        "22:00",
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                HigField(
                    value =
                        rule.endTime,
                    onValueChange = {

                        onChange(
                            rule.copy(
                                endTime =
                                    normalizeTimeInput(
                                        it
                                    )
                            )
                        )
                    },
                    label =
                        "Ends",
                    placeholder =
                        "06:00",
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            if (
                rule.isOvernight
            ) {

                CupertinoText(
                    text =
                        "Continues past midnight · ${
                            rule.endTime
                        } is on the following day.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.blue
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                HigButton(
                    text =
                        if (
                            rule.isAllDayPreset
                        ) {
                            "All Day ✓"
                        } else {
                            "All Day"
                        },
                    onClick = {

                        onChange(
                            rule.copy(
                                startTime =
                                    "00:00",
                                endTime =
                                    "23:59"
                            )
                        )
                    },
                    style =
                        if (
                            rule.isAllDayPreset
                        ) {
                            HigButtonStyle.Primary
                        } else {
                            HigButtonStyle.Gray
                        },
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                HigButton(
                    text =
                        if (
                            rule.scope ==
                            ScheduleRuleScope.RECURRING &&
                            rule.days.size ==
                            7
                        ) {
                            "Every Day ✓"
                        } else {
                            "Every Day"
                        },
                    onClick = {

                        onChange(
                            rule.copy(
                                scope =
                                    ScheduleRuleScope.RECURRING,
                                days =
                                    ScheduleSemantics
                                        .orderedDayKeys
                                        .toSet()
                            )
                        )
                    },
                    style =
                        if (
                            rule.scope ==
                            ScheduleRuleScope.RECURRING &&
                            rule.days.size ==
                            7
                        ) {
                            HigButtonStyle.Primary
                        } else {
                            HigButtonStyle.Gray
                        },
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            CupertinoText(
                text =
                    ScheduleSemantics
                        .ruleSummary(
                            rule
                        ),
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )
        }
    }
}

private fun normalizeTimeInput(
    raw: String
): String =
    raw
        .filter {
                character ->
            character.isDigit() ||
                character ==
                ':'
        }
        .take(
            5
        )
