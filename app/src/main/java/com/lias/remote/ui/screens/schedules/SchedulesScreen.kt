package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MiniWeekStrip
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun SchedulesScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    
    var showEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }

    HigLargeTitleScaffold(
        title = "Schedules",
        scrollState = scrollState,
        navTrailing = {
            HigTextButton(text = "＋", onClick = { editingSchedule = null; showEditor = true })
        }
    ) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { ListSectionHeader("${state.schedules.size} Configured") }
            
            items(state.schedules, key = { it.id }) { schedule ->
                GroupedListCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                CupertinoText(
                                    text = if (schedule.name == "Bedtime") "🛏 Bedtime" else if (schedule.name == "Homework") "📚 Homework" else schedule.name,
                                    style = HigTypography.headline,
                                    fontWeight = FontWeight.Bold
                                )
                                CupertinoText("${schedule.mode.uppercase()} · ${schedule.timezone}", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)
                            }
                            StatusPill(
                                text = schedule.mode,
                                tone = if (schedule.mode == "downtime") PillTone.BLOCKED else PillTone.ALLOWED
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        MiniWeekStrip(schedules = listOf(schedule))
                        Spacer(modifier = Modifier.height(8.dp))

                        CupertinoText(
                            text = "Used by policies · Weekly recurring window",
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        ScheduleEditorSheet(
            initialSchedule = editingSchedule,
            onDismiss = { showEditor = false },
            onSave = { viewModel.saveSchedule(it); showEditor = false }
        )
    }
}

@Composable
fun ScheduleEditorSheet(initialSchedule: Schedule?, onDismiss: () -> Unit, onSave: (Schedule) -> Unit) {
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var mode by remember { mutableStateOf(initialSchedule?.mode ?: "downtime") }
    var timezone by remember { mutableStateOf(initialSchedule?.timezone ?: "UTC") }
    val rules = remember { mutableStateOf<List<ScheduleRule>>(initialSchedule?.rules ?: listOf(ScheduleRule(listOf("mon", "tue", "wed", "thu", "fri"), "22:00", "06:00", "block"))) }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(
                title = if (initialSchedule == null) "New Schedule" else "Edit Schedule",
                onCancel = onDismiss,
                trailingAction = {
                    HigButton(
                        text = "Save",
                        onClick = { onSave(Schedule(id = initialSchedule?.id ?: "sched_${System.currentTimeMillis()}", name = name, mode = mode, timezone = timezone, rules = rules.value)) },
                        style = HigButtonStyle.Primary
                    )
                }
            )

            HigField(value = name, onValueChange = { name = it }, label = "Schedule Name", placeholder = "e.g. Bedtime")

            Column(modifier = Modifier.fillMaxWidth()) {
                CupertinoText("MODE", style = HigTypography.caption, color = LiasThemeColors.tertiaryLabel)
                SegmentedControl(
                    options = listOf("Downtime", "Whitelist"),
                    selectedOption = if (mode == "downtime") "Downtime" else "Whitelist",
                    onOptionSelected = { mode = it.lowercase() },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            HigField(value = timezone, onValueChange = { timezone = it }, label = "Timezone", placeholder = "America/New_York")
        }
    }
}
