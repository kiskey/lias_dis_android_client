// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt
// Version: 9.0.0
//
// Purpose:
//   Policy management UI.
//
// Corrections:
//   - Global default treated as unique.
//   - Temporary pause/extend policies are read-only system overrides.
//   - New arbitrary global policies cannot be created.
//   - Real target names displayed.
//   - Empty schedule bundle warning shown.
//   - Rule enable/disable remains unavailable for temporary policies.
//   - infrastructure policies are never user-editable.
//   - Loading / empty / stale / failure states supported.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.core.util.PolicyValidation
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.ScreenStateTone
import com.lias.remote.ui.components.ScreenStateView
import com.lias.remote.ui.components.StaleDataNotice
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Pencil
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash

@Composable
fun RulesScreen(
    viewModel: LiasViewModel
) {
    val state by
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    var showWizard by
        remember {
            mutableStateOf(
                false
            )
        }

    var editingPolicy by
        remember {
            mutableStateOf<Policy?>(
                null
            )
        }

    var policyToDelete by
        remember {
            mutableStateOf<Policy?>(
                null
            )
        }

    val globalPolicy =
        state.policies.find {
            it.id ==
                "global_default"
        }

    val permanentPolicies =
        state.policies
            .filterNot {
                isSystemTemporaryPolicy(
                    it
                )
            }
            .filterNot {
                it.id ==
                    "global_default"
            }
            .filterNot {
                it.targetID ==
                    "infrastructure"
            }

    val tagPolicies =
        permanentPolicies
            .filter {
                it.type ==
                    "tag"
            }
            .sortedWith(
                compareByDescending<Policy> {
                    it.enabled
                }.thenByDescending {
                    it.priority
                }
            )

    val devicePolicies =
        permanentPolicies
            .filter {
                it.type ==
                    "device"
            }
            .sortedWith(
                compareByDescending<Policy> {
                    it.enabled
                }.thenByDescending {
                    it.priority
                }
            )

    val temporaryPolicies =
        state.policies
            .filter {
                isSystemTemporaryPolicy(
                    it
                )
            }

    HigLargeTitleScaffold(
        title =
            "Rules",
        scrollState =
            scrollState,
        navTrailing = {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                HigTextButton(
                    text =
                        "Export",
                    onClick = {
                        viewModel
                            .exportPolicies()
                    }
                )

                HigTextButton(
                    text =
                        "＋",
                    onClick = {
                        editingPolicy =
                            null

                        showWizard =
                            true
                    }
                )
            }
        }
    ) { padding ->

        LazyColumn(
            state =
                scrollState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                padding
        ) {

            when (
                val sync =
                    state.syncState
            ) {

                SyncState.Idle,
                SyncState.Loading -> {

                    if (
                        !state.isInitialLoaded
                    ) {
                        item {
                            ScreenStateView(
                                title =
                                    "Loading Rules",
                                message =
                                    "Synchronizing LIAS policy configuration."
                            )
                        }

                        return@LazyColumn
                    }
                }

                is SyncState.Failed -> {

                    item {
                        ScreenStateView(
                            title =
                                "Unable to Load Rules",
                            message =
                                sync.message,
                            actionText =
                                "Try Again",
                            onAction =
                                viewModel::refresh,
                            tone =
                                ScreenStateTone.ERROR
                        )
                    }

                    return@LazyColumn
                }

                is SyncState.Stale -> {

                    item {
                        StaleDataNotice(
                            message =
                                sync.message,
                            onRefresh =
                                viewModel::refresh
                        )
                    }
                }

                is SyncState.Ready ->
                    Unit
            }

            // --------------------------------------------------------
            // Global
            // --------------------------------------------------------

            item {
                ListSectionHeader(
                    "Global Access"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    if (
                        globalPolicy ==
                        null
                    ) {
                        GroupedListRow(
                            primaryText =
                                "Global Access Switch",
                            secondaryText =
                                "The server did not return global_default."
                        )
                    } else {
                        PolicyRow(
                            policy =
                                globalPolicy,
                            tags =
                                state.tags,
                            devices =
                                state.devices,
                            schedules =
                                state.schedules,
                            editable =
                                true,
                            toggleable =
                                false,
                            deletable =
                                false,
                            onToggle = {},
                            onEdit = {
                                editingPolicy =
                                    globalPolicy

                                showWizard =
                                    true
                            },
                            onDelete = {}
                        )
                    }
                }
            }

            // --------------------------------------------------------
            // Tag policies
            // --------------------------------------------------------

            if (
                tagPolicies
                    .isNotEmpty()
            ) {

                item {
                    ListSectionHeader(
                        "Tag Rules · ${tagPolicies.size}"
                    )
                }

                item {
                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        tagPolicies
                            .forEachIndexed { index, policy ->

                                PolicyRow(
                                    policy =
                                        policy,
                                    tags =
                                        state.tags,
                                    devices =
                                        state.devices,
                                    schedules =
                                        state.schedules,
                                    editable =
                                        true,
                                    toggleable =
                                        true,
                                    deletable =
                                        true,
                                    onToggle = { enabled ->

                                        viewModel.savePolicy(
                                            policy.copy(
                                                enabled =
                                                    enabled
                                            )
                                        )
                                    },
                                    onEdit = {
                                        editingPolicy =
                                            policy

                                        showWizard =
                                            true
                                    },
                                    onDelete = {
                                        policyToDelete =
                                            policy
                                    },
                                    showDivider =
                                        index <
                                            tagPolicies
                                                .lastIndex
                                )
                            }
                    }
                }
            }

            // --------------------------------------------------------
            // Device policies
            // --------------------------------------------------------

            if (
                devicePolicies
                    .isNotEmpty()
            ) {

                item {
                    ListSectionHeader(
                        "Device Rules · ${devicePolicies.size}"
                    )
                }

                item {
                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        devicePolicies
                            .forEachIndexed { index, policy ->

                                PolicyRow(
                                    policy =
                                        policy,
                                    tags =
                                        state.tags,
                                    devices =
                                        state.devices,
                                    schedules =
                                        state.schedules,
                                    editable =
                                        true,
                                    toggleable =
                                        true,
                                    deletable =
                                        true,
                                    onToggle = { enabled ->

                                        viewModel.savePolicy(
                                            policy.copy(
                                                enabled =
                                                    enabled
                                            )
                                        )
                                    },
                                    onEdit = {
                                        editingPolicy =
                                            policy

                                        showWizard =
                                            true
                                    },
                                    onDelete = {
                                        policyToDelete =
                                            policy
                                    },
                                    showDivider =
                                        index <
                                            devicePolicies
                                                .lastIndex
                                )
                            }
                    }
                }
            }

            // --------------------------------------------------------
            // Temporary server-owned policies
            // --------------------------------------------------------

            if (
                temporaryPolicies
                    .isNotEmpty()
            ) {

                item {
                    ListSectionHeader(
                        "Temporary Overrides · ${temporaryPolicies.size}"
                    )
                }

                item {
                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        temporaryPolicies
                            .forEachIndexed { index, policy ->

                                PolicyRow(
                                    policy =
                                        policy,
                                    tags =
                                        state.tags,
                                    devices =
                                        state.devices,
                                    schedules =
                                        state.schedules,
                                    editable =
                                        false,
                                    toggleable =
                                        false,
                                    deletable =
                                        false,
                                    onToggle = {},
                                    onEdit = {},
                                    onDelete = {},
                                    showDivider =
                                        index <
                                            temporaryPolicies
                                                .lastIndex
                                )
                            }
                    }
                }
            }

            if (
                tagPolicies.isEmpty() &&
                devicePolicies.isEmpty() &&
                temporaryPolicies.isEmpty()
            ) {

                item {
                    ScreenStateView(
                        title =
                            "No Custom Rules",
                        message =
                            "Create a tag or device rule. Global Access remains available above.",
                        actionText =
                            "Create Rule",
                        onAction = {
                            editingPolicy =
                                null

                            showWizard =
                                true
                        }
                    )
                }
            }

            // --------------------------------------------------------
            // Precedence explanation
            // --------------------------------------------------------

            item {
                ListSectionHeader(
                    "How Rules Are Evaluated"
                )
            }

            item {
                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "1 · Infrastructure",
                        secondaryText =
                            "Always allowed and immune to every access rule.",
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "2 · Global Access",
                        secondaryText =
                            "Global Block or Allow immediately overrides every non-infrastructure device.",
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "3 · Device Rule",
                        secondaryText =
                            "In Global Schedule mode, the highest-priority matching device rule is evaluated first.",
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "4 · Tag Rules",
                        secondaryText =
                            "If no device rule applies, matching tag rules are combined. Any blocking tag rule blocks access.",
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "5 · Global Schedule",
                        secondaryText =
                            "Used as the fallback when no device or tag rule applies."
                    )
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Wizard
    // ----------------------------------------------------------------

    if (showWizard) {

        PolicyWizardSheet(
            initialPolicy =
                editingPolicy,
            tags =
                state.tags,
            devices =
                state.devices,
            schedules =
                state.schedules,
            existingPolicies =
                state.policies,
            onValidateSchedules = { scheduleIds ->

                viewModel.validatePolicy(
                    scheduleIds
                )
            },
            onDismiss = {
                showWizard =
                    false

                editingPolicy =
                    null
            },
            onSave = { policy ->

                viewModel.savePolicy(
                    policy
                )

                showWizard =
                    false

                editingPolicy =
                    null
            }
        )
    }

    // ----------------------------------------------------------------
    // Delete
    // ----------------------------------------------------------------

    policyToDelete
        ?.let { policy ->

            HigAlertDialog(
                onDismissRequest = {
                    policyToDelete =
                        null
                },
                title =
                    "Delete Rule",
                message =
                    when (
                        policy.type
                    ) {
                        "device" ->
                            "Delete “${policy.name}”? This device will fall through to applicable tag rules, then the global policy."

                        "tag" ->
                            "Delete “${policy.name}”? Devices using this tag will continue through any other matching tag rules or the global fallback."

                        else ->
                            "Delete “${policy.name}”?"
                    },
                confirmText =
                    "Delete",
                onConfirm = {
                    viewModel.deletePolicy(
                        policy.id,
                        policy.name,
                        policy
                    )

                    policyToDelete =
                        null
                },
                isDestructive =
                    true
            )
        }
}

