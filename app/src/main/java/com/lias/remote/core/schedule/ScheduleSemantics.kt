// ====================================================================
// File:
// app/src/main/java/com/lias/remote/core/schedule/ScheduleSemantics.kt
// Version: 27.0.3
//
// Purpose:
//   Canonical schedule semantics and validation utilities.
//
// IMPORTANT OWNERSHIP:
//
//   ScheduleDraft.kt owns:
//     - ScheduleRuleScope
//     - ScheduleRuleDraft
//     - ScheduleDraft
//
//   This file MUST NOT redeclare those types.
//
// Canonical scopes:
//     ScheduleRuleScope.RECURRING
//     ScheduleRuleScope.CALENDAR
//
// LIAS semantics preserved:
//   - Downtime schedules BLOCK during matching rules.
//   - Whitelist schedules ALLOW during matching rules.
//   - Overnight windows are valid.
//   - Equal start/end times are invalid zero-duration windows.
//   - Recurring weekday rules are supported.
//   - Calendar start/end date rules are supported.
//   - Calendar end date is inclusive on the LIAS server.
//   - LIAS remains authoritative for final enforcement.
// ====================================================================

package com.lias.remote.core.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ====================================================================
// Canonical day ordering
// ====================================================================

val SCHEDULE_DAYS_ORDERED: List<String> =
    listOf(
        "mon",
        "tue",
        "wed",
        "thu",
        "fri",
        "sat",
        "sun"
    )

val SCHEDULE_WEEKDAYS: Set<String> =
    linkedSetOf(
        "mon",
        "tue",
        "wed",
        "thu",
        "fri"
    )

// ====================================================================
// Shared helpers used by ScheduleDraft.kt
// ====================================================================

/**
 * Convert a valid HH:mm time into minutes after midnight.
 *
 * Returns null for malformed values.
 *
 * Examples:
 *
 *   00:00 -> 0
 *   06:30 -> 390
 *   22:00 -> 1320
 */
fun minutesOfDayOrNull(
    value: String
): Int? {

    val parsed =
        parseScheduleTimeOrNull(
            value
        )
            ?: return null

    return parsed.hour *
        60 +
        parsed.minute
}

/**
 * Normalize all accepted weekday representations to LIAS's canonical
 * three-character lowercase representation.
 */
fun normalizeDay(
    raw: String
): String? =
    when (
        raw
            .trim()
            .lowercase()
    ) {

        "mon",
        "monday" ->
            "mon"

        "tue",
        "tues",
        "tuesday" ->
            "tue"

        "wed",
        "weds",
        "wednesday" ->
            "wed"

        "thu",
        "thur",
        "thurs",
        "thursday" ->
            "thu"

        "fri",
        "friday" ->
            "fri"

        "sat",
        "saturday" ->
            "sat"

        "sun",
        "sunday" ->
            "sun"

        else ->
            null
    }

/**
 * Normalize, deduplicate, and order weekdays Monday -> Sunday.
 *
 * This is intentionally a top-level function because ScheduleDraft.kt
 * already depends on it.
 */
fun orderedDays(
    days: Iterable<String>
): List<String> {

    val normalized =
        days
            .mapNotNull(
                ::normalizeDay
            )
            .toSet()

    return SCHEDULE_DAYS_ORDERED
        .filter {
            it in normalized
        }
}

/**
 * Convenience overload for nullable lists from wire models.
 */
fun orderedDays(
    days: List<String>?
): List<String> =
    orderedDays(
        days.orEmpty()
            .asIterable()
    )

/**
 * Inclusive recurring day range.
 *
 * Handles week wrapping:
 *
 *   mon -> fri
 *     mon tue wed thu fri
 *
 *   fri -> mon
 *     fri sat sun mon
 */
fun expandRecurringDayRange(
    fromDay: String,
    toDay: String
): List<String> {

    val from =
        normalizeDay(
            fromDay
        )
            ?: return emptyList()

    val to =
        normalizeDay(
            toDay
        )
            ?: return emptyList()

    val startIndex =
        SCHEDULE_DAYS_ORDERED
            .indexOf(
                from
            )

    val endIndex =
        SCHEDULE_DAYS_ORDERED
            .indexOf(
                to
            )

    if (
        startIndex < 0 ||
        endIndex < 0
    ) {
        return emptyList()
    }

    val result =
        mutableListOf<String>()

    var index =
        startIndex

    while (
        true
    ) {

        result +=
            SCHEDULE_DAYS_ORDERED[
                index
            ]

        if (
            index ==
            endIndex
        ) {
            break
        }

        index =
            (
                index +
                    1
                ) %
                SCHEDULE_DAYS_ORDERED.size
    }

    return result
}

// ====================================================================
// Validation result
// ====================================================================

data class ScheduleValidationResult(
    val valid: Boolean,
    val errors: List<String>
) {

    val firstError: String?
        get() =
            errors.firstOrNull()
}

// ====================================================================
// Schedule semantics
// ====================================================================

