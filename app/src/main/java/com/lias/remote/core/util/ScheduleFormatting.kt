// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ScheduleFormatting.kt
// Version: 8.0.0
//
// Purpose:
//   Human-readable schedule descriptions for the Android UI.
//
// No business logic lives here.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ScheduleFormatting {

    private val inputFormatter =
        DateTimeFormatter.ofPattern(
            "HH:mm",
            Locale.US
        )

    private val displayFormatter =
        DateTimeFormatter.ofPattern(
            "h:mm a",
            Locale.US
        )

    fun modeTitle(
        mode: String
    ): String =
        when (
            mode
                .trim()
                .lowercase()
        ) {
            "whitelist" ->
                "Whitelist"

            else ->
                "Downtime"
        }

    fun modeExplanation(
        mode: String
    ): String =
        when (
            mode
                .trim()
                .lowercase()
        ) {
            "whitelist" ->
                "Blocked outside Allow windows."

            else ->
                "Allowed outside Block windows."
        }

    fun scheduleSummary(
        schedule: Schedule
    ): String {
        val rules =
            schedule.safeRules

        return when {
            rules.isEmpty() ->
                "No time windows"

            rules.size == 1 ->
                ruleSummary(
                    rules.first()
                )

            else ->
                "${rules.size} time windows"
        }
    }

    fun ruleSummary(
        rule: ScheduleRule
    ): String {
        val days =
            formatDays(
                rule.safeDays
            )

        val times =
            "${formatTime(rule.startTime)} – ${formatTime(rule.endTime)}"

        val overnight =
            if (
                isOvernight(
                    rule
                )
            ) {
                " · overnight"
            } else {
                ""
            }

        val action =
            rule.action
                .replaceFirstChar {
                    if (
                        it.isLowerCase()
                    ) {
                        it.titlecase()
                    } else {
                        it.toString()
                    }
                }

        return buildString {
            append(action)
            append(" · ")
            append(days)
            append(" · ")
            append(times)
            append(overnight)

            if (
                !rule.startDate.isNullOrBlank() &&
                !rule.endDate.isNullOrBlank()
            ) {
                append(" · ")
                append(
                    rule.startDate
                )
                append(" to ")
                append(
                    rule.endDate
                )
            }
        }
    }

    fun formatDays(
        days: List<String>
    ): String {
        val normalized =
            days
                .map {
                    ScheduleProjection
                        .normalizeDay(it)
                }
                .distinct()

        if (
            normalized.size == 7
        ) {
            return "Every day"
        }

        if (
            normalized ==
            listOf(
                "mon",
                "tue",
                "wed",
                "thu",
                "fri"
            )
        ) {
            return "Weekdays"
        }

        if (
            normalized.toSet() ==
            setOf(
                "sat",
                "sun"
            )
        ) {
            return "Weekends"
        }

        return normalized
            .sortedBy {
                ScheduleProjection
                    .daysOrder
                    .indexOf(it)
            }
            .joinToString(
                ", "
            ) {
                when (it) {
                    "mon" -> "Mon"
                    "tue" -> "Tue"
                    "wed" -> "Wed"
                    "thu" -> "Thu"
                    "fri" -> "Fri"
                    "sat" -> "Sat"
                    "sun" -> "Sun"
                    else -> it
                }
            }
            .ifBlank {
                "No recurring days"
            }
    }

    fun formatTime(
        value: String
    ): String =
        try {
            LocalTime
                .parse(
                    value,
                    inputFormatter
                )
                .format(
                    displayFormatter
                )
        } catch (
            _: Exception
        ) {
            value
        }

    fun isOvernight(
        rule: ScheduleRule
    ): Boolean =
        try {
            val start =
                LocalTime.parse(
                    rule.startTime,
                    inputFormatter
                )

            val end =
                LocalTime.parse(
                    rule.endTime,
                    inputFormatter
                )

            start.isAfter(
                end
            )

        } catch (
            _: Exception
        ) {
            false
        }

    fun policyUsageText(
        usageCount: Int
    ): String =
        when (usageCount) {
            0 ->
                "Not used by any rule"

            1 ->
                "Used by 1 rule"

            else ->
                "Used by $usageCount rules"
        }
}
