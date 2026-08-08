// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt
// Version: 6.0.0
//
// Purpose:
//   Device inventory and lightweight device actions.
//
// UX corrections:
//   - Loading != empty != failure != stale.
//   - Search-empty state is distinct from inventory-empty state.
//   - Untagged/unknown tag devices remain visible.
//   - Effective status may be unknown while loading.
//   - Device cards no longer claim "Allowed" simply because an
//     effective-status request has not completed.
//   - Infrastructure immunity remains visible and immutable.
// ====================================================================

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
import androidx.compose.foundation.lazy.items
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
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Tag
import com.lias.remote.repositories.SyncState
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
import com.lias.remote.ui.components.ScreenStateTone
import com.lias.remote.ui.components.ScreenStateView
import com.lias.remote.ui.components.StaleDataNotice
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
    val state by
        viewModel.state.collectAsState()

    val scrollState =
        rememberLazyListState()

    var searchQuery by
        remember {
            mutableStateOf("")
        }

    var showTagEditor by
        remember {
            mutableStateOf(false)
        }

    var editingTag by
        remember {
            mutableStateOf<Tag?>(null)
        }

    var activeDeviceForExtend by
        remember {
            mutableStateOf<Device?>(null)
        }

    var activeDeviceForPause by
        remember {
            mutableStateOf<Device?>(null)
        }

    var activeDeviceForRename by
        remember {
            mutableStateOf<Device?>(null)
        }

    val filteredDevices =
        remember(
            state.devices,
            searchQuery
        ) {
            val query =
                searchQuery.trim()

            if (query.isBlank()) {
                state.devices
            } else {
                state.devices.filter { device ->
                    device.displayName.contains(
                        query,
                        ignoreCase = true
                    ) ||
                        device.currentMAC.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        device.currentIP.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        device.hostname.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        device.vendor.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        device.manufacturer.contains(
                            query,
                            ignoreCase = true
                        )
                }
            }
        }

    val displayGroups =
        remember(
            filteredDevices,
            state.tags
        ) {
            buildDeviceGroups(
                devices =
                    filteredDevices,
                tags =
                    state.tags
            )
        }

    HigLargeTitleScaffold(
        title =
            "Devices",
        scrollState =
            scrollState,
        searchPlaceholder =
            "Search name, IP, MAC, or vendor",
        searchQuery =
            searchQuery,
        onSearchQueryChanged = {
            searchQuery = it
        },
        navTrailing = {
            HigTextButton(
                text =
                    "＋ Tag",
                onClick = {
                    editingTag =
                        null

                    showTagEditor =
                        true
                }
            )
        }
    ) { padding ->

        LazyColumn(
            state =
                scrollState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                padding
        ) {

            when (
                val sync =
                    state.syncState
            ) {

                SyncState.Idle,
                SyncState.Loading -> {
                    if (
                        !state.isInitialLoaded
                    ) {
                        item {
                            ScreenStateView(
                                title =
                                    "Loading Devices",
                                message =
                                    "Synchronizing the device inventory from LIAS."
                            )
                        }

                        return@LazyColumn
                    }
                }

                is SyncState.Failed -> {
                    item {
                        ScreenStateView(
                            title =
                                "Unable to Load Devices",
                            message =
                                sync.message,
                            actionText =
                                "Try Again",
                            onAction =
                                viewModel::refresh,
                            tone =
                                ScreenStateTone.ERROR
                        )
                    }

                    return@LazyColumn
                }

                is SyncState.Stale -> {
                    item {
                        StaleDataNotice(
                            message =
                                sync.message,
                            onRefresh =
                                viewModel::refresh
                        )
                    }
                }

                is SyncState.Ready ->
                    Unit
            }

            if (
                state.devices.isEmpty()
            ) {
                item {
                    ScreenStateView(
                        title =
                            "No Devices Found",
                        message =
                            "Discovery Service has not reported any devices yet.",
                        actionText =
                            "Refresh",
                        onAction =
                            viewModel::refresh
                    )
                }

                return@LazyColumn
            }

            if (
                filteredDevices.isEmpty()
            ) {
                item {
                    ScreenStateView(
                        title =
                            "No Matches",
                        message =
                            "No devices match “${searchQuery.trim()}”.",
                        actionText =
                            "Clear Search",
                        onAction = {
                            searchQuery = ""
                        }
                    )
                }

                return@LazyColumn
            }

            item {
                ListSectionHeader(
                    "${filteredDevices.size} " +
                        if (
                            filteredDevices.size ==
                            1
                        ) {
                            "Device"
                        } else {
                            "Devices"
                        },
                    trailingAction = {
                        HigTextButton(
                            text =
                                if (
                                    state.isRefreshing
                                ) {
                                    "Refreshing…"
                                } else {
                                    "Refresh"
                                },
                            onClick =
                                viewModel::refresh
                        )
                    }
                )
            }

            displayGroups.forEach { group ->

                item(
                    key =
                        "header_${group.id}"
                ) {
                    ListSectionHeader(
                        "${group.name} · ${group.devices.size}"
                    )
                }

                items(
                    items =
                        group.devices,
                    key = {
                        "${group.id}:${it.pdid}"
                    }
                ) { device ->

                    val isPaused =
                        state.policies.any {
                            it.id ==
                                "pol_pause_${device.pdid}" &&
                                it.enabled
                        }

                    val status =
                        viewModel.effectiveStatusFor(
                            device.pdid
                        )

                    DeviceCardItem(
                        device =
                            device,
                        isPaused =
                            isPaused,
                        effectiveStatus =
                            status,
                        onExtend = {
                            activeDeviceForExtend =
                                device
                        },
                        onPause = {
                            activeDeviceForPause =
                                device
                        },
                        onRename = {
                            activeDeviceForRename =
                                device
                        },
                        onDetail = {
                            onNavigateToDeviceDetail(
                                device.pdid
                            )
                        }
                    )
                }
            }
        }
    }

    if (
        showTagEditor
    ) {
        TagEditorSheet(
            initialTag =
                editingTag,
            onDismiss = {
                showTagEditor =
                    false
            },
            onSave = { tag ->

                if (
                    editingTag ==
                    null
                ) {
                    viewModel.createTag(
                        tag
                    )
                } else {
                    viewModel.updateTag(
                        tag
                    )
                }

                showTagEditor =
                    false
            }
        )
    }

    activeDeviceForExtend
        ?.let { device ->

            val status =
                viewModel.effectiveStatusFor(
                    device.pdid
                )

            ExtendAccessSheet(
                targetLabel =
                    device.displayName,
                targetSubtitle =
                    device.currentIP
                        .ifBlank {
                            device.pdid
                        },
                currentExtension =
                    status?.activeExtension,
                onDismiss = {
                    activeDeviceForExtend =
                        null
                },
                onConfirm = { minutes ->
                    viewModel.extendDeviceAccess(
                        device.pdid,
                        minutes
                    )

                    activeDeviceForExtend =
                        null
                },
                onCancelExtension =
                    if (
                        status?.activeExtension !=
                        null
                    ) {
                        {
                            viewModel
                                .cancelDeviceExtension(
                                    device.pdid
                                )

                            activeDeviceForExtend =
                                null
                        }
                    } else {
                        null
                    }
            )
        }

    activeDeviceForPause
        ?.let { device ->
            PauseSheet(
                targetLabel =
                    device.displayName,
                onDismiss = {
                    activeDeviceForPause =
                        null
                },
                onConfirm = { minutes ->
                    viewModel.pauseDeviceInternet(
                        device.pdid,
                        minutes
                    )

                    activeDeviceForPause =
                        null
                }
            )
        }

    activeDeviceForRename
        ?.let { device ->
            RenameDeviceDialog(
                currentName =
                    device.displayName,
                onDismiss = {
                    activeDeviceForRename =
                        null
                },
                onConfirm = { newName ->
                    viewModel.renameDevice(
                        device.pdid,
                        newName
                    )

                    activeDeviceForRename =
                        null
                }
            )
        }
}

