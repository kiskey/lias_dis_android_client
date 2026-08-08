// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt
// Version: 19.0.0
//
// Purpose:
//   Authoritative device detail and management surface.
//
// Batch 19:
//   - Removes pol_pause_<pdid> inference entirely.
//   - EffectiveStatus drives Pause/Resume/Extend.
//   - Adds tag management.
//   - Adds complete DIS identity/enrichment evidence.
//   - Adds historical MAC/IP observations.
//   - Shows source_info provenance without inventing certainty.
//   - User creation leaves ID blank for server generation.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.device.DevicePresentation
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.User
import com.lias.remote.core.models.temporaryAccessKind
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.util.ConfigurationSafety
import com.lias.remote.core.util.EffectiveAccessFormatter
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.ScreenStateView
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

    val state by
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    val device =
        state.devices
            .find {
                it.pdid ==
                    pdid
            }

    if (
        device == null
    ) {

        HigLargeTitleScaffold(
            title =
                "Device",
            scrollState =
                scrollState,
            navLeading = {

                HigTextButton(
                    text =
                        "‹ Devices",
                    onClick =
                        onBack
                )
            }
        ) { padding ->

            ScreenStateView(
                title =
                    "Device Unavailable",
                message =
                    "This device is no longer present in the current LIAS inventory.",
                actionText =
                    "Back to Devices",
                onAction =
                    onBack,
                modifier =
                    Modifier.padding(
                        padding
                    )
            )
        }

        return
    }

    var logs by
        remember(
            pdid
        ) {
            mutableStateOf<List<FlowLog>>(
                emptyList()
            )
        }

    var loadingLogs by
        remember(
            pdid
        ) {
            mutableStateOf(
                true
            )
        }

    var showExtend by
        remember {
            mutableStateOf(
                false
            )
        }

    var showPause by
        remember {
            mutableStateOf(
                false
            )
        }

    var showRename by
        remember {
            mutableStateOf(
                false
            )
        }

    var showTags by
        remember {
            mutableStateOf(
                false
            )
        }

    var showUser by
        remember {
            mutableStateOf(
                false
            )
        }

    val status =
        viewModel.effectiveStatusFor(
            pdid
        )

    val access =
        EffectiveAccessFormatter
            .present(
                status
            )

    val temporary =
        status?.temporaryAccessKind
            ?: TemporaryAccessKind.NONE

    val infrastructure =
        ConfigurationSafety
            .INFRASTRUCTURE_TAG_ID in
            DevicePresentation
                .normalizedTagIds(
                    device
                )

    val assignedUser =
        state.users
            .find {
                it.id ==
                    device.userID
            }

    val tagNames =
        DevicePresentation
            .tagNames(
                device,
                state.tags
            )

    LaunchedEffect(
        pdid
    ) {

        loadingLogs =
            true

        when (
            val result =
                viewModel
                    .getDeviceLogs(
                        pdid
                    )
        ) {

            is ApiResult.Success ->
                logs =
                    result.data

            else ->
                Unit
        }

        loadingLogs =
            false
    }

    HigLargeTitleScaffold(
        title =
            device.displayName,
        scrollState =
            scrollState,
        navLeading = {

            HigTextButton(
                text =
                    "‹ Devices",
                onClick =
                    onBack
            )
        },
        navTrailing = {

            HigTextButton(
                text =
                    "Rename",
                onClick = {
                    showRename =
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

            item {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            CupertinoText(
                                text =
                                    device.displayName,
                                style =
                                    HigTypography.title1,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    LiasThemeColors.label
                            )

                            CupertinoText(
                                text =
                                    if (
                                        device.online
                                    ) {
                                        "Online · ${
                                            device.currentIP
                                                .ifBlank {
                                                    "IP unavailable"
                                                }
                                        }"
                                    } else {
                                        "Offline"
                                    },
                                style =
                                    HigTypography.subheadline,
                                color =
                                    if (
                                        device.online
                                    ) {
                                        LiasThemeColors.green
                                    } else {
                                        LiasThemeColors
                                            .secondaryLabel
                                    }
                            )
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
                                        access.title
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

                                    access.isBlocked ->
                                        PillTone.BLOCKED

                                    access.isAllowed ->
                                        PillTone.ALLOWED

                                    else ->
                                        PillTone.INFO
                                }
                        )
                    }

                    CupertinoText(
                        text =
                            if (
                                infrastructure
                            ) {
                                "Infrastructure protection keeps this device online regardless of ordinary LIAS access rules."
                            } else {
                                access.explanation
                            },
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.secondaryLabel
                    )

                    if (
                        !infrastructure &&
                        status != null
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
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
                                        onClick = {

                                            viewModel
                                                .unpauseDeviceInternet(
                                                    pdid
                                                )
                                        },
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
                                        onClick = {

                                            viewModel
                                                .cancelDeviceExtension(
                                                    pdid
                                                )
                                        },
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
                                            "Extend",
                                        onClick = {
                                            showExtend =
                                                true
                                        },
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
                                        onClick = {
                                            showPause =
                                                true
                                        },
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

            item {

                ListSectionHeader(
                    "Classification"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Tags",
                        secondaryText =
                            tagNames.joinToString(
                                " · "
                            )
                                .ifBlank {
                                    "Generic Devices"
                                },
                        trailingContent = {

                            CupertinoText(
                                text =
                                    "›",
                                style =
                                    HigTypography.headline,
                                color =
                                    LiasThemeColors.tertiaryLabel
                            )
                        },
                        showDivider =
                            true,
                        onClick = {
                            showTags =
                                true
                        }
                    )

                    GroupedListRow(
                        primaryText =
                            "Assigned User",
                        secondaryText =
                            assignedUser?.name
                                ?: "Unassigned",
                        trailingContent = {

                            CupertinoText(
                                text =
                                    "›",
                                style =
                                    HigTypography.headline,
                                color =
                                    LiasThemeColors.tertiaryLabel
                            )
                        },
                        onClick = {
                            showUser =
                                true
                        }
                    )
                }
            }

            item {

                ListSectionHeader(
                    "Identity"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Identity Tier",
                        secondaryText =
                            DevicePresentation
                                .identityTierTitle(
                                    device.identityTier
                                ),
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Confidence",
                        secondaryText =
                            DevicePresentation
                                .confidencePercent(
                                    device.confidence
                                ),
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Device Type",
                        secondaryText =
                            DevicePresentation
                                .deviceTypeTitle(
                                    device
                                ),
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Hostname",
                        secondaryText =
                            device.hostname.ifBlank {
                                "Not observed"
                            },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Canonical Hostname",
                        secondaryText =
                            device.canonicalHostname
                                .ifBlank {
                                    "Not available"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Manufacturer",
                        secondaryText =
                            device.manufacturer
                                .ifBlank {
                                    "Unknown"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Vendor",
                        secondaryText =
                            device.vendor.ifBlank {
                                "Unknown"
                            },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Model",
                        secondaryText =
                            device.model.ifBlank {
                                "Unknown"
                            }
                    )
                }
            }

            item {

                CupertinoText(
                    text =
                        DevicePresentation
                            .identityTierExplanation(
                                device.identityTier
                            ),
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel,
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        )
                )
            }

            item {

                ListSectionHeader(
                    "Network Identity"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Current MAC",
                        secondaryText =
                            device.currentMAC.ifBlank {
                                "Unavailable"
                            },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Known MAC Addresses",
                        secondaryText =
                            device.safeMacs
                                .distinct()
                                .joinToString(
                                    "\n"
                                )
                                .ifBlank {
                                    device.currentMAC
                                        .ifBlank {
                                            "None"
                                        }
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Current IP",
                        secondaryText =
                            device.currentIP.ifBlank {
                                "Unavailable"
                            },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Known IP Addresses",
                        secondaryText =
                            device.safeIps
                                .distinct()
                                .joinToString(
                                    "\n"
                                )
                                .ifBlank {
                                    device.currentIP
                                        .ifBlank {
                                            "None"
                                        }
                                }
                    )
                }
            }

            if (
                device.safeServices
                    .isNotEmpty()
            ) {

                item {

                    ListSectionHeader(
                        "Observed Services"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            )
                    ) {

                        device.safeServices
                            .distinct()
                            .sorted()
                            .forEachIndexed {
                                    index,
                                    service ->

                                GroupedListRow(
                                    primaryText =
                                        service,
                                    showDivider =
                                        index <
                                            device.safeServices
                                                .distinct()
                                                .size -
                                            1
                                )
                            }
                    }
                }
            }

            item {

                ListSectionHeader(
                    "Discovery Evidence"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Identification",
                        secondaryText =
                            if (
                                device.isFullyIdentified
                            ) {
                                "Fully identified"
                            } else {
                                "Still being enriched"
                            },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "First Seen",
                        secondaryText =
                            device.firstSeen.ifBlank {
                                "Unknown"
                            },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Last Seen",
                        secondaryText =
                            device.lastSeen.ifBlank {
                                "Unknown"
                            },
                        showDivider =
                            true
                    )

                    if (
                        device.lastEnrichedAt
                            .isNotBlank()
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Last Enriched",
                            secondaryText =
                                device.lastEnrichedAt,
                            showDivider =
                                true
                        )
                    }

                    if (
                        device.lastNmapScanAt
                            .isNotBlank()
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Last Nmap Scan",
                            secondaryText =
                                device.lastNmapScanAt,
                            showDivider =
                                true
                        )
                    }

                    if (
                        device.nmapAttemptCount >
                        0
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Nmap Attempts",
                            secondaryText =
                                device.nmapAttemptCount
                                    .toString()
                        )
                    }
                }
            }

            if (
                device.safeSourceInfo
                    .isNotEmpty()
            ) {

                item {

                    ListSectionHeader(
                        "Field Provenance"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            )
                    ) {

                        val sources =
                            device.safeSourceInfo
                                .entries
                                .sortedBy {
                                    it.key
                                }

                        sources.forEachIndexed {
                                index,
                                entry ->

                            GroupedListRow(
                                primaryText =
                                    DevicePresentation
                                        .sourceTitle(
                                            entry.key
                                        ),
                                secondaryText =
                                    DevicePresentation
                                        .sourceSummary(
                                            entry.value
                                        ),
                                showDivider =
                                    index <
                                        sources.lastIndex
                            )
                        }
                    }
                }
            }

            item {

                ListSectionHeader(
                    "Recent Access Decisions"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        )
                ) {

                    when {

                        loadingLogs ->

                            GroupedListRow(
                                primaryText =
                                    "Loading activity…"
                            )

                        logs.isEmpty() ->

                            GroupedListRow(
                                primaryText =
                                    "No recent activity logged"
                            )

                        else ->

                            logs
                                .take(
                                    100
                                )
                                .forEachIndexed {
                                        index,
                                        log ->

                                    GroupedListRow(
                                        primaryText =
                                            log.timestamp,
                                        secondaryText =
                                            if (
                                                log.bytes >
                                                0
                                            ) {
                                                "${log.bytes} bytes"
                                            } else {
                                                null
                                            },
                                        trailingContent = {

                                            StatusPill(
                                                text =
                                                    log.action
                                                        .replaceFirstChar {
                                                            it.uppercase()
                                                        },
                                                tone =
                                                    if (
                                                        log.action ==
                                                        "block"
                                                    ) {
                                                        PillTone.BLOCKED
                                                    } else {
                                                        PillTone.ALLOWED
                                                    }
                                            )
                                        },
                                        showDivider =
                                            index <
                                                logs
                                                    .take(
                                                        100
                                                    )
                                                    .lastIndex
                                    )
                                }
                    }
                }
            }
        }
    }

    if (
        showExtend
    ) {

        ExtendAccessSheet(
            targetLabel =
                device.displayName,
            targetSubtitle =
                device.currentIP.ifBlank {
                    device.pdid
                },
            currentExtension =
                status?.activeExtension
                    ?.takeIf {
                        temporary ==
                            TemporaryAccessKind.EXTEND
                    },
            onDismiss = {
                showExtend =
                    false
            },
            onConfirm = {
                minutes ->

                viewModel.extendDeviceAccess(
                    pdid,
                    minutes
                )

                showExtend =
                    false
            },
            onCancelExtension =
                if (
                    temporary ==
                    TemporaryAccessKind.EXTEND
                ) {
                    {

                        viewModel.cancelDeviceExtension(
                            pdid
                        )

                        showExtend =
                            false
                    }
                } else {
                    null
                }
        )
    }

    if (
        showPause
    ) {

        PauseSheet(
            targetLabel =
                device.displayName,
            onDismiss = {
                showPause =
                    false
            },
            onConfirm = {
                minutes ->

                viewModel.pauseDeviceInternet(
                    pdid,
                    minutes
                )

                showPause =
                    false
            }
        )
    }

    if (
        showRename
    ) {

        DeviceRenameDialog(
            currentName =
                device.displayName,
            onDismiss = {
                showRename =
                    false
            },
            onConfirm = {
                newName ->

                viewModel.renameDevice(
                    pdid,
                    newName
                )

                showRename =
                    false
            }
        )
    }

    if (
        showTags
    ) {

        MoveTagSheet(
            device =
                device,
            allTags =
                state.tags,
            onDismiss = {
                showTags =
                    false
            },
            onConfirm = {
                tagIds ->

                viewModel.assignTags(
                    pdid,
                    tagIds
                )

                showTags =
                    false
            }
        )
    }

    if (
        showUser
    ) {

        UserAssignmentSheet(
            users =
                state.users,
            assignedUserId =
                device.userID,
            onDismiss = {
                showUser =
                    false
            },
            onSelectUser = {
                userId ->

                viewModel.assignDeviceUser(
                    pdid,
                    userId
                )

                showUser =
                    false
            },
            onCreateUser = {
                userName ->

                /*
                 * Server owns canonical user ID generation.
                 */
                viewModel.createUser(
                    User(
                        id =
                            "",
                        name =
                            userName.trim()
                    )
                )
            }
        )
    }
}
