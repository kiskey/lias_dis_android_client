// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ConfigurationSafety.kt
// Version: 15.0.0
//
// Purpose:
//   Cross-resource dependency analysis for destructive LIAS actions.
//
// Why this exists:
//   LIAS intentionally keeps policies, schedules, tags and device-tag
//   mappings decoupled. Some backend delete operations therefore do not
//   perform cascading cleanup.
//
// Android must not treat those deletes as harmless.
//
// Safety rules:
//   - Referenced schedules cannot be casually deleted.
//   - Tags still assigned to devices cannot be deleted.
//   - Tags still targeted by policies cannot be deleted.
//   - Active tag extensions count as tag policies/dependencies.
//   - Built-in tags cannot be deleted.
//   - infrastructure is super-immutable.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag

data class ScheduleDependencyImpact(
    val schedule: Schedule,
    val referencingPolicies: List<Policy>
) {

    val hasDependencies: Boolean
        get() =
            referencingPolicies.isNotEmpty()

    val canDeleteSafely: Boolean
        get() =
            !hasDependencies

    val summary: String
        get() =
            when (
                referencingPolicies.size
            ) {
                0 ->
                    "This schedule is not referenced by any rule."

                1 ->
                    "1 rule still references this schedule."

                else ->
                    "${referencingPolicies.size} rules still reference this schedule."
            }
}

data class TagDependencyImpact(
    val tag: Tag,
    val assignedDevices: List<Device>,
    val targetingPolicies: List<Policy>
) {

    val isInfrastructure: Boolean
        get() =
            tag.id ==
                ConfigurationSafety.INFRASTRUCTURE_TAG_ID

    val isBuiltIn: Boolean
        get() =
            tag.builtin

    val hasDeviceDependencies: Boolean
        get() =
            assignedDevices.isNotEmpty()

    val hasPolicyDependencies: Boolean
        get() =
            targetingPolicies.isNotEmpty()

    val hasDependencies: Boolean
        get() =
            hasDeviceDependencies ||
                hasPolicyDependencies

    val canDeleteSafely: Boolean
        get() =
            !isBuiltIn &&
                !isInfrastructure &&
                !hasDependencies

    val summary: String
        get() =
            buildString {

                if (isInfrastructure) {
                    append(
                        "Infrastructure is immutable."
                    )

                    return@buildString
                }

                if (isBuiltIn) {
                    append(
                        "Built-in system tags cannot be deleted."
                    )

                    return@buildString
                }

                if (!hasDependencies) {
                    append(
                        "This tag has no remaining dependencies."
                    )

                    return@buildString
                }

                if (
                    assignedDevices.isNotEmpty()
                ) {
                    append(
                        "${assignedDevices.size} "
                    )

                    append(
                        if (
                            assignedDevices.size ==
                            1
                        ) {
                            "device is"
                        } else {
                            "devices are"
                        }
                    )

                    append(
                        " still assigned"
                    )
                }

                if (
                    targetingPolicies.isNotEmpty()
                ) {

                    if (
                        assignedDevices.isNotEmpty()
                    ) {
                        append(
                            " · "
                        )
                    }

                    append(
                        "${targetingPolicies.size} "
                    )

                    append(
                        if (
                            targetingPolicies.size ==
                            1
                        ) {
                            "rule targets"
                        } else {
                            "rules target"
                        }
                    )

                    append(
                        " this tag"
                    )
                }

                append(".")
            }
}

data class DeviceTagAssignmentValidation(
    val normalizedTagIds: List<String>,
    val error: String? = null
) {

    val isValid: Boolean
        get() =
            error == null
}

object ConfigurationSafety {

    const val INFRASTRUCTURE_TAG_ID =
        "infrastructure"

    const val GENERIC_TAG_ID =
        "generic"

    fun scheduleImpact(
        schedule: Schedule,
        policies: List<Policy>
    ): ScheduleDependencyImpact {

        val references =
            policies.filter { policy ->

                policy
                    .resolveScheduleIDs()
                    .contains(
                        schedule.id
                    )
            }

        return ScheduleDependencyImpact(
            schedule =
                schedule,
            referencingPolicies =
                references
                    .sortedBy {
                        it.name.lowercase()
                    }
        )
    }

    fun tagImpact(
        tag: Tag,
        devices: List<Device>,
        policies: List<Policy>
    ): TagDependencyImpact {

        val assignedDevices =
            devices.filter { device ->

                device.safeTags
                    .contains(
                        tag.id
                    )
            }

        val targetingPolicies =
            policies.filter { policy ->

                policy.type ==
                    "tag" &&
                    policy.targetID ==
                    tag.id
            }

        return TagDependencyImpact(
            tag =
                tag,
            assignedDevices =
                assignedDevices
                    .sortedBy {
                        it.displayName
                            .lowercase()
                    },
            targetingPolicies =
                targetingPolicies
                    .sortedByDescending {
                        it.priority
                    }
        )
    }

    /**
     * Normal tag-management UI must never add or remove the
     * infrastructure tag.
     *
     * Adding infrastructure grants complete immunity from LIAS access
     * enforcement; removing it removes that immunity.
     *
     * Those operations require a dedicated advanced workflow, not a
     * generic tag checkbox.
     */
    fun validateNormalDeviceTagAssignment(
        device: Device,
        requestedTagIds: List<String>
    ): DeviceTagAssignmentValidation {

        val current =
            device.safeTags
                .filter {
                    it.isNotBlank()
                }
                .distinct()

        val requested =
            requestedTagIds
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .toMutableList()

        val currentlyInfrastructure =
            INFRASTRUCTURE_TAG_ID in
                current

        val requestsInfrastructure =
            INFRASTRUCTURE_TAG_ID in
                requested

        if (
            currentlyInfrastructure &&
            !requestsInfrastructure
        ) {
            return DeviceTagAssignmentValidation(
                normalizedTagIds =
                    current,
                error =
                    "Infrastructure protection cannot be removed from normal tag management."
            )
        }

        if (
            !currentlyInfrastructure &&
            requestsInfrastructure
        ) {
            return DeviceTagAssignmentValidation(
                normalizedTagIds =
                    current,
                error =
                    "Infrastructure protection cannot be granted from normal tag management."
            )
        }

        if (
            requested.isEmpty()
        ) {
            requested.add(
                GENERIC_TAG_ID
            )
        }

        /*
         * generic represents an otherwise-unclassified device.
         * Do not keep it beside meaningful classification tags.
         */
        if (
            requested.size >
                1 &&
            requested.contains(
                GENERIC_TAG_ID
            )
        ) {
            requested.remove(
                GENERIC_TAG_ID
            )
        }

        return DeviceTagAssignmentValidation(
            normalizedTagIds =
                requested.distinct()
        )
    }

    fun dependencyPolicyNames(
        impact: ScheduleDependencyImpact
    ): String =
        impact
            .referencingPolicies
            .joinToString(
                separator =
                    ", "
            ) {
                it.name.ifBlank {
                    it.id
                }
            }

    fun dependencyDeviceNames(
        impact: TagDependencyImpact
    ): String =
        impact
            .assignedDevices
            .joinToString(
                separator =
                    ", "
            ) {
                it.displayName
            }

    fun dependencyTagPolicyNames(
        impact: TagDependencyImpact
    ): String =
        impact
            .targetingPolicies
            .joinToString(
                separator =
                    ", "
            ) {
                it.name.ifBlank {
                    it.id
                }
            }
}
