// ====================================================================
// File:
// app/src/test/java/com/lias/remote/core/network/LiasSseClientRegressionTest.kt
// Version: 27.6.0
//
// Purpose:
//   Verify LIAS SSE replay and credential lifecycle.
//
// Protects:
//   - initial connection has no replay cursor
//   - same-server reconnect sends Last-Event-ID
//   - disconnect/reconnect preserves replay state
//   - server replacement resets replay cursor
//   - token replacement is used on subsequent request
//   - SSE device ID extraction
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.LiasEvent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiasSseClientRegressionTest {

    private lateinit var serverA:
        MockWebServer

    private lateinit var serverB:
        MockWebServer

    private lateinit var scope:
        CoroutineScope

    @Before
    fun setUp() {

        serverA =
            MockWebServer()

        serverB =
            MockWebServer()

        serverA.start()
        serverB.start()

        scope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.Default
            )
    }

    @After
    fun tearDown() {

        scope.cancel()

        serverA.shutdown()
        serverB.shutdown()
    }

    @Test
    fun `initial SSE request contains no Last Event ID`() {

        serverA.enqueue(
            sseResponse(
                id =
                    100L,
                pdid =
                    "pdid_a"
            )
        )

        val client =
            sseClientFor(
                serverA
            )

        client.connect(
            scope
        )

        val request =
            serverA.takeRequest(
                3,
                TimeUnit.SECONDS
            )

        requireNotNull(
            request
        )

        assertNull(
            request.getHeader(
                "Last-Event-ID"
            )
        )

        client.disconnect()
    }

    @Test
    fun `same server reconnect sends last event id`() {

        serverA.enqueue(
            sseResponse(
                id =
                    123456789L,
                pdid =
                    "pdid_a"
            )
        )

        /*
         * Second response keeps the reconnect deterministic.
         */
        serverA.enqueue(
            sseResponse(
                id =
                    123456790L,
                pdid =
                    "pdid_a"
            )
        )

        val client =
            sseClientFor(
                serverA
            )

        client.connect(
            scope
        )

        val first =
            serverA.takeRequest(
                3,
                TimeUnit.SECONDS
            )

        requireNotNull(
            first
        )

        assertNull(
            first.getHeader(
                "Last-Event-ID"
            )
        )

        val second =
            serverA.takeRequest(
                5,
                TimeUnit.SECONDS
            )

        requireNotNull(
            second
        )

        assertEquals(
            "123456789",
            second.getHeader(
                "Last-Event-ID"
            )
        )

        client.disconnect()
    }

    @Test
    fun `manual reconnect to same server preserves replay cursor`() {

        serverA.enqueue(
            sseResponse(
                id =
                    900L,
                pdid =
                    "pdid_x"
            )
        )

        val client =
            sseClientFor(
                serverA
            )

        client.connect(
            scope
        )

        requireNotNull(
            serverA.takeRequest(
                3,
                TimeUnit.SECONDS
            )
        )

        /*
         * Give the parser enough time to consume the frame before
         * stopping the reconnect loop.
         */
        Thread.sleep(
            150L
        )

        client.disconnect()

        serverA.enqueue(
            sseResponse(
                id =
                    901L,
                pdid =
                    "pdid_x"
            )
        )

        client.connect(
            scope
        )

        val reconnect =
            serverA.takeRequest(
                3,
                TimeUnit.SECONDS
            )

        requireNotNull(
            reconnect
        )

        assertEquals(
            "900",
            reconnect.getHeader(
                "Last-Event-ID"
            )
        )

        client.disconnect()
    }

    @Test
    fun `changing logical server resets replay cursor`() {

        serverA.enqueue(
            sseResponse(
                id =
                    999999L,
                pdid =
                    "pdid_old"
            )
        )

        val client =
            sseClientFor(
                serverA
            )

        client.connect(
            scope
        )

        requireNotNull(
            serverA.takeRequest(
                3,
                TimeUnit.SECONDS
            )
        )

        Thread.sleep(
            150L
        )

        client.disconnect()

        /*
         * Logical LIAS server replacement.
         *
         * The old event timestamp must NOT be sent to server B.
         */
        client.baseUrl =
            serverB
                .url(
                    "/"
                )
                .toString()

        serverB.enqueue(
            sseResponse(
                id =
                    10L,
                pdid =
                    "pdid_new"
            )
        )

        client.connect(
            scope
        )

        val serverBRequest =
            serverB.takeRequest(
                3,
                TimeUnit.SECONDS
            )

        requireNotNull(
            serverBRequest
        )

        assertNull(
            serverBRequest.getHeader(
                "Last-Event-ID"
            )
        )

        client.disconnect()
    }

    @Test
    fun `replacement auth token is used by reconnect`() {

        serverA.enqueue(
            sseResponse(
                id =
                    10L,
                pdid =
                    "pdid_a"
            )
        )

        serverA.enqueue(
            sseResponse(
                id =
                    11L,
                pdid =
                    "pdid_a"
            )
        )

        val client =
            sseClientFor(
                serverA
            )

        client.authToken =
            "old-token"

        client.connect(
            scope
        )

        val first =
            serverA.takeRequest(
                3,
                TimeUnit.SECONDS
            )

        requireNotNull(
            first
        )

        assertEquals(
            "Bearer old-token",
            first.getHeader(
                "Authorization"
            )
        )

        client.authToken =
            "new-token"

        val second =
            serverA.takeRequest(
                5,
                TimeUnit.SECONDS
            )

        requireNotNull(
            second
        )

        assertEquals(
            "Bearer new-token",
            second.getHeader(
                "Authorization"
            )
        )

        client.disconnect()
    }

    @Test
    fun `SSE payload exposes device pdid`() =
        runBlocking {

            serverA.enqueue(
                sseResponse(
                    id =
                        500L,
                    pdid =
                        "pdid_test_device"
                )
            )

            val client =
                sseClientFor(
                    serverA
                )

            val eventDeferred =
                async(
                    start =
                        CoroutineStart.UNDISPATCHED
                ) {

                    client.events
                        .first()
                }

            client.connect(
                scope
            )

            val event:
                LiasEvent =
                withTimeout(
                    3_000L
                ) {
                    eventDeferred.await()
                }

            assertEquals(
                "device.online",
                event.type
            )

            assertEquals(
                "pdid_test_device",
                event.deviceID
            )

            client.disconnect()
        }

    @Test
    fun `identity candidate event resolves source pdid`() =
        runBlocking {
            serverA.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(
                        "Content-Type",
                        "text/event-stream"
                    )
                    .setBody(
                        "id: 501\n" +
                            "event: identity.candidate.changed\n" +
                            "data: {\"candidate_id\":42,\"source_pdid\":\"pdid_source\",\"target_pdid\":\"pdid_target\",\"status\":\"pending\"}\n\n"
                    )
            )

            val client = sseClientFor(serverA)
            val eventDeferred =
                async(start = CoroutineStart.UNDISPATCHED) {
                    client.events.first()
                }

            client.connect(scope)

            val event =
                withTimeout(3_000L) {
                    eventDeferred.await()
                }

            assertEquals(
                EventConstants.IDENTITY_CANDIDATE_CHANGED,
                event.type
            )
            assertEquals("pdid_source", event.deviceID)

            client.disconnect()
        }

    private fun sseClientFor(
        server: MockWebServer
    ): LiasSseClient =
        LiasSseClient(
            OkHttpClient.Builder()
                .readTimeout(
                    0,
                    TimeUnit.SECONDS
                )
                .build()
        )
            .apply {

                baseUrl =
                    server.url(
                        "/"
                    )
                        .toString()
            }

    private fun sseResponse(
        id: Long,
        pdid: String
    ): MockResponse =
        MockResponse()
            .setResponseCode(
                200
            )
            .setHeader(
                "Content-Type",
                "text/event-stream"
            )
            .setBody(
                buildString {

                    append(
                        "id: "
                    )

                    append(
                        id
                    )

                    append(
                        "\n"
                    )

                    append(
                        "event: device.online\n"
                    )

                    append(
                        "data: {\"pdid\":\""
                    )

                    append(
                        pdid
                    )

                    append(
                        "\",\"confirmed_by\":[\"netlink\"]}\n\n"
                    )
                }
            )
}
