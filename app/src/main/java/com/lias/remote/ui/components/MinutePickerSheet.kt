// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/MinutePickerSheet.kt
// Version: 1.1.0
// Purpose: Scrollable minute wheel picker sheet with CupertinoPicker integration.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemGreenDark
import io.github.robinpcrd.cupertino.CupertinoPicker
import io.github.robinpcrd.cupertino.rememberCupertinoPickerState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MinutePickerSheet(
    targetLabel: String,
    targetSubtitle: String,
    currentExtension: ExtensionInfo?,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
    onCancelExtension: (() -> Unit)? = null,
    minMinutes: Int = 1,
    maxMinutes: Int = 120,
    quickPicks: List<Int> = listOf(15, 30, 60, 90, 120)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialMinutes = remember(currentExtension) {
        currentExtension?.let { ExtendHelper.minutesUntil(it.expiresAt) }?.takeIf { it in minMinutes..maxMinutes } ?: 30
    }

    val wheelItems = remember(minMinutes, maxMinutes) { (minMinutes..maxMinutes).toList() }
    val initialIndex = (initialMinutes - minMinutes).coerceIn(0, wheelItems.size - 1)
    val pickerState = rememberCupertinoPickerState(initialItem = initialIndex)

    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }
    selectedMinutes = wheelItems.getOrNull(pickerState.selectedItem) ?: initialMinutes

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Cancel / Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Extend Access — $targetLabel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (targetSubtitle.isNotBlank()) {
                        Text(
                            text = targetSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Quick Pick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPicks.forEach { pick ->
                    val isSelected = selectedMinutes == pick
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else LiasThemeColors.fill,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val targetIdx = (pick - minMinutes).coerceIn(0, wheelItems.size - 1)
                                pickerState.setValue(targetIdx)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pick}m",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Scrollable Cupertino Minute Wheel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CupertinoPicker(
                    state = pickerState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(wheelItems.size) { index ->
                        val minuteValue = wheelItems[index]
                        Text(
                            text = "$minuteValue min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Live Readout
            Text(
                text = "$selectedMinutes minutes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W800,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Primary Grant Action (Solid SystemGreen)
            HigButton(
                text = "Allow for $selectedMinutes Minutes",
                onClick = { onConfirm(selectedMinutes) },
                style = HigButtonStyle.Primary,
                modifier = Modifier.background(SystemGreenDark, RoundedCornerShape(12.dp))
            )

            // Cancel Active Extension (if present)
            if (currentExtension != null && onCancelExtension != null) {
                val left = ExtendHelper.minutesUntil(currentExtension.expiresAt)
                TextButton(onClick = onCancelExtension) {
                    Text(
                        text = "Cancel current extension (${left}m left)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