object ScheduleSemantics {

    /**
     * LIAS-supported modes.
     */
    fun normalizeMode(
        rawMode: String
    ): String =
        when (
            rawMode
                .trim()
                .lowercase()
        ) {

            "whitelist" ->
                "whitelist"

            else ->
                "downtime"
        }

    /**
     * Map schedule mode to the action serialized into each rule.
     *
     * Downtime:
     *   matching rule = BLOCK
     *   outside rule  = ALLOW
     *
     * Whitelist:
     *   matching rule = ALLOW
     *   outside rule  = BLOCK
     */
    fun actionForMode(
        mode: String
    ): String =
        when (
            normalizeMode(
                mode
            )
        ) {

            "whitelist" ->
                "allow"

            else ->
                "block"
        }

    /**
     * Validate a complete draft without attempting to reproduce the
     * LIAS enforcement engine.
     *
     * Final conflict/precedence decisions still belong to LIAS.
     */
    fun validate(
        draft: ScheduleDraft
    ): ScheduleValidationResult {

        val errors =
            mutableListOf<String>()

        validateName(
            draft =
                draft,
            errors =
                errors
        )

        validateMode(
            draft =
                draft,
            errors =
                errors
        )

        validateTimezone(
            draft =
                draft,
            errors =
                errors
        )

        draft.rules
            .forEachIndexed {
                    index,
                    rule ->

                validateRule(
                    index =
                        index,
                    rule =
                        rule,
                    errors =
                        errors
                )
            }

        return ScheduleValidationResult(
            valid =
                errors.isEmpty(),
            errors =
                errors
        )
    }

    private fun validateName(
        draft: ScheduleDraft,
        errors: MutableList<String>
    ) {

        if (
            draft.name
                .trim()
                .isBlank()
        ) {

            errors +=
                "Schedule name is required."
        }
    }

    private fun validateMode(
        draft: ScheduleDraft,
        errors: MutableList<String>
    ) {

        val rawMode =
            draft.mode
                .trim()
                .lowercase()

        if (
            rawMode !in
            setOf(
                "downtime",
                "whitelist"
            )
        ) {

            errors +=
                "Schedule mode must be Downtime or Whitelist."
        }
    }

    private fun validateTimezone(
        draft: ScheduleDraft,
        errors: MutableList<String>
    ) {

        val timezone =
            draft.timezone
                .trim()

        if (
            timezone.isBlank()
        ) {

            errors +=
                "Timezone is required."

            return
        }

        try {

            ZoneId.of(
                timezone
            )

        } catch (
            _: Exception
        ) {

            errors +=
                "Timezone '$timezone' is not valid."
        }
    }

    private fun validateRule(
        index: Int,
        rule: ScheduleRuleDraft,
        errors: MutableList<String>
    ) {

        val label =
            "Rule ${index + 1}"

        val startMinutes =
            minutesOfDayOrNull(
                rule.startTime
            )

        val endMinutes =
            minutesOfDayOrNull(
                rule.endTime
            )

        if (
            startMinutes ==
            null
        ) {

            errors +=
                "$label has an invalid start time."
        }

        if (
            endMinutes ==
            null
        ) {

            errors +=
                "$label has an invalid end time."
        }

        if (
            startMinutes !=
            null &&
            endMinutes !=
            null &&
            startMinutes ==
            endMinutes
        ) {

            /*
             * LIAS treats equal start/end as a zero-duration rule,
             * not as a 24-hour rule.
             */
            errors +=
                "$label must have a non-zero time window."
        }

        when (
            rule.scope
        ) {

            ScheduleRuleScope.RECURRING -> {

                validateRecurringRule(
                    label =
                        label,
                    rule =
                        rule,
                    errors =
                        errors
                )
            }

            ScheduleRuleScope.CALENDAR -> {

                validateCalendarRule(
                    label =
                        label,
                    rule =
                        rule,
                    errors =
                        errors
                )
            }
        }
    }

    private fun validateRecurringRule(
        label: String,
        rule: ScheduleRuleDraft,
        errors: MutableList<String>
    ) {

        val normalized =
            orderedDays(
                rule.days
            )

        if (
            normalized.isEmpty()
        ) {

            errors +=
                "$label must include at least one weekday."

            return
        }

        val invalidDays =
            rule.days
                .filter {
                    normalizeDay(
                        it
                    ) ==
                        null
                }

        if (
            invalidDays.isNotEmpty()
        ) {

            errors +=
                "$label contains an unsupported weekday."
        }
    }

