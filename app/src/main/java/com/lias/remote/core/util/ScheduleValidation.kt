// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ScheduleValidation.kt
// Version: 8.0.0
//
// Purpose:
//   Immediate client-side schedule validation.
//
// Backend parity:
//   Mirrors LIAS validation rules without replacing server authority.
//
// Backend rules reflected here:
//   - non-empty schedule name
//   - valid schedule mode
//   - valid IANA timezone
//   - at least one rule
//   - days required unless complete calendar-date range is supplied
//   - HH:mm start/end
//   - start != end
//   - YYYY-MM-DD dates
//   - complete date range, not one-sided
//   - end date >= start date
//   - block / allow actions only
//   - contradictory internal overlap detection
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class ScheduleValidationResult(
    val errors: List<String> =
        emptyList(),

    val warnings: List<String> =
        emptyList(),

    val conflicts: List<Conflict> =
        emptyList()
) {
    val isValid: Boolean
        get() =
            errors.isEmpty() &&
                conflicts.isEmpty()
}

object ScheduleValidation {

    private val timeFormatter =
        DateTimeFormatter.ofPattern(
            "HH:mm",
            Locale.US
        )

    private val dateFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE

    private val validDays =
        setOf(
            "sun",
            "sunday",
            "mon",
            "monday",
            "tue",
            "tuesday",
            "wed",
            "wednesday",
            "thu",
            "thursday",
            "fri",
            "friday",
            "sat",
            "saturday"
        )

    fun validate(
        schedule: Schedule
    ): ScheduleValidationResult {

        val errors =
            mutableListOf<String>()

        val warnings =
            mutableListOf<String>()

        if (
            schedule.name
                .trim()
                .isBlank()
        ) {
            errors.add(
                "Enter a schedule name."
            )
        }

        val normalizedMode =
            schedule.mode
                .trim()
                .lowercase()

        if (
            normalizedMode !=
                "downtime" &&
            normalizedMode !=
                "whitelist"
        ) {
            errors.add(
                "Choose Downtime or Whitelist mode."
            )
        }

        if (
            !isValidTimezone(
                schedule.timezone
            )
        ) {
            errors.add(
                "Enter a valid IANA timezone, such as America/Los_Angeles."
            )
        }

        if (
            schedule.safeRules
                .isEmpty()
        ) {
            errors.add(
                "Add at least one time window."
            )
        }

        schedule.safeRules
            .forEachIndexed { index, rule ->

                errors.addAll(
                    validateRule(
                        rule = rule,
                        ruleNumber =
                            index + 1
                    )
                )
            }

        /*
         * Internal contradictions are rejected by the backend using
         * MergeSchedules([]Schedule{s}).
         */
        val conflicts =
            if (
                errors.isEmpty()
            ) {
                ScheduleProjection
                    .detectConflicts(
                        listOf(
                            schedule
                        )
                    )
            } else {
                emptyList()
            }

        if (
            normalizedMode ==
                "downtime" &&
            schedule.safeRules.any {
                it.action.equals(
                    "allow",
                    ignoreCase = true
                )
            }
        ) {
            warnings.add(
                "This Downtime schedule contains Allow windows. LIAS supports this, but mixed actions can be harder to reason about."
            )
        }

        if (
            normalizedMode ==
                "whitelist" &&
            schedule.safeRules.any {
                it.action.equals(
                    "block",
                    ignoreCase = true
                )
            }
        ) {
            warnings.add(
                "This Whitelist schedule contains Block windows. LIAS supports this, but mixed actions can be harder to reason about."
            )
        }

        return ScheduleValidationResult(
            errors =
                errors.distinct(),
            warnings =
                warnings.distinct(),
            conflicts =
                conflicts
        )
    }