@Composable
private fun PolicyRow(
    policy: Policy,
    tags: List<com.lias.remote.core.models.Tag>,
    devices: List<com.lias.remote.core.models.Device>,
    schedules: List<com.lias.remote.core.models.Schedule>,
    editable: Boolean,
    toggleable: Boolean,
    deletable: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean = false
) {
    val selectedSchedules =
        policy.resolveScheduleIDs()
            .mapNotNull { id ->
                schedules.find {
                    it.id == id
                }
            }

    val target =
        PolicyValidation
            .targetLabel(
                policy =
                    policy,
                tags =
                    tags,
                devices =
                    devices
            )

    val emptySchedule =
        policy.action ==
            "schedule" &&
            policy.resolveScheduleIDs()
                .isEmpty()

    val secondary =
        buildString {

            append(
                target
            )

            if (
                policy.type !=
                "global"
            ) {
                append(
                    " · Priority "
                )
                append(
                    policy.priority
                )
            }

            when (
                policy.action
            ) {
                "schedule" -> {
                    append(
                        " · "
                    )

                    append(
                        when {
                            selectedSchedules.isEmpty() ->
                                "No schedules"

                            selectedSchedules.size == 1 ->
                                selectedSchedules
                                    .first()
                                    .name

                            else ->
                                "${selectedSchedules.size} schedules"
                        }
                    )
                }

                "allow" ->
                    append(
                        " · Always Allow"
                    )

                "block" ->
                    append(
                        " · Always Block"
                    )
            }

            if (
                !policy.enabled
            ) {
                append(
                    " · Disabled"
                )
            }

            if (
                emptySchedule
            ) {
                append(
                    " · Defaults to Allow"
                )
            }
        }

    val row = @Composable {

        GroupedListRow(
            primaryText =
                policy.name.ifBlank {
                    when {
                        policy.id ==
                            "global_default" ->
                            "Global Access Switch"

                        else ->
                            "Unnamed Rule"
                    }
                },
            secondaryText =
                secondary,
            trailingContent = {

                when {

                    toggleable -> {
                        CupertinoSwitch(
                            checked =
                                policy.enabled,
                            onCheckedChange =
                                onToggle
                        )
                    }

                    policy.id ==
                        "global_default" -> {

                        StatusPill(
                            text =
                                policy.action
                                    .replaceFirstChar {
                                        it.uppercase()
                                    },
                            tone =
                                when (
                                    policy.action
                                ) {
                                    "allow" ->
                                        PillTone.ALLOWED

                                    "block" ->
                                        PillTone.BLOCKED

                                    else ->
                                        PillTone.INFO
                                }
                        )
                    }

                    else -> {
                        StatusPill(
                            text =
                                "Temporary",
                            tone =
                                PillTone.INFO
                        )
                    }
                }
            },
            showDivider =
                showDivider,
            onClick =
                if (editable) {
                    onEdit
                } else {
                    null
                }
        )
    }

    if (
        editable ||
        deletable
    ) {

        HigSwipeRow(
            leadingAction =
                if (editable) {
                    SwipeAction(
                        icon =
                            CupertinoIcons
                                .Outlined
                                .Pencil,
                        color =
                            LiasThemeColors.blue,
                        onTrigger =
                            onEdit
                    )
                } else {
                    null
                },
            trailingAction =
                if (deletable) {
                    SwipeAction(
                        icon =
                            CupertinoIcons
                                .Outlined
                                .Trash,
                        color =
                            LiasThemeColors.red,
                        onTrigger =
                            onDelete
                    )
                } else {
                    null
                }
        ) {
            row()
        }

    } else {
        row()
    }
}

private fun isSystemTemporaryPolicy(
    policy: Policy
): Boolean =
    policy.id.startsWith(
        "pol_pause_"
    ) ||
        policy.id.startsWith(
            "pol_extend_"
        ) ||
        !policy.reasonTag
            .isNullOrBlank() ||
        !policy.expiresAt
            .isNullOrBlank()
