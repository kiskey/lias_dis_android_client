package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToConnection: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    HigLargeTitleScaffold(title = "Settings", scrollState = scrollState) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { ListSectionHeader("Server") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GroupedListRow(
                        primaryText = "Connection",
                        secondaryText = if (uiState.serverUrl.isNotBlank()) "${uiState.serverUrl} · Connected" else "Not configured",
                        trailingContent = { CupertinoText("›", style = HigTypography.headline, color = LiasThemeColors.tertiaryLabel) },
                        showDivider = true,
                        onClick = onNavigateToConnection
                    )
                    GroupedListRow(
                        primaryText = "Server Health",
                        secondaryText = "v2.7 · 12ms latency · SSE live"
                    )
                }
            }

            item { ListSectionHeader("Appearance") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CupertinoText("Theme", style = HigTypography.headline, color = LiasThemeColors.label)
                        SegmentedControl(
                            options = listOf("System", "Light", "Dark"),
                            selectedOption = uiState.themeMode.replaceFirstChar { it.uppercase() },
                            onOptionSelected = { mode -> viewModel.updateThemeMode(mode.lowercase()) },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        CupertinoText(
                            text = "Follows your device's appearance setting.",
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item { ListSectionHeader("Quick Controls") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GroupedListRow(
                        primaryText = "Vacation Mode",
                        secondaryText = "Blocks all non-infrastructure devices",
                        trailingContent = {
                            CupertinoSwitch(
                                checked = uiState.vacationMode,
                                onCheckedChange = { viewModel.toggleVacationMode(it) }
                            )
                        },
                        showDivider = true
                    )
                    GroupedListRow(
                        primaryText = "Notifications",
                        secondaryText = "Security alerts & device changes",
                        trailingContent = { CupertinoSwitch(checked = true, onCheckedChange = {}) }
                    )
                }
            }

            item { ListSectionHeader("Danger Zone") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GroupedListRow(
                        primaryText = "Flush nftables Table",
                        secondaryText = "Rebuilds automatically on next sync",
                        isDestructive = true,
                        trailingContent = { CupertinoText("›", style = HigTypography.headline, color = LiasThemeColors.tertiaryLabel) },
                        onClick = { viewModel.flushNftables() }
                    )
                }
            }

            item { ListSectionHeader("About") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GroupedListRow(
                        primaryText = "LIAS Remote",
                        trailingContent = { CupertinoText("v1.6.0 (5)", style = HigTypography.subheadline, color = LiasThemeColors.tertiaryLabel) },
                        showDivider = true
                    )
                    GroupedListRow(
                        primaryText = "Server Version",
                        trailingContent = { CupertinoText("v2.7", style = HigTypography.subheadline, color = LiasThemeColors.tertiaryLabel) }
                    )
                }
            }
        }
    }
}
