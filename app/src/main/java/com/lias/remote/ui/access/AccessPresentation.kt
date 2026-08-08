// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/access/AccessPresentation.kt
// Version: 26.0.0
//
// Purpose:
//   Single UI interpretation layer for LIAS EffectiveStatus.
//
// Critical rule:
//   Android NEVER derives enforcement from:
//     - raw policy IDs
//     - policy priority
//     - schedule evaluation
//     - connectivity state
//
// EffectiveStatus from LIAS is authoritative.
// ====================================================================

package com.lias.remote.ui.access

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.ui.components.PillTone

enum class AccessKind {
    INFRASTRUCTURE,
    PAUSED,
    EXTENDED,
    BLOCKED,
    ALLOWED,
    UNKNOWN
}

data class AccessPresentation(
    val kind: AccessKind,
    val label: String,
    val tone: PillTone,
    val canExtend: Boolean,
    val canPause: Boolean,
    val canResumePause: Boolean,
    val canManageExtension: Boolean,
    val activeExtension: ExtensionInfo?,
    val source: String?
) {

    val isPaused: Boolean
        get() =
            kind ==
                AccessKind.PAUSED

    val isBlocked: Boolean
        get() =
            kind ==
                AccessKind.BLOCKED ||
                kind ==
                AccessKind.PAUSED

    val isInfrastructure: Boolean
        get() =
            kind ==
                AccessKind.INFRASTRUCTURE

    val isKnown: Boolean
        get() =
            kind !=
                AccessKind.UNKNOWN
}

object AccessPresentationResolver {

    fun resolve(
        device: Device,
        status: EffectiveStatus?
    ): AccessPresentation {

        val deviceIsInfrastructure =
            device.safeTags.any {
                it.equals(
                    "infrastructure",
                    ignoreCase =
                        true
                )
            }

        /*
         * Infrastructure is protected even while EffectiveStatus is
         * temporarily unavailable.
         */
        if (
            deviceIsInfrastructure ||
            status
                ?.source
                ?.equals(
                    "infrastructure",
                    ignoreCase =
                        true
                ) ==
            true
        ) {

            return AccessPresentation(
                kind =
                    AccessKind.INFRASTRUCTURE,
                label =
                    "Protected",
                tone =
                    PillTone.INFO,
                canExtend =
                    false,
                canPause =
                    false,
                canResumePause =
                    false,
                canManageExtension =
                    false,
                activeExtension =
                    null,
                source =
                    status?.source
            )
        }

        if (
            status ==
            null
        ) {

            return AccessPresentation(
                kind =
                    AccessKind.UNKNOWN,
                label =
                    "Status Unavailable",
                tone =
                    PillTone.INFO,
                canExtend =
                    false,
                canPause =
                    false,
                canResumePause =
                    false,
                canManageExtension =
                    false,
                activeExtension =
                    null,
                source =
                    null
            )
        }

        val active =
            status.activeExtension

        val pauseActive =
            active
                ?.reasonTag
                ?.equals(
                    "pause",
                    ignoreCase =
                        true
                ) ==
                true

        val extensionActive =
            active
                ?.reasonTag
                ?.equals(
                    "extend_access",
                    ignoreCase =
                        true
                ) ==
                true

        if (
            pauseActive
        ) {

            return AccessPresentation(
                kind =
                    AccessKind.PAUSED,
                label =
                    pauseLabel(
                        active
                    ),
                tone =
                    PillTone.PAUSED,
                canExtend =
                    status.extendAvailable,
                canPause =
                    false,
                canResumePause =
                    true,
                canManageExtension =
                    false,
                activeExtension =
                    active,
                source =
                    status.source
            )
        }

        if (
            extensionActive
        ) {

            return AccessPresentation(
                kind =
                    AccessKind.EXTENDED,
                label =
                    extensionLabel(
                        active
                    ),
                tone =
                    PillTone.ALLOWED,
                canExtend =
                    true,
                canPause =
                    status.pauseAvailable,
                canResumePause =
                    false,
                canManageExtension =
                    true,
                activeExtension =
                    active,
                source =
                    status.source
            )
        }

        return if (
            status.action.equals(
                "block",
                ignoreCase =
                    true
            )
        ) {

            AccessPresentation(
                kind =
                    AccessKind.BLOCKED,
                label =
                    "Blocked",
                tone =
                    PillTone.BLOCKED,
                canExtend =
                    status.extendAvailable,
                canPause =
                    false,
                canResumePause =
                    false,
                canManageExtension =
                    false,
                activeExtension =
                    null,
                source =
                    status.source
            )

        } else {

            AccessPresentation(
                kind =
                    AccessKind.ALLOWED,
                label =
                    "Allowed",
                tone =
                    PillTone.ALLOWED,
                canExtend =
                    false,
                canPause =
                    status.pauseAvailable,
                canResumePause =
                    false,
                canManageExtension =
                    false,
                activeExtension =
                    null,
                source =
                    status.source
            )
        }
    }

    private fun pauseLabel(
        extension: ExtensionInfo
    ): String {

        val minutes =
            extension.minutesLeft

        return if (
            minutes >
            0
        ) {
            "Paused · ${minutes}m"
        } else {
            "Paused"
        }
    }

    private fun extensionLabel(
        extension: ExtensionInfo
    ): String {

        val minutes =
            extension.minutesLeft

        return if (
            minutes >
            0
        ) {
            "Extended · ${minutes}m"
        } else {
            "Extended"
        }
    }
}
