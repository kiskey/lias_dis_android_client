// ====================================================================
// File: app/src/test/java/com/lias/remote/core/schedule/ScheduleSemanticsTest.kt
// Version: 18.0.0
//
// Purpose:
//   Regression tests for schedule editor semantics.
// ====================================================================

package com.lias.remote.core.schedule

import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleSemanticsTest {

    @Test
    fun `new schedule id remains blank`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Bedtime",
                timezone =
                    "UTC"
            )
                .toSchedule(
                    initialSchedule =
                        null
                )

        assertEquals(
            "",
            schedule.id
        )
    }

    @Test
    fun `existing schedule id is preserved`() {

        val existing =
            Schedule(
                id =
                    "sched_existing",
                name =
                    "Bedtime",
                mode =
                    "downtime",
                timezone =
                    "UTC",
                rules =
                    listOf(
                        ScheduleRule(
                            days =
                                listOf(
                                    "mon"
                                ),
                            startTime =
                                "22:00",
                            endTime =
                                "06:00",
                            action =
                                "block"
                        )
                    )
            )

        val schedule =
            ScheduleDraft
                .fromSchedule(
                    existing
                )
                .toSchedule(
                    existing
                )

        assertEquals(
            "sched_existing",
            schedule.id
        )
    }

    @Test
    fun `downtime mode forces block action`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Bedtime",
                mode =
                    "downtime",
                timezone =
                    "UTC",
                rules =
                    listOf(
                        ScheduleRuleDraft(
                            days =
                                setOf(
                                    "mon"
                                ),
                            startTime =
                                "22:00",
                            endTime =
                                "06:00"
                        )
                    )
            )
                .toSchedule(
                    null
                )

        assertEquals(
            "block",
            schedule.safeRules
                .single()
                .action
        )
    }

    @Test
    fun `whitelist mode forces allow action`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Homework",
                mode =
                    "whitelist",
                timezone =
                    "UTC",
                rules =
                    listOf(
                        ScheduleRuleDraft(
                            days =
                                setOf(
                                    "mon",
                                    "tue"
                                ),
                            startTime =
                                "15:00",
                            endTime =
                                "17:00"
                        )
                    )
            )
                .toSchedule(
                    null
                )

        assertEquals(
            "allow",
            schedule.safeRules
                .single()
                .action
        )
    }

    @Test
    fun `overnight rule is detected`() {

        val rule =
            ScheduleRuleDraft(
                startTime =
                    "22:00",
                endTime =
                    "06:00"
            )

        assertTrue(
            rule.isOvernight
        )
    }

    @Test
    fun `same start and end is invalid`() {

        val result =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Bad Window",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    startTime =
                                        "10:00",
                                    endTime =
                                        "10:00"
                                )
                            )
                    )
                )

        assertFalse(
            result.valid
        )

        assertTrue(
            result.issues
                .any {
                    it.message.contains(
                        "same time"
                    )
                }
        )
    }

    @Test
    fun `invalid timezone is rejected`() {

        val result =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Bedtime",
                        timezone =
                            "Mars/Olympus_Mons"
                    )
                )

        assertFalse(
            result.valid
        )
    }

    @Test
    fun `iana timezone is accepted`() {

        assertTrue(
            ScheduleSemantics
                .validTimezone(
                    "America/Los_Angeles"
                )
        )
    }

    @Test
    fun `recurring rule requires a day`() {

        val result =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Bedtime",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    scope =
                                        ScheduleRuleScope.RECURRING,
                                    days =
                                        emptySet()
                                )
                            )
                    )
                )

        assertFalse(
            result.valid
        )
    }

    @Test
    fun `calendar rule may have empty weekdays`() {

        val result =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "School Vacation",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    scope =
                                        ScheduleRuleScope.CALENDAR,
                                    days =
                                        emptySet(),
                                    startDate =
                                        "2026-12-20",
                                    endDate =
                                        "2027-01-03",
                                    startTime =
                                        "08:00",
                                    endTime =
                                        "22:00"
                                )
                            )
                    )
                )

        assertTrue(
            result.valid
        )
    }

    @Test
    fun `calendar rule serializes dates and no weekdays`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Vacation",
                mode =
                    "whitelist",
                timezone =
                    "America/Los_Angeles",
                rules =
                    listOf(
                        ScheduleRuleDraft(
                            scope =
                                ScheduleRuleScope.CALENDAR,
                            days =
                                setOf(
                                    "mon",
                                    "tue"
                                ),
                            startDate =
                                "2026-12-20",
                            endDate =
                                "2026-12-31",
                            startTime =
                                "09:00",
                            endTime =
                                "20:00"
                        )
                    )
            )
                .toSchedule(
                    null
                )

        val rule =
            schedule.safeRules
                .single()

        assertTrue(
            rule.safeDays
                .isEmpty()
        )

        assertEquals(
            "2026-12-20",
            rule.startDate
        )

        assertEquals(
            "2026-12-31",
            rule.endDate
        )

        assertEquals(
            "allow",
            rule.action
        )
    }

    @Test
    fun `calendar end date before start date is rejected`() {

        val result =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Bad Dates",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    scope =
                                        ScheduleRuleScope.CALENDAR,
                                    startDate =
                                        "2026-12-31",
                                    endDate =
                                        "2026-12-01"
                                )
                            )
                    )
                )

        assertFalse(
            result.valid
        )

        assertTrue(
            result.issues
                .any {
                    it.message.contains(
                        "starts after"
                    )
                }
        )
    }

    @Test
    fun `calendar fields are cleared when changing to recurring wire rule`() {

        val draft =
            ScheduleRuleDraft(
                scope =
                    ScheduleRuleScope.RECURRING,
                days =
                    setOf(
                        "sat",
                        "sun"
                    ),
                startTime =
                    "22:00",
                endTime =
                    "06:00",
                startDate =
                    "2026-12-20",
                endDate =
                    "2026-12-31"
            )

        val wire =
            draft.toRule(
                "downtime"
            )

        assertNull(
            wire.startDate
        )

        assertNull(
            wire.endDate
        )

        assertEquals(
            listOf(
                "sat",
                "sun"
            ),
            wire.safeDays
        )
    }

    @Test
    fun `weekday order is deterministic`() {

        val result =
            ScheduleSemantics
                .orderedDays(
                    setOf(
                        "sun",
                        "wed",
                        "mon",
                        "fri"
                    )
                )

        assertEquals(
            listOf(
                "mon",
                "wed",
                "fri",
                "sun"
            ),
            result
        )
    }

    @Test
    fun `all day preset remains valid backend compatible range`() {

        val rule =
            ScheduleRuleDraft(
                startTime =
                    "00:00",
                endTime =
                    "23:59"
            )

        assertTrue(
            rule.isAllDayPreset
        )

        val result =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "All Day",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                rule
                            )
                    )
                )

        assertTrue(
            result.valid
        )
    }
}
