// ====================================================================
// File:
// app/src/main/java/com/lias/remote/core/schedule/ScheduleSemantics.kt
// Version: 27.0.2
//
// Purpose:
//   Client-side schedule draft semantics.
//
// Architectural authority:
//   LIAS remains the final enforcement authority.
//
// This layer performs:
//   - safe form validation
//   - weekly rule normalization
//   - calendar date-range validation
//   - overnight-window representation
//   - mode → action mapping
//   - canonical DTO conversion
//
// Supported LIAS schedule forms:
//
//   WEEKLY
//     selected weekdays + start/end time
//
//   CALENDAR
//     inclusive start/end dates + start/end time
//
// Important:
//   An overnight rule such as 22:00 -> 06:00 is VALID.
//   Equal start/end times are invalid zero-duration windows.
// ====================================================================

package com.lias.remote.core.schedule

import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

enum class ScheduleRuleScope {

    WEEKLY,

    CALENDAR
}

data class ScheduleRuleDraft(

    val scope:
        ScheduleRuleScope =
        ScheduleRuleScope.WEEKLY,

    val days:
        Set<String> =
        DEFAULT_WEEKDAYS,

    val startTime:
        String =
        "22:00",

    val endTime:
        String =
        "06:00",

    val startDate:
        String =
        "",

    val endDate:
        String =
        ""
) {

    val isOvernight:
        Boolean
        get() {

            val start =
                parseTimeOrNull(
                    startTime
                )
                    ?: return false

            val end =
                parseTimeOrNull(
                    endTime
                )
                    ?: return false

            return end <
                start
        }

    val isZeroDuration:
        Boolean
        get() {

            val start =
                parseTimeOrNull(
                    startTime
                )
                    ?: return false

            val end =
                parseTimeOrNull(
                    endTime
                )
                    ?: return false

            return start ==
                end
        }

    fun normalizedDays():
        List<String> {

        if (
            scope ==
            ScheduleRuleScope.CALENDAR
        ) {

            /*
             * LIAS calendar rules are date-gated before weekly
             * evaluation. The dashboard also serializes all seven days
             * for calendar ranges, which keeps representation
             * compatible across clients.
             */
            return ALL_DAYS
        }

        return days
            .mapNotNull {
                normalizeDay(
                    it
                )
            }
            .distinct()
            .sortedBy {
                ALL_DAYS.indexOf(
                    it
                )
            }
    }

    fun toScheduleRule(
        scheduleMode: String
    ): ScheduleRule {

        val action =
            ScheduleSemantics
                .actionForMode(
                    scheduleMode
                )

        return ScheduleRule(
            days =
                normalizedDays(),
            startTime =
                startTime.trim(),
            endTime =
                endTime.trim(),
            action =
                action,
            startDate =
                if (
                    scope ==
                    ScheduleRuleScope.CALENDAR
                ) {

                    startDate
                        .trim()
                        .ifBlank {
                            null
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
                        .ifBlank {
                            null
                        }

                } else {
                    null
                }
        )
    }

    companion object {

        private fun parseTimeOrNull(
            value: String
        ): LocalTime? =
            try {

                LocalTime.parse(
                    value.trim(),
                    TIME_FORMAT
                )

            } catch (
                _: Exception
            ) {
                null
            }
    }
}

data class ScheduleDraft(

    val name:
        String =
        "",

    val mode:
        String =
        "downtime",

    val timezone:
        String =
        "UTC",

    val rules:
        List<ScheduleRuleDraft> =
        emptyList()
) {

    fun toSchedule(
        initialSchedule: Schedule?
    ): Schedule {

        val normalizedMode =
            ScheduleSemantics
                .normalizeMode(
                    mode
                )

        return Schedule(
            /*
             * Empty ID means CREATE.
             *
             * LIAS owns canonical schedule-ID generation.
             */
            id =
                initialSchedule
                    ?.id
                    .orEmpty(),
            name =
                name.trim(),
            mode =
                normalizedMode,
            timezone =
                timezone
                    .trim()
                    .ifBlank {
                        "UTC"
                    },
            rules =
                rules.map {
                    rule ->

                    rule.toScheduleRule(
                        normalizedMode
                    )
                }
        )
    }

    companion object {

        fun fromSchedule(
            schedule: Schedule
        ): ScheduleDraft =
            ScheduleDraft(
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
                            "UTC"
                        },
                rules =
                    schedule.safeRules
                        .map {
                            rule ->

                            val hasCalendarRange =
                                !rule.startDate
                                    .isNullOrBlank() &&
                                    !rule.endDate
                                        .isNullOrBlank()

                            ScheduleRuleDraft(
                                scope =
                                    if (
                                        hasCalendarRange
                                    ) {
                                        ScheduleRuleScope.CALENDAR
                                    } else {
                                        ScheduleRuleScope.WEEKLY
                                    },
                                days =
                                    rule.safeDays
                                        .mapNotNull {
                                            normalizeDay(
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
            )
    }
}

data class ScheduleValidationResult(

    val valid:
        Boolean,

    val errors:
        List<String>
) {

    val firstError:
        String?
        get() =
            errors.firstOrNull()
}

object ScheduleSemantics {

    fun normalizeMode(
        raw: String
    ): String =
        when (
            raw
                .trim()
                .lowercase()
        ) {

            "whitelist" ->
                "whitelist"

            else ->
                "downtime"
        }

    /**
     * The schedule mode owns the wire action.
     *
     * Downtime:
     *   rules BLOCK
     *   default ALLOW
     *
     * Whitelist:
     *   rules ALLOW
     *   default BLOCK
     */
    fun actionForMode(
        rawMode: String
    ): String =
        if (
            normalizeMode(
                rawMode
            ) ==
            "whitelist"
        ) {
            "allow"
        } else {
            "block"
        }

    fun validate(
        draft: ScheduleDraft
    ): ScheduleValidationResult {

        val errors =
            mutableListOf<String>()

        if (
            draft.name
                .trim()
                .isBlank()
        ) {

            errors +=
                "Schedule name is required."
        }

        if (
            draft.mode
                .trim()
                .lowercase() !in
            setOf(
                "downtime",
                "whitelist"
            )
        ) {

            errors +=
                "Schedule mode must be Downtime or Whitelist."
        }

        val timezone =
            draft.timezone
                .trim()

        if (
            timezone.isBlank()
        ) {

            errors +=
                "Timezone is required."

        } else {

            try {

                ZoneId.of(
                    timezone
                )

            } catch (
                _: Exception
            ) {

                errors +=
                    "Timezone '$timezone' is not valid on this device."
            }
        }

        draft.rules
            .forEachIndexed {
                    index,
                    rule ->

                errors +=
                    validateRule(
                        index =
                            index,
                        rule =
                            rule
                    )
            }

        return ScheduleValidationResult(
            valid =
                errors.isEmpty(),
            errors =
                errors
        )
    }

    private fun validateRule(
        index: Int,
        rule: ScheduleRuleDraft
    ): List<String> {

        val errors =
            mutableListOf<String>()

        val label =
            "Rule ${index + 1}"

        val startTime =
            parseTime(
                rule.startTime
            )

        val endTime =
            parseTime(
                rule.endTime
            )

        if (
            startTime ==
            null
        ) {

            errors +=
                "$label has an invalid start time."
        }

        if (
            endTime ==
            null
        ) {

            errors +=
                "$label has an invalid end time."
        }

        if (
            startTime !=
            null &&
            endTime !=
            null &&
            startTime ==
            endTime
        ) {

            errors +=
                "$label must have a non-zero time window."
        }

        when (
            rule.scope
        ) {

            ScheduleRuleScope.WEEKLY -> {

                val normalizedDays =
                    rule.normalizedDays()

                if (
                    normalizedDays.isEmpty()
                ) {

                    errors +=
                        "$label must include at least one day."
                }

                val unsupportedDays =
                    rule.days
                        .filter {
                            normalizeDay(
                                it
                            ) ==
                            null
                        }

                if (
                    unsupportedDays.isNotEmpty()
                ) {

                    errors +=
                        "$label contains an unsupported weekday."
                }
            }

            ScheduleRuleScope.CALENDAR -> {

                val startDateText =
                    rule.startDate
                        .trim()

                val endDateText =
                    rule.endDate
                        .trim()

                if (
                    startDateText.isBlank() ||
                    endDateText.isBlank()
                ) {

                    errors +=
                        "$label requires both a start date and end date."

                } else {

                    val startDate =
                        parseDate(
                            startDateText
                        )

                    val endDate =
                        parseDate(
                            endDateText
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
                        endDate <
                        startDate
                    ) {

                        errors +=
                            "$label end date cannot be before its start date."
                    }
                }
            }
        }

        return errors
    }

    /**
     * Human-readable rule description.
     *
     * This is deliberately presentation-only. It never evaluates
     * enforcement locally.
     */
    fun describeRule(
        rule: ScheduleRuleDraft
    ): String =
        buildString {

            when (
                rule.scope
            ) {

                ScheduleRuleScope.WEEKLY -> {

                    val days =
                        rule.normalizedDays()

                    append(
                        if (
                            days.isEmpty()
                        ) {
                            "No days"
                        } else {
                            days.joinToString(
                                ", "
                            ) {
                                day ->

                                day.uppercase()
                            }
                        }
                    )
                }

                ScheduleRuleScope.CALENDAR -> {

                    val startDate =
                        rule.startDate
                            .trim()

                    val endDate =
                        rule.endDate
                            .trim()

                    append(
                        if (
                            startDate.isBlank() ||
                            endDate.isBlank()
                        ) {
                            "Calendar dates not set"
                        } else {
                            "$startDate to $endDate"
                        }
                    )
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
                rule.isOvernight
            ) {

                append(
                    " · ends next day"
                )
            }
        }

    private fun parseTime(
        value: String
    ): LocalTime? =
        try {

            LocalTime.parse(
                value.trim(),
                TIME_FORMAT
            )

        } catch (
            _: DateTimeParseException
        ) {
            null
        }

    private fun parseDate(
        value: String
    ): LocalDate? =
        try {

            LocalDate.parse(
                value.trim(),
                DATE_FORMAT
            )

        } catch (
            _: DateTimeParseException
        ) {
            null
        }
}

/*
 * LIAS wire weekday convention.
 *
 * Keep three-letter lowercase names because:
 *   - the existing ScheduleRule model uses string days
 *   - the LIAS parser accepts these names
 *   - the web client emits the same representation.
 */
val ALL_DAYS:
    List<String> =
    listOf(
        "mon",
        "tue",
        "wed",
        "thu",
        "fri",
        "sat",
        "sun"
    )

val DEFAULT_WEEKDAYS:
    Set<String> =
    linkedSetOf(
        "mon",
        "tue",
        "wed",
        "thu",
        "fri"
    )

private val TIME_FORMAT =
    DateTimeFormatter.ofPattern(
        "HH:mm"
    )

private val DATE_FORMAT =
    DateTimeFormatter.ISO_LOCAL_DATE

private fun normalizeDay(
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
