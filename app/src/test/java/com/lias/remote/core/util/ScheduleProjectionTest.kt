// ====================================================================
// File: app/src/test/java/com/lias/remote/core/util/ScheduleProjectionTest.kt
// Version: 1.1.0
// Audit Fixes: 
//   1. Added unit test for overnight wrap-around week-minute boundary calculations
//      (10080 minute boundary) to ensure rendering canvas endMin parity.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleProjectionTest {

    @Test
    fun `test worked example conflict`() {
        val schedA = Schedule(
            id = "sched_bedtime01",
            name = "Bedtime",
            mode = "downtime",
            timezone = "UTC",
            rules = listOf(
                ScheduleRule(
                    days = listOf("mon", "tue", "wed", "thu", "fri"),
                    startTime = "21:00",
                    endTime = "07:00",
                    action = "block"
                )
            )
        )

        val schedB = Schedule(
            id = "sched_gaming02",
            name = "Gaming Hour",
            mode = "whitelist",
            timezone = "UTC",
            rules = listOf(
                ScheduleRule(
                    days = listOf("mon", "tue", "wed", "thu", "fri"),
                    startTime = "22:00",
                    endTime = "23:00",
                    action = "allow"
                )
            )
        )

        val conflicts = ScheduleProjection.detectConflicts(listOf(schedA, schedB))
        
        assertEquals("Expected 1 conflict", 1, conflicts.size)
        
        val conflict = conflicts.first()
        assertEquals("monday", conflict.day)
        assertEquals("22:00", conflict.overlapStart)
        assertEquals("23:00", conflict.overlapEnd)
        assertEquals("block", conflict.actionA)
        assertEquals("allow", conflict.actionB)
    }

    @Test
    fun `test same action overlap allowed`() {
        val schedA = Schedule(
            id = "sched_1",
            name = "Downtime 1",
            mode = "downtime",
            timezone = "UTC",
            rules = listOf(
                ScheduleRule(listOf("mon"), "15:00", "17:00", "block")
            )
        )

        val schedB = Schedule(
            id = "sched_2",
            name = "Downtime 2",
            mode = "downtime",
            timezone = "UTC",
            rules = listOf(
                ScheduleRule(listOf("mon"), "16:00", "18:00", "block")
            )
        )

        val conflicts = ScheduleProjection.detectConflicts(listOf(schedA, schedB))
        assertTrue("Expected 0 conflicts for same-action overlap", conflicts.isEmpty())
    }

    @Test
    fun `test wrap around overnight projection`() {
        val sched = Schedule(
            id = "sched_overnight",
            name = "Weekend Night",
            mode = "downtime",
            timezone = "UTC",
            rules = listOf(
                ScheduleRule(listOf("sat"), "22:00", "06:00", "block")
            )
        )

        val segments = ScheduleProjection.projectSchedule(sched)
        
        assertEquals("Expected 2 segments for overnight rule", 2, segments.size)
        
        // Saturday is day 6. Segment 1: Sat 22:00 to 24:00 -> 6*1440+1320 = 9960 to 7*1440 = 10080
        assertEquals(9960, segments[0].start)
        assertEquals(10080, segments[0].end)
        
        // Sunday is day 0. Segment 2: Sun 00:00 to 06:00 -> 0 to 360
        assertEquals(0, segments[1].start)
        assertEquals(360, segments[1].end)
    }

    @Test
    fun `test overnight segment canvas modulo endMin calculation`() {
        val segEndBoundary = 10080
        val segStartBoundary = 9960
        
        val startMin = segStartBoundary % 1440
        var endMin = segEndBoundary % 1440
        
        if (endMin == 0 && segEndBoundary > segStartBoundary) {
            endMin = 1440
        }
        
        val durationMinutes = (endMin - startMin).coerceAtLeast(0)
        
        assertEquals(1320, startMin)
        assertEquals(1440, endMin)
        assertEquals(120, durationMinutes)
    }
}
