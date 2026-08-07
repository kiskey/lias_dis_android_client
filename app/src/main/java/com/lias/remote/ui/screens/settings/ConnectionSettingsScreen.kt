// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/ConnectionSettingsScreen.kt
// Version: 2.1.0
// Audit Fixes:
//   1. Migrated inputs to HigField and test trigger to HigButton.
//   2. Added thin navigation bar header (`‹ Settings` back and `Save`).
//   3. Rendered connection status feedback banner.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold

@Composable
fun ConnectionSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tempUrl by remember { mutableStateOf(state.serverUrl) }
    var tempToken by remember { mutableStateOf(state.authToken) }

    HigLargeTitleScaffold(
        title = "",
        navLeading = {
            TextButton(onClick = onBack) {
                Text("‹ Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        navTrailing = {
            TextButton(onClick = {
                viewModel.updateServerUrl(tempUrl)
                viewModel.updateAuthToken(tempToken)
                viewModel.saveSettings()
                onBack()
            }) {
                Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Connection Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W800
            )

            HigField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                label = "Server URL",
                placeholder = "http://192.168.1.1:8081"
            )

            HigField(
                value = tempToken,
                onValueChange = { tempToken = it },
                label = "Auth Token (Optional)",
                visualTransformation = PasswordVisualTransformation()
            )

            HigButton(
                text = "Test Connection",
                onClick = {
                    viewModel.updateServerUrl(tempUrl)
                    viewModel.updateAuthToken(tempToken)
                    viewModel.testConnection()
                },
                style = HigButtonStyle.Secondary
            )

            if (state.isTesting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            state.testResult?.let { result ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result,
                    color = if (result.startsWith("Connection successful") || result.startsWith("Settings saved")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
