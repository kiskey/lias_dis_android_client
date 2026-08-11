// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt
// Version: 27.4.0
//
// Purpose:
//   User-facing Settings with progressive disclosure.
//
// Normal-user surface:
//   - Connection
//   - Server health
//   - Appearance
//   - Vacation Mode
//
// Advanced surface:
//   - Policy JSON backup
//   - Policy JSON restore
//   - LIAS nftables table flush
//
// Removed:
//   - fake Notifications switch
//   - hard-coded server version
//   - hard-coded latency
//   - hard-coded SSE-live claim
// ====================================================================

package com.lias.remote.ui.screens.settings

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.core.util.PolicyBackupIo
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoSwitch
import com.slapps.cupertino.CupertinoText

@Composable
fun SettingsScreen(
    viewModel:
        SettingsViewModel,
    onNavigateToConnection:
        () -> Unit
) {

    val state by
        viewModel.uiState
            .collectAsState()

    val context =
        LocalContext.current

    val scrollState =
        rememberLazyListState()

    var showFlushDialog by
        remember {
            mutableStateOf(
                false
            )
        }

    var showRestoreDialog by
        remember {
            mutableStateOf(
                false
            )
        }

    var pendingRestorePayload by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    var pendingExportPayload by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    val createBackupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .CreateDocument(
                    PolicyBackupIo
                        .MIME_TYPE
                )
        ) { uri ->

            val payload =
                pendingExportPayload

            pendingExportPayload =
                null

            if (
                uri != null &&
                payload != null
            ) {

                PolicyBackupIo.write(
                    resolver =
                        context
                            .contentResolver,
                    uri =
                        uri,
                    payload =
                        payload
                )
            }
        }

    val openBackupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument()
        ) { uri ->

            if (
                uri != null
            ) {

                PolicyBackupIo.read(
                    resolver =
                        context
                            .contentResolver,
                    uri =
                        uri
                )
                    .onSuccess {
                        payload ->

                        pendingRestorePayload =
                            payload

                        showRestoreDialog =
                            true
                    }
            }
        }

    LaunchedEffect(
        state.savedServerUrl
    ) {

        if (
            state.savedServerUrl
                .isNotBlank()
        ) {
            viewModel
                .refreshServerHealth()
        }
    }

    val appVersion =
        remember {

            try {
                val info =
                    context.packageManager
                        .getPackageInfo(
                            context.packageName,
                            0
                        )

                info.versionName
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Unknown"

            } catch (
                _: PackageManager.NameNotFoundException
            ) {
                "Unknown"
            }
        }

    HigLargeTitleScaffold(
        title =
            "Settings",
        scrollState =
            scrollState
    ) { padding, navigationHeader ->

        LazyColumn(
            state =
                scrollState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                padding
        ) {

            item(
                key =
                    "cupertino-navigation-header"
            ) {
                navigationHeader()
            }


            // ========================================================
            // SERVER
            // ========================================================

            item {

                ListSectionHeader(
                    "Server"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Connection",
                        secondaryText =
                            if (
                                state.savedServerUrl
                                    .isBlank()
                            ) {
                                "Not configured"
                            } else {
                                state.savedServerUrl
                            },
                        trailingContent = {

                            CupertinoText(
                                text =
                                    "›",
                                style =
                                    HigTypography.headline,
                                color =
                                    LiasThemeColors
                                        .tertiaryLabel
                            )
                        },
                        showDivider =
                            true,
                        onClick =
                            onNavigateToConnection
                    )

                    GroupedListRow(
                        primaryText =
                            "Server Health",
                        secondaryText =
                            serverHealthText(
                                state =
                                    state
                            ),
                        trailingContent = {

                            CupertinoText(
                                text =
                                    when (
                                        state.connectionState
                                    ) {

                                        ConnectionState.CONNECTED ->
                                            "Connected"

                                        ConnectionState.CONNECTING ->
                                            "Connecting"

                                        ConnectionState.RECONNECTING ->
                                            "Reconnecting"

                                        ConnectionState.DISCONNECTED ->
                                            "Offline"
                                    },
                                style =
                                    HigTypography.subheadline,
                                color =
                                    connectionColor(
                                        state.connectionState
                                    )
                            )
                        },
                        onClick = {
                            viewModel
                                .refreshServerHealth()
                        }
                    )
                }
            }

            // ========================================================
            // APPEARANCE
            // ========================================================

            item {

                ListSectionHeader(
                    "Appearance"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    ) {

                        CupertinoText(
                            text =
                                "Theme",
                            style =
                                HigTypography.headline,
                            color =
                                LiasThemeColors.label
                        )

                        SegmentedControl(
                            options =
                                listOf(
                                    "System",
                                    "Light",
                                    "Dark"
                                ),
                            selectedOption =
                                state.themeMode
                                    .replaceFirstChar {
                                        it.uppercase()
                                    },
                            onOptionSelected = {
                                mode ->

                                viewModel
                                    .updateThemeMode(
                                        mode.lowercase()
                                    )
                            },
                            modifier =
                                Modifier.padding(
                                    top =
                                        8.dp
                                )
                        )

                        CupertinoText(
                            text =
                                if (
                                    state.themeMode ==
                                    "system"
                                ) {
                                    "Matches the device appearance."
                                } else {
                                    "Uses the selected appearance throughout LIAS Remote."
                                },
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors
                                    .tertiaryLabel,
                            modifier =
                                Modifier.padding(
                                    top =
                                        8.dp
                                )
                        )
                    }
                }
            }

            // ========================================================
            // QUICK CONTROL
            // ========================================================

            item {

                ListSectionHeader(
                    "Network Control"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Vacation Mode",
                        secondaryText =
                            if (
                                state.vacationMode
                            ) {
                                "All non-infrastructure devices are blocked."
                            } else {
                                "Immediately block all non-infrastructure devices."
                            },
                        trailingContent = {

                            CupertinoSwitch(
                                checked =
                                    state.vacationMode,
                                onCheckedChange = {
                                    enabled ->

                                    viewModel
                                        .toggleVacationMode(
                                            enabled
                                        )
                                }
                            )
                        }
                    )
                }
            }

            // ========================================================
            // ADVANCED DISCLOSURE
            // ========================================================

            item {

                ListSectionHeader(
                    "Advanced"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Advanced Controls",
                        secondaryText =
                            "Policy backup and firewall maintenance",
                        trailingContent = {

                            CupertinoSwitch(
                                checked =
                                    state.advancedMode,
                                onCheckedChange = {
                                    enabled ->

                                    viewModel
                                        .setAdvancedMode(
                                            enabled
                                        )
                                }
                            )
                        }
                    )
                }
            }

            if (
                state.advancedMode
            ) {

                item {

                    ListSectionHeader(
                        "Policy Backup"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Export Policies",
                            secondaryText =
                                if (
                                    state.isExportingPolicies
                                ) {
                                    "Preparing backup…"
                                } else {
                                    "Save the server policy configuration as JSON."
                                },
                            trailingContent = {

                                CupertinoText(
                                    text =
                                        "›",
                                    style =
                                        HigTypography.headline,
                                    color =
                                        LiasThemeColors
                                            .tertiaryLabel
                                )
                            },
                            showDivider =
                                true,
                            onClick = {

                                if (
                                    !state
                                        .isExportingPolicies
                                ) {

                                    viewModel
                                        .exportPolicies {
                                            payload ->

                                            pendingExportPayload =
                                                payload

                                            createBackupLauncher
                                                .launch(
                                                    PolicyBackupIo
                                                        .DEFAULT_FILE_NAME
                                                )
                                        }
                                }
                            }
                        )

                        GroupedListRow(
                            primaryText =
                                "Restore Policies",
                            secondaryText =
                                if (
                                    state.isImportingPolicies
                                ) {
                                    "Restoring…"
                                } else {
                                    "Choose a LIAS policy JSON backup."
                                },
                            trailingContent = {

                                CupertinoText(
                                    text =
                                        "›",
                                    style =
                                        HigTypography.headline,
                                    color =
                                        LiasThemeColors
                                            .tertiaryLabel
                                )
                            },
                            onClick = {

                                if (
                                    !state
                                        .isImportingPolicies
                                ) {

                                    openBackupLauncher
                                        .launch(
                                            arrayOf(
                                                PolicyBackupIo
                                                    .MIME_TYPE,
                                                "text/json",
                                                "text/plain"
                                            )
                                        )
                                }
                            }
                        )
                    }
                }

                item {

                    ListSectionHeader(
                        "System Maintenance"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        GroupedListRow(
                            primaryText =
                                if (
                                    state.isFlushing
                                ) {
                                    "Flushing LIAS Rules…"
                                } else {
                                    "Flush LIAS nftables Table"
                                },
                            secondaryText =
                                "Troubleshooting only · LIAS rebuilds the table on a subsequent synchronization.",
                            isDestructive =
                                true,
                            trailingContent = {

                                CupertinoText(
                                    text =
                                        "›",
                                    style =
                                        HigTypography.headline,
                                    color =
                                        LiasThemeColors.red
                                )
                            },
                            onClick = {

                                if (
                                    !state.isFlushing
                                ) {
                                    showFlushDialog =
                                        true
                                }
                            }
                        )
                    }
                }
            }

            // ========================================================
            // ABOUT
            // ========================================================

            item {

                ListSectionHeader(
                    "About"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "LIAS Remote",
                        secondaryText =
                            "Android client",
                        trailingContent = {

                            CupertinoText(
                                text =
                                    appVersion,
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors
                                        .tertiaryLabel
                            )
                        },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "LIAS Server",
                        secondaryText =
                            state.serverVersion
                                ?.let {
                                    "Version $it"
                                }
                                ?: if (
                                    state.savedServerUrl
                                        .isBlank()
                                ) {
                                    "Not configured"
                                } else {
                                    "Version unavailable"
                                },
                        trailingContent = {

                            state.healthLatencyMs
                                ?.let { latency ->

                                    CupertinoText(
                                        text =
                                            "$latency ms",
                                        style =
                                            HigTypography.subheadline,
                                        color =
                                            LiasThemeColors
                                                .tertiaryLabel
                                    )
                                }
                        }
                    )
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Policy restore confirmation
    // ----------------------------------------------------------------

    if (
        showRestoreDialog
    ) {

        HigAlertDialog(
            onDismissRequest = {

                showRestoreDialog =
                    false

                pendingRestorePayload =
                    null
            },
            title =
                "Restore Policies?",
            message =
                "This sends the selected policy backup to LIAS. Existing server policy configuration may be changed.",
            confirmText =
                "Restore",
            onConfirm = {

                pendingRestorePayload
                    ?.let {
                        payload ->

                        viewModel
                            .importPolicies(
                                payload
                            )
                    }

                pendingRestorePayload =
                    null

                showRestoreDialog =
                    false
            },
            isDestructive =
                true
        )
    }

    // ----------------------------------------------------------------
    // Firewall confirmation
    // ----------------------------------------------------------------

    if (
        showFlushDialog
    ) {

        HigAlertDialog(
            onDismissRequest = {

                showFlushDialog =
                    false
            },
            title =
                "Flush LIAS nftables Table?",
            message =
                "This temporarily removes LIAS access-control enforcement. Use it only for troubleshooting. LIAS will rebuild its rules on a subsequent synchronization.",
            confirmText =
                "Flush Rules",
            onConfirm = {

                viewModel
                    .flushNftables()

                showFlushDialog =
                    false
            },
            isDestructive =
                true
        )
    }
}

private fun serverHealthText(
    state:
        com.lias.remote.ui.SettingsUiState
): String {

    if (
        state.savedServerUrl
            .isBlank()
    ) {
        return "No server configured"
    }

    if (
        state.isRefreshingServerHealth
    ) {
        return "Checking server…"
    }

    state.healthError
        ?.let {
            return it
        }

    val version =
        state.serverVersion

    val latency =
        state.healthLatencyMs

    return when {

        version != null &&
            latency != null ->
            "LIAS $version · $latency ms"

        version != null ->
            "LIAS $version"

        else ->
            "Tap to check server health"
    }
}

@Composable
private fun connectionColor(
    state:
        ConnectionState
) =
    when (state) {

        ConnectionState.CONNECTED ->
            LiasThemeColors.green

        ConnectionState.CONNECTING,
        ConnectionState.RECONNECTING ->
            LiasThemeColors.orange

        ConnectionState.DISCONNECTED ->
            LiasThemeColors.red
    }
