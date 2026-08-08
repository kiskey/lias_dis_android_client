// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/DeviceUserMutations.kt
// Version: 15.0.0
//
// Purpose:
//   Server-confirmed device/user mutations.
//
// Batch 15:
//   - infrastructure cannot be added/removed through ordinary tagging.
//   - unknown tags cannot be assigned.
//   - empty tag list normalizes to generic.
//   - authoritative device is fetched after mutation.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.DeviceTagRequest
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.RenameDeviceRequest
import com.lias.remote.core.network.UserDeviceRequest
import com.lias.remote.core.util.ConfigurationSafety

suspend fun EventRepository.assignDeviceTags(
    pdid: String,
    tagIds: List<String>
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val device =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?: return@mutate ApiResult.HttpError(
                    code =
                        404,
                    message =
                        "Device is no longer available."
                )

        val validation =
            ConfigurationSafety
                .validateNormalDeviceTagAssignment(
                    device =
                        device,
                    requestedTagIds =
                        tagIds
                )

        if (
            !validation.isValid
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    validation.error
                        ?: "Invalid tag assignment."
            )
        }

        val knownTagIds =
            _state.value
                .tags
                .map {
                    it.id
                }
                .toSet()

        val unknownTags =
            validation
                .normalizedTagIds
                .filterNot {
                    it in knownTagIds
                }

        if (
            unknownTags.isNotEmpty()
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "One or more selected tags no longer exist."
            )
        }

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
                        validation
                            .normalizedTagIds
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshSingleDevice(
                pdid
            )

            refreshSingleDeviceStatus(
                pdid
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

        val normalized =
            name.trim()

        if (
            normalized.isBlank()
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "Device name cannot be empty."
            )
        }

        val result =
            api.post<
                Unit,
                RenameDeviceRequest
            >(
                Endpoints.deviceRename(
                    pdid
                ),
                RenameDeviceRequest(
                    normalized
                )
            )

        if (
            result is
            ApiResult.Success
        ) {
            refreshSingleDevice(
                pdid
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

        if (
            userId.isNotBlank() &&
            _state.value.users
                .none {
                    it.id ==
                        userId
                }
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "The selected user no longer exists."
            )
        }

        val result =
            api.post<
                Unit,
                UserDeviceRequest
            >(
                Endpoints.deviceUser(
                    pdid
                ),
                UserDeviceRequest(
                    userId =
                        userId
                )
            )

        if (
            result is
            ApiResult.Success
        ) {
            refreshSingleDevice(
                pdid
            )
        }

        result
    }

suspend fun EventRepository.createUser(
    user: User
): ApiResult<User> =
    mutations.mutate(
        resourceKey =
            "users:create"
    ) {

        val result =
            api.post<
                User,
                User
            >(
                Endpoints.USERS,
                user
            )

        if (
            result is
            ApiResult.Success
        ) {
            upsertUser(
                result.data
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
