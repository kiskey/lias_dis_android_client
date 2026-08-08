// ====================================================================
// File: SecurityAlertSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Persistent sheet for security events. Captures security.alert
//          SSE events with Trust / Investigate / Block actions.
//          Preserves SecurityAlertPayload data model.
// ====================================================================

package com.lias.remote.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.SecurityAlertPayload
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigTextButton

@Composable
fun SecurityAlertSheet(
    alert: SecurityAlertPayload,
    onDismiss: () -> Unit,
    onBlock: () -> Unit,
    onTrust: () -> Unit
) {
    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alert Icon
            Text(
                text = "🚨",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Security Alert",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = alert.alertType.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Details Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Text(
                    text = "DETAILS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.W600
                )
                Text(
                    text = alert.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "Device: ${alert.pdid.takeLast(8)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "This may indicate a device impersonating another on your network. Choose how to respond:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Block Suspicious Device
            HigButton(
                text = "Block Suspicious Device",
                onClick = onBlock,
                style = HigButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )

            // Mark as Trusted
            HigButton(
                text = "Mark as Trusted (DHCP Rebind)",
                onClick = onTrust,
                style = HigButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth()
            )

            // Investigigate Later
            HigTextButton(
                text = "Investigate Later",
                onClick = onDismiss,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
