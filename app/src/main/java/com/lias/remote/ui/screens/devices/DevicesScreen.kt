// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt
// Version: 19.0.0
//
// Purpose:
//   High-signal device inventory.
//
// Batch 19:
//   - Every physical device appears exactly once.
//   - Primary group is presentation-only and selected by tag precedence.
//   - All classifications remain visible on the device card.
//   - Search covers identity/enrichment/services, not merely IP/MAC.
//   - EffectiveStatus is the sole access-control UI authority.
//   - Quick-list controls are intentionally minimal; metadata/tag/user
//     management belongs in Device Detail.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.device.DevicePresentation
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.temporaryAccessKind
import com.lias.remote.core.util.ConfigurationSafety
import com.lias.remote.core.util.EffectiveAccessFormatter
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
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
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
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
            mutableStateOf<Tag?>(null)
        }

    val filtered =
        remember(
            state.devices,
            searchQuery
        ) {

            state.devices.filter {
                DevicePresentation
                    .matchesSearch(
                        it,
                        searchQuery
                    )
            }
        }

    val groups =
        remember(
            filtered,
            state.tags
        ) {

            DevicePresentation
                .groupDevicesOnce(
                    filtered,
                    state.tags
                )
        }

    HigLargeTitleScaffold(
        title =
            "Devices",
        scrollState =
            scrollState,
        searchPlaceholder =
            "Search devices",
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
                filtered.isEmpty()
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
                    text =
                        "${filtered.size} ${
                            if (
                                filtered.size ==
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

            groups.forEach {
                    group ->

                item(
                    key =
                        "group_${group.tag.id}"
                ) {

                    val status =
                        viewModel
                            .tagEffectiveStatusFor(
                                group.tag.id
                            )

                    DeviceGroupHeader(
                        tag =
                            group.tag,
                        count =
                            group.devices.size,
                        status =
                            status,
                        onEdit = {

                            editingTag =
                                group.tag

                            showTagEditor =
                                true
                        },
                        onExtend = {

                            viewModel.extendTagAccess(
                                group.tag.id,
                                group.tag.name,
                                60
                            )
                        },
                        onCancelExtension = {

                            viewModel.cancelTagExtension(
                                group.tag.id
                            )
                        }
                    )
                }

                items(
                    items =
                        group.devices,
                    key = {
                        it.pdid
                    }
                ) {
                    device ->

                    DeviceInventoryRow(
                        device =
                            device,
                        tagNames =
                            DevicePresentation
                                .tagNames(
                                    device,
                                    state.tags
                                ),
                        status =
                            viewModel
                                .effectiveStatusFor(
                                    device.pdid
                                ),
                        onOpen = {

                            onNavigateToDeviceDetail(
                                device.pdid
                            )
                        },
                        onPause = {

                            viewModel.pauseDeviceInternet(
                                device.pdid
                            )
                        },
                        onResume = {

                            viewModel.unpauseDeviceInternet(
                                device.pdid
                            )
                        },
                        onExtend = {

                            /*
                             * List view provides a fast one-hour action.
                             * Custom duration remains available in
                             * Device Detail.
                             */
                            viewModel.extendDeviceAccess(
                                device.pdid,
                                60
                            )
                        },
                        onCancelExtension = {

                            viewModel.cancelDeviceExtension(
                                device.pdid
                            )
                        }
                    )
                }
            }

            item {

                CupertinoText(
                    text =
                        "Multi-tag devices are shown once under their highest-precedence classification. All assigned tags remain active for LIAS rules.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel,
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
                )
            }
        }
    }

    if (
        showTagEditor
    ) {

        val impact =
            editingTag?.let {
                tag ->

                ConfigurationSafety
                    .tagImpact(
                        tag =
                            tag,
                        devices =
                            state.devices,
                        policies =
                            state.policies
                    )
            }

        TagEditorSheet(
            initialTag =
                editingTag,
            dependencyImpact =
                impact,
            onDismiss = {

                showTagEditor =
                    false

                editingTag =
                    null
            },
            onSave = {
                tag ->

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

                editingTag =
                    null
            },
            onDelete =
                editingTag?.let {
                    tag ->

                    {
                        viewModel.deleteTag(
                            tag.id
                        )

                        showTagEditor =
                            false

                        editingTag =
                            null
                    }
                }
        )
    }
}

