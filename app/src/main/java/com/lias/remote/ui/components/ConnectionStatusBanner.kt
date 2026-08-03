// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/ConnectionStatusBanner.kt
// Version: 1.0.0
// Purpose: Thin colored strip indicating SSE connection status.
//          Provides immediate visual feedback for network drops.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.network.ConnectionState

@Composable
fun ConnectionStatusBanner(connectionState: ConnectionState) {
    val (backgroundColor, text) = when (connectionState) {
        ConnectionState.CONNECTED -> Color.Transparent to ""
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary to "Connecting..."
        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.error to "Reconnecting..."
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error to "Disconnected"
    }

    if (connectionState != ConnectionState.CONNECTED) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
