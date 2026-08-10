package com.lias.remote.ui.screens.identity

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lias.remote.core.network.EngineFeatures
import com.lias.remote.core.network.IdentityCandidateDetail
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

private enum class IdentityDialog {
    MERGE,
    REJECT,
    REOPEN,
    BIND,
    SPLIT,
    REVOKE
}

@Composable
fun IdentityReviewScreen(
    viewModel: LiasViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val review = state.identityReview
    val scrollState = rememberLazyListState()

    var dialog by remember { mutableStateOf<IdentityDialog?>(null) }
    var note by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var bindingType by remember { mutableStateOf("Manual") }
    var bindingValue by remember { mutableStateOf("") }
    var splitMac by remember { mutableStateOf("") }
    var revokeTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }

    LaunchedEffect(state.supportsIdentityReview) {
        if (state.supportsIdentityReview) {
            viewModel.refreshIdentityReview(review.status)
        }
    }

    HigLargeTitleScaffold(
        title = "Identity Review",
        scrollState = scrollState,
        navLeading = {
            HigTextButton(
                text = "Back",
                onClick = onBack
            )
        },
        navTrailing = {
            HigTextButton(
                text = "Refresh",
                onClick = {
                    viewModel.refreshIdentityReview(review.status)
                },
                enabled = state.supportsIdentityReview && !review.isLoading
            )
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CupertinoText(
                    text = "Review evidence before changing device identity. A correlation score is an estimate, not proof.",
                    style = HigTypography.subheadline,
                    color = LiasThemeColors.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (!state.supportsIdentityReview) {
                item {
                    GroupedListCard(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        GroupedListRow(
                            primaryText = "Identity review unavailable",
                            secondaryText = "The connected LIAS server does not advertise the identity candidate queue. Existing device and access features remain available."
                        )
                    }
                }
                return@LazyColumn
            }

            item {
                SegmentedControl(
                    options = listOf("Pending", "Confirmed", "Rejected"),
                    selectedOption = review.status.replaceFirstChar { it.uppercase() },
                    onOptionSelected = {
                        viewModel.refreshIdentityReview(it.lowercase())
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            review.errorMessage?.let { message ->
                item {
                    GroupedListCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        GroupedListRow(
                            primaryText = "Could not load identity reviews",
                            secondaryText = message,
                            trailingContent = {
                                HigTextButton(
                                    text = "Retry",
                                    onClick = {
                                        viewModel.refreshIdentityReview(review.status)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            item {
                ListSectionHeader(
                    "${review.status.replaceFirstChar { it.uppercase() }} · ${review.candidates.size}"
                )
            }

            if (review.candidates.isEmpty() && !review.isLoading) {
                item {
                    GroupedListCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        GroupedListRow(
                            primaryText = "No ${review.status} matches",
                            secondaryText =
                                if (review.status == "pending") {
                                    "New possible matches will appear here automatically."
                                } else {
                                    "No identity decisions are recorded in this view."
                                }
                        )
                    }
                }
            }

            items(
                items = review.candidates,
                key = { it.id }
            ) { candidate ->
                IdentityCandidateRow(
                    candidate = candidate,
                    onClick = {
                        viewModel.selectIdentityCandidate(candidate.id)
                    }
                )
            }

            if (!review.nextCursor.isNullOrBlank()) {
                item {
                    HigButton(
                        text = if (review.isLoading) "Loading…" else "Load More",
                        onClick = viewModel::loadMoreIdentityCandidates,
                        enabled = !review.isLoading,
                        style = HigButtonStyle.Secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    review.selectedCandidate?.let { candidate ->
        IdentityCandidateSheet(
            candidate = candidate,
            viewModel = viewModel,
            onDismiss = viewModel::clearSelectedIdentityCandidate,
            onMerge = {
                note = ""
                confirmation = ""
                dialog = IdentityDialog.MERGE
            },
            onReject = {
                note = ""
                dialog = IdentityDialog.REJECT
            },
            onReopen = {
                note = ""
                dialog = IdentityDialog.REOPEN
            },
            onBind = {
                bindingType = "Manual"
                bindingValue = ""
                dialog = IdentityDialog.BIND
            },
            onSplit = {
                splitMac = candidate.sourceDevice?.currentMac.orEmpty()
                confirmation = ""
                dialog = IdentityDialog.SPLIT
            },
            onRevoke = { pdid, aliasId ->
                revokeTarget = pdid to aliasId
                dialog = IdentityDialog.REVOKE
            }
        )

        when (dialog) {
            IdentityDialog.MERGE ->
                IdentityDecisionDialog(
                    title = "Confirm identity merge",
                    message = "${candidate.targetLabel()} will survive. LIAS will reconcile identity-linked records. Type MERGE to continue.",
                    confirmText = "Merge Records",
                    confirmationLabel = "Type MERGE",
                    confirmation = confirmation,
                    onConfirmationChange = { confirmation = it },
                    note = note,
                    onNoteChange = { note = it },
                    enabled = confirmation == "MERGE",
                    onDismiss = { dialog = null },
                    onConfirm = {
                        viewModel.confirmIdentityCandidate(candidate, note)
                        dialog = null
                    }
                )

            IdentityDialog.REJECT ->
                NoteDialog(
                    title = "Reject possible match",
                    message = "Both device records will remain separate. This decision can be reopened when supported.",
                    confirmText = "Reject Match",
                    note = note,
                    onNoteChange = { note = it },
                    onDismiss = { dialog = null },
                    onConfirm = {
                        viewModel.rejectIdentityCandidate(candidate, note)
                        dialog = null
                    }
                )

            IdentityDialog.REOPEN ->
                NoteDialog(
                    title = "Reopen identity review",
                    message = "Return this candidate to the pending review queue?",
                    confirmText = "Reopen",
                    note = note,
                    onNoteChange = { note = it },
                    onDismiss = { dialog = null },
                    onConfirm = {
                        viewModel.reopenIdentityCandidate(candidate, note)
                        dialog = null
                    }
                )

            IdentityDialog.BIND ->
                BindingDialog(
                    bindingType = bindingType,
                    value = bindingValue,
                    onTypeChange = { bindingType = it },
                    onValueChange = { bindingValue = it },
                    onDismiss = { dialog = null },
                    onConfirm = {
                        viewModel.bindIdentity(
                            candidate.targetPdid,
                            bindingType.toWireBindingType(),
                            bindingValue
                        )
                        dialog = null
                    }
                )

            IdentityDialog.SPLIT ->
                SplitDialog(
                    mac = splitMac,
                    confirmation = confirmation,
                    onMacChange = { splitMac = it },
                    onConfirmationChange = { confirmation = it },
                    onDismiss = { dialog = null },
                    onConfirm = {
                        viewModel.splitIdentity(
                            candidate.targetPdid,
                            splitMac
                        )
                        dialog = null
                    }
                )

            IdentityDialog.REVOKE ->
                HigAlertDialog(
                    onDismissRequest = { dialog = null },
                    title = "Revoke identity binding",
                    message = "Future observations will no longer use this verified binding.",
                    confirmText = "Revoke",
                    isDestructive = true,
                    onConfirm = {
                        revokeTarget?.let { (pdid, aliasId) ->
                            viewModel.revokeIdentityBinding(pdid, aliasId)
                        }
                        dialog = null
                    }
                )

            null -> Unit
        }
    }
}

@Composable
private fun IdentityCandidateRow(
    candidate: IdentityCandidateDetail,
    onClick: () -> Unit
) {
    GroupedListCard(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        GroupedListRow(
            primaryText = "${candidate.sourceLabel()} → ${candidate.targetLabel()}",
            secondaryText = "${candidate.factors.size} matches · ${candidate.conflicts.size} conflicts${if (candidate.ambiguous) " · Ambiguous" else ""}",
            trailingContent = {
                StatusPill(
                    text = "${candidate.scorePercent}%",
                    tone = if (candidate.ambiguous) PillTone.WARN else PillTone.INFO
                )
            },
            onClick = onClick
        )
    }
}

@Composable
private fun IdentityCandidateSheet(
    candidate: IdentityCandidateDetail,
    viewModel: LiasViewModel,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
    onReject: () -> Unit,
    onReopen: () -> Unit,
    onBind: () -> Unit,
    onSplit: () -> Unit,
    onRevoke: (String, Long) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val profiles = state.identityReview.profiles

    HigModalSheet(
        onDismiss = onDismiss,
        accessibilityLabel = "Identity candidate details"
    ) {
        HigSheetHeader(
            title = "Review Match",
            onCancel = onDismiss
        )

        Column(
            modifier = Modifier
                .fillMaxHeight(0.78f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CupertinoText(
                text = "Correlation score ${candidate.scorePercent}% — not proof. Confirm only when history and evidence support the same physical device.",
                style = HigTypography.body,
                color = LiasThemeColors.label,
                fontWeight = FontWeight.SemiBold
            )

            DeviceComparisonCard(
                label = "Source record",
                name = candidate.sourceLabel(),
                pdid = candidate.sourcePdid,
                mac = candidate.sourceDevice?.currentMac.orEmpty(),
                assurance = profiles[candidate.sourcePdid]?.assurance ?: "Unknown"
            )

            DeviceComparisonCard(
                label = "Surviving target",
                name = candidate.targetLabel(),
                pdid = candidate.targetPdid,
                mac = candidate.targetDevice?.currentMac.orEmpty(),
                assurance = profiles[candidate.targetPdid]?.assurance ?: "Unknown"
            )

            EvidenceCard("Matching Evidence", candidate.factors)
            EvidenceCard("Conflicts", candidate.conflicts)

            profiles[candidate.targetPdid]
                ?.aliases
                ?.filter { it.revokedAt == null }
                ?.takeIf { it.isNotEmpty() }
                ?.let { aliases ->
                    ListSectionHeader("Verified Bindings")
                    aliases.forEach { alias ->
                        GroupedListCard {
                            GroupedListRow(
                                primaryText = alias.type.replace('_', ' '),
                                secondaryText =
                                    "${alias.source.ifBlank { "LIAS" }} · ${if (alias.verified) "Verified" else "Observed"}${alias.valueHash.takeIf { it.isNotBlank() }?.let { " · ${it.take(12)}…" }.orEmpty()}",
                                trailingContent = {
                                    HigTextButton(
                                        text = "Revoke",
                                        isDestructive = true,
                                        onClick = {
                                            onRevoke(candidate.targetPdid, alias.id)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

            if (candidate.status == "pending") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HigButton(
                        text = "Reject",
                        onClick = onReject,
                        style = HigButtonStyle.Danger,
                        modifier = Modifier.weight(1f)
                    )
                    HigButton(
                        text = "Continue to Merge",
                        onClick = onMerge,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (
                candidate.status == "rejected" &&
                state.supportsEngineFeature(
                    EngineFeatures.IDENTITY_CANDIDATE_REOPEN
                )
            ) {
                HigButton(
                    text = "Reopen Review",
                    onClick = onReopen,
                    style = HigButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (
                state.supportsEngineFeature(
                    EngineFeatures.IDENTITY_BINDINGS
                )
            ) {
                HigButton(
                    text = "Add Verified Binding",
                    onClick = onBind,
                    style = HigButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (
                state.supportsEngineFeature(
                    EngineFeatures.IDENTITY_SPLIT
                )
            ) {
                HigButton(
                    text = "Split Device Identity",
                    onClick = onSplit,
                    style = HigButtonStyle.Danger,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeviceComparisonCard(
    label: String,
    name: String,
    pdid: String,
    mac: String,
    assurance: String
) {
    GroupedListCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            CupertinoText(label.uppercase(), style = HigTypography.caption, color = LiasThemeColors.secondaryLabel)
            CupertinoText(name, style = HigTypography.headline, color = LiasThemeColors.label)
            CupertinoText(pdid, style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel)
            CupertinoText("MAC: ${mac.ifBlank { "None recorded" }}", style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel)
            CupertinoText("Assurance: ${assurance.replaceFirstChar { it.uppercase() }}", style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel)
        }
    }
}

@Composable
private fun EvidenceCard(
    title: String,
    factors: List<com.lias.remote.core.network.IdentityFactor>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ListSectionHeader(title)
        GroupedListCard {
            if (factors.isEmpty()) {
                GroupedListRow(
                    primaryText = "None supplied",
                    secondaryText = "LIAS did not provide evidence in this category."
                )
            } else {
                factors.forEachIndexed { index, factor ->
                    GroupedListRow(
                        primaryText = factor.kind.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        secondaryText = "Likelihood ratio ${factor.likelihoodRatio}",
                        trailingContent = {
                            StatusPill(
                                text = if (factor.matched) "Match" else "Conflict",
                                tone = if (factor.matched) PillTone.ALLOWED else PillTone.WARN
                            )
                        },
                        showDivider = index < factors.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentityDecisionDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmationLabel: String,
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HigAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = message,
        confirmText = confirmText,
        confirmEnabled = enabled,
        isDestructive = true,
        onConfirm = onConfirm,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HigConfiguredField(
                    value = note,
                    onValueChange = { onNoteChange(it.take(1024)) },
                    label = "Decision Note",
                    placeholder = "Optional",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6
                )
                HigConfiguredField(
                    value = confirmation,
                    onValueChange = onConfirmationChange,
                    label = confirmationLabel,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        )
                )
            }
        }
    )
}

@Composable
private fun NoteDialog(
    title: String,
    message: String,
    confirmText: String,
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HigAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = message,
        confirmText = confirmText,
        isDestructive = true,
        onConfirm = onConfirm,
        content = {
            HigConfiguredField(
                value = note,
                onValueChange = { onNoteChange(it.take(1024)) },
                label = "Decision Note",
                placeholder = "Optional",
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )
        }
    )
}

@Composable
private fun BindingDialog(
    bindingType: String,
    value: String,
    onTypeChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HigAlertDialog(
        onDismissRequest = onDismiss,
        title = "Add verified binding",
        message = "A verified identifier can anchor future observations to this PDID.",
        confirmText = "Add Binding",
        confirmEnabled = value.isNotBlank(),
        onConfirm = onConfirm,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedControl(
                    options = listOf("Manual", "MAC", "DHCP", "PPSK", "Key"),
                    selectedOption = bindingType,
                    onOptionSelected = onTypeChange
                )
                HigConfiguredField(
                    value = value,
                    onValueChange = onValueChange,
                    label = "Binding Value",
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Done
                        )
                )
            }
        }
    )
}

@Composable
private fun SplitDialog(
    mac: String,
    confirmation: String,
    onMacChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HigAlertDialog(
        onDismissRequest = onDismiss,
        title = "Split device identity",
        message = "LIAS will create a separate device record for this MAC. Type SPLIT to continue.",
        confirmText = "Split Identity",
        confirmEnabled = mac.isNotBlank() && confirmation == "SPLIT",
        isDestructive = true,
        onConfirm = onConfirm,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HigConfiguredField(
                    value = mac,
                    onValueChange = onMacChange,
                    label = "MAC to Split",
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                )
                HigConfiguredField(
                    value = confirmation,
                    onValueChange = onConfirmationChange,
                    label = "Type SPLIT",
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        )
                )
            }
        }
    )
}

private fun IdentityCandidateDetail.sourceLabel(): String =
    sourceDevice?.displayName?.takeIf { it.isNotBlank() }
        ?: sourcePdid

private fun IdentityCandidateDetail.targetLabel(): String =
    targetDevice?.displayName?.takeIf { it.isNotBlank() }
        ?: targetPdid

private fun String.toWireBindingType(): String =
    when (this) {
        "MAC" -> "mac"
        "DHCP" -> "dhcp_client_id"
        "PPSK" -> "ppsk_id"
        "Key" -> "device_public_key"
        else -> "manual"
    }
