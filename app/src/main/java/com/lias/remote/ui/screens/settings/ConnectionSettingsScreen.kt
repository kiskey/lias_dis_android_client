// ====================================================================
// File: ConnectionSettingsScreen.kt
// Version: 3.2.0 (Cupertino Refactor)
// Purpose: Standalone implementation using CupertinoScaffold,
//          CupertinoTopAppBar, and CupertinoSection. Resolves
//          duplicate overload conflict.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.section.CupertinoSection
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

@Composable
fun ConnectionSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var tempUrl by remember { mutableStateOf(state.serverUrl) }
    var tempToken by remember { mutableStateOf(state.authToken) }

    CupertinoTheme {
        CupertinoScaffold(
            topBar = {
                CupertinoTopAppBar(
                    title = { CupertinoText("Connection") },
                    navigationIcon = {
                        CupertinoButton(
                            onClick = onBack,
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) {
                            CupertinoText("‹ Settings")
                        }
                    },
                    actions = {
                        CupertinoButton(
                            onClick = {
                                viewModel.updateServerUrl(tempUrl)
                                viewModel.updateAuthToken(tempToken)
                                viewModel.saveSettings()
                                onBack()
                            },
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) {
                            CupertinoText("Save")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connection Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.W700
                )

                CupertinoSection {
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
                }

                HigButton(
                    text = "Test Connection",
                    onClick = {
                        viewModel.updateServerUrl(tempUrl)
                        viewModel.updateAuthToken(tempToken)
                        viewModel.testConnection()
                    },
                    style = HigButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.isTesting) {
                    Text("Testing...", modifier = Modifier.align(Alignment.CenterHorizontally))
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
}
