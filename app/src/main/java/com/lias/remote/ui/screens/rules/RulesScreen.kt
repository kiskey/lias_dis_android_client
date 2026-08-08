package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.SegmentedControl
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
fun RulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    
    var showWizard by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }
    var policyToDelete by remember { mutableStateOf<Policy?>(null) }

    HigLargeTitleScaffold(
        title = "Rules",
        scrollState = scrollState,
        navTrailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HigTextButton(
                    text = "Export",
                    onClick = { viewModel.exportPolicies() }
                )
                HigTextButton(
                    text = "Import",
                    onClick = { /* Document picker trigger */ }
                )
                HigTextButton(
                    text = "＋",
                    onClick = { editingPolicy = null; showWizard = true }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            val globalPolicies = state.policies.filter { it.type == "global" }
            val tagPolicies = state.policies.filter { it.type == "tag" }
            val devicePolicies = state.policies.filter { it.type == "device" }

            if (globalPolicies.isNotEmpty()) {
                item { ListSectionHeader("Global · ${globalPolicies.size} rule") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        globalPolicies.forEachIndexed { index, policy ->
                            PolicyRow(
                                policy = policy,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy },
                                showDivider = index < globalPolicies.size - 1
                            )
                        }
                    }
                }
            }

            if (tagPolicies.isNotEmpty()) {
                item { ListSectionHeader("Tag Rules · ${tagPolicies.size} rules") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        tagPolicies.forEachIndexed { index, policy ->
                            PolicyRow(
                                policy = policy,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy },
                                showDivider = index < tagPolicies.size - 1
                            )
                        }
                    }
                }
            }

            if (devicePolicies.isNotEmpty()) {
                item { ListSectionHeader("Device Rules · ${devicePolicies.size} rule") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        devicePolicies.forEachIndexed { index, policy ->
                            PolicyRow(
                                policy = policy,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy },
                                showDivider = index < devicePolicies.size - 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWizard) {
        PolicyWizardSheet(
            initialPolicy = editingPolicy,
            onDismiss = { showWizard = false },
            onSave = { viewModel.savePolicy(it); showWizard = false }
        )
    }

    policyToDelete?.let { policy ->
        HigAlertDialog(
            onDismissRequest = { policyToDelete = null },
            title = "Delete Rule",
            message = "Are you sure you want to delete the policy '${policy.name}'? Devices affected by this rule will revert to global defaults.",
            confirmText = "Delete",
            onConfirm = { viewModel.deletePolicy(policy.id, policy.name, policy) },
            isDestructive = true
        )
    }
}

@Composable
private fun PolicyRow(
    policy: Policy,
    viewModel: LiasViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean
) {
    HigSwipeRow(
        leadingAction = SwipeAction(CupertinoIcons.Outlined.Pencil, LiasThemeColors.blue, onEdit),
        trailingAction = if (policy.id != "global_default") SwipeAction(CupertinoIcons.Outlined.Trash, LiasThemeColors.red, onDelete) else null
    ) {
        GroupedListRow(
            primaryText = policy.name,
            secondaryText = "Priority ${policy.priority} · ${policy.targetID.ifBlank { "Global Scope" }}",
            trailingContent = {
                if (policy.id != "global_default") {
                    CupertinoSwitch(
                        checked = policy.enabled,
                        onCheckedChange = { enabled -> viewModel.savePolicy(policy.copy(enabled = enabled)) }
                    )
                } else {
                    StatusPill(text = policy.action, tone = PillTone.INFO)
                }
            },
            showDivider = showDivider,
            onClick = onEdit
        )
    }
}

@Composable
fun PolicyWizardSheet(
    initialPolicy: Policy?,
    onDismiss: () -> Unit,
    onSave: (Policy) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf(initialPolicy?.name ?: "") }
    var type by remember { mutableStateOf(initialPolicy?.type ?: "tag") }
    var action by remember { mutableStateOf(initialPolicy?.action ?: "schedule") }
    var priority by remember { mutableStateOf(initialPolicy?.priority?.toString() ?: "50") }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = if (initialPolicy == null) "New Rule" else "Edit Rule", onCancel = onDismiss)
            CupertinoText("Step $step of 3", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)

            when (step) {
                1 -> {
                    HigField(value = name, onValueChange = { name = it }, label = "Rule Name", placeholder = "e.g. Kids Internet Rules")
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CupertinoText("TARGET SCOPE", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)
                        SegmentedControl(
                            options = listOf("Global", "Tag", "Device"),
                            selectedOption = type.replaceFirstChar { it.uppercase() },
                            onOptionSelected = { type = it.lowercase() },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    HigButton(text = "Next", onClick = { step = 2 }, style = HigButtonStyle.Primary, modifier = Modifier.fillMaxWidth())
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CupertinoText("ENFORCEMENT ACTION", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)
                        SegmentedControl(
                            options = listOf("Allow", "Schedule", "Block"),
                            selectedOption = action.replaceFirstChar { it.uppercase() },
                            onOptionSelected = { action = it.lowercase() },
                            isDestructive = true,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    HigField(value = priority, onValueChange = { priority = it.filter { c -> c.isDigit() } }, label = "Priority (Higher Wins)")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        HigButton(text = "Back", onClick = { step = 1 }, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                        HigButton(
                            text = if (action == "schedule") "Next" else "Save Rule",
                            onClick = {
                                onSave(Policy(id = initialPolicy?.id ?: "pol_${System.currentTimeMillis()}", name = name, type = type, targetID = "", action = action, priority = priority.toIntOrNull() ?: 50, enabled = true))
                            },
                            style = HigButtonStyle.Primary, modifier = Modifier.weight(1f)
                        )
                    }
                }
                3 -> {
                    CupertinoText("ATTACH SCHEDULES", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)
                    CupertinoText("Schedule selection bundle builder is synchronized with server validation.", style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        HigButton(text = "Back", onClick = { step = 2 }, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                        HigButton(
                            text = "Save Rule",
                            onClick = {
                                onSave(Policy(id = initialPolicy?.id ?: "pol_${System.currentTimeMillis()}", name = name, type = type, targetID = "", action = action, priority = priority.toIntOrNull() ?: 50, scheduleIDs = emptyList(), enabled = true))
                            },
                            style = HigButtonStyle.Primary, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
