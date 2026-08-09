// ====================================================================
// File: app/src/main/java/com/lias/remote/core/schedule/ScheduleDraft.kt
// Version: 27.2.0
//
// Purpose:
//   Editor-safe representation of LIAS schedules.
//
// Guarantees:
//   - New schedules keep id="".
//   - Existing IDs round-trip unchanged.
//   - Schedule Mode owns rule Action:
//       downtime  -> block windows
//       whitelist -> allow windows
//   - Recurring and calendar-date rules are represented explicitly.
//   - UI state never needs to mutate wire models directly.
// ====================================================================

package com.lias.remote.core.schedule

import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import java.time.ZoneId

enum class ScheduleRuleScope {
    RECURRING,
    CALENDAR
}

data class ScheduleRuleDraft(

    val scope: ScheduleRuleScope =
        ScheduleRuleScope.RECURRING,

    val days: Set<String> =
        setOf(
            "mon",
            "tue",
            "wed",
            "thu",
            "fri"
        ),

    val startTime: String =
        "22:00",

    val endTime: String =
        "06:00",

    val startDate: String =
        "",

    val endDate: String =
        ""
) {

    val isOvernight: Boolean
        get() {

            val start =
                ScheduleSemantics
                    .minutesOfDayOrNull(
                        startTime
                    )
                    ?: return false

            val end =
                ScheduleSemantics
                    .minutesOfDayOrNull(
                        endTime
                    )
                    ?: return false

            return end < start
        }

    val isAllDayPreset: Boolean
        get() =
            startTime ==
                "00:00" &&
                endTime ==
                "23:59"

    companion object {

        fun fromRule(
            rule: ScheduleRule
        ): ScheduleRuleDraft {

            val calendar =
                !rule.startDate
                    .isNullOrBlank() &&
                    !rule.endDate
                        .isNullOrBlank()

            return ScheduleRuleDraft(
                scope =
                    if (
                        calendar
                    ) {
                        ScheduleRuleScope.CALENDAR
                    } else {
                        ScheduleRuleScope.RECURRING
                    },

                days =
                    rule.safeDays
                        .mapNotNull {
                            ScheduleSemantics
                                .normalizeDay(
                                    it
                                )
                        }
                        .toSet(),

                startTime =
                    rule.startTime,

                endTime =
                    rule.endTime,

                startDate =
                    rule.startDate
                        .orEmpty(),

                endDate =
                    rule.endDate
                        .orEmpty()
            )
        }
    }

    fun toRule(
        scheduleMode: String
    ): ScheduleRule {

        val normalizedMode =
            ScheduleSemantics
                .normalizeMode(
                    scheduleMode
                )

        return ScheduleRule(
            days =
                if (
                    scope ==
                    ScheduleRuleScope.RECURRING
                ) {
                    ScheduleSemantics
                        .orderedDays(
                            days
                        )
                } else {
                    /*
                     * Calendar rules are selected by StartDate/EndDate.
                     *
                     * LIAS validateScheduleRules explicitly permits an
                     * empty Days list when both dates are supplied.
                     */
                    emptyList()
                },

            startTime =
                startTime.trim(),

            endTime =
                endTime.trim(),

            action =
                if (
                    normalizedMode ==
                    "whitelist"
                ) {
                    "allow"
                } else {
                    "block"
                },

            startDate =
                if (
                    scope ==
                    ScheduleRuleScope.CALENDAR
                ) {
                    startDate
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                } else {
                    null
                },

            endDate =
                if (
                    scope ==
                    ScheduleRuleScope.CALENDAR
                ) {
                    endDate
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                } else {
                    null
                }
        )
    }
}

data class ScheduleDraft(

    val name: String =
        "",

    val mode: String =
        "downtime",

    val timezone: String =
        ZoneId.systemDefault()
            .id,

    val rules: List<ScheduleRuleDraft> =
        listOf(
            ScheduleRuleDraft()
        )
) {

    companion object {

        fun fromSchedule(
            schedule: Schedule?
        ): ScheduleDraft {

            if (
                schedule ==
                null
            ) {
                return ScheduleDraft()
            }

            return ScheduleDraft(
                name =
                    schedule.name,

                mode =
                    ScheduleSemantics
                        .normalizeMode(
                            schedule.mode
                        ),

                timezone =
                    schedule.timezone
                        .ifBlank {
                            ZoneId.systemDefault()
                                .id
                        },

                rules =
                    schedule.safeRules
                        .map {
                            ScheduleRuleDraft
                                .fromRule(
                                    it
                                )
                        }
                        .ifEmpty {
                            listOf(
                                defaultRuleForMode(
                                    schedule.mode
                                )
                            )
                        }
            )
        }

        fun defaultRuleForMode(
            mode: String
        ): ScheduleRuleDraft =
            if (
                ScheduleSemantics
                    .normalizeMode(
                        mode
                    ) ==
                "whitelist"
            ) {

                ScheduleRuleDraft(
                    days =
                        setOf(
                            "mon",
                            "tue",
                            "wed",
                            "thu",
                            "fri"
                        ),
                    startTime =
                        "15:00",
                    endTime =
                        "17:00"
                )

            } else {

                ScheduleRuleDraft(
                    days =
                        setOf(
                            "mon",
                            "tue",
                            "wed",
                            "thu",
                            "fri",
                            "sat",
                            "sun"
                        ),
                    startTime =
                        "22:00",
                    endTime =
                        "06:00"
                )
            }
    }

    fun toSchedule(
        initialSchedule: Schedule?
    ): Schedule =
        Schedule(
            /*
             * Blank on create. LIAS owns sched_<id> generation.
             */
            id =
                initialSchedule
                    ?.id
                    .orEmpty(),

            name =
                name.trim(),

            mode =
                ScheduleSemantics
                    .normalizeMode(
                        mode
                    ),

            timezone =
                timezone.trim(),

            rules =
                rules.map {
                    it.toRule(
                        scheduleMode =
                            mode
                    )
                }
        )

    fun withMode(
        newMode: String
    ): ScheduleDraft =
        copy(
            mode =
                ScheduleSemantics
                    .normalizeMode(
                        newMode
                    )
        )
}
