package com.lias.remote.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyMutationRequestContractTest {

    private val json =
        Json {
            encodeDefaults =
                true
        }

    @Test
    fun `authored policy request excludes server owned fields`() {

        val request =
            PolicyMutationRequest(
                name =
                    "Kids Bedtime",
                type =
                    "tag",
                targetId =
                    "kids",
                action =
                    "schedule",
                scheduleIds =
                    listOf(
                        "sched_bedtime"
                    ),
                priority =
                    50,
                enabled =
                    true
            )

        val objectValue =
            json
                .parseToJsonElement(
                    json.encodeToString(
                        request
                    )
                )
                .jsonObject

        assertEquals(
            "\"Kids Bedtime\"",
            objectValue["name"]
                .toString()
        )

        assertEquals(
            "\"kids\"",
            objectValue["target_id"]
                .toString()
        )

        assertTrue(
            "schedule_ids" in
                objectValue
        )

        assertFalse(
            "id" in
                objectValue
        )

        assertFalse(
            "created_at" in
                objectValue
        )

        assertFalse(
            "updated_at" in
                objectValue
        )

        assertFalse(
            "expires_at" in
                objectValue
        )

        assertFalse(
            "reason_tag" in
                objectValue
        )

        assertFalse(
            "schedule_id" in
                objectValue
        )
    }

    @Test
    fun `always block request keeps writable contract only`() {

        val request =
            PolicyMutationRequest(
                name =
                    "Block Tablet",
                type =
                    "device",
                targetId =
                    "pdid_tablet",
                action =
                    "block",
                scheduleIds =
                    emptyList(),
                priority =
                    90,
                enabled =
                    true
            )

        val objectValue =
            json
                .parseToJsonElement(
                    json.encodeToString(
                        request
                    )
                )
                .jsonObject

        assertEquals(
            "\"block\"",
            objectValue["action"]
                .toString()
        )

        assertEquals(
            "[]",
            objectValue["schedule_ids"]
                .toString()
        )

        assertFalse(
            "created_at" in
                objectValue
        )
    }
}
