// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/DeviceUserMutations.kt
// Version: 14.0.0
//
// Purpose:
//   Race-safe device metadata and user mutations.
//
// Rules:
//   - No speculative rename.
//   - No speculative user assignment.
//   - Server returns 204 for rename/assignment, so success is followed
//     by GET /devices/{pdid} to obtain authoritative current state.
//   - Multiple edits of one device are serialized.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.FlowLog
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

        val normalized =
            tagIds
                .filter {
                    it.isNotBlank()
                }
                .distinct()

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
                        normalized
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            /*
             * Do not locally copy(tags=normalized).
             *
             * LIAS/DIS correlation may normalize, preserve or augment
             * device metadata. Re-fetch the canonical object.
             */
            refreshSingleDevice(
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

            /*
             * Rename endpoint returns 204.
             *
             * Fetch the device after the server has persisted and
             * applied the override.
             */
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

            /*
             * Backend responds 204.
             *
             * Re-fetch rather than assuming the local Device copy is
             * now identical to LIAS.
             */
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
            "users"
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

            /*
             * LIAS may generate user_<id>.
             * Always commit the returned canonical user.
             */
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
