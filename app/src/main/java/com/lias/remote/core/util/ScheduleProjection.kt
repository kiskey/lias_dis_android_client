// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ScheduleProjection.kt
// Version: 1.2.0
// Audit Fixes:
//   1. Added expandDayRange helper method for continuous day range rule parsing.
//   2. Updated projection engine to use `schedule.safeRules` and `rule.safeDays`
//      to eliminate Kotlin nullable receiver compilation errors.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule

object ScheduleProjection {

    private val dayToIndex = mapOf(
        "sun" to 0, "sunday" to 0,
        "mon" to 1, "monday" to 1,
        "tue" to 2, "tuesday" to 2,
        "wed" to 3, "wednesday" to 3,
        "thu" to 4, "thursday" to 4,
        "fri" to 5, "friday" to 5,
        "sat" to 6, "saturday" to 6
    )

    private val dayNames = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
    val daysOrder = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    fun expandDayRange(fromDay: String, toDay: String): List<String> {
        val startIdx = daysOrder.indexOf(fromDay.lowercase().trim().take(3))
        val endIdx = daysOrder.indexOf(toDay.lowercase().trim().take(3))
        if (startIdx == -1 || endIdx == -1) return listOf(fromDay)

        val result = mutableListOf<String>()
        var curr = startIdx
        while (true) {
            result.add(daysOrder[curr])
            if (curr == endIdx) break
            curr = (curr + 1) % 7
        }
        return result
    }

    data class Segment(
        val start: Int,
        val end: Int,
        val action: String,
        val scheduleId: String,
        val scheduleName: String,
        val ruleIdx: Int
    )

    private fun parseTime(timeStr: String): Int {
        val parts = timeStr.split(":")
        return if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
        } else 0
    }

    private fun formatMinuteOfWeek(m: Int): Pair<String, String> {
        val mod = ((m % 10080) + 10080) % 10080
        val dayIdx = mod / 1440
        val minOfDay = mod % 1440
        val hh = String.format("%02d", minOfDay / 60)
        val mm = String.format("%02d", minOfDay % 60)
        return dayNames[dayIdx] to "$hh:$mm"
    }

    fun projectSchedule(schedule: Schedule): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        schedule.safeRules.forEachIndexed { ruleIdx, rule ->
            val startMin = parseTime(rule.startTime)
            val endMin = parseTime(rule.endTime)
            if (startMin == endMin) return@forEachIndexed

            rule.safeDays.forEach { dStr ->
                val dayIdx = dayToIndex[dStr.lowercase().trim()] ?: return@forEach
                
                if (startMin < endMin) {
                    segments.add(Segment(dayIdx * 1440 + startMin, dayIdx * 1440 + endMin, rule.action, schedule.id, schedule.name, ruleIdx))
                } else {
                    // Overnight wrap
                    segments.add(Segment(dayIdx * 1440 + startMin, (dayIdx + 1) * 1440, rule.action, schedule.id, schedule.name, ruleIdx))
                    val nextDayIdx = (dayIdx + 1) % 7
                    segments.add(Segment(nextDayIdx * 1440, nextDayIdx * 1440 + endMin, rule.action, schedule.id, schedule.name, ruleIdx))
                }
            }
        }
        return segments
    }

    fun detectConflicts(schedules: List<Schedule>): List<Conflict> {
        if (schedules.isEmpty()) return emptyList()

        val allSegments = schedules.flatMap { projectSchedule(it) }.sortedBy { it.start }
        val conflicts = mutableListOf<Conflict>()
        val seen = mutableSetOf<String>()

        for (i in allSegments.indices) {
            for (j in i + 1 until allSegments.size) {
                if (allSegments[j].start >= allSegments[i].end) break

                val overlapStart = maxOf(allSegments[i].start, allSegments[j].start)
                val overlapEnd = minOf(allSegments[i].end, allSegments[j].end)

                if (overlapStart < overlapEnd && allSegments[i].action != allSegments[j].action) {
                    if (allSegments[i].scheduleId != allSegments[j].scheduleId || allSegments[i].ruleIdx != allSegments[j].ruleIdx) {
                        val (startDay, startTime) = formatMinuteOfWeek(overlapStart)
                        val (_, endTime) = formatMinuteOfWeek(overlapEnd)

                        val key = "${allSegments[i].scheduleId}|${allSegments[j].scheduleId}|$startDay|$startTime|$endTime"
                        if (seen.add(key)) {
                            conflicts.add(
                                Conflict(
                                    scheduleAID = allSegments[i].scheduleId,
                                    scheduleAName = allSegments[i].scheduleName,
                                    scheduleBID = allSegments[j].scheduleId,
                                    scheduleBName = allSegments[j].scheduleName,
                                    day = startDay,
                                    overlapStart = startTime,
                                    overlapEnd = endTime,
                                    actionA = allSegments[i].action,
                                    actionB = allSegments[j].action
                                )
                            )
                        }
                    }
                }
            }
        }
        return conflicts
    }
}
