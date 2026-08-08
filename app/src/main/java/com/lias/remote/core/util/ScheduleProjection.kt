// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ScheduleProjection.kt
// Version: 8.0.0
//
// Purpose:
//   Android-side projection of LIAS schedule rules onto a seven-day
//   minute-of-week timeline.
//
// Contract:
//   Mirrors apps/lias/internal/scheduleconflict/conflict.go.
//
// Important:
//   This is a preview / preflight engine.
//
//   The LIAS backend remains authoritative for:
//     - final validation
//     - schedule persistence
//     - policy bundle validation
//     - enforcement
//
// Semantics:
//   - Sunday = day index 0, matching Go time.Weekday.
//   - Same-day rule: one segment.
//   - Overnight rule: two segments.
//   - Identical start/end: no projected segment; validation rejects it.
//   - Invalid times/days are ignored here because ScheduleValidation
//     reports them separately to the editor.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object ScheduleProjection {

    const val MINUTES_PER_DAY =
        1_440

    const val MINUTES_PER_WEEK =
        10_080

    val daysOrder =
        listOf(
            "mon",
            "tue",
            "wed",
            "thu",
            "fri",
            "sat",
            "sun"
        )

    private val dayToIndex =
        mapOf(
            "sun" to 0,
            "sunday" to 0,

            "mon" to 1,
            "monday" to 1,

            "tue" to 2,
            "tuesday" to 2,

            "wed" to 3,
            "wednesday" to 3,

            "thu" to 4,
            "thursday" to 4,

            "fri" to 5,
            "friday" to 5,

            "sat" to 6,
            "saturday" to 6
        )

    private val fullDayNames =
        listOf(
            "sunday",
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday"
        )

    private val strictTimeFormatter =
        DateTimeFormatter.ofPattern(
            "HH:mm",
            Locale.US
        )

    data class Segment(
        val start: Int,
        val end: Int,
        val action: String,
        val scheduleId: String,
        val scheduleName: String,
        val ruleIdx: Int
    )

    fun expandDayRange(
        fromDay: String,
        toDay: String
    ): List<String> {
        val from =
            normalizeDay(fromDay)

        val to =
            normalizeDay(toDay)

        val startIndex =
            daysOrder.indexOf(
                from
            )

        val endIndex =
            daysOrder.indexOf(
                to
            )

        if (
            startIndex < 0 ||
            endIndex < 0
        ) {
            return listOf(
                fromDay
            )
        }

        val result =
            mutableListOf<String>()

        var current =
            startIndex

        while (true) {
            result.add(
                daysOrder[current]
            )

            if (
                current ==
                endIndex
            ) {
                break
            }

            current =
                (
                    current + 1
                    ) % 7
        }

        return result
    }

    fun projectSchedule(
        schedule: Schedule
    ): List<Segment> {
        val segments =
            mutableListOf<Segment>()

        schedule.safeRules
            .forEachIndexed { ruleIndex, rule ->

                val startMinute =
                    parseTime(
                        rule.startTime
                    )
                        ?: return@forEachIndexed

                val endMinute =
                    parseTime(
                        rule.endTime
                    )
                        ?: return@forEachIndexed

                if (
                    startMinute ==
                    endMinute
                ) {
                    return@forEachIndexed
                }

                rule.safeDays
                    .forEach { rawDay ->

                        val dayIndex =
                            dayToIndex[
                                rawDay
                                    .trim()
                                    .lowercase()
                            ]
                                ?: return@forEach

                        if (
                            startMinute <
                            endMinute
                        ) {

                            segments.add(
                                Segment(
                                    start =
                                        dayIndex *
                                            MINUTES_PER_DAY +
                                            startMinute,

                                    end =
                                        dayIndex *
                                            MINUTES_PER_DAY +
                                            endMinute,

                                    action =
                                        normalizeAction(
                                            rule.action
                                        ),

                                    scheduleId =
                                        schedule.id,

                                    scheduleName =
                                        schedule.name,

                                    ruleIdx =
                                        ruleIndex
                                )
                            )

                        } else {

                            /*
                             * Overnight rule.
                             *
                             * Example:
                             *   Sat 22:00 -> 06:00
                             *
                             * becomes:
                             *   Sat 22:00 -> Sun 00:00
                             *   Sun 00:00 -> Sun 06:00
                             */
                            segments.add(
                                Segment(
                                    start =
                                        dayIndex *
                                            MINUTES_PER_DAY +
                                            startMinute,

                                    end =
                                        (
                                            dayIndex + 1
                                            ) *
                                            MINUTES_PER_DAY,

                                    action =
                                        normalizeAction(
                                            rule.action
                                        ),

                                    scheduleId =
                                        schedule.id,

                                    scheduleName =
                                        schedule.name,

                                    ruleIdx =
                                        ruleIndex
                                )
                            )

                            val nextDayIndex =
                                (
                                    dayIndex + 1
                                    ) % 7

                            segments.add(
                                Segment(
                                    start =
                                        nextDayIndex *
                                            MINUTES_PER_DAY,

                                    end =
                                        nextDayIndex *
                                            MINUTES_PER_DAY +
                                            endMinute,

                                    action =
                                        normalizeAction(
                                            rule.action
                                        ),

                                    scheduleId =
                                        schedule.id,

                                    scheduleName =
                                        schedule.name,

                                    ruleIdx =
                                        ruleIndex
                                )
                            )
                        }
                    }
            }

        return segments
    }

    fun detectConflicts(
        schedules: List<Schedule>
    ): List<Conflict> {
        if (
            schedules.isEmpty()
        ) {
            return emptyList()
        }

        val segments =
            schedules
                .flatMap {
                    projectSchedule(it)
                }
                .sortedWith(
                    compareBy<Segment> {
                        it.start
                    }.thenBy {
                        it.end
                    }
                )

        val conflicts =
            mutableListOf<Conflict>()

        val seen =
            mutableSetOf<String>()

        for (
            firstIndex
            in segments.indices
        ) {

            for (
                secondIndex
                in firstIndex + 1
                until segments.size
            ) {

                val first =
                    segments[
                        firstIndex
                    ]

                val second =
                    segments[
                        secondIndex
                    ]

                if (
                    second.start >=
                    first.end
                ) {
                    break
                }

                val overlapStart =
                    maxOf(
                        first.start,
                        second.start
                    )

                val overlapEnd =
                    minOf(
                        first.end,
                        second.end
                    )

                if (
                    overlapStart >=
                    overlapEnd
                ) {
                    continue
                }

                if (
                    first.action ==
                    second.action
                ) {
                    continue
                }

                /*
                 * Match backend behavior:
                 *
                 * A contradiction is ignored only when both projected
                 * segments originate from the exact same source rule.
                 */
                if (
                    first.scheduleId ==
                        second.scheduleId &&
                    first.ruleIdx ==
                        second.ruleIdx
                ) {
                    continue
                }

                val (
                    day,
                    startTime
                    ) =
                    formatMinuteOfWeek(
                        overlapStart
                    )

                val (_, endTime) =
                    formatMinuteOfWeek(
                        overlapEnd
                    )

                val key =
                    buildString {
                        append(
                            first.scheduleId
                        )
                        append('|')

                        append(
                            second.scheduleId
                        )
                        append('|')

                        append(day)
                        append('|')

                        append(startTime)
                        append('|')

                        append(endTime)
                    }

                if (
                    seen.add(key)
                ) {
                    conflicts.add(
                        Conflict(
                            scheduleAID =
                                first.scheduleId,

                            scheduleAName =
                                first.scheduleName,

                            scheduleBID =
                                second.scheduleId,

                            scheduleBName =
                                second.scheduleName,

                            day =
                                day,

                            overlapStart =
                                startTime,

                            overlapEnd =
                                endTime,

                            actionA =
                                first.action,

                            actionB =
                                second.action
                        )
                    )
                }
            }
        }

        return conflicts
    }

    fun hasMixedTimezones(
        schedules: List<Schedule>
    ): Boolean =
        schedules
            .map {
                it.timezone.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .size > 1

    fun normalizeDay(
        day: String
    ): String =
        when (
            day
                .trim()
                .lowercase()
        ) {
            "monday", "mon" ->
                "mon"

            "tuesday", "tue" ->
                "tue"

            "wednesday", "wed" ->
                "wed"

            "thursday", "thu" ->
                "thu"

            "friday", "fri" ->
                "fri"

            "saturday", "sat" ->
                "sat"

            "sunday", "sun" ->
                "sun"

            else ->
                day
                    .trim()
                    .lowercase()
        }

    private fun parseTime(
        value: String
    ): Int? =
        try {
            val time =
                LocalTime.parse(
                    value.trim(),
                    strictTimeFormatter
                )

            time.hour * 60 +
                time.minute

        } catch (
            _: DateTimeParseException
        ) {
            null
        }

    private fun formatMinuteOfWeek(
        minuteOfWeek: Int
    ): Pair<String, String> {
        val normalized =
            (
                (
                    minuteOfWeek %
                        MINUTES_PER_WEEK
                    ) +
                    MINUTES_PER_WEEK
                ) %
                MINUTES_PER_WEEK

        val dayIndex =
            normalized /
                MINUTES_PER_DAY

        val minuteOfDay =
            normalized %
                MINUTES_PER_DAY

        val hour =
            minuteOfDay / 60

        val minute =
            minuteOfDay % 60

        return fullDayNames[
            dayIndex
        ] to String.format(
            Locale.US,
            "%02d:%02d",
            hour,
            minute
        )
    }

    private fun normalizeAction(
        action: String
    ): String =
        action
            .trim()
            .lowercase()
}
