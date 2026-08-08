// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/EffectiveAccessPresentation.kt
// Version: 10.0.0
//
// Purpose:
//   Convert authoritative EffectiveStatus into UI semantics.
//
// Critical rule:
//   Raw Policy presence is not authoritative for current enforcement.
//
//   Use EffectiveStatus:
//     action
//     source
//     extend_available
//     pause_available
//     active_extension
//     active_extension.reason_tag
//
// This also prevents:
//
//   policy exists == currently active
//
// which is not generally safe for scheduled or expiring policies.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.temporaryAccessKind

data class EffectiveAccessPresentation(
    val title: String,
    val detail: String?,
    val isAllowed: Boolean,
    val isBlocked: Boolean,
    val canPause: Boolean,
    val canExtend: Boolean,
    val canResume: Boolean,
    val canCancelExtension: Boolean,
    val activeTemporaryAccess: ExtensionInfo?
)

object EffectiveAccessFormatter {

    fun present(
        status: EffectiveStatus?
    ): EffectiveAccessPresentation {

        if (
            status == null
        ) {
            return EffectiveAccessPresentation(
                title =
                    "Checking",
                detail =
                    "Waiting for authoritative status",
                isAllowed =
                    false,
                isBlocked =
                    false,
                canPause =
                    false,
                canExtend =
                    false,
                canResume =
                    false,
                canCancelExtension =
                    false,
                activeTemporaryAccess =
                    null
            )
        }

        val temporaryKind =
            status.temporaryAccessKind

        val temporary =
            status.activeExtension

        val minutesLeft =
            temporary
                ?.minutesLeft
                ?.coerceAtLeast(
                    0
                )

        return when (
            temporaryKind
        ) {

            TemporaryAccessKind.PAUSE -> {

                EffectiveAccessPresentation(
                    title =
                        "Paused",
                    detail =
                        minutesLeft?.let {
                            "$it min remaining"
                        },
                    isAllowed =
                        false,
                    isBlocked =
                        true,

                    /*
                     * While paused, "Resume" is the meaningful inverse
                     * operation, not another pause.
                     */
                    canPause =
                        false,

                    /*
                     * Backend exposes ExtendAvailable during active
                     * pause, so extending access can intentionally
                     * override the pause.
                     */
                    canExtend =
                        status.extendAvailable,

                    canResume =
                        true,

                    canCancelExtension =
                        false,

                    activeTemporaryAccess =
                        temporary
                )
            }

            TemporaryAccessKind.EXTEND -> {

                EffectiveAccessPresentation(
                    title =
                        "Extended Access",
                    detail =
                        minutesLeft?.let {
                            "$it min remaining"
                        },
                    isAllowed =
                        true,
                    isBlocked =
                        false,
                    canPause =
                        status.pauseAvailable,
                    canExtend =
                        true,
                    canResume =
                        false,
                    canCancelExtension =
                        true,
                    activeTemporaryAccess =
                        temporary
                )
            }

            TemporaryAccessKind.NONE -> {

                val isBlocked =
                    status.action
                        .equals(
                            "block",
                            ignoreCase = true
                        )

                val isAllowed =
                    status.action
                        .equals(
                            "allow",
                            ignoreCase = true
                        )

                EffectiveAccessPresentation(
                    title =
                        when {
                            isBlocked ->
                                "Blocked"

                            isAllowed ->
                                "Allowed"

                            else ->
                                "Unknown"
                        },

                    detail =
                        sourceDescription(
                            status.source
                        ),

                    isAllowed =
                        isAllowed,

                    isBlocked =
                        isBlocked,

                    canPause =
                        status.pauseAvailable,

                    canExtend =
                        status.extendAvailable,

                    canResume =
                        false,

                    canCancelExtension =
                        false,

                    activeTemporaryAccess =
                        null
                )
            }
        }
    }

    fun sourceDescription(
        source: String
    ): String? =
        when (
            source
                .trim()
                .lowercase()
        ) {
            "infrastructure" ->
                "Infrastructure immunity"

            "global" ->
                "Global access policy"

            "device_policy" ->
                "Device rule"

            "tag_policy" ->
                "Tag rule"

            "schedule" ->
                "Schedule"

            "fallback" ->
                "Default access"

            else ->
                null
        }
}
