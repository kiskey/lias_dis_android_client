// ====================================================================
// File: PauseSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: NEW variable duration pause (15/30/60/120 min) replacing
//          fixed 1h. Uses POST /api/v1/policies with expires_at to
//          create temporary block policy. No backend changes needed.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader

@Composable
fun PauseSheet(
    targetLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(60) }
    val quickPicks = listOf(15, 30, 60, 120)

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = "Pause Internet", onCancel = onDismiss)

            Text(
                text = targetLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick Picks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPicks.forEach { pick ->
                    val isSelected = selectedMinutes == pick
                    HigButton(
                        text = if (pick >= 60) "${pick / 60}h" else "${pick}m",
                        onClick = { selectedMinutes = pick },
                        style = if (isSelected) HigButtonStyle.Danger else HigButtonStyle.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Display
            Text(
                text = "${selectedMinutes} minutes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W800
            )

            Text(
                text = "All internet traffic will be blocked for this device until the timer expires.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HigButton(
                text = "Pause for " + if (selectedMinutes >= 60) "${selectedMinutes / 60} Hour${if (selectedMinutes > 60) "s" else ""}" else "$selectedMinutes Minutes",
                onClick = { onConfirm(selectedMinutes) },
                style = HigButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
