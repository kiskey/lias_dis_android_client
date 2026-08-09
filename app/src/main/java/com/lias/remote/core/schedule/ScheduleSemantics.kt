// ====================================================================
// File:
// app/src/main/java/com/lias/remote/core/schedule/ScheduleSemantics.kt
// Version: 27.2.0
//
// Purpose:
//   Canonical schedule semantics and validation utilities.
//
// Ownership:
//   ScheduleDraft.kt owns:
//     - ScheduleRuleScope
//     - ScheduleRuleDraft
//     - ScheduleDraft
//
// This file owns behavior only.
//
// LIAS parity:
//   - Downtime -> block during matching windows.
//   - Whitelist -> allow during matching windows.
//   - Overnight windows are valid.
//   - Same start/end is invalid.
//   - RECURRING and CALENDAR scopes are supported.
//   - Calendar end date is inclusive.
//   - LIAS remains authoritative for persistence/enforcement.
// ====================================================================

package com.lias.remote.core.schedule

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.util.ScheduleProjection
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

val SCHEDULE_DAYS_ORDERED: List<String> =
    listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

val SCHEDULE_WEEKDAYS: Set<String> =
    linkedSetOf("mon", "tue", "wed", "thu", "fri")

fun minutesOfDayOrNull(value: String): Int? {
    val parsed = parseScheduleTimeOrNull(value) ?: return null
    return parsed.hour * 60 + parsed.minute
}

fun normalizeDay(raw: String): String? =
    when (raw.trim().lowercase()) {
        "mon", "monday" -> "mon"
        "tue", "tues", "tuesday" -> "tue"
        "wed", "weds", "wednesday" -> "wed"
        "thu", "thur", "thurs", "thursday" -> "thu"
        "fri", "friday" -> "fri"
        "sat", "saturday" -> "sat"
        "sun", "sunday" -> "sun"
        else -> null
    }

fun orderedDays(days: Iterable<String>): List<String> {
    val normalized = days.mapNotNull(::normalizeDay).toSet()
    return SCHEDULE_DAYS_ORDERED.filter { it in normalized }
}

fun orderedDays(days: List<String>?): List<String> =
    orderedDays(days.orEmpty().asIterable())

fun expandRecurringDayRange(
    fromDay: String,
    toDay: String
): List<String> {
    val from = normalizeDay(fromDay) ?: return emptyList()
    val to = normalizeDay(toDay) ?: return emptyList()
    val startIndex = SCHEDULE_DAYS_ORDERED.indexOf(from)
    val endIndex = SCHEDULE_DAYS_ORDERED.indexOf(to)

    if (startIndex < 0 || endIndex < 0) return emptyList()

    val result = mutableListOf<String>()
    var index = startIndex

    while (true) {
        result += SCHEDULE_DAYS_ORDERED[index]
        if (index == endIndex) break
        index = (index + 1) % SCHEDULE_DAYS_ORDERED.size
    }

    return result
}

data class ScheduleValidationIssue(
    val message: String
)

data class ScheduleValidationResult(
    val valid: Boolean,
    val errors: List<String>
) {
    val issues: List<ScheduleValidationIssue>
        get() = errors.map(::ScheduleValidationIssue)

    val firstError: String?
        get() = errors.firstOrNull()
}

object ScheduleSemantics {

    val orderedDayKeys: List<String> =
        SCHEDULE_DAYS_ORDERED

    val commonTimezones: List<String> =
        listOf(
            "UTC",
            "America/Los_Angeles",
            "America/Denver",
            "America/Chicago",
            "America/New_York",
            "Europe/London",
            "Europe/Paris",
            "Asia/Kolkata",
            "Asia/Tokyo",
            "Australia/Sydney"
        )

    fun normalizeMode(rawMode: String): String =
        when (rawMode.trim().lowercase()) {
            "whitelist" -> "whitelist"
            else -> "downtime"
        }

    fun actionForMode(mode: String): String =
        if (normalizeMode(mode) == "whitelist") "allow" else "block"

    fun windowAction(mode: String): String =
        if (normalizeMode(mode) == "whitelist") "Allow" else "Block"

    fun modeTitle(mode: String): String =
        if (normalizeMode(mode) == "whitelist") "Allowed Hours" else "Downtime"

    fun modeExplanation(mode: String): String =
        if (normalizeMode(mode) == "whitelist") {
            "Internet is blocked by default and allowed only during matching windows."
        } else {
            "Internet is allowed by default and blocked during matching windows."
        }

