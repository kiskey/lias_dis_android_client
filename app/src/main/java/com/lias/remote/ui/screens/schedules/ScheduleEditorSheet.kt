// ====================================================================
// File: ScheduleEditorSheet.kt
// Version: 3.0.2 (HIG Redesign Fix)
// Purpose: Added missing HigButtonStyle import.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.SegmentedControl

@Composable
fun ScheduleEditorSheet(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var mode by remember { mutableStateOf(initialSchedule?.mode ?: "downtime") }
    var timezone by remember { mutableStateOf(initialSchedule?.timezone ?: "UTC") }

    val rules = remember {
        mutableStateOf<List<ScheduleRule>>(
            initialSchedule?.rules ?: listOf(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))
        )
    }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(
                title = if (initialSchedule == null) "New Schedule" else "Edit Schedule",
                onCancel = onDismiss,
                trailingAction = {
                    HigButton(
                        text = "Save",
                        onClick = {
                            onSave(Schedule(
                                id = initialSchedule?.id ?: "sched_${System.currentTimeMillis()}",
                                name = name,
                                mode = mode,
                                timezone = timezone,
                                rules = rules.value
                            ))
                        },
                        style = HigButtonStyle.Primary
                    )
                }
            )

            HigField(
                value = name,
                onValueChange = { name = it },
                label = "Schedule Name",
                placeholder = "e.g. Bedtime"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentedControl(
                    options = listOf("Downtime", "Whitelist"),
                    selectedOption = if (mode == "downtime") "Downtime" else "Whitelist",
                    onOptionSelected = { mode = it.lowercase() },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            HigField(
                value = timezone,
                onValueChange = { timezone = it },
                label = "Timezone",
                placeholder = "America/New_York"
            )
        }
    }
}