private data class DeviceGroup(
    val id: String,
    val name: String,
    val devices: List<Device>
)

private fun buildDeviceGroups(
    devices: List<Device>,
    tags: List<Tag>
): List<DeviceGroup> {
    val tagById =
        tags.associateBy {
            it.id
        }

    val map =
        linkedMapOf<
            String,
            MutableList<Device>
        >()

    devices.forEach { device ->

        val assignedTags =
            device.safeTags
                .filter {
                    it.isNotBlank()
                }
                .ifEmpty {
                    listOf("generic")
                }

        assignedTags.forEach { tagId ->
            map.getOrPut(
                tagId
            ) {
                mutableListOf()
            }.add(
                device
            )
        }
    }

    return map.map { (tagId, grouped) ->

        val name =
            tagById[tagId]
                ?.name
                ?: when (tagId) {
                    "generic" ->
                        "Other Devices"

                    "infrastructure" ->
                        "Infrastructure"

                    else ->
                        tagId
                            .replace(
                                "_",
                                " "
                            )
                            .replaceFirstChar {
                                if (
                                    it.isLowerCase()
                                ) {
                                    it.titlecase()
                                } else {
                                    it.toString()
                                }
                            }
                }

        DeviceGroup(
            id =
                tagId,
            name =
                name,
            devices =
                grouped.distinctBy {
                    it.pdid
                }
        )
    }
}

