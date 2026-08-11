package com.lias.remote.ui.screens.home

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Tag
import com.lias.remote.repositories.UiState
import com.lias.remote.ui.access.AccessKind
import com.lias.remote.ui.access.AccessPresentationResolver

internal data class HomeTagProtection(
    val tag: Tag,
    val status: EffectiveStatus
)

/**
 * Home is a projection of LIAS effective-status responses. It must never
 * evaluate schedules or reconstruct policy precedence on the client.
 */
internal fun UiState.homeRestrictedDevices(): List<Device> =
    devices
        .filter { device ->
            val status = effectiveStatusForDevice(device.pdid)

            if (status?.source.equals("global", ignoreCase = true)) {
                return@filter false
            }

            when (
                AccessPresentationResolver.resolve(device, status).kind
            ) {
                AccessKind.PAUSED,
                AccessKind.BLOCKED -> true

                else -> false
            }
        }
        .sortedWith(
            compareBy<Device> { device ->
                when (
                    AccessPresentationResolver.resolve(
                        device,
                        effectiveStatusForDevice(device.pdid)
                    ).kind
                ) {
                    AccessKind.PAUSED -> 0
                    AccessKind.BLOCKED -> 1
                    else -> 2
                }
            }.thenBy { it.displayName.lowercase() }
        )

/**
 * Tag rows are included only when LIAS says a real policy/schedule branch is
 * currently authoritative. Fallback, infrastructure and global results are
 * either non-enforcements or represented once elsewhere on Home.
 */
internal fun UiState.homeActiveTagProtections(): List<HomeTagProtection> =
    policies
        .asSequence()
        .filter { policy ->
            policy.enabled &&
                policy.type.equals("tag", ignoreCase = true) &&
                policy.action.lowercase() in setOf("block", "schedule") &&
                !policy.targetID.equals("infrastructure", ignoreCase = true)
        }
        .map { it.targetID }
        .filter { it.isNotBlank() }
        .toSet()
        .let { policyTagIds ->
            tags
                .mapNotNull { tag ->
                    if (tag.id !in policyTagIds) {
                        return@mapNotNull null
                    }

                    val status =
                        effectiveStatusForTag(tag.id)
                            ?: return@mapNotNull null
                    val source = status.source.trim().lowercase()

                    if (
                        source in setOf(
                            "global",
                            "fallback",
                            "infrastructure"
                        ) ||
                        !status.action.equals("block", ignoreCase = true)
                    ) {
                        null
                    } else {
                        HomeTagProtection(tag, status)
                    }
                }
                .sortedBy { it.tag.name.lowercase() }
        }

internal fun UiState.homeActivePauseDevices(): List<Device> =
    homeRestrictedDevices()
        .filter { device ->
            effectiveStatusForDevice(device.pdid)
                ?.activeExtension
                ?.reasonTag
                .equals("pause", ignoreCase = true)
        }

internal fun UiState.homeHasGlobalProtection(
    globalPolicy: Policy
): Boolean =
    isInitialLoaded &&
        policies.any { it.id == "global_default" } &&
        globalPolicy.enabled &&
        globalPolicy.action.lowercase() in setOf("allow", "block")
