// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 18.0.0
//
// Purpose:
//   Full LIAS schedule editor.
//
// Features:
//   - server-owned schedule IDs
//   - multiple editable windows
//   - recurring weekdays
//   - calendar date ranges
//   - overnight handling
//   - Downtime / Allowed Hours semantics
//   - timezone validation
//   - common timezone shortcuts
//   - live preview
//   - local recurring conflict inspection
//
// Note:
//   Actual persistence remains server-authoritative. LIAS performs its
//   own timezone/rule/conflict validation during POST/PUT.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.schedule.ScheduleDraft
import com.lias.remote.core.schedule.ScheduleRuleDraft
import com.lias.remote.core.schedule.ScheduleSemantics
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetPresentation
import com.lias.remote.ui.components.rememberHigAnimatedCompletion
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun ScheduleEditorSheet(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {

    /*
     * A copied schedule arrives with id="" from SchedulesScreen.
     * That is intentionally treated exactly like a new server object.
     */
    val isNew =
        initialSchedule ==
            null ||
            initialSchedule.id
                .isBlank()

    var draft by
        remember(
            initialSchedule
        ) {
            mutableStateOf(
                ScheduleDraft
                    .fromSchedule(
                        initialSchedule
                    )
            )
        }

    var attemptedSave by
        remember {
            mutableStateOf(
                false
            )
        }

    val validation =
        ScheduleSemantics
            .validate(
                draft
            )

    val wirePreview =
        remember(
            draft,
            initialSchedule
        ) {
            draft.toSchedule(
                initialSchedule
            )
        }

    val recurringConflicts =
        remember(
            wirePreview
        ) {
            ScheduleSemantics
                .recurringConflicts(
                    wirePreview
                )
        }

    val canSave =
        validation.valid &&
            recurringConflicts
                .isEmpty()

    fun updateRule(
        index: Int,
        updated: ScheduleRuleDraft
    ) {

        val rules =
            draft.rules
                .toMutableList()

        if (
            index !in
            rules.indices
        ) {
            return
        }

        rules[
            index
        ] =
            updated

        draft =
            draft.copy(
                rules =
                    rules
            )
    }

    HigModalSheet(
        presentation =
            HigSheetPresentation.Editor,
        onDismiss =
            onDismiss
    ) {

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

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
                    if (
                        isNew
                    ) {
                        "New Schedule"
                    } else {
                        "Edit Schedule"
                    },
                onCancel =
                    onDismiss,
                trailingAction = {

                    HigTextButton(
                        text =
                            "Save",
                        onClick = {

                            attemptedSave =
                                true

                            if (
                                canSave
                            ) {

                                val schedule =
                                    draft.toSchedule(
                                        initialSchedule
                                    )

                                animatedComplete {
                                    onSave(
                                        schedule
                                    )
                                }
                            }
                        }
                    )
                }
            )

            HigConfiguredField(
                value =
                    draft.name,
                onValueChange = {

                    draft =
                        draft.copy(
                            name =
                                it
                        )
                },
                label =
                    "Schedule Name",
                placeholder =
                    "e.g. Bedtime",
                keyboardOptions =
                    KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
            )

            CupertinoText(
                text =
                    "BEHAVIOR",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            SegmentedControl(
                options =
                    listOf(
                        "Downtime",
                        "Allowed Hours"
                    ),
                selectedOption =
                    if (
                        ScheduleSemantics
                            .normalizeMode(
                                draft.mode
                            ) ==
                        "whitelist"
                    ) {
                        "Allowed Hours"
                    } else {
                        "Downtime"
                    },
                onOptionSelected = {
                    selection ->

                    draft =
                        draft.withMode(
                            if (
                                selection ==
                                "Allowed Hours"
                            ) {
                                "whitelist"
                            } else {
                                "downtime"
                            }
                        )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            CupertinoText(
                text =
                    ScheduleSemantics
                        .modeExplanation(
                            draft.mode
                        ),
                style =
                    HigTypography.subheadline,
                color =
                    LiasThemeColors.secondaryLabel
            )

            if (
                ScheduleSemantics
                    .normalizeMode(
                        draft.mode
                    ) ==
                "whitelist"
            ) {

                CupertinoText(
                    text =
                        "Be careful: Allowed Hours blocks Internet at every time that is not covered by a selected window.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.orange
                )
            }

            CupertinoText(
                text =
                    "TIMEZONE",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            HigConfiguredField(
                value =
                    draft.timezone,
                onValueChange = {

                    draft =
                        draft.copy(
                            timezone =
                                it
                        )
                },
                label =
                    "IANA Timezone",
                placeholder =
                    "America/Los_Angeles",
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    )
            )

            CupertinoText(
                text =
                    if (
                        ScheduleSemantics
                            .validTimezone(
                                draft.timezone
                            )
                    ) {
                        "Valid timezone"
                    } else {
                        "Use an IANA timezone such as America/Los_Angeles."
                    },
                style =
                    HigTypography.caption,
                color =
                    if (
                        ScheduleSemantics
                            .validTimezone(
                                draft.timezone
                            )
                    ) {
                        LiasThemeColors.green
                    } else {
                        LiasThemeColors.red
                    }
            )

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                ScheduleSemantics
                    .commonTimezones
                    .chunked(
                        2
                    )
                    .forEach {
                            rowTimezones ->

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {

                            rowTimezones.forEach {
                                    timezone ->

                                HigButton(
                                    text =
                                        shortTimezoneLabel(
                                            timezone
                                        ),
                                    onClick = {

                                        draft =
                                            draft.copy(
                                                timezone =
                                                    timezone
                                            )
                                    },
                                    style =
                                        if (
                                            draft.timezone ==
                                            timezone
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

                            if (
                                rowTimezones.size ==
                                1
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                )
                            }
                        }
                    }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    CupertinoText(
                        text =
                            "TIME WINDOWS",
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )

                    CupertinoText(
                        text =
                            "${draft.rules.size} ${
                                if (
                                    draft.rules.size ==
                                    1
                                ) {
                                    "window"
                                } else {
                                    "windows"
                                }
                            }",
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors.secondaryLabel
                    )
                }

                HigTextButton(
                    text =
                        "＋ Add Window",
                    onClick = {

                        draft =
                            draft.copy(
                                rules =
                                    draft.rules +
                                        ScheduleDraft
                                            .defaultRuleForMode(
                                                draft.mode
                                            )
                            )
                    }
                )
            }

            draft.rules
                .forEachIndexed {
                        index,
                        rule ->

                    ScheduleRuleEditorCard(
                        index =
                            index,
                        rule =
                            rule,
                        scheduleMode =
                            draft.mode,
                        canDelete =
                            draft.rules.size >
                                1,
                        onChange = {
                            updated ->

                            updateRule(
                                index,
                                updated
                            )
                        },
                        onDelete = {

                            val updated =
                                draft.rules
                                    .toMutableList()

                            if (
                                updated.size >
                                1 &&
                                index in
                                updated.indices
                            ) {

                                updated.removeAt(
                                    index
                                )

                                draft =
                                    draft.copy(
                                        rules =
                                            updated
                                    )
                            }
                        }
                    )
                }

            SchedulePreviewCard(
                draft =
                    draft
            )

            if (
                recurringConflicts
                    .isNotEmpty()
            ) {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    CupertinoText(
                        text =
                            "Contradictory Windows",
                        style =
                            HigTypography.headline,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            LiasThemeColors.red
                    )

                    recurringConflicts
                        .forEach {
                            conflict ->

                            CupertinoText(
                                text =
                                    "• ${
                                        conflict.scheduleAName
                                    } ${
                                        conflict.actionA.uppercase()
                                    } vs ${
                                        conflict.actionB.uppercase()
                                    } · ${
                                        conflict.day
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    } ${
                                        conflict.overlapStart
                                    }–${
                                        conflict.overlapEnd
                                    }",
                                style =
                                    HigTypography.caption,
                                color =
                                    LiasThemeColors.secondaryLabel
                            )
                        }
                }
            }

            if (
                attemptedSave &&
                !validation.valid
            ) {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    CupertinoText(
                        text =
                            "Fix Before Saving",
                        style =
                            HigTypography.headline,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            LiasThemeColors.red
                    )

                    validation.issues
                        .distinctBy {
                            it.message
                        }
                        .forEach {
                            issue ->

                            CupertinoText(
                                text =
                                    "• ${issue.message}",
                                style =
                                    HigTypography.caption,
                                color =
                                    LiasThemeColors.secondaryLabel
                            )
                        }
                }
            }

            HigButton(
                text =
                    when {

                        !validation.valid ->
                            "Review Schedule"

                        recurringConflicts
                            .isNotEmpty() ->
                            "Resolve Conflicts"

                        isNew ->
                            "Create Schedule"

                        else ->
                            "Save Changes"
                    },
                onClick = {

                    attemptedSave =
                        true

                    if (
                        canSave
                    ) {

                        val schedule =
                            draft.toSchedule(
                                initialSchedule
                            )

                        animatedComplete {
                            onSave(
                                schedule
                            )
                        }
                    }
                },
                enabled =
                    canSave,
                style =
                    HigButtonStyle.Primary,
                modifier =
                    Modifier.fillMaxWidth()
            )

            CupertinoText(
                text =
                    if (
                        isNew
                    ) {
                        "LIAS will assign the schedule ID after creation."
                    } else {
                        "LIAS will validate and persist these changes before they become authoritative."
                    },
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )
        }
    }
}

private fun shortTimezoneLabel(
    timezone: String
): String {

    if (
        timezone ==
        "UTC"
    ) {
        return "UTC"
    }

    val city =
        timezone
            .substringAfterLast(
                '/'
            )
            .replace(
                '_',
                ' '
            )

    return city
        .take(
            18
        )
}
