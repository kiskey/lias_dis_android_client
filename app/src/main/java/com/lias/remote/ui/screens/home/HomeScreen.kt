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
import com.lias.remote.core.models.Device
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
import com.lias.remote.ui.navigation.LiasScreen
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.GlobalSwitchSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToTab: (LiasScreen) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    var showGlobalSheet by remember { mutableStateOf(false) }
    var activeDeviceForExtend by remember { mutableStateOf<Device?>(null) }
    var activeDeviceForPause by remember { mutableStateOf<Device?>(null) }

    val globalPolicy = state.policies.find { it.id == "global_default" } ?: Policy(
        id = "global_default", name = "Global Access Switch", type = "global", action = "schedule"
    )

    val isVacationActive = globalPolicy.action == "block"
    val totalDevices = state.devices.size
    val onlineDevices = state.devices.count { it.online }
    val offlineDevices = totalDevices - onlineDevices
    val activeEnforcements = state.policies.filter { it.enabled && (it.action == "block" || it.action == "allow") }

    HigLargeTitleScaffold(
        title = "Home",
        scrollState = scrollState,
        navTrailing = {
            CupertinoText(
                text = "🚨",
                style = HigTypography.headline,
                modifier = Modifier.clickable {
                    viewModel.triggerSecurityAlert()
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            // Hero Status Card
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isVacationActive) LiasThemeColors.orange.copy(alpha = 0.15f) 
                                else LiasThemeColors.green.copy(alpha = 0.12f)
                            )
                            .border(
                                0.5.dp, 
                                if (isVacationActive) LiasThemeColors.orange.copy(alpha = 0.4f) 
                                else LiasThemeColors.green.copy(alpha = 0.3f), 
                                RoundedCornerShape(16.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            CupertinoText(
                                text = if (isVacationActive) "✈️ VACATION MODE ACTIVE" else "🌐 NETWORK STATUS",
                                style = HigTypography.caption,
                                color = if (isVacationActive) LiasThemeColors.orange else LiasThemeColors.secondaryLabel,
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
                                text = "${activeEnforcements.size} active enforcements · Bedtime schedule ends in 2h 14m",
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
                                    text = if (isVacationActive) "✈️ Vacation ON" else "✈️ Vacation",
                                    onClick = { viewModel.toggleVacationMode(!isVacationActive) },
                                    style = if (isVacationActive) HigButtonStyle.Danger else HigButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Grid (Fully Interactive)
            item {
                ListSectionHeader("Quick Actions")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickTile(
                        icon = "📱", 
                        label = "Devices", 
                        color = LiasThemeColors.blue, 
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToTab(LiasScreen.Devices)
                    }
                    QuickTile(
                        icon = "⏱", 
                        label = "Extend Access", 
                        color = LiasThemeColors.green, 
                        modifier = Modifier.weight(1f)
                    ) {
                        activeDeviceForExtend = state.devices.firstOrNull { !it.safeTags.contains("infrastructure") }
                    }
                    QuickTile(
                        icon = "⏸", 
                        label = "Pause Device", 
                        color = LiasThemeColors.orange, 
                        modifier = Modifier.weight(1f)
                    ) {
                        activeDeviceForPause = state.devices.firstOrNull { !it.safeTags.contains("infrastructure") }
                    }
                    QuickTile(
                        icon = "🕒", 
                        label = "Schedule", 
                        color = LiasThemeColors.indigo, 
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigateToTab(LiasScreen.Schedules)
                    }
                }
            }

            // Active Enforcements List
            item {
                ListSectionHeader("Active Enforcements", trailingAction = {
                    HigTextButton(text = "View All", onClick = { onNavigateToTab(LiasScreen.Rules) })
                })
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LiveRow(
                        icon = "🚫",
                        iconBg = LiasThemeColors.red,
                        title = "Kids — Internet Blocked",
                        subtitle = "Bedtime · Mon–Fri 22:00–06:00 · 2h 14m left",
                        tone = PillTone.BLOCKED,
                        isLast = false
                    )
                    LiveRow(
                        icon = "✓",
                        iconBg = LiasThemeColors.green,
                        title = "IoT — Update Window",
                        subtitle = "Allowed until 02:30 · 18m left",
                        tone = PillTone.ALLOWED,
                        isLast = false
                    )
                    LiveRow(
                        icon = "⏸",
                        iconBg = LiasThemeColors.orange,
                        title = "Xbox — Paused",
                        subtitle = "Manual pause · 43m left",
                        tone = PillTone.PAUSED,
                        isLast = true
                    )
                }
            }

            // Network Snapshot
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

    activeDeviceForExtend?.let { device ->
        val status = viewModel.effectiveStatusFor(device.pdid)
        ExtendAccessSheet(
            targetLabel = device.displayName,
            targetSubtitle = device.currentIP.ifBlank { device.pdid },
            currentExtension = status.activeExtension,
            onDismiss = { activeDeviceForExtend = null },
            onConfirm = { mins ->
                viewModel.extendDeviceAccess(device.pdid, mins)
                activeDeviceForExtend = null
            }
        )
    }

    activeDeviceForPause?.let { device ->
        PauseSheet(
            targetLabel = device.displayName,
            onDismiss = { activeDeviceForPause = null },
            onConfirm = { mins ->
                viewModel.pauseDeviceInternet(device.pdid, mins)
                activeDeviceForPause = null
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
