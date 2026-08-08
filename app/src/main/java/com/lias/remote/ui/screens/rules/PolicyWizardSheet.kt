// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt
// Version: 9.0.0
//
// Purpose:
//   Complete LIAS policy wizard.
//
// Steps:
//   1. Target
//   2. Enforcement
//   3. Schedules + validation
//
// Corrections:
//   - Real target selection.
//   - Real multi-schedule selection.
//   - New policies use id="" so LIAS generates the canonical ID.
//   - Arbitrary new global policies are prohibited.
//   - infrastructure is not selectable.
//   - Priority language reflects actual device-policy semantics.
//   - Empty schedule bundle warning is explicit.
//   - Local projection is validated before server preflight.
//   - /policies/validate is called before schedule-driven save.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.util.PolicyValidation
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText
import kotlinx.coroutines.launch

@Composable
fun PolicyWizardSheet(
    initialPolicy: Policy?,
    tags: List<Tag>,
    devices: List<Device>,
    schedules: List<Schedule>,
    existingPolicies: List<Policy>,
    onValidateSchedules:
        suspend (List<String>) ->
        ApiResult<List<Conflict>>,
    onDismiss: () -> Unit,
    onSave: (Policy) -> Unit
) {
    val coroutineScope =
        rememberCoroutineScope()

    val isGlobal =
        initialPolicy?.id ==
            "global_default"

    var step by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                1
            )
        }

    var name by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                initialPolicy
                    ?.name
                    ?: ""
            )
        }

    var type by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                if (isGlobal) {
                    "global"
                } else {
                    initialPolicy
                        ?.type
                        ?.takeIf {
                            it ==
                                "tag" ||
                                it ==
                                "device"
                        }
                        ?: "tag"
                }
            )
        }

    var targetId by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                initialPolicy
                    ?.targetID
                    ?: ""
            )
        }

    var action by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                initialPolicy
                    ?.action
                    ?: "schedule"
            )
        }

    var priorityText by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                (
                    initialPolicy
                        ?.priority
                        ?: if (isGlobal) {
                            0
                        } else {
                            50
                        }
                    )
                    .toString()
            )
        }

    var selectedScheduleIds by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                initialPolicy
                    ?.resolveScheduleIDs()
                    ?.distinct()
                    ?: emptyList()
            )
        }

    var isValidatingServer by
        remember {
            mutableStateOf(
                false
            )
        }

    var serverConflicts by
        remember {
            mutableStateOf<
                List<Conflict>
            >(
                emptyList()
            )
        }

    var serverError by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    val priority =
        priorityText
            .toIntOrNull()
            ?: 50

    val candidate =
        Policy(
            id =
                initialPolicy
                    ?.id
                    ?: "",
            name =
                name.trim(),
            type =
                if (isGlobal) {
                    "global"
                } else {
                    type
                },
            targetID =
                if (isGlobal) {
                    ""
                } else {
                    targetId
                },
            action =
                action,
            scheduleIDs =
                if (
                    action ==
                    "schedule"
                ) {
                    selectedScheduleIds
                        .distinct()
                } else {
                    emptyList()
                },
            scheduleID =
                null,
            priority =
                if (isGlobal) {
                    0
                } else {
                    priority
                },
            enabled =
                if (isGlobal) {
                    true
                } else {
                    initialPolicy
                        ?.enabled
                        ?: true
                },
            createdAt =
                initialPolicy
                    ?.createdAt
                    ?: "",
            updatedAt =
                initialPolicy
                    ?.updatedAt
                    ?: "",
            expiresAt =
                initialPolicy
                    ?.expiresAt,
            reasonTag =
                initialPolicy
                    ?.reasonTag
        )

    val validation =
        PolicyValidation.validate(
            policy =
                candidate,
            schedules =
                schedules,
            tags =
                tags,
            devices =
                devices,
            existingPolicies =
                existingPolicies
        )

    fun submit() {

        if (
            !validation.isValid ||
            isValidatingServer
        ) {
            return
        }

        serverConflicts =
            emptyList()

        serverError =
            null

        if (
            candidate.action !=
                "schedule" ||
            candidate
                .resolveScheduleIDs()
                .isEmpty()
        ) {
            onSave(
                candidate
            )

            return
        }

        coroutineScope.launch {

            isValidatingServer =
                true

            when (
                val result =
                    onValidateSchedules(
                        candidate
                            .resolveScheduleIDs()
                    )
            ) {

                is ApiResult.Success -> {

                    if (
                        result.data
                            .isEmpty()
                    ) {
                        onSave(
                            candidate
                        )
                    } else {
                        serverConflicts =
                            result.data
                    }
                }

                is ApiResult.AuthenticationError -> {
                    serverError =
                        result.message
                }

                is ApiResult.HttpError -> {
                    serverError =
                        result.message
                }

                is ApiResult.ConflictError -> {
                    serverConflicts =
                        result.conflicts

                    serverError =
                        result.message
                }

                is ApiResult.NetworkError -> {
                    serverError =
                        result.cause
                            .message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Unable to validate the rule with LIAS."
                }

                is ApiResult.SerializationError -> {
                    serverError =
                        "LIAS returned an invalid validation response."
                }
            }

            isValidatingServer =
                false
        }
    }

    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            24.dp,
                        vertical =
                            16.dp
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
                        initialPolicy ==
                        null
                    ) {
                        "New Rule"
                    } else if (
                        isGlobal
                    ) {
                        "Global Access"
                    } else {
                        "Edit Rule"
                    },
                onCancel =
                    onDismiss
            )

            CupertinoText(
                text =
                    "Step $step of ${if (action == "schedule") 3 else 2}",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            when (step) {

                // ====================================================
                // STEP 1 — TARGET
                // ====================================================

                1 -> {

                    HigField(
                        value =
                            name,
                        onValueChange = {
                            name = it
                        },
                        label =
                            "Rule Name",
                        placeholder =
                            if (isGlobal) {
                                "Global Access Switch"
                            } else {
                                "e.g. Kids Internet Rules"
                            }
                    )

                    if (isGlobal) {

                        CupertinoText(
                            text =
                                "GLOBAL SCOPE",
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.tertiaryLabel
                        )

                        PolicyTargetSelector(
                            type =
                                "global",
                            selectedTargetId =
                                "",
                            tags =
                                tags,
                            devices =
                                devices,
                            onTargetSelected = {}
                        )

                    } else {

                        Column {
                            CupertinoText(
                                text =
                                    "TARGET SCOPE",
                                style =
                                    HigTypography.caption,
                                color =
                                    LiasThemeColors.tertiaryLabel
                            )

                            SegmentedControl(
                                options =
                                    listOf(
                                        "Tag",
                                        "Device"
                                    ),
                                selectedOption =
                                    if (
                                        type ==
                                        "device"
                                    ) {
                                        "Device"
                                    } else {
                                        "Tag"
                                    },
                                onOptionSelected = {
                                    val newType =
                                        it.lowercase()

                                    if (
                                        newType !=
                                        type
                                    ) {
                                        type =
                                            newType

                                        targetId =
                                            ""
                                    }
                                },
                                modifier =
                                    Modifier.padding(
                                        top = 8.dp
                                    )
                            )
                        }

                        PolicyTargetSelector(
                            type =
                                type,
                            selectedTargetId =
                                targetId,
                            tags =
                                tags,
                            devices =
                                devices,
                            onTargetSelected = {
                                targetId =
                                    it
                            }
                        )
                    }

                    validation.warnings
                        .filter {
                            it.contains(
                                "already has",
                                ignoreCase =
                                    true
                            )
                        }
                        .forEach {
                            PolicyCallout(
                                title =
                                    "Precedence",
                                text =
                                    it,
                                error =
                                    false
                            )
                        }

                    HigButton(
                        text =
                            "Next",
                        onClick = {
                            step =
                                2

                            serverError =
                                null
                        },
                        enabled =
                            name.trim()
                                .isNotBlank() &&
                                (
                                    isGlobal ||
                                        targetId
                                            .isNotBlank()
                                    ),
                        style =
                            HigButtonStyle.Primary,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                // ====================================================
                // STEP 2 — ENFORCEMENT
                // ====================================================

                2 -> {

                    Column {
                        CupertinoText(
                            text =
                                "ENFORCEMENT ACTION",
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.tertiaryLabel
                        )

                        SegmentedControl(
                            options =
                                listOf(
                                    "Allow",
                                    "Schedule",
                                    "Block"
                                ),
                            selectedOption =
                                action
                                    .replaceFirstChar {
                                        it.uppercase()
                                    },
                            onOptionSelected = {
                                action =
                                    it.lowercase()

                                serverError =
                                    null

                                serverConflicts =
                                    emptyList()
                            },
                            isDestructive =
                                action ==
                                    "block",
                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }

                    PolicyCallout(
                        title =
                            when (action) {
                                "allow" ->
                                    "Allow"

                                "block" ->
                                    "Block"

                                else ->
                                    "Schedule-Driven"
                            },
                        text =
                            PolicyValidation
                                .actionExplanation(
                                    candidate
                                ),
                        error =
                            false
                    )

                    if (!isGlobal) {

                        HigField(
                            value =
                                priorityText,
                            onValueChange = { value ->

                                priorityText =
                                    value.filterIndexed { index, character ->

                                        character.isDigit() ||
                                            (
                                                character ==
                                                    '-' &&
                                                index ==
                                                    0
                                                )
                                    }
                            },
                            label =
                                "Priority",
                            placeholder =
                                "50"
                        )

                        CupertinoText(
                            text =
                                if (
                                    type ==
                                    "device"
                                ) {
                                    "For multiple rules targeting the same device, the highest priority wins."
                                } else {
                                    "Tag policies combine across matching tags. A blocking tag rule takes precedence over allowing tag rules."
                                },
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.secondaryLabel
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
                                "Back",
                            onClick = {
                                step =
                                    1
                            },
                            style =
                                HigButtonStyle.Gray,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                        HigButton(
                            text =
                                if (
                                    action ==
                                    "schedule"
                                ) {
                                    "Next"
                                } else {
                                    if (
                                        isValidatingServer
                                    ) {
                                        "Checking…"
                                    } else {
                                        "Save Rule"
                                    }
                                },
                            onClick = {
                                if (
                                    action ==
                                    "schedule"
                                ) {
                                    step =
                                        3
                                } else {
                                    submit()
                                }
                            },
                            enabled =
                                validation.isValid &&
                                    !isValidatingServer,
                            style =
                                if (
                                    action ==
                                    "block"
                                ) {
                                    HigButtonStyle.Danger
                                } else {
                                    HigButtonStyle.Primary
                                },
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }

                // ====================================================
                // STEP 3 — SCHEDULES
                // ====================================================

                3 -> {

                    CupertinoText(
                        text =
                            "ATTACH SCHEDULES",
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )

                    CupertinoText(
                        text =
                            "Select one or more reusable schedules. LIAS combines them into one effective weekly timeline.",
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors.secondaryLabel
                    )

                    PolicyScheduleSelector(
                        schedules =
                            schedules,
                        selectedScheduleIds =
                            selectedScheduleIds,
                        onSelectionChanged = {
                            selectedScheduleIds =
                                it

                            serverConflicts =
                                emptyList()

                            serverError =
                                null
                        }
                    )

                    if (
                        validation.errors
                            .isNotEmpty()
                    ) {
                        validation.errors
                            .forEach { message ->
                                PolicyCallout(
                                    title =
                                        "Cannot Save",
                                    text =
                                        message,
                                    error =
                                        true
                                )
                            }
                    }

                    validation.warnings
                        .filterNot {
                            it.contains(
                                "already has",
                                ignoreCase =
                                    true
                            )
                        }
                        .forEach {
                            PolicyCallout(
                                title =
                                    "Review",
                                text =
                                    it,
                                error =
                                    false
                            )
                        }

                    if (
                        serverConflicts
                            .isNotEmpty()
                    ) {
                        PolicyCallout(
                            title =
                                "LIAS Validation Failed",
                            text =
                                "${serverConflicts.size} contradictory ${if (serverConflicts.size == 1) "schedule window was" else "schedule windows were"} reported by the server.",
                            error =
                                true
                        )

                        serverConflicts
                            .forEach { conflict ->

                                CupertinoText(
                                    text =
                                        "• ${conflict.day.replaceFirstChar { it.titlecase() }} ${conflict.overlapStart}–${conflict.overlapEnd}: ${conflict.scheduleAName} (${conflict.actionA}) ↔ ${conflict.scheduleBName} (${conflict.actionB})",
                                    style =
                                        HigTypography.caption,
                                    color =
                                        LiasThemeColors.red
                                )
                            }
                    }

                    serverError?.let {
                        PolicyCallout(
                            title =
                                "Validation Error",
                            text =
                                it,
                            error =
                                true
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
                                "Back",
                            onClick = {
                                step =
                                    2
                            },
                            style =
                                HigButtonStyle.Gray,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                        HigButton(
                            text =
                                if (
                                    isValidatingServer
                                ) {
                                    "Checking LIAS…"
                                } else {
                                    "Save Rule"
                                },
                            onClick = {
                                submit()
                            },
                            enabled =
                                validation.isValid &&
                                    !isValidatingServer &&
                                    serverConflicts
                                        .isEmpty(),
                            style =
                                HigButtonStyle.Primary,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }
            }
        }
    }
}
