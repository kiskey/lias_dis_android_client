// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt
// Version: 1.6.0
// Audit Fixes: 
//   1. Added read-only lock state for Server URL and Auth Token to prevent accidental edits.
//   2. Provided "Edit Connection Settings" toggle button to unlock credentials for editing.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showFlushDialog by remember { mutableStateOf(false) }
    var isEditingConnection by remember { mutableStateOf(uiState.serverUrl.isBlank()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Server Connection",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (!isEditingConnection) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.serverUrl,
                    onValueChange = viewModel::updateServerUrl,
                    enabled = isEditingConnection,
                    label = { Text("LIAS Server URL") },
                    placeholder = { Text("e.g., http://192.168.1.1:8081") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = uiState.authToken,
                    onValueChange = viewModel::updateAuthToken,
                    enabled = isEditingConnection,
                    label = { Text("Auth Token (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                if (isEditingConnection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveSettings()
                                isEditingConnection = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Connection")
                        }
                        if (uiState.serverUrl.isNotBlank()) {
                            TextButton(onClick = { isEditingConnection = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::testConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Connection")
                        }
                        Button(
                            onClick = { isEditingConnection = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Edit")
                        }
                    }
                }

                if (uiState.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                uiState.testResult?.let { result ->
                    Text(
                        text = result,
                        color = if (result.startsWith("Success") || result.startsWith("Connection")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vacation Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Immediately blocks all internet access for all devices (except Infrastructure)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.vacationMode,
                        onCheckedChange = { viewModel.toggleVacationMode(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(32.dp))

        Text(
            text = "Danger Zone",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.size(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("System Maintenance", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Flush the netdev lancontrol table on the LIAS server. LIAS will rebuild it on the next sync cycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { showFlushDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Flush Nftables Table")
                }
            }
        }
    }

    if (showFlushDialog) {
        AlertDialog(
            onDismissRequest = { showFlushDialog = false },
            title = { Text("Confirm Flush") },
            text = { Text("Are you sure you want to flush all nftables rules? Internet access will be temporarily unrestricted until LIAS rebuilds the table.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.flushNftables()
                        showFlushDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Flush") }
            },
            dismissButton = {
                TextButton(onClick = { showFlushDialog = false }) { Text("Cancel") }
            }
        )
    }
}
