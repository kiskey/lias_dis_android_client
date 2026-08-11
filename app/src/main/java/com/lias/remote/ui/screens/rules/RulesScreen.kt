// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt
// Version: 17.0.0
//
// Purpose:
//   Policy inventory + entry point to complete PolicyWizardSheet.
//
// UX corrections:
//   - Global Access is separated from ordinary access rules.
//   - "New Rule" creates only tag/device rules.
//   - Policy subtitles describe target + behavior instead of exposing
//     raw target IDs.
//   - Priority appears only for device rules.
//   - Temporary pause/extend policies are not shown as user-authored
//     permanent rules.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.policy.PolicyPresentation
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.DestructiveBiometricAuth
import com.lias.remote.ui.components.findFragmentActivity
import com.lias.remote.ui.components.requiresProtectedDelete
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
import com.slapps.cupertino.CupertinoSwitch
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.Pencil
import com.slapps.cupertino.icons.outlined.Trash

@Composable
fun RulesScreen(
    viewModel: LiasViewModel
) {

    val state by
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    val hostActivity =
        LocalContext.current
            .findFragmentActivity()

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

    var policyDeleteAuthError by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var policySaveInFlight by
        remember {
            mutableStateOf(
                false
            )
        }

    val permanentPolicies =
        remember(
            state.policies
        ) {

            state.policies
                .filter {
                    it.reasonTag
                        .isNullOrBlank()
                }
        }

    val globalPolicy =
        permanentPolicies
            .find {
                it.id ==
                    "global_default"
            }

    val tagPolicies =
        permanentPolicies
            .filter {
                it.type ==
                    "tag" &&
                    it.id !=
                    "global_default"
            }
            .sortedBy {
                it.name.lowercase()
            }

    val devicePolicies =
        permanentPolicies
            .filter {
                it.type ==
                    "device"
            }
            .sortedWith(
                compareByDescending<Policy> {
                    it.priority
                }.thenBy {
                    it.name.lowercase()
                }
            )

    HigLargeTitleScaffold(
        title =
            "Rules",
        scrollState =
            scrollState,
        navTrailing = {

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
                                    "Synchronizing policy configuration from LIAS."
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

            item {

                ListSectionHeader(
                    "Global Access"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                ) {

                    if (
                        globalPolicy !=
                        null
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Global Access",
                            secondaryText =
                                PolicyPresentation
                                    .policySubtitle(
                                        globalPolicy,
                                        state.tags,
                                        state.devices,
                                        state.schedules
                                    ),
                            trailingContent = {

                                StatusPill(
                                    text =
                                        globalPolicy.action
                                            .replaceFirstChar {
                                                it.uppercase()
                                            },
                                    tone =
                                        when (
                                            globalPolicy.action
                                        ) {

                                            "block" ->
                                                PillTone.BLOCKED

                                            "allow" ->
                                                PillTone.ALLOWED

                                            else ->
                                                PillTone.INFO
                                        }
                                )
                            },
                            onClick = {

                                editingPolicy =
                                    globalPolicy

                                showWizard =
                                    true
                            }
                        )

                    } else {

                        GroupedListRow(
                            primaryText =
                                "Global Access",
                            secondaryText =
                                "Waiting for global_default from LIAS"
                        )
                    }
                }
            }

            if (
                tagPolicies.isNotEmpty()
            ) {

                item {

                    ListSectionHeader(
                        "Tag Groups · ${tagPolicies.size}"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            )
                    ) {

                        tagPolicies
                            .forEachIndexed {
                                    index,
                                    policy ->

                                PolicyRow(
                                    policy =
                                        policy,
                                    subtitle =
                                        PolicyPresentation
                                            .policySubtitle(
                                                policy,
                                                state.tags,
                                                state.devices,
                                                state.schedules
                                            ),
                                    onToggle = {
                                        enabled ->

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

                                        policyDeleteAuthError =
                                            null

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

            if (
                devicePolicies.isNotEmpty()
            ) {

                item {

                    ListSectionHeader(
                        "Specific Devices · ${devicePolicies.size}"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            )
                    ) {

                        devicePolicies
                            .forEachIndexed {
                                    index,
                                    policy ->

                                PolicyRow(
                                    policy =
                                        policy,
                                    subtitle =
                                        PolicyPresentation
                                            .policySubtitle(
                                                policy,
                                                state.tags,
                                                state.devices,
                                                state.schedules
                                            ),
                                    onToggle = {
                                        enabled ->

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

                                        policyDeleteAuthError =
                                            null

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

            if (
                tagPolicies.isEmpty() &&
                devicePolicies.isEmpty()
            ) {

                item {

                    ScreenStateView(
                        title =
                            "No Custom Rules",
                        message =
                            "Global Access is active. Add a tag or device rule when you need a more specific restriction.",
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

            item {

                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
                ) {

                    CupertinoText(
                        text =
                            "How rules are applied",
                        style =
                            HigTypography.headline,
                        color =
                            LiasThemeColors.label
                    )

                    CupertinoText(
                        text =
                            "Infrastructure is always online. Global Allow or Block overrides ordinary rules. In Global Schedule mode, a device-specific rule is checked first; otherwise matching tag rules are evaluated together.",
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.secondaryLabel,
                        modifier =
                            Modifier.padding(
                                top = 5.dp
                            )
                    )
                }
            }
        }
    }

    if (
        showWizard
    ) {

        PolicyWizardSheet(
            initialPolicy =
                editingPolicy,
            tags =
                state.tags,
            devices =
                state.devices,
            schedules =
                state.schedules,
            policies =
                permanentPolicies,
            validateSchedules =
                viewModel::validatePolicy,
            onDismiss = {

                showWizard =
                    false

                editingPolicy =
                    null
            },
            onSave = {
                policy ->

                if (
                    policySaveInFlight
                ) {
                    false

                } else {

                    policySaveInFlight =
                        true

                    val result =
                        viewModel
                            .savePolicyAwait(
                                policy
                            )

                    policySaveInFlight =
                        false

                    result is
                        ApiResult.Success
                }
            }
        )
    }

    policyToDelete
        ?.let { policy ->

            HigAlertDialog(
                onDismissRequest = {
                    policyDeleteAuthError =
                        null

                    policyToDelete =
                        null
                },
                title =
                    "Delete Rule?",
                message =
                    buildString {

                        append(
                            "Delete “"
                        )

                        append(
                            policy.name
                        )

                        append(
                            "”? "
                        )

                        when (
                            policy.type
                        ) {

                            "device" ->
                                append(
                                    "The device will fall through to matching tag rules or Global Access."
                                )

                            "tag" ->
                                append(
                                    "Devices in this tag will continue through their remaining applicable rules."
                                )

                            else ->
                                append(
                                    "This rule will be permanently removed."
                                )
                        }

                        policyDeleteAuthError
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {
                                error ->

                                append("\n\n")
                                append(error)
                            }
                    },
                confirmText =
                    "Delete Rule",
                onConfirm = {

                    if (
                        !requiresProtectedDelete(
                            policy.id
                        )
                    ) {

                        policyDeleteAuthError =
                            "Only saved rules can be deleted."

                    } else {

                        DestructiveBiometricAuth.authenticate(
                            activity =
                                hostActivity,
                            objectLabel =
                                "rule “${policy.name}”",
                            onAuthenticated = {

                                viewModel.deletePolicy(
                                    policy.id,
                                    policy.name,
                                    policy
                                )

                                policyDeleteAuthError =
                                    null

                                policyToDelete =
                                    null
                            },
                            onUnavailable = {
                                message ->

                                policyDeleteAuthError =
                                    message
                            }
                        )
                    }
                },
                isDestructive =
                    true
            )
        }
}

@Composable
private fun PolicyRow(
    policy: Policy,
    subtitle: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean
) {

    HigSwipeRow(
        leadingAction =
            SwipeAction(
                icon =
                    CupertinoIcons
                        .Outlined
                        .Pencil,
                color =
                    LiasThemeColors.blue,
                onTrigger =
                    onEdit
            ),
        trailingAction =
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
    ) {

        GroupedListRow(
            primaryText =
                policy.name,
            secondaryText =
                subtitle,
            trailingContent = {

                CupertinoSwitch(
                    checked =
                        policy.enabled,
                    onCheckedChange =
                        onToggle
                )
            },
            showDivider =
                showDivider,
            onClick =
                onEdit
        )
    }
}
