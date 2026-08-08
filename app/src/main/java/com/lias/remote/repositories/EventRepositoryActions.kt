// ====================================================================
// File:
// app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt
// Version: 27.1.0
//
// Purpose:
//   Repository operations not owned by specialized mutation modules.
//
// Canonical ownership:
//
// PolicyScheduleMutations.kt
//   - validatePolicy
//   - savePolicy
//   - deletePolicy
//   - saveSchedule
//   - deleteSchedule
//
// TagMutations.kt
//   - createTag
//   - updateTag
//   - deleteTag
//
// GlobalControlMutations.kt
//   - toggleVacationMode
//
// TemporaryAccessRepository.kt
//   - Pause
//   - Resume
//   - device Extend
//   - tag Extend
//
// This file:
//   - device tag assignment
//   - rename
//   - logs
//   - nftables maintenance
//   - policy import/export
//   - statistics
//   - users
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.NetworkStats
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.DeviceTagRequest
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.RenameDeviceRequest
import com.lias.remote.core.network.UserDeviceRequest

suspend fun EventRepository.assignDeviceTags(
    pdid: String,
    tagIds: List<String>
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val normalizedTags =
            tagIds
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .toMutableList()
                .apply {

                    /*
                     * Generic is the fallback classification.
                     *
                     * Once a meaningful tag exists, Generic must not
                     * remain alongside it.
                     */
                    if (
                        size >
                        1
                    ) {

                        remove(
                            "generic"
                        )
                    }

                    if (
                        isEmpty()
                    ) {

                        add(
                            "generic"
                        )
                    }
                }

        val previousTags =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.safeTags
                ?: listOf(
                    "generic"
                )

        /*
         * Safe optimistic presentation update.
         *
         * Canonical reconciliation still follows on success.
         */
        _state.value =
            _state.value.copy(
                devices =
                    _state.value
                        .devices
                        .map {
                            device ->

                            if (
                                device.pdid ==
                                pdid
                            ) {

                                device.copy(
                                    tags =
                                        normalizedTags
                                )

                            } else {

                                device
                            }
                        }
            )

        val result =
            api.post<
                Unit,
                DeviceTagRequest
            >(
                Endpoints.deviceTags(
                    pdid
                ),
                DeviceTagRequest(
                    tagIds =
                        normalizedTags
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()

        } else {

            _state.value =
                _state.value.copy(
                    devices =
                        _state.value
                            .devices
                            .map {
                                device ->

                                if (
                                    device.pdid ==
                                    pdid
                                ) {

                                    device.copy(
                                        tags =
                                            previousTags
                                    )

                                } else {

                                    device
                                }
                            }
                )
        }

        result
    }

suspend fun EventRepository.assignDeviceTag(
    pdid: String,
    tagId: String
): ApiResult<Unit> =
    assignDeviceTags(
        pdid =
            pdid,
        tagIds =
            listOf(
                tagId
            )
    )

suspend fun EventRepository.renameDevice(
    pdid: String,
    name: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val normalizedName =
            name.trim()

        if (
            normalizedName.isBlank()
        ) {

            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "Device name cannot be empty."
            )
        }

        val previousName =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.friendlyName
                .orEmpty()

        _state.value =
            _state.value.copy(
                devices =
                    _state.value
                        .devices
                        .map {
                            device ->

                            if (
                                device.pdid ==
                                pdid
                            ) {

                                device.copy(
                                    friendlyName =
                                        normalizedName
                                )

                            } else {

                                device
                            }
                        }
            )

        val result =
            api.post<
                Unit,
                RenameDeviceRequest
            >(
                Endpoints.deviceRename(
                    pdid
                ),
                RenameDeviceRequest(
                    normalizedName
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()

        } else {

            _state.value =
                _state.value.copy(
                    devices =
                        _state.value
                            .devices
                            .map {
                                device ->

                                if (
                                    device.pdid ==
                                    pdid
                                ) {

                                    device.copy(
                                        friendlyName =
                                            previousName
                                    )

                                } else {

                                    device
                                }
                            }
                )
        }

        result
    }

suspend fun EventRepository.getDeviceLogs(
    pdid: String
): ApiResult<List<FlowLog>> =
    api.get(
        Endpoints.deviceLogs(
            pdid
        )
    )

suspend fun EventRepository.flushNftables():
    ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "maintenance:nftables"
    ) {

        val result =
            api.post<
                Unit,
                Unit
            >(
                Endpoints.NFTABLES_FLUSH,
                Unit
            )

        if (
            result is
            ApiResult.Success
        ) {

            emitUiEvent(
                UiEvent.ShowSnackbar(
                    "LIAS firewall rules reapplied"
                )
            )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.exportPolicies():
    ApiResult<String> =
    api.getRaw(
        Endpoints.POLICIES_EXPORT
    )

suspend fun EventRepository.importPolicies(
    jsonPayload: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "policies:import"
    ) {

        val result =
            api.postRawJson(
                Endpoints.POLICIES_IMPORT,
                jsonPayload
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

suspend fun EventRepository.getNetworkStats():
    ApiResult<NetworkStats> {

    val result =
        api.get<NetworkStats>(
            Endpoints.STATS
        )

    if (
        result is
        ApiResult.Success
    ) {

        _state.value =
            _state.value.copy(
                stats =
                    result.data
            )
    }

    return result
}

suspend fun EventRepository.createUser(
    user: User
): ApiResult<User> =
    mutations.mutate(
        resourceKey =
            "users:create"
    ) {

        val normalizedName =
            user.name.trim()

        if (
            normalizedName.isBlank()
        ) {

            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "User name cannot be empty."
            )
        }

        /*
         * LIAS owns the canonical ID.
         */
        val result =
            api.post<
                User,
                User
            >(
                Endpoints.USERS,
                user.copy(
                    id =
                        "",
                    name =
                        normalizedName
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    users =
                        (
                            _state.value
                                .users
                                .filterNot {
                                    it.id ==
                                        result.data.id
                                } +
                                result.data
                            )
                            .sortedBy {
                                it.name.lowercase()
                            }
                )
        }

        result
    }

suspend fun EventRepository.assignDeviceUser(
    pdid: String,
    userId: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val previousUserId =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.userID

        val normalizedUserId =
            userId.trim()

        _state.value =
            _state.value.copy(
                devices =
                    _state.value
                        .devices
                        .map {
                            device ->

                            if (
                                device.pdid ==
                                pdid
                            ) {

                                device.copy(
                                    userID =
                                        normalizedUserId
                                            .ifBlank {
                                                null
                                            }
                                )

                            } else {

                                device
                            }
                        }
            )

        val result =
            api.post<
                Unit,
                UserDeviceRequest
            >(
                Endpoints.deviceUser(
                    pdid
                ),
                UserDeviceRequest(
                    normalizedUserId
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()

        } else {

            _state.value =
                _state.value.copy(
                    devices =
                        _state.value
                            .devices
                            .map {
                                device ->

                                if (
                                    device.pdid ==
                                    pdid
                                ) {

                                    device.copy(
                                        userID =
                                            previousUserId
                                    )

                                } else {

                                    device
                                }
                            }
                )
        }

        result
    }
