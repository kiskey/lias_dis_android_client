// ====================================================================
// File:
// app/src/test/java/com/lias/remote/core/network/LiasApiClientContractTest.kt
// Version: 23.0.0
//
// Purpose:
//   Protect the Batch 22 network-result taxonomy.
//
// Regression targets:
//   - bearer header
//   - 401 / 403 classification
//   - 409 conflict preservation
//   - valid JSON
//   - malformed successful JSON
//   - HTTP server errors
//   - empty Unit responses
//   - empty typed responses
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.Tag
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiasApiClientContractTest {

    private lateinit var server:
        MockWebServer

    private lateinit var client:
        LiasApiClient

    @Before
    fun setUp() {

        server =
            MockWebServer()

        server.start()

        client =
            LiasApiClient(
                OkHttpClient()
            )
                .apply {

                    baseUrl =
                        server.url(
                            "/"
                        )
                            .toString()
                }
    }

    @After
    fun tearDown() {

        server.shutdown()
    }

    @Test
    fun `bearer token is sent when configured`() =
        runBlocking {

            client.authToken =
                "secret-test-token"

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "id":"kids",
                      "name":"Kids",
                      "color":"#FF9500",
                      "precedence":80,
                      "builtin":true
                    }
                    """.trimIndent()
                )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.Success
            )

            val request =
                server.takeRequest()

            assertEquals(
                "Bearer secret-test-token",
                request.getHeader(
                    "Authorization"
                )
            )
        }

    @Test
    fun `authorization header is absent without token`() =
        runBlocking {

            client.authToken =
                null

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "id":"kids",
                      "name":"Kids",
                      "color":"#FF9500",
                      "precedence":80,
                      "builtin":true
                    }
                    """.trimIndent()
                )
            )

            client.get<Tag>(
                "/api/v1/tags/kids"
            )

            val request =
                server.takeRequest()

            assertNull(
                request.getHeader(
                    "Authorization"
                )
            )
        }

    @Test
    fun `401 becomes AuthenticationError`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        401
                    )
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "message":"invalid token"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.AuthenticationError
            )

            val error =
                result as
                    ApiResult.AuthenticationError

            assertEquals(
                401,
                error.code
            )

            assertTrue(
                error.message
                    .contains(
                        "invalid token",
                        ignoreCase =
                            true
                    )
            )
        }

    @Test
    fun `403 becomes AuthenticationError`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        403
                    )
                    .setBody(
                        """
                        {
                          "message":"forbidden"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.AuthenticationError
            )

            assertEquals(
                403,
                (
                    result as
                        ApiResult.AuthenticationError
                    ).code
            )
        }

    @Test
    fun `409 preserves conflicts and server message`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        409
                    )
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "message":"schedule contradiction",
                          "conflicts":[
                            {
                              "schedule_a_id":"a",
                              "schedule_a_name":"Bedtime",
                              "schedule_b_id":"b",
                              "schedule_b_name":"Gaming",
                              "day":"monday",
                              "overlap_start":"22:00",
                              "overlap_end":"23:00",
                              "action_a":"block",
                              "action_b":"allow"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/test"
                )

            assertTrue(
                result is
                    ApiResult.ConflictError
            )

            val conflict =
                result as
                    ApiResult.ConflictError

            assertEquals(
                1,
                conflict.conflicts.size
            )

            assertEquals(
                "Bedtime",
                conflict.conflicts
                    .single()
                    .scheduleAName
            )

            assertTrue(
                conflict.message
                    .contains(
                        "contradiction"
                    )
            )
        }

    @Test
    fun `malformed successful payload becomes SerializationError`() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "id": 123,
                      "totally_wrong": true
                    }
                    """.trimIndent()
                )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.SerializationError
            )
        }

    @Test
    fun `valid typed response decodes normally`() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "id":"kids",
                      "name":"Kids Devices",
                      "color":"#FF9500",
                      "precedence":80,
                      "builtin":true
                    }
                    """.trimIndent()
                )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.Success
            )

            val tag =
                (
                    result as
                        ApiResult.Success
                    ).data

            assertEquals(
                "kids",
                tag.id
            )

            assertEquals(
                80,
                tag.precedence
            )
        }

    @Test
    fun `500 remains HttpError and is retryable`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        500
                    )
                    .setBody(
                        "database unavailable"
                    )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.HttpError
            )

            assertEquals(
                500,
                (
                    result as
                        ApiResult.HttpError
                    ).code
            )

            assertTrue(
                result
                    .isRetryableTransportFailure()
            )
        }

    @Test
    fun `400 remains non retryable HttpError`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        400
                    )
                    .setBody(
                        "invalid request"
                    )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.HttpError
            )

            assertFalse(
                result
                    .isRetryableTransportFailure()
            )
        }

    @Test
    fun `empty 204 is valid for Unit`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        204
                    )
            )

            val result =
                client.delete<Unit>(
                    "/api/v1/policies/pol_x"
                )

            assertTrue(
                result is
                    ApiResult.Success
            )
        }

    @Test
    fun `empty successful typed response is serialization error`() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        200
                    )
            )

            val result =
                client.get<Tag>(
                    "/api/v1/tags/kids"
                )

            assertTrue(
                result is
                    ApiResult.SerializationError
            )
        }

    private fun jsonResponse(
        body: String
    ): MockResponse =
        MockResponse()
            .setResponseCode(
                200
            )
            .setHeader(
                "Content-Type",
                "application/json"
            )
            .setBody(
                body
            )
}
