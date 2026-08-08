package com.lias.remote.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.screens.GlobalSwitchSheet
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    var showGlobalSheet by remember { mutableStateOf(false) }

    val globalPolicy = state.policies.find { it.id == "global_default" } ?: Policy(
        id = "global_default", name = "Global Access Switch", type = "global", action = "schedule"
    )

    val totalDevices = state.devices.size
    val onlineDevices = state.devices.count { it.online }
    val offlineDevices = totalDevices - onlineDevices
    val activeEnforcements = state.policies.filter { it.enabled && it.action == "block" }

    HigLargeTitleScaffold(
        title = "Home",
        scrollState = scrollState,
        navTrailing = {
            CupertinoText(
                text = "🚨",
                style = HigTypography.headline,
                modifier = Modifier.clickable {
                    // Security alert sheet trigger
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            // Hero Network Status Card
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LiasThemeColors.green.copy(alpha = 0.12f))
                            .border(0.5.dp, LiasThemeColors.green.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            CupertinoText(
                                text = "🌐 NETWORK STATUS",
                                style = HigTypography.caption,
                                color = LiasThemeColors.secondaryLabel,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CupertinoText(
                                text = "$onlineDevices devices online",
                                style = HigTypography.title1,
                                color = LiasThemeColors.label,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CupertinoText(
                                text = "${activeEnforcements.size} active enforcements · Bedtime active",
                                style = HigTypography.subheadline,
                                color = LiasThemeColors.secondaryLabel
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HigButton(
                                    text = "Global Switch",
                                    onClick = { showGlobalSheet = true },
                                    style = HigButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                HigButton(
                                    text = "✈️ Vacation",
                                    onClick = { /* Vacation toggle */ },
                                    style = HigButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions 4-Tile Grid
            item {
                ListSectionHeader("Quick Actions")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickTile("📱", "Devices", LiasThemeColors.blue, Modifier.weight(1f)) {}
                    QuickTile("⏱", "Extend Access", LiasThemeColors.green, Modifier.weight(1f)) {}
                    QuickTile("⏸", "Pause Device", LiasThemeColors.orange, Modifier.weight(1f)) {}
                    QuickTile("🕒", "Schedule", LiasThemeColors.indigo, Modifier.weight(1f)) {}
                }
            }

            // Live Enforcements Grouped List
            item {
                ListSectionHeader("Active Enforcements", trailingAction = {
                    HigTextButton(text = "View All", onClick = { /* Navigate to rules */ })
                })
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (activeEnforcements.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            CupertinoText("No active enforcements", style = HigTypography.body, color = LiasThemeColors.secondaryLabel)
                        }
                    } else {
                        activeEnforcements.forEachIndexed { index, policy ->
                            LiveRow(
                                icon = "🚫",
                                iconBg = LiasThemeColors.red,
                                title = policy.name,
                                subtitle = "Target: ${policy.targetID.ifBlank { "Global" }}",
                                tone = PillTone.BLOCKED,
                                isLast = index == activeEnforcements.size - 1
                            )
                        }
                    }
                }
            }

            // Network Snapshot Metrics Bar
            item {
                ListSectionHeader("Network Snapshot")
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricColumn(value = "$totalDevices", label = "Total", color = LiasThemeColors.label)
                        Box(modifier = Modifier.width(0.5.dp).height(30.dp).background(LiasThemeColors.separator))
                        MetricColumn(value = "$onlineDevices", label = "Online", color = LiasThemeColors.green)
                        Box(modifier = Modifier.width(0.5.dp).height(30.dp).background(LiasThemeColors.separator))
                        MetricColumn(value = "$offlineDevices", label = "Offline", color = LiasThemeColors.tertiaryLabel)
                    }
                }
            }
        }
    }

    if (showGlobalSheet) {
        GlobalSwitchSheet(
            currentPolicy = globalPolicy,
            onDismiss = { showGlobalSheet = false },
            onSave = { policy ->
                viewModel.savePolicy(policy)
                showGlobalSheet = false
            }
        )
    }
}

@Composable
private fun QuickTile(icon: String, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LiasThemeColors.secondaryBackground)
            .border(0.5.dp, LiasThemeColors.separator, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            CupertinoText(icon, style = HigTypography.headline, color = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        CupertinoText(
            text = label,
            style = HigTypography.caption,
            color = LiasThemeColors.label,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun LiveRow(icon: String, iconBg: Color, title: String, subtitle: String, tone: PillTone, isLast: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) { CupertinoText(icon, color = Color.White) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                CupertinoText(title, style = HigTypography.headline, color = LiasThemeColors.label)
                CupertinoText(subtitle, style = HigTypography.subheadline, color = LiasThemeColors.tertiaryLabel)
            }
            StatusPill(text = tone.name, tone = tone)
        }
        if (!isLast) {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(start = 16.dp).background(LiasThemeColors.separator))
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CupertinoText(value, style = HigTypography.title1, fontWeight = FontWeight.ExtraBold, color = color)
        CupertinoText(label.uppercase(), style = HigTypography.caption, color = color, fontWeight = FontWeight.Bold)
    }
}