@Composable
private fun DeviceGroupHeader(
    tag: Tag,
    count: Int,
    status: EffectiveStatus?,
    onEdit: () -> Unit,
    onExtend: () -> Unit,
    onCancelExtension: () -> Unit
) {

    val infrastructure =
        tag.id ==
            ConfigurationSafety
                .INFRASTRUCTURE_TAG_ID

    val temporary =
        status?.temporaryAccessKind
            ?: TemporaryAccessKind.NONE

    ListSectionHeader(
        text =
            "${tag.name} · $count",
        trailingAction = {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                HigTextButton(
                    text =
                        if (
                            tag.builtin
                        ) {
                            "Info"
                        } else {
                            "Edit"
                        },
                    onClick =
                        onEdit
                )

                when {

                    infrastructure ->

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

                    temporary ==
                        TemporaryAccessKind.EXTEND ->

                        HigTextButton(
                            text =
                                "Cancel",
                            onClick =
                                onCancelExtension,
                            isDestructive =
                                true
                        )

                    status?.action ==
                        "block" &&
                        status.extendAvailable ->

                        HigTextButton(
                            text =
                                "Extend All",
                            onClick =
                                onExtend
                        )
                }
            }
        }
    )
}

@Composable
private fun DeviceInventoryRow(
    device: Device,
    tagNames: List<String>,
    status: EffectiveStatus?,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExtend: () -> Unit,
    onCancelExtension: () -> Unit
) {

    val infrastructure =
        DevicePresentation.INFRASTRUCTURE in
            DevicePresentation
                .normalizedTagIds(
                    device
                )

    val presentation =
        EffectiveAccessFormatter
            .present(
                status
            )

    val temporary =
        status?.temporaryAccessKind
            ?: TemporaryAccessKind.NONE

    GroupedListCard(
        modifier =
            Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
                .heightIn(
                    min = 64.dp
                )
                .semantics {
                    role =
                        Role.Button
                }
                .clickable {
                    onOpen()
                }
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

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
                                temporary ==
                                    TemporaryAccessKind.PAUSE
                        )

                        CupertinoText(
                            text =
                                device.displayName,
                            style =
                                HigTypography.headline,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                LiasThemeColors.label,
                            modifier =
                                Modifier.padding(
                                    start = 8.dp
                                )
                        )
                    }

                    CupertinoText(
                        text =
                            buildString {

                                append(
                                    if (
                                        device.online
                                    ) {
                                        device.currentIP
                                            .ifBlank {
                                                "Online"
                                            }
                                    } else {
                                        "Offline"
                                    }
                                )

                                append(" · ")

                                append(
                                    DevicePresentation
                                        .deviceTypeTitle(
                                            device
                                        )
                                )
                            },
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.secondaryLabel,
                        modifier =
                            Modifier.padding(
                                top = 3.dp
                            )
                    )

                    if (
                        tagNames.isNotEmpty()
                    ) {

                        CupertinoText(
                            text =
                                tagNames.joinToString(
                                    " · "
                                ),
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.tertiaryLabel,
                            modifier =
                                Modifier.padding(
                                    top = 3.dp
                                )
                        )
                    }
                }

                StatusPill(
                    text =
                        when {

                            infrastructure ->
                                "Immune"

                            temporary ==
                                TemporaryAccessKind.PAUSE ->
                                "Paused"

                            temporary ==
                                TemporaryAccessKind.EXTEND ->
                                "Extended"

                            else ->
                                presentation.title
                        },
                    tone =
                        when {

                            infrastructure ->
                                PillTone.INFO

                            temporary ==
                                TemporaryAccessKind.PAUSE ->
                                PillTone.PAUSED

                            temporary ==
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

            if (
                !infrastructure &&
                status != null
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 10.dp
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    when {

                        temporary ==
                            TemporaryAccessKind.PAUSE ->

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

                        temporary ==
                            TemporaryAccessKind.EXTEND ->

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

                        status.action ==
                            "block" &&
                            status.extendAvailable ->

                            HigButton(
                                text =
                                    "Extend 1h",
                                onClick =
                                    onExtend,
                                style =
                                    HigButtonStyle.Secondary,
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )

                        status.pauseAvailable ->

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

                    HigButton(
                        text =
                            "Details",
                        onClick =
                            onOpen,
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
