// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/ConnectionStatusBanner.kt
// Version: 7.0.0
//
// Purpose:
//   Main-shell transport/synchronization status presentation.
//
// Fix:
//   Imports SyncState.hasUsableData explicitly.
//
// Semantics:
//   - Ready + connected: no banner.
//   - Reconnecting + usable cache: cached data remains visible.
//   - Stale: warn without replacing usable content.
//   - Initial failure: clearly communicate that LIAS data failed.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.repositories.SyncState
import com.lias.remote.repositories.hasUsableData
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun ConnectionStatusBanner(
    connectionState: ConnectionState,
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    val message =
        when {

            syncState is SyncState.Failed ->
                "Unable to load LIAS data"

            syncState is SyncState.Stale &&
                connectionState ==
                    ConnectionState.CONNECTED ->
                "Some data may be out of date"

            connectionState ==
                ConnectionState.CONNECTING ->
                "Connecting to LIAS…"

            connectionState ==
                ConnectionState.RECONNECTING &&
                syncState.hasUsableData ->
                "Reconnecting · Showing last known data"

            connectionState ==
                ConnectionState.RECONNECTING ->
                "Reconnecting to LIAS…"

            connectionState ==
                ConnectionState.DISCONNECTED &&
                syncState.hasUsableData ->
                "Connection lost · Showing last known data"

            connectionState ==
                ConnectionState.DISCONNECTED ->
                "Server disconnected"

            else ->
                null
        }

    if (message == null) {
        return
    }

    val isError =
        syncState is SyncState.Failed

    val isWarning =
        syncState is SyncState.Stale ||
            connectionState ==
                ConnectionState.RECONNECTING ||
            (
                connectionState ==
                    ConnectionState.DISCONNECTED &&
                    syncState.hasUsableData
                )

    val background =
        when {
            isError ->
                LiasThemeColors.red
                    .copy(
                        alpha = 0.12f
                    )

            isWarning ->
                LiasThemeColors.orange
                    .copy(
                        alpha = 0.13f
                    )

            else ->
                LiasThemeColors.blue
                    .copy(
                        alpha = 0.10f
                    )
        }

    val foreground =
        when {
            isError ->
                LiasThemeColors.red

            isWarning ->
                LiasThemeColors.orange

            else ->
                LiasThemeColors.blue
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    background
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 7.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        CupertinoText(
            text = message,
            style =
                HigTypography.subheadline,
            color =
                foreground,
            textAlign =
                TextAlign.Center
        )
    }
}