    fun validateBundle(
        schedules: List<Schedule>
    ): ScheduleValidationResult {

        if (
            schedules.isEmpty()
        ) {
            return ScheduleValidationResult()
        }

        val errors =
            mutableListOf<String>()

        val warnings =
            mutableListOf<String>()

        schedules.forEach { schedule ->

            val result =
                validate(
                    schedule
                )

            result.errors.forEach { error ->
                errors.add(
                    "${schedule.name}: $error"
                )
            }
        }

        if (
            ScheduleProjection
                .hasMixedTimezones(
                    schedules
                )
        ) {
            errors.add(
                "Schedules attached to the same policy must use the same timezone."
            )
        }

        val conflicts =
            if (
                errors.isEmpty()
            ) {
                ScheduleProjection
                    .detectConflicts(
                        schedules
                    )
            } else {
                emptyList()
            }

        return ScheduleValidationResult(
            errors =
                errors.distinct(),
            warnings =
                warnings.distinct(),
            conflicts =
                conflicts
        )
    }

    fun isValidTimezone(
        timezone: String
    ): Boolean {
        val value =
            timezone.trim()

        if (
            value.isBlank()
        ) {
            return false
        }

        return try {
            ZoneId.of(value)
            true
        } catch (
            _: Exception
        ) {
            false
        }
    }

    private fun validateRule(
        rule: ScheduleRule,
        ruleNumber: Int
    ): List<String> {

        val errors =
            mutableListOf<String>()

        val startDate =
            rule.startDate
                ?.trim()
                .orEmpty()

        val endDate =
            rule.endDate
                ?.trim()
                .orEmpty()

        val hasAnyDate =
            startDate.isNotBlank() ||
                endDate.isNotBlank()

        val hasCompleteDateRange =
            startDate.isNotBlank() &&
                endDate.isNotBlank()

        if (
            rule.safeDays.isEmpty() &&
            !hasCompleteDateRange
        ) {
            errors.add(
                "Window $ruleNumber needs at least one day, or both a start and end date."
            )
        }

        rule.safeDays
            .forEach { day ->

                if (
                    day.trim()
                        .lowercase() !in
                    validDays
                ) {
                    errors.add(
                        "Window $ruleNumber contains an invalid day: $day."
                    )
                }
            }

        val start =
            parseTime(
                rule.startTime
            )

        val end =
            parseTime(
                rule.endTime
            )

        if (
            start == null
        ) {
            errors.add(
                "Window $ruleNumber has an invalid start time. Use HH:mm."
            )
        }

        if (
            end == null
        ) {
            errors.add(
                "Window $ruleNumber has an invalid end time. Use HH:mm."
            )
        }

        if (
            start != null &&
            end != null &&
            start == end
        ) {
            errors.add(
                "Window $ruleNumber start and end times cannot be identical."
            )
        }

        val action =
            rule.action
                .trim()
                .lowercase()

        if (
            action != "allow" &&
            action != "block"
        ) {
            errors.add(
                "Window $ruleNumber must use Allow or Block."
            )
        }

        if (
            hasAnyDate &&
            !hasCompleteDateRange
        ) {
            errors.add(
                "Window $ruleNumber must include both start and end dates."
            )
        }

        val parsedStartDate =
            if (
                startDate.isNotBlank()
            ) {
                parseDate(
                    startDate
                )
            } else {
                null
            }

        val parsedEndDate =
            if (
                endDate.isNotBlank()
            ) {
                parseDate(
                    endDate
                )
            } else {
                null
            }

        if (
            startDate.isNotBlank() &&
            parsedStartDate == null
        ) {
            errors.add(
                "Window $ruleNumber start date must use YYYY-MM-DD."
            )
        }

        if (
            endDate.isNotBlank() &&
            parsedEndDate == null
        ) {
            errors.add(
                "Window $ruleNumber end date must use YYYY-MM-DD."
            )
        }

        if (
            parsedStartDate != null &&
            parsedEndDate != null &&
            parsedEndDate.isBefore(
                parsedStartDate
            )
        ) {
            errors.add(
                "Window $ruleNumber end date cannot be before its start date."
            )
        }

        return errors
    }

    private fun parseTime(
        value: String
    ): LocalTime? =
        try {
            LocalTime.parse(
                value.trim(),
                timeFormatter
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
                dateFormatter
            )
        } catch (
            _: DateTimeParseException
        ) {
            null
        }
}
