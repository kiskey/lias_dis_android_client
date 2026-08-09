// ====================================================================
// File: app/src/main/java/com/lias/remote/core/policy/PolicySemantics.kt
// Version: 17.0.0
//
// Purpose:
//   Client-side semantic validation and explanatory policy logic.
//
// Important:
//   Server validation remains authoritative.
//
// Client validation catches:
//   - invalid global policy creation
//   - missing target
//   - infrastructure targeting
//   - deleted targets
//   - empty names
//   - invalid device priority
//   - shadow/duplicate target policies
//   - locally visible schedule contradictions
//   - mixed-timezone bundles
// ====================================================================

package com.lias.remote.core.policy

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.util.ScheduleProjection

data class PolicySemanticResult(
    val valid: Boolean,
    val error: String? = null
)

data class PolicyShadowWarning(
    val existingPolicy: Policy,
    val message: String
)

object PolicySemantics {

    const val GLOBAL_POLICY_ID =
        "global_default"

    const val INFRASTRUCTURE_TAG_ID =
        "infrastructure"

    fun availableTags(
        tags: List<Tag>
    ): List<Tag> =
        tags
            .filterNot {
                it.id ==
                    INFRASTRUCTURE_TAG_ID
            }
            .sortedWith(
                compareByDescending<Tag> {
                    it.precedence
                }.thenBy {
                    it.name.lowercase()
                }
            )

    fun availableDevices(
        devices: List<Device>
    ): List<Device> =
        devices
            .filterNot {
                it.safeTags.contains(
                    INFRASTRUCTURE_TAG_ID
                )
            }
            .sortedBy {
                it.displayName
                    .lowercase()
            }

