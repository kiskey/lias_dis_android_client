// ====================================================================
// File: app/src/main/java/com/lias/remote/core/schedule/ScheduleSemantics.kt
// Version: 18.0.0
//
// Purpose:
//   Client-side validation and presentation semantics.
//
// LIAS remains authoritative. These checks mirror the backend where
// practical and catch mistakes before a network round-trip.
//
// Server-aligned checks:
//   - non-empty name
//   - mode downtime|whitelist
//   - valid IANA timezone
//   - >= 1 rule
//   - HH:mm times
//   - start != end
//   - recurring rule has >= 1 weekday
//   - calendar rule has BOTH dates
//   - YYYY-MM-DD dates
//
// Additional safe client check:
//   - calendar start date must not be after end date
// ====================================================================

package com.lias.remote.core.schedule

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.util.ScheduleProjection
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ScheduleValidationIssue(
    val ruleIndex: Int? = null,
    val message: String
)

data class ScheduleValidationResult(
    val issues: List<ScheduleValidationIssue>
) {

    val valid: Boolean
        get() =
            issues.isEmpty()

    val firstMessage: String?
        get() =
            issues
                .firstOrNull()
                ?.message
}

object ScheduleSemantics {

    val orderedDayKeys =
        listOf(
            "mon",
            "tue",
            "wed",
            "thu",
            "fri",
            "sat",
            "sun"
        )

    private val dayLabels =
        mapOf(
            "mon" to "Mon",
            "tue" to "Tue",
            "wed" to "Wed",
            "thu" to "Thu",
            "fri" to "Fri",
            "sat" to "Sat",
            "sun" to "Sun"
        )

    private val timeFormatter =
        DateTimeFormatter
            .ofPattern(
                "HH:mm"
            )

    private val dateFormatter =
        DateTimeFormatter
            .ISO_LOCAL_DATE

