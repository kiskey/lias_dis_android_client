// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/policies/PoliciesScreen.kt
// Version: 1.6.0
// Audit Fixes: 
//   1. Updated PolicyWizardSheet caller to pass viewModel for server-side validation (GAP-A01).
// ====================================================================

package com.lias.remote.ui.screens.policies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.WeeklyTimeline

@Composable
fun PoliciesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    
    var showWizard by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }
    var policyToDelete by remember { mutableStateOf<Policy?>(null) }

    Scaffold(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.policies.isEmpty() && state.isInitialLoaded) {
                Text(
                    text = "No policies yet. Tap + to create one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.policies, key = { it.id }) { policy ->
                        PolicyCard(
                            policy = policy,
                            schedules = state.schedules,
                            onEditClick = {
                                editingPolicy = policy
                                showWizard = true
                            },
                            onDeleteClick = {
                                policyToDelete = policy
                            }
                        )
                    }
                }
            }
        }
    }

    if (showWizard) {
        PolicyWizardSheet(
            viewModel = viewModel, // Passed here for server-side validation
            initialPolicy = editingPolicy,
            tags = state.tags,
            schedules = state.schedules,
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
private fun PolicyCard(
    policy: Policy,
    schedules: List<com.lias.remote.core.models.Schedule>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    policy.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Policy")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                val actionColor = when (policy.action) {
                    "allow" -> MaterialTheme.colorScheme.primary
                    "block" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(actionColor, CircleShape)
                )
                Text(
                    text = " ${policy.action.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = actionColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Scope: ${policy.type} | Target: ${policy.targetID.ifBlank { "Global" }} | Priority: ${policy.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (policy.action == "schedule") {
                val attachedSchedules = policy.getScheduleIDs().mapNotNull { id ->
                    schedules.find { it.id == id }
                }
                val missingSchedules = policy.getScheduleIDs().filter { id ->
                    schedules.none { it.id == id }
                }

                Spacer(modifier = Modifier.size(12.dp))
                
                if (attachedSchedules.isEmpty() && missingSchedules.isEmpty()) {
                    Text(
                        text = "⚠️ No schedules attached. Defaults to ALLOW ALL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Attached Schedules (${attachedSchedules.size}): ${attachedSchedules.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (missingSchedules.isNotEmpty()) {
                    Text(
                        text = "⚠️ Missing Schedule(s): ${missingSchedules.joinToString()} (Fails Closed: BLOCK)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (attachedSchedules.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(8.dp))
                    WeeklyTimeline(schedules = attachedSchedules)
                }
            }
        }
    }
}
