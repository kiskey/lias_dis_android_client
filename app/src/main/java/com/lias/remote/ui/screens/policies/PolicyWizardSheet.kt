// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/policies/PolicyWizardSheet.kt
// Version: 2.1.0
// Audit Fixes:
//   1. Replaced all Checkbox and FilterChip selection controls with single-select GroupedListRows with trailing checkmarks.
//   2. Replaced progress bar with HIG step pills and caption ("Step N of 3 — Enforcement").
//   3. Migrated inputs to HigField and wizard actions to HigButton.
//   4. Step 3 conflict preview renders using DetailedWeekGrid with red conflict bands.
// ====================================================================

package com.lias.remote.ui.screens.policies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.DetailedWeekGrid
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemGreenDark
import com.lias.remote.ui.theme.SystemOrangeDark
import com.lias.remote.ui.theme.SystemRedDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyWizardSheet(
    viewModel: LiasViewModel,
    initialPolicy: Policy?,
    tags: List<Tag>,
    schedules: List<Schedule>,
    existingPolicies: List<Policy> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Policy) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.state.collectAsState()
    val allPolicies = if (existingPolicies.isNotEmpty()) existingPolicies else uiState.policies

    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf(initialPolicy?.name ?: "") }
    var type by remember { mutableStateOf(initialPolicy?.type ?: "tag") }
    var targetID by remember { mutableStateOf(initialPolicy?.targetID ?: "") }
    var action by remember { mutableStateOf(initialPolicy?.action ?: "schedule") }
    var priority by remember { mutableStateOf(initialPolicy?.priority?.toString() ?: "50") }

    val selectedSchedules = remember {
        mutableStateListOf<String>().apply { addAll(initialPolicy?.resolveScheduleIDs() ?: emptyList()) }
    }

    var shadowWarning by remember { mutableStateOf<String?>(null) }
    var serverConflictWarning by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedScheduleObjects = schedules.filter { it.id in selectedSchedules }
    val localConflicts = remember(selectedScheduleObjects) {
        ScheduleProjection.detectConflicts(selectedScheduleObjects)
    }

    LaunchedEffect(type, targetID, name, allPolicies) {
        if (type != "global" && targetID.isNotBlank()) {
            val existing = allPolicies.find {
                it.id != initialPolicy?.id && it.type == type && it.targetID == targetID
            }
            if (existing != null) {
                shadowWarning = "⚠️ Shadow Policy Warning: Policy '${existing.name}' already targets this $type. The higher priority policy will take precedence."
            } else {
                shadowWarning = null
            }
        } else {
            shadowWarning = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HIG Step Indicator Pills
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        Surface(
                            shape = CircleShape,
                            color = if (step == i) MaterialTheme.colorScheme.primary else LiasThemeColors.fill,
                            modifier = Modifier.size(width = 24.dp, height = 6.dp)
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (step) {
                        1 -> "Step 1 of 3 — Scope & Target"
                        2 -> "Step 2 of 3 — Enforcement"
                        else -> "Step 3 of 3 — Schedules"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = if (initialPolicy == null) "New Rule" else "Edit Rule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(48.dp)) // Spacer symmetry
            }

            when (step) {
                1 -> {
                    HigField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Rule Name",
                        placeholder = "e.g. Nursery Downtime"
                    )

                    Text("TARGET TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Single-select GroupedListRow with trailing checkmarks (No Material chips/checkboxes)
                    GroupedListCard {
                        listOf("global" to "Global (Network-wide)", "tag" to "Tag Group", "device" to "Specific Device").forEachIndexed { index, (typeOpt, labelStr) ->
                            val isSelected = type == typeOpt
                            GroupedListRow(
                                primaryText = labelStr,
                                trailingContent = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                showDivider = index < 2,
                                onClick = {
                                    type = typeOpt
                                    if (typeOpt == "global") targetID = ""
                                }
                            )
                        }
                    }

                    if (type == "tag") {
                        Text("SELECT TAG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GroupedListCard {
                            val selectableTags = tags.filter { it.id != "infrastructure" }
                            selectableTags.forEachIndexed { index, tag ->
                                val isSelected = targetID == tag.id
                                GroupedListRow(
                                    primaryText = tag.name,
                                    trailingContent = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    showDivider = index < selectableTags.size - 1,
                                    onClick = { targetID = tag.id }
                                )
                            }
                        }
                    }

                    shadowWarning?.let {
                        Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                    }

                    HigButton(
                        text = "Next",
                        onClick = { step = 2 },
                        enabled = name.isNotBlank() && (type == "global" || targetID.isNotBlank()),
                        style = HigButtonStyle.Primary
                    )
                }

                2 -> {
                    Text("ENFORCEMENT ACTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Single-select GroupedListRow with colored leading dot and trailing checkmarks
                    GroupedListCard {
                        val actions = listOf(
                            Triple("schedule", "Scheduled Access", SystemOrangeDark),
                            Triple("allow", "Always Allow", SystemGreenDark),
                            Triple("block", "Always Block", SystemRedDark)
                        )
                        actions.forEachIndexed { index, (actOpt, labelStr, dotColor) ->
                            val isSelected = action == actOpt
                            GroupedListRow(
                                primaryText = labelStr,
                                leadingContent = {
                                    Surface(shape = CircleShape, color = dotColor, modifier = Modifier.size(10.dp)) {}
                                },
                                trailingContent = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                showDivider = index < actions.size - 1,
                                onClick = { action = actOpt }
                            )
                        }
                    }

                    HigField(
                        value = priority,
                        onValueChange = { priority = it.filter { c -> c.isDigit() } },
                        label = "Priority Number (Higher value wins)"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        HigButton(
                            text = "Back",
                            onClick = { step = 1 },
                            style = HigButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HigButton(
                            text = if (action == "schedule") "Next" else "Save Rule",
                            onClick = {
                                if (action != "schedule") {
                                    onSave(Policy(
                                        id = initialPolicy?.id ?: "pol_${System.currentTimeMillis()}",
                                        name = name, type = type, targetID = targetID,
                                        action = action, priority = priority.toIntOrNull() ?: 50,
                                        enabled = initialPolicy?.enabled ?: true
                                    ))
                                } else {
                                    step = 3
                                }
                            },
                            style = HigButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                3 -> {
                    Text("ATTACH SCHEDULES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (selectedSchedules.isEmpty()) {
                        Text("⚠️ No schedules selected. Policy will default to ALLOW ALL.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (localConflicts.isNotEmpty()) {
                        Text("⚠️ Local schedule conflict detected!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    serverConflictWarning?.let { warning ->
                        Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    GroupedListCard {
                        schedules.forEachIndexed { index, sched ->
                            val isChecked = sched.id in selectedSchedules
                            GroupedListRow(
                                primaryText = sched.name,
                                secondaryText = "${sched.mode.uppercase()} · ${sched.timezone}",
                                trailingContent = {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            serverConflictWarning = null
                                            if (it) selectedSchedules.add(sched.id)
                                            else selectedSchedules.remove(sched.id)
                                        }
                                    )
                                },
                                showDivider = index < schedules.size - 1,
                                onClick = {
                                    serverConflictWarning = null
                                    if (isChecked) selectedSchedules.remove(sched.id)
                                    else selectedSchedules.add(sched.id)
                                }
                            )
                        }
                    }

                    if (selectedScheduleObjects.isNotEmpty()) {
                        DetailedWeekGrid(schedules = selectedScheduleObjects, conflicts = localConflicts)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        HigButton(
                            text = "Back",
                            onClick = { step = 2 },
                            style = HigButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HigButton(
                            text = if (isSaving) "Validating..." else "Save Rule",
                            onClick = {
                                isSaving = true
                                serverConflictWarning = null
                                scope.launch {
                                    val serverResult = viewModel.validatePolicy(selectedSchedules.toList())
                                    isSaving = false
                                    when (serverResult) {
                                        is ApiResult.Success -> {
                                            if (serverResult.data.isEmpty()) {
                                                onSave(Policy(
                                                    id = initialPolicy?.id ?: "pol_${System.currentTimeMillis()}",
                                                    name = name, type = type, targetID = targetID,
                                                    action = action, priority = priority.toIntOrNull() ?: 50,
                                                    scheduleIDs = selectedSchedules.toList(),
                                                    enabled = initialPolicy?.enabled ?: true
                                                ))
                                            } else {
                                                serverConflictWarning = "⚠️ Server detected ${serverResult.data.size} schedule conflict(s)."
                                            }
                                        }
                                        is ApiResult.HttpError -> {
                                            serverConflictWarning = "⚠️ Validation error: ${serverResult.message}"
                                        }
                                        is ApiResult.NetworkError -> {
                                            serverConflictWarning = "⚠️ Network error: ${serverResult.cause.message}"
                                        }
                                        else -> {
                                            serverConflictWarning = "⚠️ Unexpected validation error occurred."
                                        }
                                    }
                                }
                            },
                            enabled = localConflicts.isEmpty() && !isSaving,
                            style = HigButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