    val commonTimezones:
        List<String>
        get() {

            val local =
                ZoneId.systemDefault()
                    .id

            return listOf(
                local,
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
                .distinct()
        }

    fun normalizeMode(
        mode: String
    ): String =
        if (
            mode.equals(
                "whitelist",
                ignoreCase = true
            )
        ) {
            "whitelist"
        } else {
            "downtime"
        }

    fun normalizeDay(
        day: String
    ): String {

        val normalized =
            day.trim()
                .lowercase()

        return when {

            normalized.startsWith(
                "mon"
            ) ->
                "mon"

            normalized.startsWith(
                "tue"
            ) ->
                "tue"

            normalized.startsWith(
                "wed"
            ) ->
                "wed"

            normalized.startsWith(
                "thu"
            ) ->
                "thu"

            normalized.startsWith(
                "fri"
            ) ->
                "fri"

            normalized.startsWith(
                "sat"
            ) ->
                "sat"

            normalized.startsWith(
                "sun"
            ) ->
                "sun"

            else ->
                ""
        }
    }

    fun orderedDays(
        days: Collection<String>
    ): List<String> {

        val normalized =
            days.map {
                normalizeDay(
                    it
                )
            }
                .filter {
                    it.isNotBlank()
                }
                .toSet()

        return orderedDayKeys
            .filter {
                it in normalized
            }
    }

    fun dayLabel(
        day: String
    ): String =
        dayLabels[
            normalizeDay(
                day
            )
        ] ?: day

    fun modeTitle(
        mode: String
    ): String =
        if (
            normalizeMode(
                mode
            ) ==
            "whitelist"
        ) {
            "Allowed Hours"
        } else {
            "Downtime"
        }

    fun modeExplanation(
        mode: String
    ): String =
        if (
            normalizeMode(
                mode
            ) ==
            "whitelist"
        ) {
            "Internet is blocked by default and allowed only during the windows below."
        } else {
            "Internet is allowed by default and blocked during the windows below."
        }

    fun windowAction(
        mode: String
    ): String =
        if (
            normalizeMode(
                mode
            ) ==
            "whitelist"
        ) {
            "Allow"
        } else {
            "Block"
        }

    fun validTimezone(
        timezone: String
    ): Boolean {

        if (
            timezone.trim()
                .isBlank()
        ) {
            return false
        }

        return try {

            ZoneId.of(
                timezone.trim()
            )

            true

        } catch (
            _: DateTimeException
        ) {
            false
        }
    }

    fun validTime(
        time: String
    ): Boolean =
        try {

            LocalTime.parse(
                time.trim(),
                timeFormatter
            )

            true

        } catch (
            _: DateTimeParseException
        ) {
            false
        }

    fun validDate(
        date: String
    ): Boolean =
        try {

            LocalDate.parse(
                date.trim(),
                dateFormatter
            )

            true

        } catch (
            _: DateTimeParseException
        ) {
            false
        }

    fun minutesOfDayOrNull(
        time: String
    ): Int? =
        try {

            val parsed =
                LocalTime.parse(
                    time.trim(),
                    timeFormatter
                )

            parsed.hour *
                60 +
                parsed.minute

        } catch (
            _: DateTimeParseException
        ) {
            null
        }

    fun validate(
        draft: ScheduleDraft
    ): ScheduleValidationResult {

        val issues =
            mutableListOf<
                ScheduleValidationIssue
            >()

        if (
            draft.name
                .trim()
                .isBlank()
        ) {

            issues +=
                ScheduleValidationIssue(
                    message =
                        "Enter a schedule name."
                )
        }

        if (
            draft.mode
                .lowercase() !in
            setOf(
                "downtime",
                "whitelist"
            )
        ) {

            issues +=
                ScheduleValidationIssue(
                    message =
                        "Choose Downtime or Allowed Hours."
                )
        }

        if (
            !validTimezone(
                draft.timezone
            )
        ) {

            issues +=
                ScheduleValidationIssue(
                    message =
                        "Enter a valid IANA timezone such as America/Los_Angeles."
                )
        }

        if (
            draft.rules
                .isEmpty()
        ) {

            issues +=
                ScheduleValidationIssue(
                    message =
                        "Add at least one time window."
                )
        }

        draft.rules
            .forEachIndexed {
                    index,
                    rule ->

                val displayIndex =
                    index + 1

                if (
                    !validTime(
                        rule.startTime
                    )
                ) {

                    issues +=
                        ScheduleValidationIssue(
                            ruleIndex =
                                index,
                            message =
                                "Window $displayIndex has an invalid start time. Use HH:mm."
                        )
                }

                if (
                    !validTime(
                        rule.endTime
                    )
                ) {

                    issues +=
                        ScheduleValidationIssue(
                            ruleIndex =
                                index,
                            message =
                                "Window $displayIndex has an invalid end time. Use HH:mm."
                        )
                }

                if (
                    validTime(
                        rule.startTime
                    ) &&
                    validTime(
                        rule.endTime
                    ) &&
                    rule.startTime ==
                    rule.endTime
                ) {

                    issues +=
                        ScheduleValidationIssue(
                            ruleIndex =
                                index,
                            message =
                                "Window $displayIndex cannot start and end at the same time."
                        )
                }

                when (
                    rule.scope
                ) {

                    ScheduleRuleScope.RECURRING -> {

                        if (
                            orderedDays(
                                rule.days
                            )
                                .isEmpty()
                        ) {

                            issues +=
                                ScheduleValidationIssue(
                                    ruleIndex =
                                        index,
                                    message =
                                        "Window $displayIndex needs at least one weekday."
                                )
                        }
                    }

                    ScheduleRuleScope.CALENDAR -> {

                        if (
                            rule.startDate
                                .isBlank() ||
                            rule.endDate
                                .isBlank()
                        ) {

                            issues +=
                                ScheduleValidationIssue(
                                    ruleIndex =
                                        index,
                                    message =
                                        "Window $displayIndex needs both a start date and end date."
                                )

                        } else {

                            val validStart =
                                validDate(
                                    rule.startDate
                                )

                            val validEnd =
                                validDate(
                                    rule.endDate
                                )

                            if (
                                !validStart
                            ) {

                                issues +=
                                    ScheduleValidationIssue(
                                        ruleIndex =
                                            index,
                                        message =
                                            "Window $displayIndex has an invalid start date. Use YYYY-MM-DD."
                                    )
                            }

                            if (
                                !validEnd
                            ) {

                                issues +=
                                    ScheduleValidationIssue(
                                        ruleIndex =
                                            index,
                                        message =
                                            "Window $displayIndex has an invalid end date. Use YYYY-MM-DD."
                                    )
                            }

                            if (
                                validStart &&
                                validEnd
                            ) {

                                val start =
                                    LocalDate.parse(
                                        rule.startDate,
                                        dateFormatter
                                    )

                                val end =
                                    LocalDate.parse(
                                        rule.endDate,
                                        dateFormatter
                                    )

                                if (
                                    start.isAfter(
                                        end
                                    )
                                ) {

                                    issues +=
                                        ScheduleValidationIssue(
                                            ruleIndex =
                                                index,
                                            message =
                                                "Window $displayIndex starts after its end date."
                                        )
                                }
                            }
                        }
                    }
                }
            }

        return ScheduleValidationResult(
            issues =
                issues
        )
    }

    /**
     * Local recurring-window conflict preview.
     *
     * ScheduleProjection works on recurring weekday windows.
     * The server remains authoritative when the schedule is saved.
     */
    fun recurringConflicts(
        schedule: Schedule
    ): List<Conflict> =
        ScheduleProjection
            .detectConflicts(
                listOf(
                    schedule
                )
            )

    fun ruleSummary(
        rule: ScheduleRuleDraft
    ): String =
        buildString {

            when (
                rule.scope
            ) {

                ScheduleRuleScope.RECURRING -> {

                    val ordered =
                        orderedDays(
                            rule.days
                        )

                    append(
                        if (
                            ordered.size ==
                            7
                        ) {
                            "Every day"
                        } else {
                            ordered
                                .joinToString(
                                    " "
                                ) {
                                    dayLabel(
                                        it
                                    )
                                }
                        )
                }

                ScheduleRuleScope.CALENDAR -> {

                    append(
                        rule.startDate.ifBlank {
                            "Start date"
                        }
                    )

                    append(" – ")

                    append(
                        rule.endDate.ifBlank {
                            "End date"
                        }
                    )
                }
            }

            append(" · ")

            append(
                rule.startTime
            )

            append("–")

            append(
                rule.endTime
            )

            if (
                rule.isOvernight
            ) {
                append(
                    " · next day"
                )
            }
        }
}
