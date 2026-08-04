// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/ConnectionStatusBanner.kt
// Version: 1.2.0
// Audit Fixes: 
//   1. Fully guarded against layout jumps and updated M3 theme color mappings.
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.network.ConnectionState

@Composable
fun ConnectionStatusBanner(connectionState: ConnectionState) {
    if (connectionState == ConnectionState.CONNECTED) return
    
    val (backgroundColor, text) = when (connectionState) {
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary to "Connecting..."
        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.error to "Reconnecting..."
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error to "Disconnected"
        else -> return
    }

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
