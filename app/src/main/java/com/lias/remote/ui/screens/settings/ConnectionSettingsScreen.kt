// ====================================================================
// File: ConnectionSettingsScreen.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Server connection form. Preserves /health API test contract.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import io.github.robinpcrd.cupertino.CupertinoActivityIndicator

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
        navLeading = { HigTextButton(text = "‹ Settings", onClick = onBack) },
        navTrailing = {
            HigTextButton(
                text = "Save",
                onClick = {
                    viewModel.updateServerUrl(tempUrl)
                    viewModel.updateAuthToken(tempToken)
                    viewModel.saveSettings()
                    onBack()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Connection Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
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
                CupertinoActivityIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            state.testResult?.let { result ->
                Text(
                    text = result,
                    color = if (result.startsWith("Connection successful") || result.startsWith("Settings saved")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
