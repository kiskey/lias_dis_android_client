// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Moved misplaced `import androidx.compose.foundation.layout.Spacer` from the 
//      bottom of the file to the correct import block at the top.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var mode by remember { mutableStateOf(initialSchedule?.mode ?: "downtime") }
    var timezone by remember { mutableStateOf(initialSchedule?.timezone ?: "UTC") }
    
    val rules = remember {
        mutableStateListOf<ScheduleRule>().apply {
            if (initialSchedule != null) addAll(initialSchedule.rules)
            else add(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Schedule Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "downtime",
                    onClick = { mode = "downtime" },
                    label = { Text("Downtime") }
                )
                FilterChip(
                    selected = mode == "whitelist",
                    onClick = { mode = "whitelist" },
                    label = { Text("Whitelist") }
                )
            }

            OutlinedTextField(
                value = timezone,
                onValueChange = { timezone = it },
                label = { Text("Timezone (e.g. America/New_York)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Rules", style = MaterialTheme.typography.titleMedium)
            
            rules.forEachIndexed { index, rule ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").forEach { day ->
                                FilterChip(
                                    selected = day in rule.days,
                                    onClick = {
                                        val newDays = rule.days.toMutableList()
                                        if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
                                        rules[index] = rule.copy(days = newDays)
                                    },
                                    label = { Text(day.take(3).uppercase()) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = rule.startTime,
                                onValueChange = { rules[index] = rule.copy(startTime = it) },
                                label = { Text("Start (HH:MM)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rule.endTime,
                                onValueChange = { rules[index] = rule.copy(endTime = it) },
                                label = { Text("End (HH:MM)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(Schedule(
                        id = initialSchedule?.id ?: "sched_${System.currentTimeMillis()}",
                        name = name,
                        mode = mode,
                        timezone = timezone,
                        rules = rules.toList()
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Schedule")
            }
        }
    }
}