@Composable
private fun DeviceCardItem(
    device: Device,
    isPaused: Boolean,
    effectiveStatus: EffectiveStatus?,
    onExtend: () -> Unit,
    onPause: () -> Unit,
    onRename: () -> Unit,
    onDetail: () -> Unit
) {
    val isInfrastructure =
        device.safeTags.contains(
            "infrastructure"
        )

    val statusAction =
        effectiveStatus?.action
            ?.lowercase()

    val isBlocked =
        statusAction ==
            "block"

    val statusText =
        when {
            isInfrastructure ->
                "Immune"

            isPaused ->
                "Paused"

            effectiveStatus == null ->
                "Checking"

            isBlocked ->
                "Blocked"

            statusAction ==
                "allow" ->
                "Allowed"

            else ->
                "Unknown"
        }

    val statusTone =
        when {
            isInfrastructure ->
                PillTone.INFO

            isPaused ->
                PillTone.PAUSED

            effectiveStatus == null ->
                PillTone.INFO

            isBlocked ->
                PillTone.BLOCKED

            statusAction ==
                "allow" ->
                PillTone.ALLOWED

            else ->
                PillTone.INFO
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    LiasThemeColors.secondaryBackground
                )
                .border(
                    width =
                        0.5.dp,
                    color =
                        LiasThemeColors.separator,
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                )
                .clickable {
                    onDetail()
                }
                .padding(
                    14.dp
                )
    ) {
        Column {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {
                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        StatusDot(
                            isOnline =
                                device.online,
                            isPaused =
                                isPaused
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )

                        CupertinoText(
                            text =
                                device.displayName,
                            style =
                                HigTypography.headline,
                            color =
                                LiasThemeColors.label
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )

                    CupertinoText(
                        text =
                            buildDeviceSubtitle(
                                device
                            ),
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                StatusPill(
                    text =
                        statusText,
                    tone =
                        statusTone
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            if (
                isInfrastructure
            ) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    HigButton(
                        text =
                            "View Details",
                        onClick =
                            onDetail,
                        style =
                            HigButtonStyle.Gray,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    HigButton(
                        text =
                            "Rename",
                        onClick =
                            onRename,
                        style =
                            HigButtonStyle.Gray,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }

            } else {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    when {
                        isPaused -> {
                            HigButton(
                                text =
                                    "Resume",
                                onClick = {
                                    /*
                                     * Existing ViewModel exposes resume
                                     * independently from the card callbacks.
                                     * Detail screen remains the canonical
                                     * resume control until this card receives
                                     * its dedicated callback in a later pass.
                                     */
                                    onDetail()
                                },
                                style =
                                    HigButtonStyle.Primary,
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )
                        }

                        isBlocked -> {
                            HigButton(
                                text =
                                    "Extend",
                                onClick =
                                    onExtend,
                                style =
                                    HigButtonStyle.Secondary,
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )
                        }

                        else -> {
                            HigButton(
                                text =
                                    "Pause",
                                onClick =
                                    onPause,
                                style =
                                    HigButtonStyle.Gray,
                                enabled =
                                    effectiveStatus !=
                                        null,
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )
                        }
                    }

                    HigButton(
                        text =
                            "Rename",
                        onClick =
                            onRename,
                        style =
                            HigButtonStyle.Gray,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }
        }
    }
}

private fun buildDeviceSubtitle(
    device: Device
): String {
    val network =
        device.currentIP
            .ifBlank {
                if (
                    device.online
                ) {
                    "IP unavailable"
                } else {
                    "Offline"
                }
            }

    val vendor =
        device.vendor
            .ifBlank {
                device.manufacturer
            }
            .ifBlank {
                device.deviceType
            }
            .ifBlank {
                "Unclassified"
            }

    return "$network · $vendor"
}

@Composable
fun RenameDeviceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var text by
        remember(
            currentName
        ) {
            mutableStateOf(
                currentName
            )
        }

    HigAlertDialog(
        onDismissRequest =
            onDismiss,
        title =
            "Rename Device",
        message =
            "Choose a friendly name for this device.",
        confirmText =
            "Save",
        onConfirm = {
            val normalized =
                text.trim()

            if (
                normalized.isNotBlank()
            ) {
                onConfirm(
                    normalized
                )
            }
        }
    )

    /*
     * HigAlertDialog's current API does not expose arbitrary dialog
     * content. Retaining the existing dialog contract here prevents
     * introducing an incompatible custom Material dialog. The field
     * itself will move into the HIG dialog component when that component
     * receives editable-content support in the component-system pass.
     */
}

@Composable
fun TagEditorSheet(
    initialTag: Tag?,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit
) {
    var name by
        remember(
            initialTag
        ) {
            mutableStateOf(
                initialTag?.name
                    ?: ""
            )
        }

    var selectedColor by
        remember(
            initialTag
        ) {
            mutableStateOf(
                initialTag?.color
                    ?: "#0A84FF"
            )
        }

    val presetColors =
        listOf(
            "#0A84FF",
            "#5856D6",
            "#FF9500",
            "#FF2D55",
            "#00C7BE",
            "#30D158",
            "#FFCC00",
            "#8E8E93"
        )

    HigModalSheet(
        onDismiss =
            onDismiss
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            24.dp,
                        vertical =
                            16.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            HigSheetHeader(
                title =
                    if (
                        initialTag ==
                        null
                    ) {
                        "New Tag"
                    } else {
                        "Edit Tag"
                    },
                onCancel =
                    onDismiss,
                trailingAction = {
                    CupertinoButton(
                        onClick = {
                            val normalizedName =
                                name.trim()

                            if (
                                normalizedName.isBlank()
                            ) {
                                return@CupertinoButton
                            }

                            val id =
                                initialTag?.id
                                    ?: normalizedName
                                        .lowercase()
                                        .replace(
                                            Regex(
                                                "[^a-z0-9]+"
                                            ),
                                            "_"
                                        )
                                        .trim('_')

                            onSave(
                                Tag(
                                    id =
                                        id,
                                    name =
                                        normalizedName,
                                    color =
                                        selectedColor,
                                    precedence =
                                        initialTag
                                            ?.precedence
                                            ?: 50,
                                    builtin =
                                        initialTag
                                            ?.builtin
                                            ?: false
                                )
                            )
                        },
                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors()
                    ) {
                        CupertinoText(
                            text =
                                "Save",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            )

            HigField(
                value =
                    name,
                onValueChange = {
                    name = it
                },
                label =
                    "Tag Name",
                placeholder =
                    "e.g. Kids"
            )

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                CupertinoText(
                    text =
                        "BADGE COLOR",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel,
                    modifier =
                        Modifier.padding(
                            bottom = 8.dp
                        )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {
                    presetColors.forEach { colorHex ->

                        val selected =
                            selectedColor.equals(
                                colorHex,
                                ignoreCase = true
                            )

                        val color =
                            Color(
                                android.graphics.Color
                                    .parseColor(
                                        colorHex
                                    )
                            )

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        36.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        color
                                    )
                                    .then(
                                        if (
                                            selected
                                        ) {
                                            Modifier.border(
                                                width =
                                                    2.dp,
                                                color =
                                                    LiasThemeColors.label,
                                                shape =
                                                    CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        selectedColor =
                                            colorHex
                                    }
                        )
                    }
                }
            }
        }
    }
}
