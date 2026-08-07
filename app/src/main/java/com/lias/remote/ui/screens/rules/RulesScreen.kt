// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt
// Version: 2.3.0
// Purpose: Policy Rules screen with categorized Global/Tag/Device policy cards.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ContextMenuItem
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigContextMenu
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MinutePickerSheet
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.screens.policies.PolicyWizardSheet
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.SystemGreenDark

@Composable
fun RulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showWizard by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }
    var policyToDelete by remember { mutableStateOf<Policy?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    var activeTagForExtend by remember { mutableStateOf<Tag?>(null) }

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

    HigLargeTitleScaffold(
        title = "Rules",
        navTrailing = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Actions", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Import Policies") },
                        leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            importLauncher.launch("application/json")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Policies") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            viewModel.exportPolicies {}
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPolicy = null
                    showWizard = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Policy", tint = Color.White)
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.policies.isEmpty() && state.isInitialLoaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No policies configured. Tap + to create one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                GroupedList {
                    val globalPolicies = state.policies.filter { it.type == "global" }
                    if (globalPolicies.isNotEmpty()) {
                        item { ListSectionHeader("Global Rules") }
                        item {
                            GroupedListCard {
                                globalPolicies.forEachIndexed { index, policy ->
                                    PolicyRow(
                                        policy = policy,
                                        schedules = state.schedules,
                                        viewModel = viewModel,
                                        showDivider = index < globalPolicies.size - 1,
                                        onEdit = { editingPolicy = policy; showWizard = true },
                                        onDelete = { policyToDelete = policy },
                                        onExtendTag = { tagId ->
                                            val t = state.tags.find { it.id == tagId }
                                            if (t != null) activeTagForExtend = t
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val tagPolicies = state.policies.filter { it.type == "tag" }
                    if (tagPolicies.isNotEmpty()) {
                        item { ListSectionHeader("Tag Rules") }
                        item {
                            GroupedListCard {
                                tagPolicies.forEachIndexed { index, policy ->
                                    PolicyRow(
                                        policy = policy,
                                        schedules = state.schedules,
                                        viewModel = viewModel,
                                        showDivider = index < tagPolicies.size - 1,
                                        onEdit = { editingPolicy = policy; showWizard = true },
                                        onDelete = { policyToDelete = policy },
                                        onExtendTag = { tagId ->
                                            val t = state.tags.find { it.id == tagId }
                                            if (t != null) activeTagForExtend = t
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val devicePolicies = state.policies.filter { it.type == "device" }
                    if (devicePolicies.isNotEmpty()) {
                        item { ListSectionHeader("Device Rules") }
                        item {
                            GroupedListCard {
                                devicePolicies.forEachIndexed { index, policy ->
                                    PolicyRow(
                                        policy = policy,
                                        schedules = state.schedules,
                                        viewModel = viewModel,
                                        showDivider = index < devicePolicies.size - 1,
                                        onEdit = { editingPolicy = policy; showWizard = true },
                                        onDelete = { policyToDelete = policy },
                                        onExtendTag = { }
                                    )
                                }
                            }
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

    activeTagForExtend?.let { tag ->
        val effectiveStatus = viewModel.effectiveStatusForTag(tag.id)
        MinutePickerSheet(
            targetLabel = tag.name,
            targetSubtitle = "Tag Group",
            currentExtension = effectiveStatus.activeExtension,
            onDismiss = { activeTagForExtend = null },
            onConfirm = { minutes ->
                viewModel.extendTagAccess(tag.id, minutes)
                activeTagForExtend = null
            },
            onCancelExtension = if (effectiveStatus.activeExtension != null) {
                {
                    viewModel.cancelTagExtension(tag.id)
                    activeTagForExtend = null
                }
            } else null
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
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
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
    showDivider: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExtendTag: (tagId: String) -> Unit
) {
    val isInfra = policy.targetID == "infrastructure"
    val isGlobal = policy.id == "global_default"
    val isPaused = policy.id.startsWith("pol_pause_")
    val canToggle = !isGlobal && !isPaused && !isInfra

    val tagStatus = if (policy.type == "tag") viewModel.effectiveStatusForTag(policy.targetID) else null
    val canExtendTag = ExtendHelper.isExtendAvailable(tagStatus)
    val activeTagExtension = tagStatus?.activeExtension

    val attachedSchedules = policy.resolveScheduleIDs().mapNotNull { id -> schedules.find { it.id == id } }
    val noScheduleWarning = policy.action == "schedule" && attachedSchedules.isEmpty()

    val subtitle = if (noScheduleWarning) {
        "Target: ${policy.targetID.ifBlank { "Global" }} · ⚠️ No schedule attached (defaults to Allow All)"
    } else {
        "Target: ${policy.targetID.ifBlank { "Global" }} · Priority: ${policy.priority}"
    }

    val contextMenuItems = listOf(
        ContextMenuItem(
            label = "Edit Rule",
            icon = Icons.Default.Edit,
            onClick = onEdit
        ),
        ContextMenuItem(
            label = "Delete Rule",
            icon = Icons.Default.Delete,
            isDestructive = true,
            onClick = onDelete
        )
    )

    HigContextMenu(
        items = contextMenuItems,
        onClick = { if (!isInfra) onEdit() }
    ) {
        HigSwipeRow(
            leadingAction = SwipeAction(
                label = "Edit",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onTrigger = onEdit
            ),
            trailingAction = SwipeAction(
                label = "Delete",
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.error,
                onTrigger = onDelete
            )
        ) {
            GroupedListRow(
                primaryText = policy.name + if (!policy.enabled) " (Disabled)" else if (isInfra) " 🔒" else "",
                secondaryText = subtitle,
                showDivider = showDivider,
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canExtendTag && policy.type == "tag") {
                            IconButton(
                                onClick = { onExtendTag(policy.targetID) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = "Extend Tag Access",
                                    tint = SystemGreenDark
                                )
                            }
                        }

                        if (activeTagExtension != null) {
                            val left = ExtendHelper.minutesUntil(activeTagExtension.expiresAt)
                            StatusPill(
                                text = "Allowed · ${left}m left",
                                tone = PillTone.Allowed
                            )
                        } else if (canToggle) {
                            Switch(
                                checked = policy.enabled,
                                onCheckedChange = { enabled ->
                                    viewModel.savePolicy(policy.copy(enabled = enabled))
                                }
                            )
                        } else {
                            val pillTone = when (policy.action) {
                                "block" -> PillTone.Blocked
                                "allow" -> PillTone.Allowed
                                else -> PillTone.Scheduled
                            }
                            StatusPill(
                                text = policy.action.uppercase(),
                                tone = pillTone
                            )
                        }
                    }
                },
                onClick = { if (!isInfra) onEdit() }
            )
        }
    }
}
