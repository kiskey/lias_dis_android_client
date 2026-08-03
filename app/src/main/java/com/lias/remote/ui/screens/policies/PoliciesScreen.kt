// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/policies/PoliciesScreen.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Removed unsafe `= viewModel()` default parameter.
//   2. Wired Delete IconButton to `viewModel.deletePolicy()`.
//   3. Replaced FAB placeholder with `PolicyWizardSheet` implementation.
// ====================================================================

package com.lias.remote.ui.screens.policies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable
fun PoliciesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    
    // FIX 3.3: State for Policy Wizard
    var showWizard by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // FIX 3.3: Open Wizard for new policy
                    editingPolicy = null
                    showWizard = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Policy")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.policies, key = { it.id }) { policy ->
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
                            // FIX 3.2: Wired delete policy
                            IconButton(onClick = { viewModel.deletePolicy(policy.id) }) {
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
                    }
                }
            }
        }
    }

    // FIX 3.3: Render Wizard
    if (showWizard) {
        PolicyWizardSheet(
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
}
