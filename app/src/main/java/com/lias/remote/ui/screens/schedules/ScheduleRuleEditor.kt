// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleRuleEditor.kt
// Version: 8.0.0
//
// Purpose:
//   Editor for one LIAS ScheduleRule.
//
// UX:
//   - Explicit day selection.
//   - 24-hour HH:mm input matching backend contract.
//   - Allow / Block action control.
//   - Optional date range.
//   - Clearly identifies overnight windows.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.core.util.ScheduleFormatting
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun ScheduleRuleEditor(
    rule: ScheduleRule,
    ruleNumber: Int,
    onRuleChanged: (ScheduleRule) -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedDays =
        rule.safeDays
            .map {
                ScheduleProjection
                    .normalizeDay(it)
            }
            .toSet()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    LiasThemeColors.secondaryBackground
                )
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
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            CupertinoText(
                text =
                    "Window $ruleNumber",
                style =
                    HigTypography.headline,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    LiasThemeColors.label
            )

            if (
                onDelete != null
            ) {
                HigTextButton(
                    text =
                        "Delete",
                    onClick =
                        onDelete
                )
            }
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    7.dp
                )
        ) {
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
                        5.dp
                    )
            ) {
                ScheduleProjection
                    .daysOrder
                    .forEach { day ->

                        val selected =
                            day in
                                selectedDays

                        DayButton(
                            day =
                                day,
                            selected =
                                selected,
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            onClick = {

                                val updated =
                                    if (selected) {
                                        selectedDays -
                                            day
                                    } else {
                                        selectedDays +
                                            day
                                    }

                                onRuleChanged(
                                    rule.copy(
                                        days =
                                            ScheduleProjection
                                                .daysOrder
                                                .filter {
                                                    it in
                                                        updated
                                                }
                                    )
                                )
                            }
                        )
                    }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                HigTextButton(
                    text =
                        "Weekdays",
                    onClick = {
                        onRuleChanged(
                            rule.copy(
                                days =
                                    listOf(
                                        "mon",
                                        "tue",
                                        "wed",
                                        "thu",
                                        "fri"
                                    )
                            )
                        )
                    }
                )

                HigTextButton(
                    text =
                        "Every Day",
                    onClick = {
                        onRuleChanged(
                            rule.copy(
                                days =
                                    ScheduleProjection
                                        .daysOrder
                            )
                        )
                    }
                )

                HigTextButton(
                    text =
                        "Clear",
                    onClick = {
                        onRuleChanged(
                            rule.copy(
                                days =
                                    emptyList()
                            )
                        )
                    }
                )
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            HigConfiguredField(
                value =
                    rule.startTime,
                onValueChange = {
                    onRuleChanged(
                        rule.copy(
                            startTime =
                                it
                        )
                    )
                },
                label =
                    "Start",
                placeholder =
                    "22:00",
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            HigConfiguredField(
                value =
                    rule.endTime,
                onValueChange = {
                    onRuleChanged(
                        rule.copy(
                            endTime =
                                it
                        )
                    )
                },
                label =
                    "End",
                placeholder =
                    "06:00",
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                modifier =
                    Modifier.weight(
                        1f
                    )
            )
        }

        if (
            ScheduleFormatting
                .isOvernight(
                    rule
                )
        ) {
            CupertinoText(
                text =
                    "This window continues into the following day.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.orange
            )
        }

        Column {
            CupertinoText(
                text =
                    "ACTION",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            SegmentedControl(
                options =
                    listOf(
                        "Block",
                        "Allow"
                    ),
                selectedOption =
                    if (
                        rule.action.equals(
                            "allow",
                            ignoreCase = true
                        )
                    ) {
                        "Allow"
                    } else {
                        "Block"
                    },
                onOptionSelected = {
                    onRuleChanged(
                        rule.copy(
                            action =
                                it.lowercase()
                        )
                    )
                },
                modifier =
                    Modifier.padding(
                        top = 7.dp
                    ),
                isDestructive =
                    rule.action.equals(
                        "block",
                        ignoreCase = true
                    )
            )
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            CupertinoText(
                text =
                    "OPTIONAL DATE RANGE",
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
                        10.dp
                    )
            ) {
                HigConfiguredField(
                    value =
                        rule.startDate
                            .orEmpty(),
                    onValueChange = {
                        onRuleChanged(
                            rule.copy(
                                startDate =
                                    it
                                        .ifEmpty {
                                            null
                                        }
                            )
                        )
                    },
                    label =
                        "Start Date",
                    placeholder =
                        "YYYY-MM-DD",
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                HigConfiguredField(
                    value =
                        rule.endDate
                            .orEmpty(),
                    onValueChange = {
                        onRuleChanged(
                            rule.copy(
                                endDate =
                                    it
                                        .ifEmpty {
                                            null
                                        }
                            )
                        )
                    },
                    label =
                        "End Date",
                    placeholder =
                        "YYYY-MM-DD",
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }
        }
    }
}

@Composable
private fun DayButton(
    day: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val label =
        when (day) {
            "mon" -> "M"
            "tue" -> "T"
            "wed" -> "W"
            "thu" -> "T"
            "fri" -> "F"
            "sat" -> "S"
            "sun" -> "S"
            else -> "?"
        }

    Box(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        9.dp
                    )
                )
                .background(
                    if (selected) {
                        LiasThemeColors.blue
                    } else {
                        LiasThemeColors.fill2
                    }
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 9.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        CupertinoText(
            text =
                label,
            style =
                HigTypography.subheadline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                if (selected) {
                    androidx.compose.ui.graphics.Color.White
                } else {
                    LiasThemeColors.secondaryLabel
                }
        )
    }
}
