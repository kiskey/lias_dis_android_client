// ====================================================================
// File: ExtendAccessSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Refined extend access sheet with quick chips, slider,
//          and cancel active extension. Preserves POST /extend and
//          DELETE /extend API contracts.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton

@Composable
fun ExtendAccessSheet(
    targetLabel: String,
    targetSubtitle: String,
    currentExtension: ExtensionInfo?,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
    onCancelExtension: (() -> Unit)? = null
) {
    var selectedMinutes by remember { mutableFloatStateOf(30f) }
    val quickPicks = listOf(15, 30, 60, 120)

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = "Extend Access", onCancel = onDismiss)

            Text(
                text = targetLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = targetSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Active Extension Info
            currentExtension?.let { ext ->
                val minsLeft = ExtendHelper.minutesUntil(ext.expiresAt)
                Text(
                    text = "Active extension: ${minsLeft}m left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W600
                )
            }

            // Quick Picks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPicks.forEach { pick ->
                    val isSelected = selectedMinutes.toInt() == pick
                    HigButton(
                        text = "${pick}m",
                        onClick = { selectedMinutes = pick.toFloat() },
                        style = if (isSelected) HigButtonStyle.Primary else HigButtonStyle.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Slider
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${selectedMinutes.toInt()} minutes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.W700
                )
                Slider(
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            HigButton(
                text = "Allow for ${selectedMinutes.toInt()} Minutes",
                onClick = { onConfirm(selectedMinutes.toInt()) },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            // Cancel Active Extension
            if (currentExtension != null && onCancelExtension != null) {
                HigTextButton(
                    text = "Cancel current extension",
                    onClick = onCancelExtension,
                    isDestructive = true,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
