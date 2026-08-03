// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Removed unsafe `= viewModel()` default parameter.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Server Connection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(24.dp))

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
                    label = { Text("LIAS Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = uiState.authToken,
                    onValueChange = viewModel::updateAuthToken,
                    label = { Text("Auth Token (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                if (uiState.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Connection")
                    }
                    Button(
                        onClick = viewModel::saveSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Settings")
                    }
                }

                uiState.testResult?.let { result ->
                    Text(
                        text = result,
                        color = if (result.startsWith("Success") || result.startsWith("Connection")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
