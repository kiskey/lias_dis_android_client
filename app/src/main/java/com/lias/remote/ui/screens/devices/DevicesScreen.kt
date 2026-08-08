// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt
// Version: 11.0.0
//
// Purpose:
//   Authoritative device/tag access-management screen.
//
// Batch 11 corrections:
//   - Zero pol_pause_* inference.
//   - Device buttons use EffectiveStatus availability.
//   - Active Pause and Extend countdowns are authoritative.
//   - Tag-level Extend All is implemented.
//   - Tag extension cancellation is implemented.
//   - Infrastructure remains immutable.
//   - Search/loading/stale/empty behavior retained from Batch 6.
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.temporaryAccessKind
import com.lias.remote.core.util.EffectiveAccessFormatter
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.ScreenStateTone
import com.lias.remote.ui.components.ScreenStateView
import com.lias.remote.ui.components.StaleDataNotice
import com.lias.remote.ui.components.StatusDot
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.formatTemporaryDuration
import com.lias.remote.ui.components.rememberTemporaryMinutesLeft
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail:
        (String) -> Unit
) {

    val state by
        viewModel.state
            .collectAsState()

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
            mutableStateOf<Tag?>(
                null
            )
        }

    var activeDeviceForExtend by
        remember {
            mutableStateOf<Device?>(
                null
            )
        }

    var activeDeviceForPause by
        remember {
            mutableStateOf<Device?>(
                null
            )
        }

    var activeDeviceForRename by
        remember {
            mutableStateOf<Device?>(
                null
            )
        }

    var activeTagForExtend by
        remember {
            mutableStateOf<Tag?>(
                null
            )
        }

    val filteredDevices =
        remember(
            state.devices,
            searchQuery
        ) {

            val query =
                searchQuery.trim()

            if (
                query.isBlank()
            ) {
                state.devices
            } else {

                state.devices
                    .filter { device ->

                        device.displayName
                            .contains(
                                query,
                                true
                            ) ||
                            device.currentMAC
                                .contains(
                                    query,
                                    true
                                ) ||
                            device.currentIP
                                .contains(
                                    query,
                                    true
                                ) ||
                            device.hostname
                                .contains(
                                    query,
                                    true
                                ) ||
                            device.vendor
                                .contains(
                                    query,
                                    true
                                ) ||
                            device.manufacturer
                                .contains(
                                    query,
                                    true
                                )
                    }
            }
        }

    val groups =
        remember(
            filteredDevices,
            state.tags
        ) {
            buildDeviceGroups(
                filteredDevices,
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
            searchQuery =
                it
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
                                    "Synchronizing device and access state from LIAS."
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
                state.devices
                    .isEmpty()
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
                filteredDevices
                    .isEmpty()
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
                            searchQuery =
                                ""
                        }
                    )
                }

                return@LazyColumn
            }

            item {
                ListSectionHeader(
                    "${filteredDevices.size} ${
                        if (
                            filteredDevices.size ==
                            1
                        ) {
                            "Device"
                        } else {
                            "Devices"
                        }
                    }",
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

            groups.forEach { group ->

                item(
                    key =
                        "header_${group.id}"
                ) {

                    val tag =
                        state.tags.find {
                            it.id ==
                                group.id
                        }

                    val tagStatus =
                        viewModel
                            .tagEffectiveStatusFor(
                                group.id
                            )

                    DeviceGroupHeader(
                        group =
                            group,
                        tag =
                            tag,
                        status =
                            tagStatus,
                        onExtend = {
                            if (
                                tag != null
                            ) {
                                activeTagForExtend =
                                    tag
                            }
                        },
                        onCancelExtension = {
                            viewModel
                                .cancelTagExtension(
                                    group.id
                                )
                        }
                    )
                }

                items(
                    items =
                        group.devices,
                    key = {
                        "${group.id}:${it.pdid}"
                    }
                ) { device ->

                    DeviceCardItem(
                        device =
                            device,
                        effectiveStatus =
                            viewModel
                                .effectiveStatusFor(
                                    device.pdid
                                ),
                        onExtend = {
                            activeDeviceForExtend =
                                device
                        },
                        onPause = {
                            activeDeviceForPause =
                                device
                        },
                        onResume = {
                            viewModel
                                .unpauseDeviceInternet(
                                    device.pdid
                                )
                        },
                        onCancelExtension = {
                            viewModel
                                .cancelDeviceExtension(
                                    device.pdid
                                )
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
                    viewModel
                        .createTag(
                            tag
                        )
                } else {
                    viewModel
                        .updateTag(
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
                viewModel
                    .effectiveStatusFor(
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
                    status
                        ?.activeExtension
                        ?.takeIf {
                            status.temporaryAccessKind ==
                                TemporaryAccessKind.EXTEND
                        },
                onDismiss = {
                    activeDeviceForExtend =
                        null
                },
                onConfirm = { minutes ->

                    viewModel
                        .extendDeviceAccess(
                            device.pdid,
                            minutes
                        )

                    activeDeviceForExtend =
                        null
                },
                onCancelExtension =
                    if (
                        status?.temporaryAccessKind ==
                        TemporaryAccessKind.EXTEND
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

                    viewModel
                        .pauseDeviceInternet(
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

            DeviceRenameDialog(
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

    activeTagForExtend
        ?.let { tag ->

            val status =
                viewModel
                    .tagEffectiveStatusFor(
                        tag.id
                    )

            ExtendAccessSheet(
                targetLabel =
                    tag.name,
                targetSubtitle =
                    "All devices using this tag",
                currentExtension =
                    status
                        ?.activeExtension
                        ?.takeIf {
                            status.temporaryAccessKind ==
                                TemporaryAccessKind.EXTEND
                        },
                onDismiss = {
                    activeTagForExtend =
                        null
                },
                onConfirm = { minutes ->

                    viewModel
                        .extendTagAccess(
                            tag.id,
                            tag.name,
                            minutes
                        )

                    activeTagForExtend =
                        null
                },
                onCancelExtension =
                    if (
                        status?.temporaryAccessKind ==
                        TemporaryAccessKind.EXTEND
                    ) {
                        {
                            viewModel
                                .cancelTagExtension(
                                    tag.id
                                )

                            activeTagForExtend =
                                null
                        }
                    } else {
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

    val knownTags =
        tags.associateBy {
            it.id
        }

    val grouped =
        linkedMapOf<
            String,
            MutableList<Device>
        >()

    devices.forEach { device ->

        val ids =
            device.safeTags
                .filter {
                    it.isNotBlank()
                }
                .ifEmpty {
                    listOf(
                        "generic"
                    )
                }

        ids.forEach { tagId ->

            grouped.getOrPut(
                tagId
            ) {
                mutableListOf()
            }.add(
                device
            )
        }
    }

    return grouped
        .map { (tagId, members) ->

            DeviceGroup(
                id =
                    tagId,
                name =
                    knownTags[
                        tagId
                    ]?.name
                        ?: when (
                            tagId
                        ) {
                            "generic" ->
                                "Other Devices"

                            "infrastructure" ->
                                "Infrastructure"

                            else ->
                                tagId
                        },
                devices =
                    members
                        .distinctBy {
                            it.pdid
                        }
            )
        }
}

@Composable
private fun DeviceGroupHeader(
    group: DeviceGroup,
    tag: Tag?,
    status: EffectiveStatus?,
    onExtend: () -> Unit,
    onCancelExtension: () -> Unit
) {

    val isInfrastructure =
        group.id ==
            "infrastructure"

    val temporaryKind =
        status
            ?.temporaryAccessKind
            ?: TemporaryAccessKind.NONE

    val minutesLeft =
        rememberTemporaryMinutesLeft(
            status?.activeExtension
        )

    ListSectionHeader(
        text =
            "${group.name} · ${group.devices.size}",
        trailingAction = {

            when {

                isInfrastructure -> {

                    CupertinoText(
                        text =
                            "IMMUNE",
                        style =
                            HigTypography.caption,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            LiasThemeColors.secondaryLabel
                    )
                }

                temporaryKind ==
                    TemporaryAccessKind.EXTEND -> {

                    HigTextButton(
                        text =
                            if (
                                minutesLeft !=
                                    null &&
                                minutesLeft > 0
                            ) {
                                "Cancel · ${
                                    formatTemporaryDuration(
                                        minutesLeft
                                    )
                                }"
                            } else {
                                "Cancel Extension"
                            },
                        onClick =
                            onCancelExtension,
                        isDestructive =
                            true
                    )
                }

                status?.action
                    ?.equals(
                        "block",
                        true
                    ) == true &&
                    status.extendAvailable -> {

                    HigTextButton(
                        text =
                            "Extend All",
                        onClick =
                            onExtend
                    )
                }

                else ->
                    Unit
            }
        }
    )
}

@Composable
private fun DeviceCardItem(
    device: Device,
    effectiveStatus: EffectiveStatus?,
    onExtend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancelExtension: () -> Unit,
    onRename: () -> Unit,
    onDetail: () -> Unit
) {

    val isInfrastructure =
        device.safeTags
            .contains(
                "infrastructure"
            )

    val presentation =
        EffectiveAccessFormatter
            .present(
                effectiveStatus
            )

    val temporaryKind =
        effectiveStatus
            ?.temporaryAccessKind
            ?: TemporaryAccessKind.NONE

    val minutesLeft =
        rememberTemporaryMinutesLeft(
            effectiveStatus
                ?.activeExtension
        )

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
                    LiasThemeColors
                        .secondaryBackground
                )
                .border(
                    0.5.dp,
                    LiasThemeColors.separator,
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
                                temporaryKind ==
                                    TemporaryAccessKind.PAUSE
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
                            LiasThemeColors
                                .tertiaryLabel
                    )

                    if (
                        presentation.detail !=
                        null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )

                        CupertinoText(
                            text =
                                presentation.detail,
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors
                                    .secondaryLabel
                        )
                    }
                }

                StatusPill(
                    text =
                        statusText(
                            isInfrastructure =
                                isInfrastructure,
                            presentationTitle =
                                presentation.title,
                            minutesLeft =
                                minutesLeft,
                            temporaryKind =
                                temporaryKind
                        ),
                    tone =
                        when {

                            isInfrastructure ->
                                PillTone.INFO

                            temporaryKind ==
                                TemporaryAccessKind.PAUSE ->
                                PillTone.PAUSED

                            temporaryKind ==
                                TemporaryAccessKind.EXTEND ->
                                PillTone.ALLOWED

                            presentation.isBlocked ->
                                PillTone.BLOCKED

                            presentation.isAllowed ->
                                PillTone.ALLOWED

                            else ->
                                PillTone.INFO
                        }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                when {

                    isInfrastructure -> {

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
                    }

                    temporaryKind ==
                        TemporaryAccessKind.PAUSE -> {

                        HigButton(
                            text =
                                "Resume",
                            onClick =
                                onResume,
                            style =
                                HigButtonStyle.Primary,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                        if (
                            presentation
                                .canExtend
                        ) {

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
                    }

                    temporaryKind ==
                        TemporaryAccessKind.EXTEND -> {

                        HigButton(
                            text =
                                "Cancel Extension",
                            onClick =
                                onCancelExtension,
                            style =
                                HigButtonStyle.Gray,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }

                    presentation
                        .canExtend -> {

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

                    presentation
                        .canPause -> {

                        HigButton(
                            text =
                                "Pause",
                            onClick =
                                onPause,
                            style =
                                HigButtonStyle.Gray,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }

                    else -> {

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

private fun statusText(
    isInfrastructure: Boolean,
    presentationTitle: String,
    minutesLeft: Int?,
    temporaryKind: TemporaryAccessKind
): String {

    if (
        isInfrastructure
    ) {
        return "Immune"
    }

    if (
        temporaryKind ==
        TemporaryAccessKind.PAUSE
    ) {
        return if (
            minutesLeft !=
                null &&
            minutesLeft > 0
        ) {
            "Paused · ${formatTemporaryDuration(minutesLeft)}"
        } else {
            "Paused"
        }
    }

    if (
        temporaryKind ==
        TemporaryAccessKind.EXTEND
    ) {
        return if (
            minutesLeft !=
                null &&
            minutesLeft > 0
        ) {
            "Extended · ${formatTemporaryDuration(minutesLeft)}"
        } else {
            "Extended"
        }
    }

    return presentationTitle
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
