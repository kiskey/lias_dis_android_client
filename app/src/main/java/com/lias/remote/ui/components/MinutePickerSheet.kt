// ====================================================================
// File: MinutePickerSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Native iOS minute picker sheet for Extend Access features.
//          Uses quick-pick chips and a slider for granular control.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec

@Composable
fun MinutePickerSheet(
    targetLabel: String,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
    quickPicks: List<Int> = listOf(15, 30, 60, 120)
) {
    var selectedMinutes by remember { mutableFloatStateOf(30f) }

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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

            // Granular Slider
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
        }
    }
}
