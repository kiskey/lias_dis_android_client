package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun DeviceDetailScreen(
    pdid: String,
    viewModel: LiasViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    val device = state.devices.find { it.pdid == pdid } ?: return

    var logs by remember { mutableStateOf<List<FlowLog>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(true) }
    var showExtendSheet by remember { mutableStateOf(false) }
    var showPauseSheet by remember { mutableStateOf(false) }
    var showUserAssignmentSheet by remember { mutableStateOf(false) }

    val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
    val assignedUser = state.users.find { it.id == device.userID }

    LaunchedEffect(pdid) {
        val result = viewModel.getDeviceLogs(pdid)
        if (result is ApiResult.Success<List<FlowLog>>) {
            logs = result.data
        }
        isLoadingLogs = false
    }

    HigLargeTitleScaffold(
        title = "",
        scrollState = scrollState,
        navLeading = { HigTextButton(text = "‹ Devices", onClick = onBack) }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            // Profile Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LiasThemeColors.blue),
                        contentAlignment = Alignment.Center
                    ) {
                        CupertinoText("📱", style = HigTypography.largeTitle, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    CupertinoText(device.displayName, style = HigTypography.title1, fontWeight = FontWeight.ExtraBold)
                    CupertinoText(
                        text = if (device.online) "● Online now · ${device.currentIP}" else "● Offline",
                        style = HigTypography.body,
                        color = if (device.online) LiasThemeColors.green else LiasThemeColors.tertiaryLabel,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Sticky Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPaused) {
                            HigButton(
                                text = "▶ Resume",
                                onClick = { viewModel.unpauseDeviceInternet(device.pdid) },
                                style = HigButtonStyle.Primary,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            HigButton(
                                text = "⏱ Extend",
                                onClick = { showExtendSheet = true },
                                style = HigButtonStyle.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                            HigButton(
                                text = "✏ Rename",
                                onClick = { /* Prompt rename */ },
                                style = HigButtonStyle.Gray,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Identity Grouped Section
            item { ListSectionHeader("Identity") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GroupedListRow(primaryText = "Hostname", secondaryText = device.hostname.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "MAC Address", secondaryText = device.currentMAC.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "Vendor", secondaryText = device.vendor.ifBlank { "Unknown" }, showDivider = true)
                    GroupedListRow(primaryText = "Type", secondaryText = device.deviceType.ifBlank { "Unclassified" }, showDivider = true)
                    
                    GroupedListRow(
                        primaryText = "Assigned User",
                        secondaryText = assignedUser?.name ?: "Unassigned",
                        trailingContent = { CupertinoText("›", style = HigTypography.headline, color = LiasThemeColors.tertiaryLabel) },
                        onClick = { showUserAssignmentSheet = true }
                    )
                }
            }

            // Discovered Services Section
            if (device.safeServices.isNotEmpty()) {
                item { ListSectionHeader("Discovered Services") }
                item {
                    GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        device.safeServices.forEachIndexed { index, service ->
                            GroupedListRow(
                                primaryText = service,
                                showDivider = index < device.safeServices.size - 1
                            )
                        }
                    }
                }
            }

            // Activity Log Section
            item { ListSectionHeader("Activity · Last 24h") }
            item {
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (isLoadingLogs) {
                        GroupedListRow(primaryText = "Loading logs...")
                    } else if (logs.isEmpty()) {
                        GroupedListRow(primaryText = "No recent activity logged.")
                    } else {
                        logs.forEachIndexed { index, log ->
                            val isBlock = log.action == "block"
                            GroupedListRow(
                                primaryText = log.timestamp,
                                secondaryText = if (log.bytes > 0) "${log.bytes} bytes transferred" else null,
                                trailingContent = { 
                                    StatusPill(text = log.action, tone = if (isBlock) PillTone.BLOCKED else PillTone.ALLOWED) 
                                },
                                showDivider = index < logs.size - 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExtendSheet) {
        val status = viewModel.effectiveStatusFor(device.pdid)
        ExtendAccessSheet(
            targetLabel = device.displayName,
            targetSubtitle = device.currentIP.ifBlank { device.pdid },
            currentExtension = status.activeExtension,
            onDismiss = { showExtendSheet = false },
            onConfirm = { mins ->
                viewModel.extendDeviceAccess(device.pdid, mins)
                showExtendSheet = false
            },
            onCancelExtension = if (status.activeExtension != null) {
                { viewModel.cancelDeviceExtension(device.pdid); showExtendSheet = false }
            } else null
        )
    }

    if (showPauseSheet) {
        PauseSheet(
            targetLabel = device.displayName,
            onDismiss = { showPauseSheet = false },
            onConfirm = { mins ->
                viewModel.pauseDeviceInternet(device.pdid, mins)
                showPauseSheet = false
            }
        )
    }

    if (showUserAssignmentSheet) {
        UserAssignmentSheet(
            users = state.users,
            assignedUserId = device.userID,
            onDismiss = { showUserAssignmentSheet = false },
            onSelectUser = { userId ->
                viewModel.assignDeviceUser(device.pdid, userId)
                showUserAssignmentSheet = false
            },
            onCreateUser = { userName ->
                viewModel.createUser(User(id = "user_${System.currentTimeMillis()}", name = userName))
            }
        )
    }
}

@Composable
fun UserAssignmentSheet(
    users: List<User>,
    assignedUserId: String?,
    onDismiss: () -> Unit,
    onSelectUser: (userId: String) -> Unit,
    onCreateUser: (name: String) -> Unit
) {
    var newUserName by remember { mutableStateOf("") }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = "Assign User", onCancel = onDismiss)

            if (users.isNotEmpty()) {
                GroupedListCard {
                    users.forEachIndexed { index, user ->
                        val isSelected = user.id == assignedUserId
                        GroupedListRow(
                            primaryText = user.name,
                            trailingContent = {
                                if (isSelected) {
                                    CupertinoText("✓", color = LiasThemeColors.blue, fontWeight = FontWeight.Bold)
                                }
                            },
                            showDivider = index < users.size - 1,
                            onClick = { onSelectUser(user.id) }
                        )
                    }
                }
            }

            HigField(
                value = newUserName,
                onValueChange = { newUserName = it },
                label = "New User Profile",
                placeholder = "e.g. John Doe"
            )

            HigButton(
                text = "Create & Assign User",
                onClick = {
                    if (newUserName.isNotBlank()) {
                        onCreateUser(newUserName)
                        newUserName = ""
                    }
                },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
