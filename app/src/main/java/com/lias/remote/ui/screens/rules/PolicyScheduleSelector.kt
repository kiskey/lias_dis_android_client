// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/PolicyScheduleSelector.kt
// Version: 9.0.0
//
// Purpose:
//   Multi-schedule selection + merged timeline preview.
//
// Rules:
//   - Multiple schedules may be attached.
//   - Mixed timezones are blocked locally.
//   - Contradictory actions are highlighted.
//   - Empty selection is allowed because LIAS deliberately defaults
//     schedule-driven empty bundles to ALLOW.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.util.ScheduleFormatting
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.core.util.ScheduleValidation
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.WeeklyTimeline
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun PolicyScheduleSelector(
    schedules: List<Schedule>,
    selectedScheduleIds: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIds =
        selectedScheduleIds
            .distinct()

    val selectedSchedules =
        selectedIds.mapNotNull { id ->
            schedules.find {
                it.id == id
            }
        }

    val validation =
        ScheduleValidation
            .validateBundle(
                selectedSchedules
            )

    Column(
        modifier =
            modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        if (
            schedules.isEmpty()
        ) {
            GroupedListCard {
                GroupedListRow(
                    primaryText =
                        "No Schedules Available",
                    secondaryText =
                        "Create schedules first, or save this rule without schedules to use LIAS default-open behavior."
                )
            }

        } else {

            GroupedListCard {

                schedules
                    .sortedBy {
                        it.name.lowercase()
                    }
                    .forEachIndexed { index, schedule ->

                        val selected =
                            schedule.id in
                                selectedIds

                        GroupedListRow(
                            primaryText =
                                schedule.name,
                            secondaryText =
                                "${ScheduleFormatting.modeTitle(schedule.mode)} · ${schedule.timezone} · ${ScheduleFormatting.scheduleSummary(schedule)}",
                            trailingContent = {
                                if (selected) {
                                    CupertinoText(
                                        text =
                                            "✓",
                                        style =
                                            HigTypography.headline,
                                        color =
                                            LiasThemeColors.blue,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            },
                            showDivider =
                                index <
                                    schedules.lastIndex,
                            onClick = {

                                val updated =
                                    if (selected) {
                                        selectedIds -
                                            schedule.id
                                    } else {
                                        selectedIds +
                                            schedule.id
                                    }

                                onSelectionChanged(
                                    updated
                                        .distinct()
                                )
                            }
                        )
                    }
            }
        }

        if (
            selectedSchedules.isEmpty()
        ) {
            PolicyCallout(
                title =
                    "Default Open",
                text =
                    "No schedules are attached. LIAS will allow access for this schedule-driven rule until schedules are added.",
                error =
                    false
            )

        } else {

            CupertinoText(
                text =
                    "MERGED TIMELINE",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            WeeklyTimeline(
                schedules =
                    selectedSchedules,
                conflicts =
                    validation.conflicts,
                modifier =
                    Modifier.fillMaxWidth()
            )

            val timezones =
                selectedSchedules
                    .map {
                        it.timezone
                    }
                    .distinct()

            CupertinoText(
                text =
                    if (
                        timezones.size == 1
                    ) {
                        "Timezone · ${timezones.first()}"
                    } else {
                        "Mixed timezones · ${timezones.joinToString(", ")}"
                    },
                style =
                    HigTypography.caption,
                color =
                    if (
                        timezones.size > 1
                    ) {
                        LiasThemeColors.red
                    } else {
                        LiasThemeColors.secondaryLabel
                    }
            )

            if (
                ScheduleProjection
                    .hasMixedTimezones(
                        selectedSchedules
                    )
            ) {
                PolicyCallout(
                    title =
                        "Mixed Timezones",
                    text =
                        "LIAS schedule bundles must use one timezone. Align these schedules before saving the rule.",
                    error =
                        true
                )
            }

            if (
                validation.conflicts
                    .isNotEmpty()
            ) {
                PolicyCallout(
                    title =
                        "Schedule Conflict",
                    text =
                        "${validation.conflicts.size} contradictory ${if (validation.conflicts.size == 1) "window was" else "windows were"} found. Resolve them before saving.",
                    error =
                        true
                )
            }
        }
    }
}

@Composable
fun PolicyCallout(
    title: String,
    text: String,
    error: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                3.dp
            )
    ) {
        CupertinoText(
            text =
                title,
            style =
                HigTypography.subheadline,
            color =
                if (error) {
                    LiasThemeColors.red
                } else {
                    LiasThemeColors.orange
                },
            fontWeight =
                FontWeight.SemiBold
        )

        CupertinoText(
            text =
                text,
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.secondaryLabel
        )
    }
}
