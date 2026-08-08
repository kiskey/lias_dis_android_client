// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/ConnectionStatusBanner.kt
// Version: 5.0.0
//
// Purpose:
//   Cupertino-style status banner for the main application shell.
//
// Important UX distinction:
//
//   SSE connected + Sync ready:
//       No banner.
//
//   SSE reconnecting + cached data:
//       "Reconnecting…"
//       User can continue using cached data.
//
//   SSE disconnected + cached data:
//       "Connection lost"
//       User can still inspect cached information.
//
//   Initial sync failed:
//       This is NOT represented as merely "Server Disconnected".
//       The screen can present the actual data-loading failure.
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
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun ConnectionStatusBanner(
    connectionState: ConnectionState,
    syncState: SyncState,
    modifier: Modifier = Modifier
) {

    val message =
        when {

            syncState is
                SyncState.Failed ->
                "Unable to load LIAS data"

            connectionState ==
                ConnectionState.CONNECTING ->
                "Connecting to LIAS…"

            connectionState ==
                ConnectionState.RECONNECTING ->
                if (
                    syncState.hasUsableData
                ) {
                    "Reconnecting to LIAS…"
                } else {
                    "Reconnecting…"
                }

            connectionState ==
                ConnectionState.DISCONNECTED ->
                if (
                    syncState.hasUsableData
                ) {
                    "Connection lost · Showing saved data"
                } else {
                    "Server disconnected"
                }

            else ->
                null
        }

    if (
        message == null
    ) {
        return
    }

    val background =
        when {

            syncState is
                SyncState.Failed ->
                LiasThemeColors.red.copy(
                    alpha = 0.12f
                )

            syncState is
                SyncState.Stale ->
                LiasThemeColors.orange.copy(
                    alpha = 0.14f
                )

            else ->
                LiasThemeColors.orange.copy(
                    alpha = 0.14f
                )
        }

    val foreground =
        when {

            syncState is
                SyncState.Failed ->
                LiasThemeColors.red

            else ->
                LiasThemeColors.label
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    background
                )
                .padding(
                    vertical = 7.dp,
                    horizontal = 16.dp
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
