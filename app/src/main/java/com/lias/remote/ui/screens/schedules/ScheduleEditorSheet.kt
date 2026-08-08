// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 8.0.0
//
// Purpose:
//   Complete Schedule editor.
//
// Corrections:
//   - Rules are actually editable.
//   - Multiple rules supported.
//   - Overnight windows supported.
//   - Client validation mirrors LIAS.
//   - Backend generates new IDs.
//   - IANA timezone defaults to Android system timezone.
//   - Mode semantics explained in context.
//   - Internal contradictory windows disable Save.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.core.util.ScheduleFormatting
import com.lias.remote.core.util.ScheduleValidation
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText
import java.time.ZoneId

@Composable
fun ScheduleEditorSheet(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val isExisting =
        initialSchedule
            ?.id
            ?.isNotBlank() == true

    var name by
        remember(
            initialSchedule
        ) {
            mutableStateOf(
                initialSchedule
                    ?.name
                    .orEmpty()
            )
        }

    var mode by
        remember(
            initialSchedule
        ) {
            mutableStateOf(
                initialSchedule
                    ?.mode
                    ?.lowercase()
                    ?.takeIf {
                        it ==
                            "downtime" ||
                            it ==
                            "whitelist"
                    }
                    ?: "downtime"
            )
        }

    var timezone by
        remember(
            initialSchedule
        ) {
            mutableStateOf(
                initialSchedule
                    ?.timezone
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: ZoneId
                        .systemDefault()
                        .id
            )
        }

    var rules by
        remember(
            initialSchedule
        ) {
            mutableStateOf(
                initialSchedule
                    ?.safeRules
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: listOf(
                        defaultRuleForMode(
                            "downtime"
                        )
                    )
            )
        }

    val candidate =
        Schedule(
            id =
                if (isExisting) {
                    initialSchedule
                        ?.id
                        .orEmpty()
                } else {
                    /*
                     * Empty ID is deliberate.
                     * LIAS CreateSchedule generates the canonical ID.
                     */
                    ""
                },
            name =
                name.trim(),
            mode =
                mode,
            timezone =
                timezone.trim(),
            rules =
                rules
        )

    val validation =
        ScheduleValidation.validate(
            candidate
        )

    HigModalSheet(
        onDismiss =
            onDismiss
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            HigSheetHeader(
                title =
                    when {
                        isExisting ->
                            "Edit Schedule"

                        initialSchedule !=
                            null ->
                            "Copy Schedule"

                        else ->
                            "New Schedule"
                    },
                onCancel =
                    onDismiss,
                trailingAction = {
                    HigButton(
                        text =
                            "Save",
                        onClick = {
                            if (
                                validation.isValid
                            ) {
                                onSave(
                                    candidate
                                )
                            }
                        },
                        enabled =
                            validation.isValid,
                        style =
                            HigButtonStyle.Primary
                    )
                }
            )

            HigField(
                value =
                    name,
                onValueChange = {
                    name = it
                },
                label =
                    "Schedule Name",
                placeholder =
                    "e.g. Bedtime"
            )

            Column {
                CupertinoText(
                    text =
                        "MODE",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel
                )

                SegmentedControl(
                    options =
                        listOf(
                            "Downtime",
                            "Whitelist"
                        ),
                    selectedOption =
                        if (
                            mode ==
                            "whitelist"
                        ) {
                            "Whitelist"
                        } else {
                            "Downtime"
                        },
                    onOptionSelected = { selected ->

                        val newMode =
                            selected.lowercase()

                        val oldDefaultAction =
                            if (
                                mode ==
                                "whitelist"
                            ) {
                                "allow"
                            } else {
                                "block"
                            }

                        val newDefaultAction =
                            if (
                                newMode ==
                                "whitelist"
                            ) {
                                "allow"
                            } else {
                                "block"
                            }

                        /*
                         * Only migrate rules that still match the old
                         * mode's default action. Explicit mixed-action
                         * rules remain untouched.
                         */
                        rules =
                            rules.map { rule ->
                                if (
                                    rule.action.equals(
                                        oldDefaultAction,
                                        ignoreCase = true
                                    )
                                ) {
                                    rule.copy(
                                        action =
                                            newDefaultAction
                                    )
                                } else {
                                    rule
                                }
                            }

                        mode =
                            newMode
                    },
                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        )
                )

                CupertinoText(
                    text =
                        ScheduleFormatting
                            .modeExplanation(
                                mode
                            ),
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.secondaryLabel,
                    modifier =
                        Modifier.padding(
                            top = 7.dp
                        )
                )
            }

            HigField(
                value =
                    timezone,
                onValueChange = {
                    timezone = it
                },
                label =
                    "Timezone",
                placeholder =
                    "America/Los_Angeles"
            )

            CupertinoText(
                text =
                    "Use an IANA timezone. Schedules attached to one policy must use the same timezone.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.secondaryLabel
            )

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                CupertinoText(
                    text =
                        "TIME WINDOWS",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel
                )

                rules.forEachIndexed { index, rule ->

                    ScheduleRuleEditor(
                        rule =
                            rule,
                        ruleNumber =
                            index + 1,
                        onRuleChanged = { updated ->

                            rules =
                                rules.toMutableList()
                                    .apply {
                                        set(
                                            index,
                                            updated
                                        )
                                    }
                        },
                        onDelete =
                            if (
                                rules.size >
                                1
                            ) {
                                {
                                    rules =
                                        rules
                                            .filterIndexed {
                                                ruleIndex,
                                                _ ->

                                                ruleIndex !=
                                                    index
                                            }
                                }
                            } else {
                                null
                            }
                    )
                }

                HigButton(
                    text =
                        "Add Time Window",
                    onClick = {
                        rules =
                            rules +
                                defaultRuleForMode(
                                    mode
                                )
                    },
                    style =
                        HigButtonStyle.Gray,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            if (
                validation.errors
                    .isNotEmpty()
            ) {
                ValidationCallout(
                    title =
                        "Fix Before Saving",
                    messages =
                        validation.errors,
                    isError =
                        true
                )
            }

            if (
                validation.conflicts
                    .isNotEmpty()
            ) {
                ValidationCallout(
                    title =
                        "Conflicting Windows",
                    messages =
                        validation.conflicts.map { conflict ->
                            "${conflict.day.replaceFirstChar { it.titlecase() }} ${conflict.overlapStart}–${conflict.overlapEnd}: ${conflict.actionA} conflicts with ${conflict.actionB}."
                        },
                    isError =
                        true
                )
            }

            if (
                validation.warnings
                    .isNotEmpty()
            ) {
                ValidationCallout(
                    title =
                        "Review",
                    messages =
                        validation.warnings,
                    isError =
                        false
                )
            }
        }
    }
}

@Composable
private fun ValidationCallout(
    title: String,
    messages: List<String>,
    isError: Boolean
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {
        CupertinoText(
            text =
                title,
            style =
                HigTypography.subheadline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                if (isError) {
                    LiasThemeColors.red
                } else {
                    LiasThemeColors.orange
                }
        )

        messages.forEach { message ->
            CupertinoText(
                text =
                    "• $message",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.secondaryLabel
            )
        }
    }
}

private fun defaultRuleForMode(
    mode: String
): ScheduleRule =
    ScheduleRule(
        days =
            listOf(
                "mon",
                "tue",
                "wed",
                "thu",
                "fri"
            ),
        startTime =
            if (
                mode ==
                "whitelist"
            ) {
                "15:00"
            } else {
                "22:00"
            },
        endTime =
            if (
                mode ==
                "whitelist"
            ) {
                "17:00"
            } else {
                "06:00"
            },
        action =
            if (
                mode ==
                "whitelist"
            ) {
                "allow"
            } else {
                "block"
            }
    )
