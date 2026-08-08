// ====================================================================
// File: app/src/main/java/com/lias/remote/core/policy/PolicyPresentation.kt
// Version: 17.0.0
//
// Purpose:
//   Human-readable descriptions for policies and conflicts.
//
// Keeps engine terminology out of ordinary UI wherever possible.
// ====================================================================

package com.lias.remote.core.policy

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag

object PolicyPresentation {

    fun targetName(
        type: String,
        targetId: String,
        tags: List<Tag>,
        devices: List<Device>
    ): String =
        when (
            type
        ) {

            "global" ->
                "All non-infrastructure devices"

            "tag" ->
                tags.find {
                    it.id ==
                        targetId
                }
                    ?.name
                    ?: targetId.ifBlank {
                        "No tag selected"
                    }

            "device" ->
                devices.find {
                    it.pdid ==
                        targetId
                }
                    ?.displayName
                    ?: targetId.ifBlank {
                        "No device selected"
                    }

            else ->
                targetId
        }

    fun scopeTitle(
        type: String
    ): String =
        when (
            type
        ) {

            "global" ->
                "Global"

            "tag" ->
                "Tag Group"

            "device" ->
                "Single Device"

            else ->
                type
        }

    fun policySubtitle(
        policy: Policy,
        tags: List<Tag>,
        devices: List<Device>,
        schedules: List<Schedule>
    ): String {

        val target =
            targetName(
                policy.type,
                policy.targetID,
                tags,
                devices
            )

        val action =
            PolicySemantics
                .actionTitle(
                    policy.action
                )

        val scheduleCount =
            policy.resolveScheduleIDs()
                .count { id ->

                    schedules.any {
                        it.id ==
                            id
                    }
                }

        return buildString {

            append(target)

            append(" · ")

            append(action)

            if (
                policy.action ==
                "schedule"
            ) {

                append(" · ")

                append(scheduleCount)

                append(
                    if (
                        scheduleCount ==
                        1
                    ) {
                        " schedule"
                    } else {
                        " schedules"
                    }
                )
            }

            if (
                policy.type ==
                "device"
            ) {

                append(" · Priority ")

                append(
                    policy.priority
                )
            }
        }
    }

    fun conflictSummary(
        conflict: Conflict
    ): String =
        buildString {

            append(
                conflict.scheduleAName
            )

            append(" ")

            append(
                conflict.actionA
                    .uppercase()
            )

            append(" conflicts with ")

            append(
                conflict.scheduleBName
            )

            append(" ")

            append(
                conflict.actionB
                    .uppercase()
            )

            append(" · ")

            append(
                conflict.day
                    .replaceFirstChar {
                        it.uppercase()
                    }
            )

            append(" ")

            append(
                conflict.overlapStart
            )

            append("–")

            append(
                conflict.overlapEnd
            )
        }

    fun scheduleSubtitle(
        schedule: Schedule
    ): String =
        buildString {

            append(
                if (
                    schedule.mode.equals(
                        "whitelist",
                        true
                    )
                ) {
                    "Allow window"
                } else {
                    "Block window"
                }
            )

            append(" · ")

            append(
                schedule.timezone
            )

            append(" · ")

            append(
                schedule.safeRules.size
            )

            append(
                if (
                    schedule.safeRules.size ==
                    1
                ) {
                    " window"
                } else {
                    " windows"
                }
            )
        }
}
