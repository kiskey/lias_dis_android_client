// ====================================================================
// File: RulesScreen.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Policy list grouped by type. Toggles for enable/disable.
//          Preserves Policy API and validation contracts.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.screens.policies.PolicyWizardSheet
import com.lias.remote.ui.theme.HigSpec
import io.github.robinpcrd.cupertino.CupertinoSwitch

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPolicy = null
                    showWizard = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Rule", tint = Color.White)
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
                item { ListSectionHeader("Global") }
                item {
                    GroupedListCard {
                        globalPolicies.forEach { policy ->
                            PolicyRow(
                                policy = policy,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy }
                            )
                        }
                    }
                }
            }

            if (tagPolicies.isNotEmpty()) {
                item { ListSectionHeader("Tag Rules") }
                item {
                    GroupedListCard {
                        tagPolicies.forEach { policy ->
                            PolicyRow(
                                policy = policy,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy }
                            )
                        }
                    }
                }
            }

            if (devicePolicies.isNotEmpty()) {
                item { ListSectionHeader("Device Rules") }
                item {
                    GroupedListCard {
                        devicePolicies.forEach { policy ->
                            PolicyRow(
                                policy = policy,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWizard) {
        PolicyWizardSheet(
            viewModel = viewModel,
            initialPolicy = editingPolicy,
            onDismiss = { showWizard = false },
            onSave = { 
                viewModel.savePolicy(it)
                showWizard = false
            }
        )
    }

    policyToDelete?.let { policy ->
        HigAlertDialog(
            onDismissRequest = { policyToDelete = null },
            title = "Delete Rule",
            message = "Are you sure you want to delete the policy '${policy.name}'?",
            confirmText = "Delete",
            onConfirm = { viewModel.deletePolicy(policy.id) },
            isDestructive = true
        )
    }
}

@Composable
private fun PolicyRow(
    policy: Policy,
    viewModel: LiasViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    HigSwipeRow(
        leadingAction = SwipeAction(
            icon = Icons.Filled.Edit,
            color = MaterialTheme.colorScheme.primary,
            onTrigger = onEdit
        ),
        trailingAction = SwipeAction(
            icon = Icons.Filled.Delete,
            color = MaterialTheme.colorScheme.error,
            onTrigger = onDelete
        )
    ) {
        GroupedListRow(
            primaryText = policy.name,
            secondaryText = "Target: ${policy.targetID.ifBlank { "Global" }} · Priority: ${policy.priority}",
            trailingContent = {
                if (policy.id != "global_default") {
                    CupertinoSwitch(
                        checked = policy.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.savePolicy(policy.copy(enabled = enabled))
                        }
                    )
                } else {
                    StatusPill(text = policy.action, tone = PillTone.INFO)
                }
            },
            onClick = onEdit
        )
    }
}
