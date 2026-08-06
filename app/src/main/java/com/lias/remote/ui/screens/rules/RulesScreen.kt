// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt
// Version: 2.0.1
// Audit Fixes:
//   1. Added `import androidx.compose.foundation.lazy.items` to resolve model parameter list overloading.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeActionRow
import com.lias.remote.ui.screens.policies.PolicyWizardSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showWizard by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }
    var policyToDelete by remember { mutableStateOf<Policy?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val jsonString = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                jsonString?.let { json -> viewModel.importPolicies(json) }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rules", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import Policies") },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch("application/json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Policies") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.exportPolicies {}
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPolicy = null
                    showWizard = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Policy")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.policies.isEmpty() && state.isInitialLoaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No policies yet. Tap + to create one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                GroupedList {
                    // Global Rules
                    val globalPolicies = state.policies.filter { it.type == "global" }
                    if (globalPolicies.isNotEmpty()) {
                        item { ListSectionHeader("Global") }
                        items(globalPolicies, key = { it.id }) { policy ->
                            PolicyRow(
                                policy = policy,
                                schedules = state.schedules,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy }
                            )
                        }
                    }

                    // Tag Rules
                    val tagPolicies = state.policies.filter { it.type == "tag" }
                    if (tagPolicies.isNotEmpty()) {
                        item { ListSectionHeader("Tag Rules") }
                        items(tagPolicies, key = { it.id }) { policy ->
                            PolicyRow(
                                policy = policy,
                                schedules = state.schedules,
                                viewModel = viewModel,
                                onEdit = { editingPolicy = policy; showWizard = true },
                                onDelete = { policyToDelete = policy }
                            )
                        }
                    }

                    // Device Rules
                    val devicePolicies = state.policies.filter { it.type == "device" }
                    if (devicePolicies.isNotEmpty()) {
                        item { ListSectionHeader("Device Rules") }
                        items(devicePolicies, key = { it.id }) { policy ->
                            PolicyRow(
                                policy = policy,
                                schedules = state.schedules,
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
            tags = state.tags,
            schedules = state.schedules,
            existingPolicies = state.policies,
            onDismiss = { showWizard = false },
            onSave = { policy ->
                viewModel.savePolicy(policy)
                showWizard = false
            }
        )
    }

    policyToDelete?.let { policy ->
        AlertDialog(
            onDismissRequest = { policyToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete the policy '${policy.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePolicy(policy.id)
                        policyToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { policyToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PolicyRow(
    policy: Policy,
    schedules: List<Schedule>,
    viewModel: LiasViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isInfra = policy.targetID == "infrastructure"
    val isGlobal = policy.id == "global_default"
    val isPaused = policy.id.startsWith("pol_pause_")
    val canToggle = !isGlobal && !isPaused && !isInfra

    SwipeActionRow(
        onSwipeLeft = { if (!isGlobal && !isPaused) onDelete() },
        onSwipeRight = { if (!isInfra) onEdit() }
    ) {
        GroupedListRow(
            primaryText = policy.name + if (!policy.enabled) " (Disabled)" else "",
            secondaryText = "Target: ${policy.targetID.ifBlank { "Global" }} · Priority: ${policy.priority}",
            trailingContent = {
                if (canToggle) {
                    Switch(
                        checked = policy.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.savePolicy(policy.copy(enabled = enabled))
                        }
                    )
                } else {
                    val isBlock = policy.action == "block"
                    StatusPill(
                        text = policy.action.uppercase(),
                        color = if (isBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        backgroundColor = if (isBlock) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                }
            },
            onClick = { if (!isInfra) onEdit() }
        )
    }
}
