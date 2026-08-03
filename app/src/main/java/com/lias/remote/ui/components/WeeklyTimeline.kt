// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/WeeklyTimeline.kt
// Version: 1.0.0
// Purpose: Custom Canvas component drawing a 7-day timeline grid.
//          Visualizes schedule rules with colored bands matching the web UI.
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule

@Composable
fun WeeklyTimeline(
    schedules: List<Schedule>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF0071E3), Color(0xFF34C759), Color(0xFFFF9500),
        Color(0xFFAF52DE), Color(0xFF5856D6), Color(0xFF00C7BE)
    )
    val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

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
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
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
                        
                        schedules.forEachIndexed { schedIdx, schedule ->
                            val color = colors[schedIdx % colors.size]
                            
                            schedule.rules.forEach { rule ->
                                val ruleDays = rule.days.map { it.lowercase().take(3) }
                                val currentDayStr = days[dayIdx].take(3).lowercase()
                                
                                if (currentDayStr in ruleDays) {
                                    val startParts = rule.startTime.split(":")
                                    val endParts = rule.endTime.split(":")
                                    
                                    val startMin = (startParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
                                    val endMin = (endParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)
                                    
                                    if (startMin < endMin) {
                                        // Normal window
                                        val left = (startMin / 1440f) * canvasWidth
                                        val width = ((endMin - startMin) / 1440f) * canvasWidth
                                        drawRect(
                                            color = color,
                                            topLeft = Offset(left, 2f),
                                            size = Size(width, canvasHeight - 4f)
                                        )
                                    } else if (startMin > endMin) {
                                        // Overnight window (starts today, ends tomorrow)
                                        // We only draw the "starts today" part here to keep Canvas simple.
                                        // A fully accurate wrap-around requires drawing on the next day's track.
                                        val left = (startMin / 1440f) * canvasWidth
                                        val width = ((1440 - startMin) / 1440f) * canvasWidth
                                        drawRect(
                                            color = color,
                                            topLeft = Offset(left, 2f),
                                            size = Size(width, canvasHeight - 4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
