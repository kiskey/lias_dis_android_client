// ====================================================================
// File: GlobalSwitchSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Global Access Switch confirmation sheet. Requires explicit
//          confirmation for "Block All" kill-switch action.
//          Preserves global_default policy API contract.
// ====================================================================

package com.lias.remote.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.SegmentedControl

@Composable
fun GlobalSwitchSheet(
    currentPolicy: Policy,
    onDismiss: () -> Unit,
    onSave: (Policy) -> Unit
) {
    var selectedAction by remember { mutableStateOf(currentPolicy.action) }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🌐",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Global Access Switch",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Controls every non-infrastructure device on your network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Mode Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Text(
                    text = "CHOOSE MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.W600
                )
                SegmentedControl(
                    options = listOf("Allow All", "Schedule", "Block All"),
                    selectedOption = when(selectedAction) {
                        "allow" -> "Allow All"
                        "block" -> "Block All"
                        else -> "Schedule"
                    },
                    onOptionSelected = { selection ->
                        selectedAction = when(selection) {
                            "Allow All" -> "allow"
                            "Block All" -> "block"
                            else -> "schedule"
                        }
                    },
                    isDestructive = true,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = when(selectedAction) {
                        "allow" -> "Bypass all schedules (emergency override)"
                        "block" -> "Kill-switch — blocks every device (except Infrastructure)"
                        else -> "Normal operation — schedules apply"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Block All Warning
            if (selectedAction == "block") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⚠️ Block All requires confirmation and will disconnect all family devices immediately.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.W500
                    )
                }
            }

            HigButton(
                text = "Save",
                onClick = {
                    onSave(currentPolicy.copy(action = selectedAction, enabled = true))
                },
                style = if (selectedAction == "block") HigButtonStyle.Danger else HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
