// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/PolicyValidation.kt
// Version: 9.0.0
//
// Purpose:
//   Client-side preflight validation and explanation for LIAS policies.
//
// Important:
//   The LIAS backend remains authoritative.
//
//   This validator exists to prevent obviously invalid configurations
//   from reaching the server and to explain precedence behavior before
//   the user commits a rule.
//
// Backend-aligned semantics:
//   - Only global_default is the effective global policy.
//   - infrastructure is immutable.
//   - Device rules: highest priority matching device rule wins.
//   - Tag rules: all matching tags participate; BLOCK is fail-closed.
//   - Schedule action with no schedules is intentionally default-open.
//   - Schedule bundles must not mix timezones.
//   - Contradictory schedule windows are invalid.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag

data class PolicyValidationResult(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

object PolicyValidation {

    fun validate(
        policy: Policy,
        schedules: List<Schedule>,
        tags: List<Tag>,
        devices: List<Device>,
        existingPolicies: List<Policy>
    ): PolicyValidationResult {

        val errors =
            mutableListOf<String>()

        val warnings =
            mutableListOf<String>()

        val name =
            policy.name.trim()

        val type =
            policy.type
                .trim()
                .lowercase()

        val action =
            policy.action
                .trim()
                .lowercase()

        if (name.isBlank()) {
            errors.add(
                "Enter a rule name."
            )
        }

        if (
            type !in
            setOf(
                "global",
                "tag",
                "device"
            )
        ) {
            errors.add(
                "Choose a valid target scope."
            )
        }

        if (
            action !in
            setOf(
                "allow",
                "block",
                "schedule"
            )
        ) {
            errors.add(
                "Choose Allow, Schedule, or Block."
            )
        }

        // ------------------------------------------------------------
        // Global
        // ------------------------------------------------------------

        if (type == "global") {

            if (
                policy.id !=
                "global_default"
            ) {
                errors.add(
                    "LIAS supports one global policy: Global Access Switch."
                )
            }

            if (
                policy.targetID
                    .isNotBlank()
            ) {
                errors.add(
                    "The global policy cannot target a tag or device."
                )
            }
        }

        // ------------------------------------------------------------
        // Tag
        // ------------------------------------------------------------

        if (type == "tag") {

            if (
                policy.targetID
                    .isBlank()
            ) {
                errors.add(
                    "Choose a tag."
                )
            }

            if (
                policy.targetID ==
                "infrastructure"
            ) {
                errors.add(
                    "Infrastructure is protected and cannot be targeted by a rule."
                )
            }

            if (
                policy.targetID
                    .isNotBlank() &&
                tags.none {
                    it.id ==
                        policy.targetID
                }
            ) {
                errors.add(
                    "The selected tag no longer exists."
                )
            }

            val siblingPolicies =
                existingPolicies.filter {
                    it.id != policy.id &&
                        it.enabled &&
                        it.type ==
                            "tag" &&
                        it.targetID ==
                            policy.targetID
                }

            if (
                siblingPolicies
                    .isNotEmpty()
            ) {
                warnings.add(
                    "This tag already has ${siblingPolicies.size} enabled ${if (siblingPolicies.size == 1) "rule" else "rules"}. LIAS combines matching tag rules and BLOCK takes precedence."
                )
            }
        }

        // ------------------------------------------------------------
        // Device
        // ------------------------------------------------------------

        if (type == "device") {

            if (
                policy.targetID
                    .isBlank()
            ) {
                errors.add(
                    "Choose a device."
                )
            }

            val device =
                devices.find {
                    it.pdid ==
                        policy.targetID
                }

            if (
                policy.targetID
                    .isNotBlank() &&
                device == null
            ) {
                errors.add(
                    "The selected device is no longer in the LIAS inventory."
                )
            }

            if (
                device?.safeTags
                    ?.contains(
                        "infrastructure"
                    ) == true
            ) {
                errors.add(
                    "Infrastructure devices are immune to access rules."
                )
            }

            val siblings =
                existingPolicies.filter {
                    it.id != policy.id &&
                        it.enabled &&
                        it.type ==
                            "device" &&
                        it.targetID ==
                            policy.targetID
                }

            if (
                siblings.isNotEmpty()
            ) {
                val highest =
                    siblings.maxByOrNull {
                        it.priority
                    }

                warnings.add(
                    buildString {
                        append(
                            "This device already has "
                        )
                        append(
                            siblings.size
                        )
                        append(
                            if (
                                siblings.size == 1
                            ) {
                                " enabled rule. "
                            } else {
                                " enabled rules. "
                            }
                        )

                        append(
                            "LIAS uses the highest-priority matching device rule"
                        )

                        highest?.let {
                            append(
                                "; current highest is “"
                            )
                            append(
                                it.name
                            )
                            append(
                                "” at priority "
                            )
                            append(
                                it.priority
                            )
                        }

                        append(".")
                    }
                )
            }
        }

        // ------------------------------------------------------------
        // Schedules
        // ------------------------------------------------------------

        val scheduleIds =
            policy.resolveScheduleIDs()
                .distinct()

        if (
            action != "schedule" &&
            scheduleIds.isNotEmpty()
        ) {
            warnings.add(
                "Attached schedules are ignored unless enforcement is Schedule."
            )
        }

        if (
            action ==
                "schedule"
        ) {

            if (
                scheduleIds.isEmpty()
            ) {
                warnings.add(
                    "No schedules are attached. LIAS intentionally defaults this rule to ALLOW."
                )
            } else {

                val selectedSchedules =
                    scheduleIds.mapNotNull { id ->
                        schedules.find {
                            it.id == id
                        }
                    }

                if (
                    selectedSchedules.size !=
                    scheduleIds.size
                ) {
                    errors.add(
                        "One or more attached schedules no longer exist."
                    )
                }

                if (
                    selectedSchedules.isNotEmpty()
                ) {

                    val bundleValidation =
                        ScheduleValidation
                            .validateBundle(
                                selectedSchedules
                            )

                    errors.addAll(
                        bundleValidation.errors
                    )

                    if (
                        bundleValidation.conflicts
                            .isNotEmpty()
                    ) {
                        errors.add(
                            "Attached schedules contain contradictory time windows."
                        )
                    }

                    if (
                        ScheduleProjection
                            .hasMixedTimezones(
                                selectedSchedules
                            )
                    ) {
                        errors.add(
                            "Schedules attached to one rule must use the same timezone."
                        )
                    }
                }
            }
        }

        return PolicyValidationResult(
            errors =
                errors.distinct(),
            warnings =
                warnings.distinct()
        )
    }

    fun targetLabel(
        policy: Policy,
        tags: List<Tag>,
        devices: List<Device>
    ): String =
        when (
            policy.type
                .lowercase()
        ) {
            "global" ->
                "All non-infrastructure devices"

            "tag" ->
                tags.find {
                    it.id ==
                        policy.targetID
                }
                    ?.name
                    ?: policy.targetID
                        .ifBlank {
                            "No tag selected"
                        }

            "device" ->
                devices.find {
                    it.pdid ==
                        policy.targetID
                }
                    ?.displayName
                    ?: policy.targetID
                        .ifBlank {
                            "No device selected"
                        }

            else ->
                policy.targetID
                    .ifBlank {
                        "Unknown target"
                    }
        }

    fun actionExplanation(
        policy: Policy
    ): String =
        when (
            policy.action
                .lowercase()
        ) {
            "allow" ->
                if (
                    policy.id ==
                    "global_default"
                ) {
                    "Allows all non-infrastructure devices and bypasses lower-level rules."
                } else {
                    "Allows this target whenever this rule is the effective policy."
                }

            "block" ->
                if (
                    policy.id ==
                    "global_default"
                ) {
                    "Blocks every non-infrastructure device immediately."
                } else {
                    "Blocks this target whenever this rule is effective."
                }

            "schedule" ->
                if (
                    policy.resolveScheduleIDs()
                        .isEmpty()
                ) {
                    "Schedule-driven with no schedules attached — currently defaults to Allow."
                } else {
                    "Access is determined by the attached schedule bundle."
                }

            else ->
                "Unknown enforcement action."
        }
}