    fun validTimezone(timezone: String): Boolean {
        val value = timezone.trim()
        if (value.isBlank()) return false
        return try {
            ZoneId.of(value)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun validate(draft: ScheduleDraft): ScheduleValidationResult {
        val errors = mutableListOf<String>()

        if (draft.name.trim().isBlank()) {
            errors += "Schedule name is required."
        }

        val rawMode = draft.mode.trim().lowercase()
        if (rawMode !in setOf("downtime", "whitelist")) {
            errors += "Schedule mode must be Downtime or Whitelist."
        }

        if (!validTimezone(draft.timezone)) {
            errors += "Timezone '${draft.timezone.trim()}' is not valid."
        }

        if (draft.rules.isEmpty()) {
            errors += "Add at least one time window."
        }

        draft.rules.forEachIndexed { index, rule ->
            validateRule(index, rule, errors)
        }

        return ScheduleValidationResult(
            valid = errors.isEmpty(),
            errors = errors.distinct()
        )
    }

    private fun validateRule(
        index: Int,
        rule: ScheduleRuleDraft,
        errors: MutableList<String>
    ) {
        val label = "Rule ${index + 1}"
        val startMinutes = minutesOfDayOrNull(rule.startTime)
        val endMinutes = minutesOfDayOrNull(rule.endTime)

        if (startMinutes == null) {
            errors += "$label has an invalid start time."
        }

        if (endMinutes == null) {
            errors += "$label has an invalid end time."
        }

        if (
            startMinutes != null &&
            endMinutes != null &&
            startMinutes == endMinutes
        ) {
            errors += "$label start and end cannot be the same time."
        }

        when (rule.scope) {
            ScheduleRuleScope.RECURRING -> {
                val normalized = orderedDays(rule.days)
                if (normalized.isEmpty()) {
                    errors += "$label must include at least one weekday."
                }

                if (rule.days.any { normalizeDay(it) == null }) {
                    errors += "$label contains an unsupported weekday."
                }
            }

            ScheduleRuleScope.CALENDAR -> {
                val startText = rule.startDate.trim()
                val endText = rule.endDate.trim()

                if (startText.isBlank() || endText.isBlank()) {
                    errors += "$label requires both a start date and end date."
                    return
                }

                val startDate = parseScheduleDateOrNull(startText)
                val endDate = parseScheduleDateOrNull(endText)

                if (startDate == null) {
                    errors += "$label has an invalid start date."
                }

                if (endDate == null) {
                    errors += "$label has an invalid end date."
                }

                if (
                    startDate != null &&
                    endDate != null &&
                    startDate.isAfter(endDate)
                ) {
                    errors += "$label starts after its end date."
                }
            }
        }
    }

    fun recurringConflicts(schedule: Schedule): List<Conflict> =
        ScheduleProjection.detectConflicts(listOf(schedule))

    fun describeRule(rule: ScheduleRuleDraft): String =
        buildString {
            when (rule.scope) {
                ScheduleRuleScope.RECURRING -> {
                    val days = orderedDays(rule.days)
                    append(
                        when {
                            days.isEmpty() -> "No days"
                            days == SCHEDULE_DAYS_ORDERED -> "Every day"
                            days == SCHEDULE_WEEKDAYS.toList() -> "Weekdays"
                            else -> days.joinToString(", ") { dayLabel(it) }
                        }
                    )
                }

                ScheduleRuleScope.CALENDAR -> {
                    val start = rule.startDate.trim()
                    val end = rule.endDate.trim()

                    when {
                        start.isBlank() || end.isBlank() ->
                            append("Calendar dates not set")
                        start == end ->
                            append(start)
                        else ->
                            append("$start to $end")
                    }
                }
            }

            append(" · ${rule.startTime.trim()}–${rule.endTime.trim()}")

            if (isOvernight(rule)) {
                append(" · ends next day")
            }
        }

    fun ruleSummary(rule: ScheduleRuleDraft): String =
        describeRule(rule)

    fun isOvernight(rule: ScheduleRuleDraft): Boolean {
        val start = minutesOfDayOrNull(rule.startTime) ?: return false
        val end = minutesOfDayOrNull(rule.endTime) ?: return false
        return end < start
    }

    fun isZeroDuration(rule: ScheduleRuleDraft): Boolean {
        val start = minutesOfDayOrNull(rule.startTime) ?: return false
        val end = minutesOfDayOrNull(rule.endTime) ?: return false
        return start == end
    }

    fun dayLabel(day: String): String =
        when (normalizeDay(day)) {
            "mon" -> "Mon"
            "tue" -> "Tue"
            "wed" -> "Wed"
            "thu" -> "Thu"
            "fri" -> "Fri"
            "sat" -> "Sat"
            "sun" -> "Sun"
            else -> day
        }

    fun orderedDays(days: Iterable<String>): List<String> =
        com.lias.remote.core.schedule.orderedDays(days)

    fun normalizeDay(day: String): String? =
        com.lias.remote.core.schedule.normalizeDay(day)

    fun minutesOfDayOrNull(time: String): Int? =
        com.lias.remote.core.schedule.minutesOfDayOrNull(time)
}

private val SCHEDULE_TIME_FORMAT =
    DateTimeFormatter.ofPattern("HH:mm")

private val SCHEDULE_DATE_FORMAT =
    DateTimeFormatter.ISO_LOCAL_DATE

private fun parseScheduleTimeOrNull(raw: String): LocalTime? =
    try {
        LocalTime.parse(raw.trim(), SCHEDULE_TIME_FORMAT)
    } catch (_: DateTimeParseException) {
        null
    } catch (_: Exception) {
        null
    }

private fun parseScheduleDateOrNull(raw: String): LocalDate? =
    try {
        LocalDate.parse(raw.trim(), SCHEDULE_DATE_FORMAT)
    } catch (_: DateTimeParseException) {
        null
    } catch (_: Exception) {
        null
    }
