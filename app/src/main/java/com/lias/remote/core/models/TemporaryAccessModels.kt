// ====================================================================
// File: app/src/main/java/com/lias/remote/core/models/TemporaryAccessModels.kt
// Version: 10.0.0
//
// Purpose:
//   Canonical Android models and helpers for LIAS temporary access.
//
// Backend semantics:
//
//   Pause:
//     POST /devices/{pdid}/pause
//     - fixed 60-minute duration
//     - server creates temporary schedule + policy
//     - reason_tag = "pause"
//
//   Extend:
//     POST /devices/{pdid}/extend
//     - accepts 1..120 minutes
//     - server creates temporary ALLOW policy
//     - reason_tag = "extend_access"
//
// Important:
//   Android must not synthesize temporary Policy/Schedule objects.
// ====================================================================

package com.lias.remote.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PauseInternetResponse(
    val status: String = ""
)

@Serializable
data class ExtendAccessResponse(
    val status: String = "",
    @SerialName("expires_at")
    val expiresAt: String = "",
    val minutes: Int = 0
)

enum class TemporaryAccessKind {
    NONE,
    PAUSE,
    EXTEND
}

val ExtensionInfo.kind: TemporaryAccessKind
    get() =
        when (
            reasonTag
                .trim()
                .lowercase()
        ) {
            "pause" ->
                TemporaryAccessKind.PAUSE

            "extend_access" ->
                TemporaryAccessKind.EXTEND

            else ->
                TemporaryAccessKind.NONE
        }

val EffectiveStatus.temporaryAccessKind:
    TemporaryAccessKind
    get() =
        activeExtension
            ?.kind
            ?: TemporaryAccessKind.NONE

val EffectiveStatus.activePause:
    ExtensionInfo?
    get() =
        activeExtension
            ?.takeIf {
                it.kind ==
                    TemporaryAccessKind.PAUSE
            }

val EffectiveStatus.activeAccessExtension:
    ExtensionInfo?
    get() =
        activeExtension
            ?.takeIf {
                it.kind ==
                    TemporaryAccessKind.EXTEND
            }

val EffectiveStatus.isPausedByTemporaryOverride:
    Boolean
    get() =
        temporaryAccessKind ==
            TemporaryAccessKind.PAUSE

val EffectiveStatus.hasTemporaryExtension:
    Boolean
    get() =
        temporaryAccessKind ==
            TemporaryAccessKind.EXTEND
