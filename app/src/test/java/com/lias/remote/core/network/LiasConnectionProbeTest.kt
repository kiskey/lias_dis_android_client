// ====================================================================
// File:
// app/src/test/java/com/lias/remote/core/network/LiasConnectionProbeTest.kt
// Version: 23.0.0
//
// Purpose:
//   Verify isolated connection probing introduced in Batch 22.
//
// Critical regression:
//   Testing a candidate LIAS server must never modify the live
//   EventRepository REST client's server URL or bearer token.
// ====================================================================

package com.lias.remote.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiasConnectionProbeTest {

    private lateinit var workingServer:
        MockWebServer

    private lateinit var candidateServer:
        MockWebServer

    private lateinit var httpClient:
        OkHttpClient

    @Before
    fun setUp() {

        workingServer =
            MockWebServer()

        candidateServer =
            MockWebServer()

        workingServer.start()
        candidateServer.start()

        httpClient =
            OkHttpClient()
    }

    @After
    fun tearDown() {

        workingServer.shutdown()
        candidateServer.shutdown()
    }

    @Test
    fun `successful candidate probe does not mutate live api client`() =
        runBlocking {

            val liveClient =
                LiasApiClient(
                    httpClient
                )
                    .apply {

                        baseUrl =
                            workingServer
                                .url(
                                    "/"
                                )
                                .toString()

                        authToken =
                            "working-token"
                    }

            candidateServer.enqueue(
                healthResponse(
                    version =
                        "3.0.0"
                )
            )

            val probe =
                LiasConnectionProbe(
                    httpClient
                )

            val candidateUrl =
                candidateServer
                    .url(
                        "/"
                    )
                    .toString()

            val result =
                probe.probe(
                    rawUrl =
                        candidateUrl,
                    authToken =
                        "candidate-token"
                )

            assertTrue(
                result is
                    ApiResult.Success
            )

            /*
             * Candidate test must leave the live repository client
             * completely untouched.
             */
            assertEquals(
                workingServer
                    .url(
                        "/"
                    )
                    .toString(),
                liveClient.baseUrl
            )

            assertEquals(
                "working-token",
                liveClient.authToken
            )

            val probeRequest =
                candidateServer
                    .takeRequest()

            assertEquals(
                "Bearer candidate-token",
                probeRequest
                    .getHeader(
                        "Authorization"
                    )
            )
        }

    @Test
    fun `failed candidate probe still does not mutate live client`() =
        runBlocking {

            val liveClient =
                LiasApiClient(
                    httpClient
                )
                    .apply {

                        baseUrl =
                            workingServer
                                .url(
                                    "/"
                                )
                                .toString()

                        authToken =
                            "good-token"
                    }

            candidateServer.enqueue(
                MockResponse()
                    .setResponseCode(
                        401
                    )
                    .setBody(
                        """
                        {
                          "message":"wrong token"
                        }
                        """.trimIndent()
                    )
            )

            val probe =
                LiasConnectionProbe(
                    httpClient
                )

            val result =
                probe.probe(
                    rawUrl =
                        candidateServer
                            .url(
                                "/"
                            )
                            .toString(),
                    authToken =
                        "wrong-token"
                )

            assertTrue(
                result is
                    ApiResult.AuthenticationError
            )

            assertEquals(
                workingServer
                    .url(
                        "/"
                    )
                    .toString(),
                liveClient.baseUrl
            )

            assertEquals(
                "good-token",
                liveClient.authToken
            )
        }

    @Test
    fun `probe normalizes URL without scheme`() =
        runBlocking {

            candidateServer.enqueue(
                healthResponse(
                    version =
                        "3.0.0"
                )
            )

            val hostPort =
                "${candidateServer.hostName}:${candidateServer.port}"

            val result =
                LiasConnectionProbe(
                    httpClient
                )
                    .probe(
                        rawUrl =
                            hostPort,
                        authToken =
                            null
                    )

            assertTrue(
                result is
                    ApiResult.Success
            )

            val success =
                (
                    result as
                        ApiResult.Success
                    ).data

            assertTrue(
                success.normalizedUrl
                    .startsWith(
                        "http://"
                    )
            )
        }

    @Test
    fun `invalid URL fails before network request`() =
        runBlocking {

            val result =
                LiasConnectionProbe(
                    httpClient
                )
                    .probe(
                        rawUrl =
                            "http://",
                        authToken =
                            null
                    )

            assertTrue(
                result is
                    ApiResult.HttpError
            )

            assertEquals(
                0,
                candidateServer
                    .requestCount
            )
        }

    @Test
    fun `probe without token sends no authorization header`() =
        runBlocking {

            candidateServer.enqueue(
                healthResponse(
                    version =
                        "3.0.0"
                )
            )

            LiasConnectionProbe(
                httpClient
            )
                .probe(
                    rawUrl =
                        candidateServer
                            .url(
                                "/"
                            )
                            .toString(),
                    authToken =
                        null
                )

            val request =
                candidateServer
                    .takeRequest()

            assertNull(
                request.getHeader(
                    "Authorization"
                )
            )
        }

    @Test
    fun `malformed health payload surfaces serialization error`() =
        runBlocking {

            candidateServer.enqueue(
                MockResponse()
                    .setResponseCode(
                        200
                    )
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "this_is_not_health": true
                        }
                        """.trimIndent()
                    )
            )

            val result =
                LiasConnectionProbe(
                    httpClient
                )
                    .probe(
                        rawUrl =
                            candidateServer
                                .url(
                                    "/"
                                )
                                .toString(),
                        authToken =
                            null
                    )

            assertTrue(
                result is
                    ApiResult.SerializationError
            )
        }

    private fun healthResponse(
        version: String
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
                """
                {
                  "status":"ok",
                  "version":"$version"
                }
                """.trimIndent()
            )
}
