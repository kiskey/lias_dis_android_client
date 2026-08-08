// ====================================================================
// File: DevicesScreen.kt
// Version: 3.2.0 (Cupertino Refactor)
// Purpose: Fixed mutableStateOf type inference. Uses CupertinoSection
//          for grouped lists.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigSpec
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.section.CupertinoSection

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingTag = null; showTagEditor = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) { Icon(Icons.Filled.Add, "New Tag", tint = Color.White) }
        }
    ) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            state.tags.forEach { tag ->
                val devicesInTag = groupedDevices[tag.id] ?: emptyList()
                if (devicesInTag.isNotEmpty()) {
                    item(key = "header_${tag.id}") { ListSectionHeader("${tag.name} · ${devicesInTag.size}") }
                    item(key = "card_${tag.id}") {
                        CupertinoSection {
                            devicesInTag.forEachIndexed { index, device ->
                                val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
                                
                                HigSwipeRow(
                                    trailingAction = SwipeAction(
                                        icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        color = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        onTrigger = {
                                            if (isPaused) viewModel.unpauseDeviceInternet(device.pdid)
                                            else activeDeviceForPause = device
                                        }
                                    )
                                ) {
                                    GroupedListRow(
                                        primaryText = device.displayName,
                                        secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown" }}",
                                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        showDivider = index < devicesInTag.size - 1,
                                        onClick = { onNavigateToDeviceDetail(device.pdid) }
                                    )
                                }
                            }
                        }
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
                        CupertinoText("Save")
                    }
                }
            )

            HigField(value = name, onValueChange = { name = it }, label = "Tag Name", placeholder = "e.g. Nursery")

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("BADGE COLOR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presetColors.forEach { colorHex ->
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape).border(4.dp, color, CircleShape) else Modifier)
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }
        }
    }
}