    private fun validateCalendarRule(
        label: String,
        rule: ScheduleRuleDraft,
        errors: MutableList<String>
    ) {

        val startText =
            rule.startDate
                .trim()

        val endText =
            rule.endDate
                .trim()

        if (
            startText.isBlank() ||
            endText.isBlank()
        ) {

            errors +=
                "$label requires both a start date and end date."

            return
        }

        val startDate =
            parseScheduleDateOrNull(
                startText
            )

        val endDate =
            parseScheduleDateOrNull(
                endText
            )

        if (
            startDate ==
            null
        ) {

            errors +=
                "$label has an invalid start date."
        }

        if (
            endDate ==
            null
        ) {

            errors +=
                "$label has an invalid end date."
        }

        if (
            startDate !=
            null &&
            endDate !=
            null &&
            endDate.isBefore(
                startDate
            )
        ) {

            errors +=
                "$label end date cannot be before its start date."
        }
    }

    /**
     * Presentation-only description.
     *
     * This function does NOT determine whether a rule is currently
     * active. LIAS remains the runtime enforcement authority.
     */
    fun describeRule(
        rule: ScheduleRuleDraft
    ): String =
        buildString {

            when (
                rule.scope
            ) {

                ScheduleRuleScope.RECURRING -> {

                    val days =
                        orderedDays(
                            rule.days
                        )

                    append(
                        when {

                            days.isEmpty() ->
                                "No days"

                            days ==
                                SCHEDULE_DAYS_ORDERED ->
                                "Every day"

                            days ==
                                SCHEDULE_WEEKDAYS.toList() ->
                                "Weekdays"

                            else ->
                                days.joinToString(
                                    ", "
                                ) {
                                    day ->

                                    dayDisplayName(
                                        day
                                    )
                                }
                        }
                    )
                }

                ScheduleRuleScope.CALENDAR -> {

                    val start =
                        rule.startDate
                            .trim()

                    val end =
                        rule.endDate
                            .trim()

                    if (
                        start.isBlank() ||
                        end.isBlank()
                    ) {

                        append(
                            "Calendar dates not set"
                        )

                    } else if (
                        start ==
                        end
                    ) {

                        append(
                            start
                        )

                    } else {

                        append(
                            start
                        )

                        append(
                            " to "
                        )

                        append(
                            end
                        )
                    }
                }
            }

            append(
                " · "
            )

            append(
                rule.startTime
                    .trim()
            )

            append(
                "–"
            )

            append(
                rule.endTime
                    .trim()
            )

            if (
                isOvernight(
                    rule
                )
            ) {

                append(
                    " · ends next day"
                )
            }
        }

    /**
     * The ending time being earlier than the starting time means the
     * rule crosses midnight.
     *
     * Example:
     *   22:00 -> 06:00
     */
    fun isOvernight(
        rule: ScheduleRuleDraft
    ): Boolean {

        val start =
            minutesOfDayOrNull(
                rule.startTime
            )
                ?: return false

        val end =
            minutesOfDayOrNull(
                rule.endTime
            )
                ?: return false

        return end <
            start
    }

    fun isZeroDuration(
        rule: ScheduleRuleDraft
    ): Boolean {

        val start =
            minutesOfDayOrNull(
                rule.startTime
            )
                ?: return false

        val end =
            minutesOfDayOrNull(
                rule.endTime
            )
                ?: return false

        return start ==
            end
    }

    /**
     * Canonical day ordering for presentation/conversion callers.
     */
    fun orderedDays(
        days: Iterable<String>
    ): List<String> =
        com.lias.remote.core.schedule
            .orderedDays(
                days
            )

    /**
     * Canonical normalization exposed through ScheduleSemantics for
     * callers that prefer object-style access.
     */
    fun normalizeDay(
        day: String
    ): String? =
        com.lias.remote.core.schedule
            .normalizeDay(
                day
            )

    fun minutesOfDayOrNull(
        time: String
    ): Int? =
        com.lias.remote.core.schedule
            .minutesOfDayOrNull(
                time
            )
}

// ====================================================================
// Internal parsing
// ====================================================================

private val SCHEDULE_TIME_FORMAT:
    DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        "HH:mm"
    )

private val SCHEDULE_DATE_FORMAT:
    DateTimeFormatter =
    DateTimeFormatter.ISO_LOCAL_DATE

private fun parseScheduleTimeOrNull(
    raw: String
): LocalTime? =
    try {

        LocalTime.parse(
            raw.trim(),
            SCHEDULE_TIME_FORMAT
        )

    } catch (
        _: DateTimeParseException
    ) {

        null

    } catch (
        _: Exception
    ) {

        null
    }

private fun parseScheduleDateOrNull(
    raw: String
): LocalDate? =
    try {

        LocalDate.parse(
            raw.trim(),
            SCHEDULE_DATE_FORMAT
        )

    } catch (
        _: DateTimeParseException
    ) {

        null

    } catch (
        _: Exception
    ) {

        null
    }

private fun dayDisplayName(
    normalizedDay: String
): String =
    when (
        normalizedDay
    ) {

        "mon" ->
            "Mon"

        "tue" ->
            "Tue"

        "wed" ->
            "Wed"

        "thu" ->
            "Thu"

        "fri" ->
            "Fri"

        "sat" ->
            "Sat"

        "sun" ->
            "Sun"

        else ->
            normalizedDay
    }
