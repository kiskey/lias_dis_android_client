// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt
// Version: 26.0.0
//
// Purpose:
//   Canonical device inventory.
//
// Batch 26:
//   - Each device renders exactly once.
//   - Highest-precedence assigned tag becomes presentation group.
//   - Multi-tag membership itself remains untouched.
//   - Generic disappears from grouping when meaningful tags exist.
//   - Device actions obey EffectiveStatus capabilities.
//   - Tag Extend All is functional and server-authoritative.
//   - No pol_pause_<pdid> inspection.
//   - No empty action handlers.
//   - No emoji-as-interface.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.access.AccessKind
import com.lias.remote.ui.access.AccessPresentation
import com.lias.remote.ui.access.AccessPresentationResolver
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.StatusDot
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

private data class DeviceSection(
    val id: String,
    val title: String,
    val precedence: Int,
    val devices: List<Device>,
    val tag: Tag?
)

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
            mutableStateOf(
                ""
            )
        }

    var showTagEditor by
        remember {
            mutableStateOf(
                false
            )
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

    val sections =
        remember(
            state.devices,
            state.tags,
            searchQuery
        ) {

            buildDeviceSections(
                devices =
                    state.devices,
                tags =
                    state.tags,
                query =
                    searchQuery
            )
        }

    HigLargeTitleScaffold(
        title =
            "Devices",
        scrollState =
            scrollState,
        searchPlaceholder =
            "Search by name, IP, or MAC",
        searchQuery =
            searchQuery,
        onSearchQueryChanged = {
            searchQuery =
                it
        },
        navTrailing = {

            HigTextButton(
                text =
                    "New Tag",
                onClick = {

                    editingTag =
                        null

                    showTagEditor =
                        true
                }
            )
        }
    ) {
        padding ->

        LazyColumn(
            state =
                scrollState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                padding
        ) {

            if (
                state.isLoadingInitialData
            ) {

                item(
                    key =
                        "loading"
                ) {

                    CupertinoText(
                        text =
                            "Loading devices…",
                        style =
                            HigTypography.body,
                        color =
                            LiasThemeColors
                                .secondaryLabel,
                        modifier =
                            Modifier.padding(
                                24.dp
                            )
                    )
                }

            } else if (
                sections.isEmpty()
            ) {

                item(
                    key =
                        "empty"
                ) {

                    CupertinoText(
                        text =
                            if (
                                searchQuery.isBlank()
                            ) {
                                "No devices discovered yet."
                            } else {
                                "No devices match your search."
                            },
                        style =
                            HigTypography.body,
                        color =
                            LiasThemeColors
                                .secondaryLabel,
                        modifier =
                            Modifier.padding(
                                24.dp
                            )
                    )
                }
            }

            sections.forEach {
                section ->

                item(
                    key =
                        "header_${section.id}"
                ) {

                    ListSectionHeader(
                        text =
                            "${section.title} · ${section.devices.size}",
                        trailingAction = {

                            val tag =
                                section.tag

                            val status =
                                tag
                                    ?.let {
                                        state
                                            .effectiveStatusForTag(
                                                it.id
                                            )
                                    }

                            val extensionActive =
                                status
                                    ?.activeExtension
                                    ?.reasonTag
                                    ?.equals(
                                        "extend_access",
                                        ignoreCase =
                                            true
                                    ) ==
                                    true

                            if (
                                tag !=
                                null &&
                                !tag.id.equals(
                                    "infrastructure",
                                    ignoreCase =
                                        true
                                ) &&
                                (
                                    status?.extendAvailable ==
                                        true ||
                                        extensionActive
                                    )
                            ) {

                                HigTextButton(
                                    text =
                                        if (
                                            extensionActive
                                        ) {
                                            "Manage Extension"
                                        } else {
                                            "Extend All"
                                        },
                                    onClick = {
                                        activeTagForExtend =
                                            tag
                                    }
                                )
                            }
                        }
                    )
                }

                items(
                    items =
                        section.devices,
                    key = {
                        device ->

                        device.pdid
                    }
                ) {
                    device ->

                    val status =
                        state
                            .effectiveStatusForDevice(
                                device.pdid
                            )

                    val presentation =
                        AccessPresentationResolver
                            .resolve(
                                device =
                                    device,
                                status =
                                    status
                            )

                    DeviceCardItem(
                        device =
                            device,
                        presentation =
                            presentation,
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
            onSave = {
                tag ->

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
        ?.let {
            device ->

            val status =
                state
                    .effectiveStatusForDevice(
                        device.pdid
                    )

            val presentation =
                AccessPresentationResolver
                    .resolve(
                        device,
                        status
                    )

            if (
                presentation.canExtend ||
                presentation.canManageExtension
            ) {

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
                            ?.activeExtension,
                    onDismiss = {
                        activeDeviceForExtend =
                            null
                    },
                    onConfirm = {
                        minutes ->

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
                            presentation
                                .canManageExtension
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

            } else {

                activeDeviceForExtend =
                    null
            }
        }

    activeDeviceForPause
        ?.let {
            device ->

            val presentation =
                AccessPresentationResolver
                    .resolve(
                        device =
                            device,
                        status =
                            state
                                .effectiveStatusForDevice(
                                    device.pdid
                                )
                    )

            if (
                presentation.canPause
            ) {

                PauseSheet(
                    targetLabel =
                        device.displayName,
                    onDismiss = {
                        activeDeviceForPause =
                            null
                    },
                    onConfirm = {
                        _ ->

                        viewModel
                            .pauseDeviceInternet(
                                device.pdid
                            )

                        activeDeviceForPause =
                            null
                    }
                )

            } else {

                activeDeviceForPause =
                    null
            }
        }

    activeDeviceForRename
        ?.let {
            device ->

            RenameDeviceDialog(
                currentName =
                    device.displayName,
                onDismiss = {
                    activeDeviceForRename =
                        null
                },
                onConfirm = {
                    name ->

                    viewModel
                        .renameDevice(
                            device.pdid,
                            name
                        )

                    activeDeviceForRename =
                        null
                }
            )
        }

    activeTagForExtend
        ?.let {
            tag ->

            val status:
                EffectiveStatus? =
                state
                    .effectiveStatusForTag(
                        tag.id
                    )

            val extensionActive =
                status
                    ?.activeExtension
                    ?.reasonTag
                    ?.equals(
                        "extend_access",
                        ignoreCase =
                            true
                    ) ==
                    true

            if (
                status?.extendAvailable ==
                true ||
                extensionActive
            ) {

                ExtendAccessSheet(
                    targetLabel =
                        tag.name,
                    targetSubtitle =
                        "Device group",
                    currentExtension =
                        status
                            ?.activeExtension,
                    onDismiss = {
                        activeTagForExtend =
                            null
                    },
                    onConfirm = {
                        minutes ->

                        viewModel
                            .extendTagAccess(
                                tagId =
                                    tag.id,
                                tagName =
                                    tag.name,
                                minutes =
                                    minutes
                            )

                        activeTagForExtend =
                            null
                    },
                    onCancelExtension =
                        if (
                            extensionActive
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

            } else {

                activeTagForExtend =
                    null
            }
        }
}

@Composable
private fun DeviceCardItem(
    device: Device,
    presentation: AccessPresentation,
    onExtend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRename: () -> Unit,
    onDetail: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        4.dp
                )
                .background(
                    color =
                        LiasThemeColors
                            .secondaryBackground,
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
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
                                presentation.isPaused
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
                                2.dp
                            )
                    )

                    CupertinoText(
                        text =
                            listOfNotNull(
                                device.currentIP
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                                device.vendor
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                            )
                                .joinToString(
                                    " · "
                                )
                                .ifBlank {
                                    "No network details"
                                },
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors
                                .tertiaryLabel
                    )
                }

                StatusPill(
                    text =
                        presentation.label,
                    tone =
                        presentation.tone
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                when {

                    presentation
                        .canResumePause ->

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

                    presentation
                        .canManageExtension ->

                        HigButton(
                            text =
                                "Manage Access",
                            onClick =
                                onExtend,
                            style =
                                HigButtonStyle.Secondary,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                    presentation.canExtend ->

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

                    presentation.canPause ->

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

                    else ->

                        HigButton(
                            text =
                                "Details",
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

                if (
                    presentation.canExtend ||
                    presentation.canPause ||
                    presentation.canResumePause ||
                    presentation.canManageExtension
                ) {

                    HigButton(
                        text =
                            "Details",
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
        }
    }
}

private fun buildDeviceSections(
    devices: List<Device>,
    tags: List<Tag>,
    query: String
): List<DeviceSection> {

    val filtered =
        if (
            query.isBlank()
        ) {

            devices

        } else {

            devices.filter {
                device ->

                device.displayName
                    .contains(
                        query,
                        ignoreCase =
                            true
                    ) ||
                    device.currentMAC
                        .contains(
                            query,
                            ignoreCase =
                                true
                        ) ||
                    device.currentIP
                        .contains(
                            query,
                            ignoreCase =
                                true
                        ) ||
                    device.hostname
                        .contains(
                            query,
                            ignoreCase =
                                true
                        )
            }
        }

    val tagById =
        tags.associateBy {
            it.id
        }

    val grouped =
        linkedMapOf<
            String,
            MutableList<Device>
        >()

    filtered.forEach {
        device ->

        val meaningfulTagIds =
            device.safeTags
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .let {
                    ids ->

                    if (
                        ids.size >
                        1
                    ) {
                        ids.filterNot {
                            it ==
                                "generic"
                        }
                    } else {
                        ids
                    }
                }

        val primaryTag =
            meaningfulTagIds
                .mapNotNull {
                    tagById[
                        it
                    ]
                }
                .maxWithOrNull(
                    compareBy<Tag> {
                        it.precedence
                    }
                        .thenBy {
                            it.name
                        }
                )

        val groupId =
            primaryTag
                ?.id
                ?: if (
                    tagById.containsKey(
                        "generic"
                    )
                ) {
                    "generic"
                } else {
                    "__unclassified"
                }

        grouped
            .getOrPut(
                groupId
            ) {
                mutableListOf()
            }
            .add(
                device
            )
    }

    return grouped
        .map {
                (
                    id,
                    sectionDevices
                ) ->

            val tag =
                tagById[
                    id
                ]

            DeviceSection(
                id =
                    id,
                title =
                    tag
                        ?.name
                        ?: "Unclassified",
                precedence =
                    tag
                        ?.precedence
                        ?: -1,
                devices =
                    sectionDevices
                        .sortedBy {
                            it.displayName
                                .lowercase()
                        },
                tag =
                    tag
            )
        }
        .sortedWith(
            compareByDescending<DeviceSection> {
                it.precedence
            }
                .thenBy {
                    it.title
                }
        )
}
