package com.lias.remote.ui.screens.devices

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusDot
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    
    var showTagEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var activeDeviceForExtend by remember { mutableStateOf<Device?>(null) }
    var activeDeviceForPause by remember { mutableStateOf<Device?>(null) }
    var activeDeviceForRename by remember { mutableStateOf<Device?>(null) }

    val groupedDevices = remember(state.devices, searchQuery, state.tags) {
        val filtered = if (searchQuery.isBlank()) state.devices else state.devices.filter {
            it.displayName.contains(searchQuery, true) || it.currentMAC.contains(searchQuery, true) || it.currentIP.contains(searchQuery, true)
        }
        val map = mutableMapOf<String, MutableList<Device>>()
        state.tags.forEach { map[it.id] = mutableListOf() }
        filtered.forEach { d ->
            val assigned = d.safeTags.ifEmpty { listOf("generic") }
            assigned.forEach { tagId -> map.getOrPut(tagId) { mutableListOf() }.add(d) }
        }
        map
    }

    HigLargeTitleScaffold(
        title = "Devices",
        scrollState = scrollState,
        searchPlaceholder = "Search by name, IP, or MAC",
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        navTrailing = {
            HigTextButton(text = "＋ Tag", onClick = { editingTag = null; showTagEditor = true })
        }
    ) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            state.tags.forEach { tag ->
                val devicesInTag = groupedDevices[tag.id] ?: emptyList()
                if (devicesInTag.isNotEmpty()) {
                    item(key = "header_${tag.id}") {
                        ListSectionHeader(
                            text = "${tag.name} · ${devicesInTag.size} devices",
                            trailingAction = {
                                if (tag.id != "infrastructure") {
                                    HigTextButton(text = "⏱ Extend All", onClick = {})
                                }
                            }
                        )
                    }
                    items(devicesInTag.size, key = { index -> devicesInTag[index].pdid }) { index ->
                        val device = devicesInTag[index]
                        val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
                        val status = viewModel.effectiveStatusFor(device.pdid)

                        DeviceCardItem(
                            device = device,
                            isPaused = isPaused,
                            statusAction = status.action,
                            onExtend = { activeDeviceForExtend = device },
                            onPause = { activeDeviceForPause = device },
                            onRename = { activeDeviceForRename = device },
                            onDetail = { onNavigateToDeviceDetail(device.pdid) }
                        )
                    }
                }
            }
        }
    }

    if (showTagEditor) {
        TagEditorSheet(
            initialTag = editingTag,
            onDismiss = { showTagEditor = false },
            onSave = { tag ->
                if (editingTag == null) viewModel.createTag(tag) else viewModel.updateTag(tag)
                showTagEditor = false
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
            },
            onCancelExtension = if (status.activeExtension != null) {
                { viewModel.cancelDeviceExtension(device.pdid); activeDeviceForExtend = null }
            } else null
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

    activeDeviceForRename?.let { device ->
        RenameDeviceDialog(
            currentName = device.displayName,
            onDismiss = { activeDeviceForRename = null },
            onConfirm = { newName ->
                viewModel.renameDevice(device.pdid, newName)
                activeDeviceForRename = null
            }
        )
    }
}

@Composable
private fun DeviceCardItem(
    device: Device,
    isPaused: Boolean,
    statusAction: String,
    onExtend: () -> Unit,
    onPause: () -> Unit,
    onRename: () -> Unit,
    onDetail: () -> Unit
) {
    val isInfra = device.safeTags.contains("infrastructure")
    val isBlockedOrPaused = isPaused || statusAction == "block"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LiasThemeColors.secondaryBackground)
            .border(0.5.dp, LiasThemeColors.separator, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(isOnline = device.online, isPaused = isPaused)
                        Spacer(modifier = Modifier.width(8.dp))
                        CupertinoText(device.displayName, style = HigTypography.headline, color = LiasThemeColors.label)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    CupertinoText(
                        text = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unclassified" }}",
                        style = HigTypography.caption,
                        color = LiasThemeColors.tertiaryLabel
                    )
                }
                StatusPill(
                    text = if (isInfra) "Immune" else if (isPaused) "Paused" else if (isBlockedOrPaused) "Blocked" else if (device.online) "Allow" else "Offline",
                    tone = if (isInfra) PillTone.INFO else if (isPaused) PillTone.PAUSED else if (isBlockedOrPaused) PillTone.BLOCKED else if (device.online) PillTone.ALLOWED else PillTone.INFO
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Infrastructure devices have NO pause/extend buttons
            if (!isInfra) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isBlockedOrPaused) {
                        HigButton(text = "⏱ Extend", onClick = onExtend, style = HigButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    } else {
                        HigButton(text = "⏸ Pause", onClick = onPause, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                    }
                    HigButton(text = "✏️", onClick = onRename, style = HigButtonStyle.Gray, modifier = Modifier.width(44.dp))
                    HigButton(text = "›", onClick = onDetail, style = HigButtonStyle.Gray, modifier = Modifier.width(44.dp))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HigButton(text = "View Details", onClick = onDetail, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                    HigButton(text = "✏️", onClick = onRename, style = HigButtonStyle.Gray, modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}

@Composable
fun RenameDeviceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    HigAlertDialog(
        onDismissRequest = onDismiss,
        title = "Rename Device",
        message = "Enter a friendly name for this device:",
        confirmText = "Save",
        onConfirm = { if (text.isNotBlank()) onConfirm(text) }
    )
}

@Composable
fun TagEditorSheet(initialTag: Tag?, onDismiss: () -> Unit, onSave: (Tag) -> Unit) {
    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialTag?.color ?: "#0A84FF") }
    val presetColors = listOf("#0A84FF", "#5856D6", "#FF9500", "#FF2D55", "#00C7BE", "#30D158", "#FFCC00", "#8E8E93")

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(
                title = if (initialTag == null) "New Tag" else "Edit Tag",
                onCancel = onDismiss,
                trailingAction = {
                    CupertinoButton(
                        onClick = {
                            val finalId = initialTag?.id ?: name.lowercase().replace(" ", "_")
                            onSave(Tag(id = finalId, name = name, color = selectedColor, precedence = initialTag?.precedence ?: 50, builtin = initialTag?.builtin ?: false))
                        },
                        colors = CupertinoButtonDefaults.plainButtonColors()
                    ) {
                        CupertinoText("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )

            HigField(value = name, onValueChange = { name = it }, label = "Tag Name", placeholder = "e.g. Nursery")

            Column(modifier = Modifier.fillMaxWidth()) {
                CupertinoText("BADGE COLOR", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presetColors.forEach { colorHex ->
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(if (isSelected) Modifier.border(2.dp, LiasThemeColors.label, CircleShape) else Modifier)
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }
        }
    }
}
