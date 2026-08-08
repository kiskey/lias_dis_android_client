// ====================================================================
// File: SettingsScreen.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Settings list. Integrated ConnectionSettingsScreen.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
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
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigSpec
import io.github.robinpcrd.cupertino.CupertinoActivityIndicator
import io.github.robinpcrd.cupertino.CupertinoSwitch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateToConnection: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    var showFlushDialog by remember { mutableStateOf(false) }

    HigLargeTitleScaffold(title = "Settings", scrollState = scrollState) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { ListSectionHeader("Server") }
            item {
                GroupedListCard {
                    GroupedListRow(
                        primaryText = "Connection",
                        secondaryText = if (uiState.serverUrl.isNotBlank()) "${uiState.serverUrl} · Connected" else "Not configured",
                        leadingContent = { Box(modifier = Modifier.size(HigSpec.IconBubbleSize), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary) } },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onNavigateToConnection
                    )
                }
            }

            item { ListSectionHeader("Appearance") }
            item {
                GroupedListCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Auto switches between Light and Dark based on system setting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SegmentedControl(
                            options = listOf("System", "Light", "Dark"),
                            selectedOption = uiState.themeMode.replaceFirstChar { it.uppercase() },
                            onOptionSelected = { mode -> viewModel.updateThemeMode(mode.lowercase()) },
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                        )
                    }
                }
            }

            item { ListSectionHeader("Controls") }
            item {
                GroupedListCard {
                    GroupedListRow(
                        primaryText = "Vacation Mode",
                        secondaryText = "Block all non-infrastructure devices",
                        leadingContent = { Box(modifier = Modifier.size(HigSpec.IconBubbleSize), contentAlignment = Alignment.Center) { Icon(Icons.Filled.FlightTakeoff, null, tint = MaterialTheme.colorScheme.error) } },
                        trailingContent = { CupertinoSwitch(checked = uiState.vacationMode, onCheckedChange = { viewModel.toggleVacationMode(it) }) }
                    )
                }
            }

            item { ListSectionHeader("Danger Zone") }
            item {
                GroupedListCard {
                    GroupedListRow(
                        primaryText = "Flush Nftables Table",
                        secondaryText = "Rebuilds automatically on next sync",
                        isDestructive = true,
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { showFlushDialog = true }
                    )
                }
            }
        }
    }

    if (showFlushDialog) {
        HigAlertDialog(
            onDismissRequest = { showFlushDialog = false },
            title = "Flush nftables?",
            message = "This will temporarily disable all LIAS enforcement. Internet access will be unrestricted until LIAS rebuilds the table.",
            confirmText = "Flush",
            onConfirm = { viewModel.flushNftables() },
            isDestructive = true
        )
    }
}

@Composable
fun ConnectionSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var tempUrl by remember { mutableStateOf(state.serverUrl) }
    var tempToken by remember { mutableStateOf(state.authToken) }

    HigLargeTitleScaffold(
        title = "",
        navLeading = { HigTextButton(text = "‹ Settings", onClick = onBack) },
        navTrailing = { HigTextButton(text = "Save", onClick = { viewModel.updateServerUrl(tempUrl); viewModel.updateAuthToken(tempToken); viewModel.saveSettings(); onBack() }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Connection Settings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            HigField(value = tempUrl, onValueChange = { tempUrl = it }, label = "Server URL", placeholder = "http://192.168.1.1:8081")
            HigField(value = tempToken, onValueChange = { tempToken = it }, label = "Auth Token (Optional)", visualTransformation = PasswordVisualTransformation())
            HigButton(
                text = "Test Connection",
                onClick = { viewModel.updateServerUrl(tempUrl); viewModel.updateAuthToken(tempToken); viewModel.testConnection() },
                style = HigButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isTesting) { CupertinoActivityIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
            state.testResult?.let { result ->
                Text(text = result, color = if (result.startsWith("Connection successful") || result.startsWith("Settings saved")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
