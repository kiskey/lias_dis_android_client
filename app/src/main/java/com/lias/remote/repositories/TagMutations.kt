// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/TagMutations.kt
// Version: 14.0.0
//
// Purpose:
//   Server-confirmed tag CRUD.
//
// Corrections:
//   - Create commits only the canonical server-created tag.
//   - Update no longer overwrites the UI before server acceptance.
//   - Failed deletes no longer remove/reappend objects.
//   - Same-tag operations serialize.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints

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

            /*
             * Tag manager owns ID and other canonical properties.
             */
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

            /*
             * Commit exactly what tagMgr.Update returned.
             */
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

        if (
            tagId ==
            "infrastructure"
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "Infrastructure is immutable."
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

            /*
             * Remove only after LIAS confirmed deletion.
             */
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

            /*
             * Tag membership/effective policy can change on multiple
             * devices, so reconcile after confirmed server mutation.
             *
             * Mutation revision protection prevents this refresh from
             * overwriting a newer concurrent mutation.
             */
            refreshAll()
        }

        result
    }
