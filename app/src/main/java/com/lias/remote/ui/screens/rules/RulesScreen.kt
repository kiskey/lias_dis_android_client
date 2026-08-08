package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun RulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    
    var showWizard by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }

    HigLargeTitleScaffold(
        title = "Rules",
        scrollState = scrollState,
        navTrailing = {
            HigTextButton(text = "＋", onClick = { editingPolicy = null; showWizard = true })
        }
    ) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            val globalPolicies = state.policies.filter { it.type == "global" }
            val tagPolicies = state.policies.filter { it.type == "tag" }
            val devicePolicies = state.policies.filter { it.type == "device" }

            if (globalPolicies.isNotEmpty()) {
                item { ListSectionHeader("Global · ${globalPolicies.size} rule") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        globalPolicies.forEachIndexed { index, policy ->
                            PolicyRow(policy = policy, viewModel = viewModel, onEdit = { editingPolicy = policy; showWizard = true }, showDivider = index < globalPolicies.size - 1)
                        }
                    }
                }
            }
            if (tagPolicies.isNotEmpty()) {
                item { ListSectionHeader("Tag Rules · ${tagPolicies.size} rules") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        tagPolicies.forEachIndexed { index, policy ->
                            PolicyRow(policy = policy, viewModel = viewModel, onEdit = { editingPolicy = policy; showWizard = true }, showDivider = index < tagPolicies.size - 1)
                        }
                    }
                }
            }
            if (devicePolicies.isNotEmpty()) {
                item { ListSectionHeader("Device Rules · ${devicePolicies.size} rule") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        devicePolicies.forEachIndexed { index, policy ->
                            PolicyRow(policy = policy, viewModel = viewModel, onEdit = { editingPolicy = policy; showWizard = true }, showDivider = index < devicePolicies.size - 1)
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
}

@Composable
private fun PolicyRow(policy: Policy, viewModel: LiasViewModel, onEdit: () -> Unit, showDivider: Boolean) {
    GroupedListRow(
        primaryText = policy.name,
        secondaryText = "Priority ${policy.priority} · ${policy.targetID.ifBlank { "Global" }}",
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

@Composable
fun PolicyWizardSheet(initialPolicy: Policy?, onDismiss: () -> Unit, onSave: (Policy) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf(initialPolicy?.name ?: "") }
    var type by remember { mutableStateOf(initialPolicy?.type ?: "tag") }
    var action by remember { mutableStateOf(initialPolicy?.action ?: "schedule") }
    var priority by remember { mutableStateOf(initialPolicy?.priority?.toString() ?: "50") }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = if (initialPolicy == null) "New Rule" else "Edit Rule", onCancel = onDismiss)
            CupertinoText("Step $step of 3", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)

            when (step) {
                1 -> {
                    HigField(value = name, onValueChange = { name = it }, label = "Rule Name", placeholder = "e.g. Kids Internet Rules")
                    HigButton(text = "Next", onClick = { step = 2 }, style = HigButtonStyle.Primary, modifier = Modifier.fillMaxWidth())
                }
                2 -> {
                    HigField(value = priority, onValueChange = { priority = it.filter { c -> c.isDigit() } }, label = "Priority (Higher wins)")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        HigButton(text = "Back", onClick = { step = 2 }, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                        HigButton(
                            text = "Save Rule",
                            onClick = { onSave(Policy(id = initialPolicy?.id ?: "pol_${System.currentTimeMillis()}", name = name, type = type, targetID = "", action = action, priority = priority.toIntOrNull() ?: 50, scheduleIDs = emptyList(), enabled = true)) },
                            style = HigButtonStyle.Primary, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