    fun validateDraft(
        draft: PolicyDraft,
        initialPolicy: Policy?,
        tags: List<Tag>,
        devices: List<Device>,
        schedules: List<Schedule>
    ): PolicySemanticResult {

        if (
            draft.name
                .trim()
                .isBlank()
        ) {
            return invalid(
                "Enter a rule name."
            )
        }

        val type =
            draft.type
                .lowercase()

        val action =
            draft.action
                .lowercase()

        if (
            type !in
            setOf(
                "global",
                "tag",
                "device"
            )
        ) {
            return invalid(
                "Choose a valid rule scope."
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
            return invalid(
                "Choose a valid access behavior."
            )
        }

        if (
            type ==
            "global"
        ) {

            if (
                initialPolicy?.id !=
                GLOBAL_POLICY_ID
            ) {
                return invalid(
                    "Global Access must be edited through the existing Global Access rule."
                )
            }

            if (
                draft.targetId
                    .isNotBlank()
            ) {
                return invalid(
                    "Global Access cannot have a device or tag target."
                )
            }
        }

        if (
            type ==
            "tag"
        ) {

            if (
                draft.targetId
                    .isBlank()
            ) {
                return invalid(
                    "Choose a tag group."
                )
            }

            if (
                draft.targetId ==
                INFRASTRUCTURE_TAG_ID
            ) {
                return invalid(
                    "Infrastructure cannot be targeted by access rules."
                )
            }

            if (
                tags.none {
                    it.id ==
                        draft.targetId
                }
            ) {
                return invalid(
                    "The selected tag no longer exists."
                )
            }
        }

        if (
            type ==
            "device"
        ) {

            if (
                draft.targetId
                    .isBlank()
            ) {
                return invalid(
                    "Choose a device."
                )
            }

            val device =
                devices.find {
                    it.pdid ==
                        draft.targetId
                }
                    ?: return invalid(
                        "The selected device no longer exists."
                    )

            if (
                device.safeTags
                    .contains(
                        INFRASTRUCTURE_TAG_ID
                    )
            ) {
                return invalid(
                    "Infrastructure devices are always online and cannot be targeted."
                )
            }

            if (
                draft.priorityText
                    .toIntOrNull() ==
                null
            ) {
                return invalid(
                    "Priority must be a whole number."
                )
            }
        }

        if (
            action ==
            "schedule"
        ) {

            val missingSchedules =
                draft.scheduleIds
                    .filterNot {
                        selectedId ->

                        schedules.any {
                            it.id ==
                                selectedId
                        }
                    }

            if (
                missingSchedules
                    .isNotEmpty()
            ) {
                return invalid(
                    "One or more selected schedules no longer exist."
                )
            }
        }

        return PolicySemanticResult(
            valid = true
        )
    }

    fun shadowWarning(
        draft: PolicyDraft,
        initialPolicy: Policy?,
        policies: List<Policy>
    ): PolicyShadowWarning? {

        if (
            draft.type ==
            "global" ||
            draft.targetId
                .isBlank()
        ) {
            return null
        }

        val existing =
            policies.firstOrNull {
                policy ->

                policy.id !=
                    initialPolicy?.id &&
                    policy.type ==
                    draft.type &&
                    policy.targetID ==
                    draft.targetId &&
                    policy.reasonTag
                        .isNullOrBlank()
            }
                ?: return null

        return PolicyShadowWarning(
            existingPolicy =
                existing,
            message =
                when (
                    draft.type
                ) {

                    "device" ->
                        "“${existing.name}” already targets this device. LIAS will use the higher-priority device rule."

                    "tag" ->
                        "“${existing.name}” already targets this tag. Tag rules are evaluated together; they do not replace one another by priority."

                    else ->
                        "Another rule already targets this selection."
                }
        )
    }

    fun selectedSchedules(
        scheduleIds: Set<String>,
        schedules: List<Schedule>
    ): List<Schedule> =
        scheduleIds
            .mapNotNull {
                    id ->

                schedules.find {
                    it.id ==
                        id
                }
            }

    fun localConflicts(
        scheduleIds: Set<String>,
        schedules: List<Schedule>
    ): List<Conflict> =
        ScheduleProjection
            .detectConflicts(
                selectedSchedules(
                    scheduleIds,
                    schedules
                )
            )

    fun selectedTimezones(
        scheduleIds: Set<String>,
        schedules: List<Schedule>
    ): List<String> =
        selectedSchedules(
            scheduleIds,
            schedules
        )
            .map {
                it.timezone
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .sorted()

    fun hasMixedTimezones(
        scheduleIds: Set<String>,
        schedules: List<Schedule>
    ): Boolean =
        selectedTimezones(
            scheduleIds,
            schedules
        )
            .size >
            1

    fun actionTitle(
        action: String
    ): String =
        when (
            action.lowercase()
        ) {

            "allow" ->
                "Always Allow"

            "block" ->
                "Always Block"

            "schedule" ->
                "Use Schedules"

            else ->
                action
        }

    fun actionExplanation(
        action: String,
        type: String
    ): String =
        when (
            action.lowercase()
        ) {

            "allow" ->

                if (
                    type ==
                    "global"
                ) {
                    "Allows every non-infrastructure device and bypasses device, tag, and schedule rules."
                } else {
                    "Allows this target whenever this rule is the effective rule."
                }

            "block" ->

                if (
                    type ==
                    "global"
                ) {
                    "Immediately blocks every non-infrastructure device."
                } else {
                    "Blocks this target whenever this rule is effective."
                }

            "schedule" ->
                "Evaluates the attached schedule bundle to decide whether access is allowed or blocked."

            else ->
                ""
        }

    fun priorityExplanation(
        type: String
    ): String =
        when (
            type
        ) {

            "device" ->
                "Priority matters only when more than one enabled device rule targets the same device. Higher numbers win."

            "tag" ->
                "Tag priority does not choose a winner. LIAS evaluates matching tag rules together; any Block result fails closed."

            "global" ->
                "Global Access has fixed precedence above ordinary device and tag rules."

            else ->
                ""
        }

    private fun invalid(
        message: String
    ) =
        PolicySemanticResult(
            valid = false,
            error = message
        )
}
