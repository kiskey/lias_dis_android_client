// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt
// Version: 28.4.0
//
// Purpose:
//   Complete LIAS policy editor.
//
// Workflow:
//   1. Who should this rule apply to?
//   2. What should happen?
//   3. When should it happen?
//
// Server validation:
//   POST /api/v1/policies/validate is authoritative before a
//   schedule-driven policy can be saved.
//
// UX:
//   - New Global policies are impossible.
//   - infrastructure is absent from selectable targets.
//   - Priority is shown only for device rules.
//   - Empty schedule bundle is permitted but clearly described as
//     unrestricted/default-open.
//   - Mixed timezones warn but do not fabricate a client-side failure.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.policy.PolicyDraft
import com.lias.remote.core.policy.PolicyPresentation
import com.lias.remote.core.policy.PolicySemantics
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetPresentation
import com.lias.remote.ui.components.rememberHigAnimatedDismiss
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoSwitch
import com.slapps.cupertino.CupertinoText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PolicyWizardSheet(
    initialPolicy: Policy?,
    tags: List<Tag>,
    devices: List<Device>,
    schedules: List<Schedule>,
    policies: List<Policy>,
    validateSchedules:
        suspend (List<String>) ->
            ApiResult<List<Conflict>>,
    onDismiss: () -> Unit,
    onSave: suspend (Policy) -> Boolean
) {

    val scope =
        rememberCoroutineScope()

    var step by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                1
            )
        }

    var draft by
        remember(
            initialPolicy
        ) {
            mutableStateOf(
                PolicyDraft.fromPolicy(
                    initialPolicy
                )
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

    var validationError by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var isValidating by
        remember {
            mutableStateOf(
                false
            )
        }

    val availableTags =
        remember(
            tags
        ) {
            PolicySemantics.availableTags(
                tags
            )
        }

    val availableDevices =
        remember(
            devices
        ) {
            PolicySemantics.availableDevices(
                devices
            )
        }

    val selectedSchedules =
        remember(
            draft.scheduleIds,
            schedules
        ) {
            PolicySemantics.selectedSchedules(
                draft.scheduleIds,
                schedules
            )
        }

    val localConflicts =
        remember(
            draft.scheduleIds,
            schedules
        ) {
            PolicySemantics.localConflicts(
                draft.scheduleIds,
                schedules
            )
        }

    val timezones =
        remember(
            draft.scheduleIds,
            schedules
        ) {
            PolicySemantics.selectedTimezones(
                draft.scheduleIds,
                schedules
            )
        }

    val semanticResult =
        PolicySemantics.validateDraft(
            draft =
                draft,
            initialPolicy =
                initialPolicy,
            tags =
                tags,
            devices =
                devices,
            schedules =
                schedules
        )

    val shadowWarning =
        PolicySemantics.shadowWarning(
            draft =
                draft,
            initialPolicy =
                initialPolicy,
            policies =
                policies
        )

    /*
     * Server validation is authoritative.
     *
     * Debounce slightly because schedule selection can be changed
     * repeatedly in quick succession.
     */
    LaunchedEffect(
        draft.action,
        draft.scheduleIds
    ) {

        serverConflicts =
            emptyList()

        validationError =
            null

        if (
            draft.action !=
            "schedule"
        ) {
            isValidating =
                false

            return@LaunchedEffect
        }

        /*
         * Empty schedule bundle is intentionally valid and evaluates
         * ALLOW according to LIAS. No server merge is necessary.
         */
        if (
            draft.scheduleIds
                .isEmpty()
        ) {
            isValidating =
                false

            return@LaunchedEffect
        }

        delay(
            200L
        )

        isValidating =
            true

        when (
            val result =
                validateSchedules(
                    draft.scheduleIds
                        .toList()
                )
        ) {

            is ApiResult.Success -> {

                serverConflicts =
                    result.data

                validationError =
                    null
            }

            is ApiResult.AuthenticationError -> {

                validationError =
                    PolicyPresentation
                        .serverValidationMessage(
                            result.message
                        )
            }

            is ApiResult.HttpError -> {

                validationError =
                    PolicyPresentation
                        .serverValidationMessage(
                            result.message
                        )
            }

            is ApiResult.ConflictError -> {

                serverConflicts =
                    result.conflicts

                validationError =
                    PolicyPresentation
                        .serverValidationMessage(
                            result.message
                        )
            }

            is ApiResult.NetworkError -> {

                validationError =
                    "Unable to validate this schedule bundle with LIAS."
            }

            is ApiResult.SerializationError -> {

                validationError =
                    "LIAS returned an invalid validation response."
            }
        }

        isValidating =
            false
    }

    fun selectType(
        newType: String
    ) {

        if (
            initialPolicy?.id ==
            PolicySemantics.GLOBAL_POLICY_ID
        ) {
            return
        }

        val target =
            when (
                newType
            ) {

                "tag" ->
                    availableTags
                        .firstOrNull()
                        ?.id
                        .orEmpty()

                "device" ->
                    availableDevices
                        .firstOrNull()
                        ?.pdid
                        .orEmpty()

                else ->
                    ""
            }

        draft =
            draft.copy(
                type =
                    newType,
                targetId =
                    target
            )
    }

    suspend fun save(
        animatedDismiss: () -> Unit
    ) {

        val currentValidation =
            PolicySemantics.validateDraft(
                draft,
                initialPolicy,
                tags,
                devices,
                schedules
            )

        if (
            !currentValidation.valid
        ) {
            validationError =
                currentValidation.error

            return
        }

        if (
            draft.action ==
                "schedule" &&
            (
                isValidating ||
                    validationError != null ||
                    localConflicts.isNotEmpty() ||
                    serverConflicts.isNotEmpty()
                )
        ) {
            return
        }

        val saved =
            onSave(
                draft.toPolicy(
                    initialPolicy
                )
            )

        if (
            saved
        ) {
            animatedDismiss()
        }
    }

    HigModalSheet(
        presentation =
            HigSheetPresentation.Editor,
        onDismiss =
            onDismiss
    ) {

        val animatedDismiss =
            rememberHigAnimatedDismiss(
                fallback =
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
                    when {

                        initialPolicy?.id ==
                            PolicySemantics
                                .GLOBAL_POLICY_ID ->
                            "Global Access"

                        initialPolicy == null ->
                            "New Access Rule"

                        else ->
                            "Edit Access Rule"
                    },
                onCancel =
                    onDismiss
            )

            CupertinoText(
                text =
                    when (
                        step
                    ) {
                        1 ->
                            "1 of 3 · Applies To"

                        2 ->
                            "2 of 3 · Access"

                        else ->
                            "3 of 3 · Schedules"
                    },
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            when (
                step
            ) {

                1 -> {

                    StepTarget(
                        draft =
                            draft,
                        initialPolicy =
                            initialPolicy,
                        availableTags =
                            availableTags,
                        availableDevices =
                            availableDevices,
                        shadowWarning =
                            shadowWarning?.message,
                        onNameChange = {

                            draft =
                                draft.copy(
                                    name =
                                        it
                                )
                        },
                        onTypeChange =
                            ::selectType,
                        onTargetChange = {

                            draft =
                                draft.copy(
                                    targetId =
                                        it
                                )
                        }
                    )

                    validationError
                        ?.let {

                            ErrorText(
                                it
                            )
                        }

                    HigButton(
                        text = "Continue",
                        onClick = {

                            val result =
                                PolicySemantics
                                    .validateDraft(
                                        draft,
                                        initialPolicy,
                                        tags,
                                        devices,
                                        schedules
                                    )

                            if (
                                result.valid
                            ) {

                                validationError =
                                    null

                                step =
                                    2

                            } else {

                                validationError =
                                    result.error
                            }
                        },
                        enabled =
                            draft.name
                                .trim()
                                .isNotBlank(),
                        style =
                            HigButtonStyle.Primary,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                2 -> {

                    StepEnforcement(
                        draft =
                            draft,
                        initialPolicy =
                            initialPolicy,
                        onActionChange = {
                            selected ->

                            draft =
                                draft.copy(
                                    action =
                                        selected,
                                    scheduleIds =
                                        if (
                                            selected ==
                                            "schedule"
                                        ) {
                                            draft.scheduleIds
                                        } else {
                                            emptySet()
                                        }
                                )
                        },
                        onPriorityChange = {

                            draft =
                                draft.copy(
                                    priorityText =
                                        it.filter {
                                                character ->
                                            character.isDigit() ||
                                                character ==
                                                '-'
                                        }
                                )
                        }
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        HigButton(
                            text = "Back",
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
                                    draft.action ==
                                    "schedule"
                                ) {
                                    "Continue"
                                } else {
                                    "Save Rule"
                                },
                            onClick = {

                                if (
                                    draft.action ==
                                    "schedule"
                                ) {
                                    step =
                                        3
                                } else {
                                    scope.launch {
                                        save(
                                            animatedDismiss
                                        )
                                    }
                                }
                            },
                            style =
                                HigButtonStyle.Primary,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }

                3 -> {

                    StepSchedules(
                        draft =
                            draft,
                        schedules =
                            schedules,
                        selectedSchedules =
                            selectedSchedules,
                        localConflicts =
                            localConflicts,
                        serverConflicts =
                            serverConflicts,
                        timezones =
                            timezones,
                        isValidating =
                            isValidating,
                        validationError =
                            validationError,
                        onToggleSchedule = {
                            scheduleId ->

                            val updated =
                                draft.scheduleIds
                                    .toMutableSet()

                            if (
                                !updated.add(
                                    scheduleId
                                )
                            ) {
                                updated.remove(
                                    scheduleId
                                )
                            }

                            draft =
                                draft.copy(
                                    scheduleIds =
                                        updated
                                )
                        }
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        HigButton(
                            text = "Back",
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
                                when {
                                    isValidating ->
                                        "Validating…"

                                    serverConflicts
                                        .isNotEmpty() ||
                                        localConflicts
                                            .isNotEmpty() ->
                                        "Resolve Conflicts"

                                    else ->
                                        "Save Rule"
                                },
                            onClick = {
                                scope.launch {
                                    save(
                                        animatedDismiss
                                    )
                                }
                            },
                            enabled =
                                !isValidating &&
                                    validationError == null &&
                                    localConflicts
                                        .isEmpty() &&
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

@Composable
private fun StepTarget(
    draft: PolicyDraft,
    initialPolicy: Policy?,
    availableTags: List<Tag>,
    availableDevices: List<Device>,
    shadowWarning: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onTargetChange: (String) -> Unit
) {

    HigConfiguredField(
        value =
            draft.name,
        onValueChange =
            onNameChange,
        label =
            "Rule Name",
        placeholder =
            "e.g. Kids Bedtime",
        keyboardOptions =
            KeyboardOptions(
                imeAction = ImeAction.Next
            )
    )

    if (
        initialPolicy?.id ==
        PolicySemantics.GLOBAL_POLICY_ID
    ) {

        CupertinoText(
            text =
                "Global Access applies to every non-infrastructure device before ordinary device and tag rules.",
            style =
                HigTypography.subheadline,
            color =
                LiasThemeColors.secondaryLabel
        )

    } else {

        CupertinoText(
            text = "APPLIES TO",
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
                    draft.type ==
                    "device"
                ) {
                    "Device"
                } else {
                    "Tag"
                },
            onOptionSelected = {
                option ->

                onTypeChange(
                    option.lowercase()
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        if (
            draft.type ==
            "tag"
        ) {

            CupertinoText(
                text =
                    "TAG GROUP",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            GroupedListCard {

                availableTags
                    .forEachIndexed {
                            index,
                            tag ->

                        GroupedListRow(
                            primaryText =
                                tag.name,
                            secondaryText =
                                if (
                                    tag.builtin
                                ) {
                                    "Built-in group"
                                } else {
                                    "Custom group"
                                },
                            trailingContent = {

                                CupertinoSwitch(
                                    checked =
                                        draft.targetId ==
                                            tag.id,
                                    onCheckedChange = {

                                        onTargetChange(
                                            tag.id
                                        )
                                    }
                                )
                            },
                            showDivider =
                                index <
                                    availableTags
                                        .lastIndex,
                            onClick = {

                                onTargetChange(
                                    tag.id
                                )
                            }
                        )
                    }
            }

            if (
                availableTags
                    .isEmpty()
            ) {

                CupertinoText(
                    text =
                        "No eligible tag groups are available.",
                    style =
                        HigTypography.subheadline,
                    color =
                        LiasThemeColors.orange
                )
            }

        } else {

            CupertinoText(
                text =
                    "DEVICE",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            GroupedListCard {

                availableDevices
                    .forEachIndexed {
                            index,
                            device ->

                        GroupedListRow(
                            primaryText =
                                device.displayName,
                            secondaryText =
                                device.currentIP.ifBlank {
                                    device.pdid
                                },
                            trailingContent = {

                                CupertinoSwitch(
                                    checked =
                                        draft.targetId ==
                                            device.pdid,
                                    onCheckedChange = {

                                        onTargetChange(
                                            device.pdid
                                        )
                                    }
                                )
                            },
                            showDivider =
                                index <
                                    availableDevices
                                        .lastIndex,
                            onClick = {

                                onTargetChange(
                                    device.pdid
                                )
                            }
                        )
                    }
            }

            if (
                availableDevices
                    .isEmpty()
            ) {

                CupertinoText(
                    text =
                        "No eligible non-infrastructure devices are available.",
                    style =
                        HigTypography.subheadline,
                    color =
                        LiasThemeColors.orange
                )
            }
        }

        CupertinoText(
            text =
                "Infrastructure devices are always online and are intentionally excluded.",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.tertiaryLabel
        )
    }

    shadowWarning
        ?.let {

            WarningText(
                title =
                    "Another Rule Uses This Target",
                text =
                    it
            )
        }
}

@Composable
private fun StepEnforcement(
    draft: PolicyDraft,
    initialPolicy: Policy?,
    onActionChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit
) {

    CupertinoText(
        text =
            "ACCESS BEHAVIOR",
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
            draft.action
                .replaceFirstChar {
                    it.uppercase()
                },
        onOptionSelected = {
            option ->

            onActionChange(
                option.lowercase()
            )
        },
        isDestructive =
            true,
        modifier =
            Modifier.fillMaxWidth()
    )

    CupertinoText(
        text =
            PolicySemantics
                .actionExplanation(
                    draft.action,
                    draft.type
                ),
        style =
            HigTypography.subheadline,
        color =
            LiasThemeColors.secondaryLabel
    )

    if (
        initialPolicy?.id ==
        PolicySemantics.GLOBAL_POLICY_ID
    ) {

        when (
            draft.action
        ) {

            "allow" ->

                WarningText(
                    title =
                        "Global Override",
                    text =
                        "Allow bypasses all ordinary device, tag, and schedule restrictions for non-infrastructure devices."
                )

            "block" ->

                WarningText(
                    title =
                        "Block All",
                    text =
                        "Every non-infrastructure device will be blocked immediately."
                )
        }
    }

    if (
        draft.type ==
        "device"
    ) {

        HigConfiguredField(
            value =
                draft.priorityText,
            onValueChange =
                onPriorityChange,
            label =
                "Priority",
            placeholder =
                "50",
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
        )
    }

    CupertinoText(
        text =
            PolicySemantics
                .priorityExplanation(
                    draft.type
                ),
        style =
            HigTypography.caption,
        color =
            LiasThemeColors.tertiaryLabel
    )
}

@Composable
private fun StepSchedules(
    draft: PolicyDraft,
    schedules: List<Schedule>,
    selectedSchedules: List<Schedule>,
    localConflicts: List<Conflict>,
    serverConflicts: List<Conflict>,
    timezones: List<String>,
    isValidating: Boolean,
    validationError: String?,
    onToggleSchedule: (String) -> Unit
) {

    CupertinoText(
        text =
            "Choose one or more reusable schedules. LIAS combines them into one effective schedule bundle and validates weekly and calendar-date conflicts.",
        style =
            HigTypography.subheadline,
        color =
            LiasThemeColors.secondaryLabel
    )

    if (
        schedules.isEmpty()
    ) {

        WarningText(
            title =
                "No Schedules Exist",
            text =
                "You may still save this rule, but an empty Schedule rule evaluates to Allow because LIAS intentionally defaults open when no schedules are attached."
        )

    } else {

        GroupedListCard {

            schedules
                .sortedBy {
                    it.name.lowercase()
                }
                .forEachIndexed {
                        index,
                        schedule ->

                    GroupedListRow(
                        primaryText =
                            schedule.name,
                        secondaryText =
                            PolicyPresentation
                                .scheduleSubtitle(
                                    schedule
                                ),
                        trailingContent = {

                            CupertinoSwitch(
                                checked =
                                    draft.scheduleIds
                                        .contains(
                                            schedule.id
                                        ),
                                onCheckedChange = {

                                    onToggleSchedule(
                                        schedule.id
                                    )
                                }
                            )
                        },
                        showDivider =
                            index <
                                schedules.lastIndex,
                        onClick = {

                            onToggleSchedule(
                                schedule.id
                            )
                        }
                    )
                }
        }
    }

    if (
        draft.scheduleIds
            .isEmpty()
    ) {

        WarningText(
            title =
                "No Schedule Selected",
            text =
                "This is valid, but the rule will evaluate to Allow for its target. Add a schedule if you intend to restrict access."
        )
    }

    if (
        timezones.size >
        1
    ) {

        WarningText(
            title =
                "LIAS Cannot Merge Mixed Timezones",
            text =
                "LIAS 2.0 rejects schedule bundles whose schedules use different timezones. Selected: ${timezones.joinToString()}. Change them to one timezone before saving."
        )
    }

    if (
        localConflicts
            .isNotEmpty()
    ) {

        ConflictList(
            title =
                "Schedule Contradiction",
            conflicts =
                localConflicts
        )
    }

    if (
        serverConflicts
            .isNotEmpty()
    ) {

        ConflictList(
            title =
                "LIAS Rejected This Bundle",
            conflicts =
                serverConflicts
        )

        CupertinoText(
            text =
                "LIAS does not silently choose between contradictory Allow and Block windows. A conflicted bundle fails closed to Block until the overlap is resolved.",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.red
        )
    }

    when {

        isValidating -> {

            CupertinoText(
                text =
                    "Checking schedule compatibility with LIAS…",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.secondaryLabel
            )
        }

        validationError !=
            null -> {

            ErrorText(
                validationError
            )
        }

        selectedSchedules
            .isNotEmpty() &&
            localConflicts
                .isEmpty() &&
            serverConflicts
                .isEmpty() -> {

            CupertinoText(
                text =
                    "${selectedSchedules.size} ${
                        if (
                            selectedSchedules.size ==
                            1
                        ) {
                            "schedule"
                        } else {
                            "schedules"
                        }
                    } selected · no contradictions reported",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.green
            )
        }
    }
}

@Composable
private fun ConflictList(
    title: String,
    conflicts: List<Conflict>
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                6.dp
            )
    ) {

        CupertinoText(
            text =
                title,
            style =
                HigTypography.headline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                LiasThemeColors.red
        )

        conflicts
            .distinctBy {
                listOf(
                    it.scheduleAID,
                    it.scheduleBID,
                    it.day,
                    it.overlapStart,
                    it.overlapEnd
                )
                    .joinToString(
                        "|"
                    )
            }
            .forEach {
                conflict ->

                CupertinoText(
                    text =
                        "• ${
                            PolicyPresentation
                                .conflictSummary(
                                    conflict
                                )
                        }",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.secondaryLabel
                )
            }
    }
}

@Composable
private fun WarningText(
    title: String,
    text: String
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                3.dp
            )
    ) {

        CupertinoText(
            text =
                title,
            style =
                HigTypography.headline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                LiasThemeColors.orange
        )

        CupertinoText(
            text =
                text,
            style =
                HigTypography.subheadline,
            color =
                LiasThemeColors.secondaryLabel
        )
    }
}

@Composable
private fun ErrorText(
    text: String
) {

    CupertinoText(
        text =
            text,
        style =
            HigTypography.subheadline,
        color =
            LiasThemeColors.red
    )
}
