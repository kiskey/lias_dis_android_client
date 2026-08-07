// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/DetailedWeekGrid.kt
// Version: 2.0.0
// Audit Fixes:
//   1. Renamed from WeeklyTimeline to DetailedWeekGrid for Policy Wizard step 3 preview.
//   2. Replaced hardcoded hexes with theme-resolved HIG system colors.
//   3. Provided backward-compatible `WeeklyTimeline` alias.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemGreenDark
import com.lias.remote.ui.theme.SystemIndigoDark
import com.lias.remote.ui.theme.SystemOrangeDark
import com.lias.remote.ui.theme.SystemPinkDark
import com.lias.remote.ui.theme.SystemTealDark

@Composable
fun DetailedWeekGrid(
    schedules: List<Schedule>,
    conflicts: List<Conflict> = emptyList(),
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        SystemBlueDark, SystemGreenDark, SystemOrangeDark,
        SystemIndigoDark, SystemPinkDark, SystemTealDark
    )
    val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    val dayNames = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

    val allSegments = remember(schedules) {
        schedules.flatMap { ScheduleProjection.projectSchedule(it) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        days.forEachIndexed { dayIdx, dayLabel ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasHeight = size.height
                        val canvasWidth = size.width

                        allSegments.forEach { seg ->
                            val segDayIdx = seg.start / 1440
                            if (segDayIdx == dayIdx) {
                                val startMin = seg.start % 1440
                                var endMin = seg.end % 1440
                                if (endMin == 0 && seg.end > seg.start) {
                                    endMin = 1440
                                }

                                val schedIdx = schedules.indexOfFirst { it.id == seg.scheduleId }
                                val color = palette[if (schedIdx != -1) schedIdx % palette.size else 0]

                                val left = (startMin / 1440f) * canvasWidth
                                val durationMinutes = (endMin - startMin).coerceAtLeast(0)
                                val width = (durationMinutes / 1440f) * canvasWidth

                                if (width > 0f) {
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(left, 2f),
                                        size = Size(width, canvasHeight - 4f)
                                    )
                                }
                            }
                        }

                        val dayNameStr = dayNames[dayIdx]
                        conflicts.filter { it.day.equals(dayNameStr, ignoreCase = true) }.forEach { c ->
                            val startParts = c.overlapStart.split(":")
                            val endParts = c.overlapEnd.split(":")
                            val startMin = (startParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
                            var endMin = (endParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)
                            if (endMin <= startMin && endMin == 0) {
                                endMin = 1440
                            }

                            val left = (startMin / 1440f) * canvasWidth
                            val durationMinutes = (endMin - startMin).coerceAtLeast(0)
                            val width = (durationMinutes / 1440f) * canvasWidth

                            if (width > 0f) {
                                drawRect(
                                    color = Color.Red.copy(alpha = 0.6f),
                                    topLeft = Offset(left, 0f),
                                    size = Size(width, canvasHeight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyTimeline(
    schedules: List<Schedule>,
    conflicts: List<Conflict> = emptyList(),
    modifier: Modifier = Modifier
) {
    DetailedWeekGrid(schedules = schedules, conflicts = conflicts, modifier = modifier)
}
