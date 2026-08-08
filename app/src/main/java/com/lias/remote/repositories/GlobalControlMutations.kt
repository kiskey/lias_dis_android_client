// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/GlobalControlMutations.kt
// Version: 15.0.0
//
// Purpose:
//   Safe global_default / Vacation Mode transitions.
//
// Backend fact:
//   POST /vacation {enabled:true}
//       -> global_default.action = BLOCK
//
//   POST /vacation {enabled:false}
//       -> global_default.action = SCHEDULE
//
// It does NOT remember a previous ALLOW state.
//
// Therefore:
//   - Vacation Mode may be enabled only from SCHEDULE.
//   - It may be disabled from BLOCK.
//   - It may not silently transform ALLOW -> BLOCK -> SCHEDULE.
//
// Direct ALLOW/BLOCK/SCHEDULE editing belongs to Global Access.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Policy
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.VacationRequest
import com.lias.remote.core.network.VacationResponse

enum class GlobalAccessMode {
    ALLOW,
    BLOCK,
    SCHEDULE,
    UNKNOWN
}

fun Policy?.globalAccessMode():
    GlobalAccessMode =
    when (
        this?.action
            ?.trim()
            ?.lowercase()
    ) {
        "allow" ->
            GlobalAccessMode.ALLOW

        "block" ->
            GlobalAccessMode.BLOCK

        "schedule" ->
            GlobalAccessMode.SCHEDULE

        else ->
            GlobalAccessMode.UNKNOWN
    }

suspend fun EventRepository.toggleVacationMode(
    enabled: Boolean
): ApiResult<VacationResponse> =
    mutations.mutate(
        resourceKey =
            "policy:global_default"
    ) {

        val globalPolicy =
            _state.value
                .policies
                .find {
                    it.id ==
                        "global_default"
                }

        val currentMode =
            globalPolicy
                .globalAccessMode()

        if (
            enabled &&
            currentMode ==
                GlobalAccessMode.ALLOW
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Global Access is currently set to Allow. Switch it to Schedule before using Vacation Mode so the previous Allow override is not lost."
            )
        }

        if (
            enabled &&
            currentMode ==
                GlobalAccessMode.BLOCK
        ) {
            /*
             * Already globally blocked.
             *
             * The backend does not distinguish an intentional Global
             * Block from Vacation Mode, so do not issue a redundant
             * mutation and claim the distinction exists.
             */
            return@mutate ApiResult.Success(
                VacationResponse(
                    vacationMode =
                        true
                )
            )
        }

        if (
            !enabled &&
            currentMode ==
                GlobalAccessMode.SCHEDULE
        ) {
            return@mutate ApiResult.Success(
                VacationResponse(
                    vacationMode =
                        false
                )
            )
        }

        val result =
            api.post<
                VacationResponse,
                VacationRequest
            >(
                Endpoints.VACATION,
                VacationRequest(
                    enabled =
                        enabled
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            /*
             * The endpoint does not return the updated Policy object.
             * Fetch the canonical global policy list instead of
             * fabricating global_default locally.
             */
            refreshAll()

            emitUiEvent(
                UiEvent.ShowSnackbar(
                    if (
                        result.data
                            .vacationMode
                    ) {
                        "All non-infrastructure devices blocked"
                    } else {
                        "Global Schedule mode restored"
                    }
                )
            )
        }

        result
    }
