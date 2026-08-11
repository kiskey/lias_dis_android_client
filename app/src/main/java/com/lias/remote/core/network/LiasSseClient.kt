// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasSseClient.kt
// Version: 23.0.0
//
// Purpose:
//   Resilient LIAS Server-Sent Events client.
//
// Batch 23 invariants:
//
//   SAME SERVER RECONNECT
//     Preserve Last-Event-ID so LIAS can replay missed events.
//
//   DIFFERENT SERVER
//     Reset Last-Event-ID because the cursor belongs to the previous
//     server's event-history timeline.
//
//   TOKEN CHANGE
//     Cancel the active request so the reconnect uses the new token.
//
//   disconnect()
//     Does NOT clear replay state. Ordinary reconnects depend on it.
//
//   changeServer()/baseUrl setter
//     DOES clear replay state when the normalized endpoint changes.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.LiasEvent
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

class LiasSseClient(
    private val client:
        OkHttpClient
) {

    private val json =
        Json {
            ignoreUnknownKeys =
                true
        }

    @Volatile
    private var configuredBaseUrl =
        "http://127.0.0.1:8081"

    var baseUrl: String
        get() =
            configuredBaseUrl

        set(value) {

            val normalized =
                normalizeUrl(
                    value
                )

            val previous =
                configuredBaseUrl

            configuredBaseUrl =
                normalized

            /*
             * Replay cursors are meaningful only inside one LIAS
             * server's event-history timeline.
             */
            if (
                normalizeUrl(
                    previous
                ) !=
                normalized
            ) {

                synchronized(
                    replayLock
                ) {
                    lastEventId =
                        0L
                }

                /*
                 * If a stream is currently active, cause the existing
                 * reconnect loop to reopen against the new endpoint.
                 */
                activeCall
                    ?.cancel()
            }
        }

    @Volatile
    private var configuredAuthToken:
        String? =
        null

    var authToken: String?
        get() =
            configuredAuthToken

        set(value) {

            val normalized =
                value
                    ?.trim()
                    ?.ifBlank {
                        null
                    }

            val changed =
                normalized !=
                    configuredAuthToken

            configuredAuthToken =
                normalized

            /*
             * Bearer credentials are applied when the HTTP request is
             * created. Restart an active request to apply a replacement
             * token immediately rather than waiting for an unrelated
             * disconnect.
             */
            if (
                changed
            ) {
                activeCall
                    ?.cancel()
            }
        }

    private val _events =
        MutableSharedFlow<LiasEvent>(
            replay =
                0,
            extraBufferCapacity =
                128
        )

    val events:
        SharedFlow<LiasEvent> =
        _events
            .asSharedFlow()

    private val _connectionState =
        MutableStateFlow(
            ConnectionState.DISCONNECTED
        )

    val connectionState:
        StateFlow<ConnectionState> =
        _connectionState
            .asStateFlow()

    @Volatile
    private var activeCall:
        Call? =
        null

    private var sseJob:
        Job? =
        null

    private val replayLock =
        Any()

    private var lastEventId:
        Long =
        0L

    fun connect(
        scope: CoroutineScope
    ) {

        /*
         * disconnect() deliberately preserves lastEventId.
         */
        disconnect()

        sseJob =
            scope.launch(
                Dispatchers.IO
            ) {

                var backoff =
                    INITIAL_RECONNECT_BACKOFF_MS

                while (
                    isActive
                ) {

                    try {

                        _connectionState.value =
                            ConnectionState.CONNECTING

                        consumeSseStream()

                        /*
                         * A normal SSE connection should not return.
                         * If it does, restart with minimum backoff.
                         */
                        backoff =
                            INITIAL_RECONNECT_BACKOFF_MS

                    } catch (
                        error: CancellationException
                    ) {

                        throw error

                    } catch (
                        _: Exception
                    ) {

                        if (
                            !isActive
                        ) {
                            break
                        }

                        _connectionState.value =
                            ConnectionState.RECONNECTING

                        delay(
                            backoff
                        )

                        backoff =
                            (
                                backoff *
                                    2L
                                )
                                .coerceAtMost(
                                    MAX_RECONNECT_BACKOFF_MS
                                )
                    }
                }

                _connectionState.value =
                    ConnectionState.DISCONNECTED
            }
    }

    /**
     * Stop the stream while retaining replay position.
     *
     * A subsequent connect() to the same configured server should send
     * Last-Event-ID so LIAS can replay missed events.
     */
    fun disconnect() {

        activeCall
            ?.cancel()

        activeCall =
            null

        sseJob
            ?.cancel()

        sseJob =
            null

        _connectionState.value =
            ConnectionState.DISCONNECTED
    }

    /**
     * Explicitly forget replay state.
     *
     * Primarily useful when a caller intentionally wants a clean event
     * timeline without replacing the endpoint.
     */
    fun resetReplayCursor() {

        synchronized(
            replayLock
        ) {
            lastEventId =
                0L
        }
    }

    private fun normalizeUrl(
        raw: String
    ): String {

        var result =
            raw.trim()

        if (
            result.isBlank()
        ) {
            return ""
        }

        if (
            !result.startsWith(
                "http://",
                ignoreCase =
                    true
            ) &&
            !result.startsWith(
                "https://",
                ignoreCase =
                    true
            )
        ) {

            result =
                "http://$result"
        }

        return result
            .trimEnd(
                '/'
            )
    }

    private suspend fun consumeSseStream() {

        val server =
            normalizeUrl(
                configuredBaseUrl
            )

        if (
            server.isBlank()
        ) {

            throw IllegalStateException(
                "LIAS SSE server URL is empty."
            )
        }

        val replayCursor =
            synchronized(
                replayLock
            ) {
                lastEventId
            }

        val request =
            Request.Builder()
                .url(
                    "$server${Endpoints.EVENTS_SSE}"
                )
                .header(
                    "Accept",
                    "text/event-stream"
                )
                .header(
                    "Cache-Control",
                    "no-cache"
                )
                .apply {

                    configuredAuthToken
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            token ->

                            header(
                                "Authorization",
                                "Bearer $token"
                            )
                        }

                    if (
                        replayCursor >
                        0L
                    ) {

                        header(
                            "Last-Event-ID",
                            replayCursor
                                .toString()
                        )
                    }
                }
                .build()

        val call =
            client.newCall(
                request
            )

        activeCall =
            call

        try {

            call.execute()
                .use {
                    response ->

                    if (
                        !response
                            .isSuccessful
                    ) {

                        throw IllegalStateException(
                            "SSE returned HTTP ${response.code}"
                        )
                    }

                    _connectionState.value =
                        ConnectionState.CONNECTED

                    val source =
                        response.body
                            ?.source()
                            ?: throw IllegalStateException(
                                "Empty SSE body"
                            )

                    var eventType =
                        ""

                    val dataBuilder =
                        StringBuilder()

                    var eventDataBytes =
                        0

                    while (
                        !source.exhausted()
                    ) {

                        val line =
                            source.readUtf8Line()
                                ?: break

                        when {

                            line.isEmpty() -> {

                                if (
                                    dataBuilder
                                        .isNotEmpty()
                                ) {

                                    emitEvent(
                                        eventType =
                                            eventType,
                                        payloadString =
                                            dataBuilder
                                                .toString()
                                    )

                                    eventType =
                                        ""

                                    dataBuilder
                                        .clear()

                                    eventDataBytes =
                                        0
                                }
                            }

                            line.startsWith(
                                "event:"
                            ) -> {

                                eventType =
                                    line
                                        .substringAfter(
                                            "event:"
                                        )
                                        .trim()
                            }

                            line.startsWith(
                                "data:"
                            ) -> {

                                val data =
                                    line
                                        .substringAfter(
                                            "data:"
                                        )
                                        .trimStart()

                                eventDataBytes +=
                                    data
                                        .encodeToByteArray()
                                        .size +
                                        if (
                                            dataBuilder.isNotEmpty()
                                        ) {
                                            1
                                        } else {
                                            0
                                        }

                                if (
                                    eventDataBytes >
                                    MAX_EVENT_DATA_BYTES
                                ) {
                                    throw IOException(
                                        "LIAS SSE event exceeded the 1 MiB contract limit."
                                    )
                                }

                                if (
                                    dataBuilder
                                        .isNotEmpty()
                                ) {
                                    dataBuilder
                                        .append(
                                            '\n'
                                        )
                                }

                                dataBuilder
                                    .append(
                                        data
                                    )
                            }

                            line.startsWith(
                                "id:"
                            ) -> {

                                val incomingId =
                                    line
                                        .substringAfter(
                                            "id:"
                                        )
                                        .trim()
                                        .toLongOrNull()

                                if (
                                    incomingId !=
                                    null
                                ) {

                                    synchronized(
                                        replayLock
                                    ) {

                                        /*
                                         * LIAS IDs are Unix nanoseconds.
                                         * Never regress within one server
                                         * timeline because of a malformed
                                         * or reordered frame.
                                         */
                                        if (
                                            incomingId >
                                            lastEventId
                                        ) {
                                            lastEventId =
                                                incomingId
                                        }
                                    }
                                }
                            }

                            line.startsWith(
                                ":"
                            ) -> {
                                /*
                                 * Standard SSE comment/heartbeat.
                                 */
                            }
                        }
                    }

                    /*
                     * Flush a final event if the server ended without an
                     * extra blank frame separator.
                     */
                    if (
                        dataBuilder
                            .isNotEmpty()
                    ) {

                        emitEvent(
                            eventType =
                                eventType,
                            payloadString =
                                dataBuilder
                                    .toString()
                        )
                    }

                    throw IllegalStateException(
                        "SSE stream closed by server"
                    )
                }

        } finally {

            if (
                activeCall ===
                call
            ) {
                activeCall =
                    null
            }
        }
    }

    private suspend fun emitEvent(
        eventType: String,
        payloadString: String
    ) {

        try {

            val payload =
                payloadString
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        json.parseToJsonElement(
                            it
                        )
                    }

            _events.emit(
                LiasEvent(
                    type =
                        eventType
                            .ifBlank {
                                "message"
                            },
                    timestamp =
                        "",
                    deviceID =
                        extractDeviceId(
                            payloadString
                        ),
                    payload =
                        payload
                )
            )

        } catch (
            _: Exception
        ) {
            /*
             * One malformed SSE event must not terminate the stream.
             * Full diagnostics for malformed SSE frames can be added
             * independently without poisoning valid subsequent events.
             */
        }
    }

    private fun extractDeviceId(
        jsonString: String
    ): String {

        if (
            jsonString
                .isBlank()
        ) {
            return ""
        }

        return try {

            val element =
                json.parseToJsonElement(
                    jsonString
                )

            if (
                element is
                JsonObject
            ) {

                element[
                    "pdid"
                ]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: element[
                        "target_id"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: element[
                        "source_pdid"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: element[
                        "target_pdid"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: element[
                        "new_pdid"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: element[
                        "device_id"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: element[
                        "old_pdid"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: ""

            } else {
                ""
            }

        } catch (
            _: Exception
        ) {
            ""
        }
    }

    companion object {

        private const val INITIAL_RECONNECT_BACKOFF_MS =
            500L

        private const val MAX_RECONNECT_BACKOFF_MS =
            10_000L

        private const val MAX_EVENT_DATA_BYTES =
            1 shl 20
    }
}
