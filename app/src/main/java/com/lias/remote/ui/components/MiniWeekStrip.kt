// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/MiniWeekStrip.kt
// Version: 1.0.0
// Purpose: Compact 7-cell weekly timeline strip (.timeline) for hero card
//          and inline schedule row visualization.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemGreenDark
import com.lias.remote.ui.theme.SystemIndigoDark
import com.lias.remote.ui.theme.SystemOrangeDark
import com.lias.remote.ui.theme.SystemPinkDark
import com.lias.remote.ui.theme.SystemTealDark

@Composable
fun MiniWeekStrip(
    schedules: List<Schedule>,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val daysOrder = ScheduleProjection.daysOrder

    val palette = listOf(
        SystemBlueDark, SystemGreenDark, SystemOrangeDark,
        SystemIndigoDark, SystemPinkDark, SystemTealDark
    )

    val segments = remember(schedules) {
        schedules.flatMap { ScheduleProjection.projectSchedule(it) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOrder.forEachIndexed { dayIdx, dayKey ->
            val projDayIdx = when (dayKey) {
                "sun" -> 0
                "mon" -> 1
                "tue" -> 2
                "wed" -> 3
                "thu" -> 4
                "fri" -> 5
                "sat" -> 6
                else -> 0
            }

            val daySegments = segments.filter { (it.start / 1440) == projDayIdx }
            val activeSegment = daySegments.firstOrNull()

            val cellColor = if (activeSegment != null) {
                val schedIdx = schedules.indexOfFirst { it.id == activeSegment.scheduleId }
                if (activeSegment.action == "block") {
                    MaterialTheme.colorScheme.error
                } else {
                    palette[if (schedIdx != -1) schedIdx % palette.size else 0]
                }
            } else {
                LiasThemeColors.fill
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(cellColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabels[dayIdx],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activeSegment != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
