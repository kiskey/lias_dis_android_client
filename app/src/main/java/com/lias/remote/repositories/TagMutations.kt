// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/TagMutations.kt
// Version: 15.0.0
//
// Purpose:
//   Dependency-aware tag persistence.
//
// Important:
//   Backend DeleteTag removes only the tag itself.
//   It does not transactionally clean:
//     - device tag mappings
//     - policies targeting the tag
//     - temporary tag extensions
//
// Therefore Android refuses destructive deletion while dependencies
// remain rather than attempting an unreliable client-side cascade.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.util.ConfigurationSafety

suspend fun EventRepository.createTag(
    tag: Tag
): ApiResult<Tag> =
    mutations.mutate(
        resourceKey =
            "tags:create"
    ) {

        val result =
            api.post<
                Tag,
                Tag
            >(
                Endpoints.TAGS,
                tag
            )

        if (
            result is
            ApiResult.Success
        ) {
            upsertTag(
                result.data
            )
        }

        result
    }

suspend fun EventRepository.updateTag(
    tag: Tag
): ApiResult<Tag> =
    mutations.mutate(
        resourceKey =
            "tag:${tag.id}"
    ) {

        if (
            tag.id.isBlank()
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "Cannot update a tag without an ID."
            )
        }

        val existing =
            _state.value
                .tags
                .find {
                    it.id ==
                        tag.id
                }
                ?: return@mutate ApiResult.HttpError(
                    code =
                        404,
                    message =
                        "The tag no longer exists."
                )

        /*
         * Built-in tags are system classifications. The backend tag
         * manager determines exactly which modifications it accepts.
         *
         * infrastructure receives stronger client-side protection.
         */
        if (
            existing.id ==
            ConfigurationSafety
                .INFRASTRUCTURE_TAG_ID
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Infrastructure is a protected system tag."
            )
        }

        val result =
            api.put<
                Tag,
                Tag
            >(
                Endpoints.tag(
                    tag.id
                ),
                tag
            )

        if (
            result is
            ApiResult.Success
        ) {
            upsertTag(
                result.data
            )
        }

        result
    }

suspend fun EventRepository.deleteTag(
    tagId: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "tag:$tagId"
    ) {

        val tag =
            _state.value
                .tags
                .find {
                    it.id ==
                        tagId
                }
                ?: return@mutate ApiResult.HttpError(
                    code =
                        404,
                    message =
                        "The tag no longer exists."
                )

        val impact =
            ConfigurationSafety
                .tagImpact(
                    tag =
                        tag,
                    devices =
                        _state.value.devices,
                    policies =
                        _state.value.policies
                )

        if (
            impact.isInfrastructure
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Infrastructure is immutable and cannot be deleted."
            )
        }

        if (
            impact.isBuiltIn
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Built-in system tags cannot be deleted."
            )
        }

        if (
            impact.assignedDevices
                .isNotEmpty()
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    buildString {

                        append(
                            "Move "
                        )

                        append(
                            impact
                                .assignedDevices
                                .size
                        )

                        append(
                            if (
                                impact
                                    .assignedDevices
                                    .size ==
                                1
                            ) {
                                " device"
                            } else {
                                " devices"
                            }
                        )

                        append(
                            " out of “"
                        )

                        append(
                            tag.name
                        )

                        append(
                            "” before deleting it."
                        )
                    }
            )
        }

        if (
            impact.targetingPolicies
                .isNotEmpty()
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    buildString {

                        append(
                            "Delete or retarget "
                        )

                        append(
                            impact
                                .targetingPolicies
                                .size
                        )

                        append(
                            if (
                                impact
                                    .targetingPolicies
                                    .size ==
                                1
                            ) {
                                " rule"
                            } else {
                                " rules"
                            }
                        )

                        append(
                            " that still use “"
                        )

                        append(
                            tag.name
                        )

                        append(
                            "” first."
                        )
                    }
            )
        }

        val result =
            api.delete<Unit>(
                Endpoints.tag(
                    tagId
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    tags =
                        _state.value.tags
                            .filterNot {
                                it.id ==
                                    tagId
                            },
                    tagEffectiveStatuses =
                        _state.value
                            .tagEffectiveStatuses -
                            tagId
                )

            refreshAll()
        }

        result
    }
