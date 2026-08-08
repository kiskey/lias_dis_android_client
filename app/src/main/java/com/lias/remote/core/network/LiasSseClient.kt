// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasSseClient.kt
// Version: 13.0.0
//
// Purpose:
//   Lifecycle-aware, replay-safe LIAS Server-Sent Events transport.
//
// Corrections:
//   - connect() is idempotent.
//   - Authentication changes force a new HTTP stream.
//   - Server changes clear Last-Event-ID.
//   - Foreground/background disconnect preserves replay position.
//   - Network-loss disconnect preserves replay position.
//   - Successful SSE handshakes reset exponential backoff.
//   - Last-Event-ID is maintained per LIAS server.
//   - Duplicate event IDs are suppressed defensively.
//   - Correct multiline SSE data parsing.
//   - Stale cancelled jobs cannot overwrite the current state.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.LiasEvent
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
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
    private val client: OkHttpClient
) {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private val _events =
        MutableSharedFlow<LiasEvent>(
            replay = 0,
            extraBufferCapacity = 256
        )

    val events:
        SharedFlow<LiasEvent> =
        _events.asSharedFlow()

    private val _connectionState =
        MutableStateFlow(
            ConnectionState.DISCONNECTED
        )

    val connectionState:
        StateFlow<ConnectionState> =
        _connectionState.asStateFlow()

    private val _lastError =
        MutableStateFlow<String?>(
            null
        )

    val lastError:
        StateFlow<String?> =
        _lastError.asStateFlow()

    private val lock =
        Any()

    @Volatile
    private var configuredBaseUrl:
        String = ""

    @Volatile
    private var configuredAuthToken:
        String? = null

    @Volatile
    private var desiredConnected:
        Boolean = false

    @Volatile
    private var networkAvailable:
        Boolean = true

    private var sseJob:
        Job? = null

    @Volatile
    private var activeCall:
        Call? = null

    /*
     * LIAS event IDs are Unix nanoseconds and fit inside signed Long.
     */
    private val lastEventId =
        AtomicLong(
            0L
        )

    private val lastDeliveredEventId =
        AtomicLong(
            0L
        )

    /*
     * Incremented for every transport replacement. An older cancelled
     * coroutine is therefore unable to publish final DISCONNECTED state
     * over a newer active stream.
     */
    private val generation =
        AtomicLong(
            0L
        )

    fun configure(
        baseUrl: String,
        authToken: String?
    ) {
        val normalizedUrl =
            normalizeUrl(
                baseUrl
            )

        val normalizedToken =
            authToken
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val serverChanged:
            Boolean

        val credentialChanged:
            Boolean

        synchronized(lock) {

            serverChanged =
                normalizedUrl !=
                    configuredBaseUrl

            credentialChanged =
                normalizedToken !=
                    configuredAuthToken

            configuredBaseUrl =
                normalizedUrl

            configuredAuthToken =
                normalizedToken

            if (serverChanged) {
                /*
                 * Replay IDs are meaningful only for the server that
                 * issued them.
                 */
                lastEventId.set(
                    0L
                )

                lastDeliveredEventId.set(
                    0L
                )
            }
        }

        if (
            serverChanged ||
            credentialChanged
        ) {
            restartIfDesired()
        }
    }

    fun setNetworkAvailable(
        available: Boolean
    ) {
        if (
            networkAvailable ==
            available
        ) {
            return
        }

        networkAvailable =
            available

        if (available) {
            restartIfDesired()
        } else {
            cancelActiveTransport(
                publishDisconnected =
                    true
            )
        }
    }

    fun connect(
        scope: CoroutineScope
    ) {
        desiredConnected =
            true

        startIfNeeded(
            scope
        )
    }

    fun disconnect(
        preserveReplayPosition: Boolean =
            true
    ) {
        desiredConnected =
            false

        cancelActiveTransport(
            publishDisconnected =
                true
        )

        if (
            !preserveReplayPosition
        ) {
            clearReplayPosition()
        }
    }

    fun reconnect(
        scope: CoroutineScope,
        preserveReplayPosition: Boolean =
            true
    ) {
        desiredConnected =
            true

        cancelActiveTransport(
            publishDisconnected =
                false
        )

        if (
            !preserveReplayPosition
        ) {
            clearReplayPosition()
        }

        startIfNeeded(
            scope
        )
    }

    fun clearReplayPosition() {
        lastEventId.set(
            0L
        )

        lastDeliveredEventId.set(
            0L
        )
    }

    fun currentReplayPosition():
        Long =
        lastEventId.get()

    private fun restartIfDesired() {
        if (
            !desiredConnected
        ) {
            return
        }

        val ownerScope =
            transportScope
                ?: return

        reconnect(
            scope =
                ownerScope,
            preserveReplayPosition =
                true
        )
    }

    @Volatile
    private var transportScope:
        CoroutineScope? = null

    private fun startIfNeeded(
        scope: CoroutineScope
    ) {
        transportScope =
            scope

        if (
            !desiredConnected ||
            !networkAvailable ||
            configuredBaseUrl.isBlank()
        ) {
            _connectionState.value =
                ConnectionState.DISCONNECTED

            return
        }

        synchronized(lock) {

            if (
                sseJob?.isActive ==
                true
            ) {
                return
            }

            val myGeneration =
                generation.incrementAndGet()

            sseJob =
                scope.launch(
                    Dispatchers.IO
                ) {

                    var backoff =
                        INITIAL_BACKOFF_MS

                    while (
                        isActive &&
                        desiredConnected &&
                        networkAvailable &&
                        configuredBaseUrl
                            .isNotBlank()
                    ) {

                        try {

                            publishState(
                                myGeneration,
                                if (
                                    backoff ==
                                    INITIAL_BACKOFF_MS
                                ) {
                                    ConnectionState.CONNECTING
                                } else {
                                    ConnectionState.RECONNECTING
                                }
                            )

                            consumeSseStream(
                                generation =
                                    myGeneration,
                                onConnected = {

                                    backoff =
                                        INITIAL_BACKOFF_MS

                                    _lastError.value =
                                        null
                                }
                            )

                            /*
                             * A normal end-of-body is still an SSE
                             * disconnect and must reconnect.
                             */
                            throw IOException(
                                "LIAS event stream closed."
                            )

                        } catch (
                            cancellation:
                                CancellationException
                        ) {

                            throw cancellation

                        } catch (
                            error: Exception
                        ) {

                            if (
                                !desiredConnected ||
                                !networkAvailable ||
                                !isActive
                            ) {
                                break
                            }

                            _lastError.value =
                                error.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "LIAS event stream disconnected."

                            publishState(
                                myGeneration,
                                ConnectionState.RECONNECTING
                            )

                            delay(
                                backoff
                            )

                            backoff =
                                (
                                    backoff *
                                        2L
                                    )
                                    .coerceAtMost(
                                        MAX_BACKOFF_MS
                                    )
                        }
                    }

                    publishState(
                        myGeneration,
                        ConnectionState.DISCONNECTED
                    )
                }
        }
    }

    private suspend fun consumeSseStream(
        generation: Long,
        onConnected: () -> Unit
    ) {
        val serverUrl =
            configuredBaseUrl

        val token =
            configuredAuthToken

        val builder =
            Request.Builder()
                .url(
                    "$serverUrl${Endpoints.EVENTS_SSE}"
                )
                .header(
                    "Accept",
                    "text/event-stream"
                )
                .header(
                    "Cache-Control",
                    "no-cache"
                )

        token
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                builder.header(
                    "Authorization",
                    "Bearer $it"
                )
            }

        lastEventId
            .get()
            .takeIf {
                it > 0L
            }
            ?.let {
                builder.header(
                    "Last-Event-ID",
                    it.toString()
                )
            }

        val call =
            client.newCall(
                builder.build()
            )

        activeCall =
            call

        try {

            call.execute()
                .use { response ->

                    if (
                        !response.isSuccessful
                    ) {
                        throw SseHttpException(
                            response.code
                        )
                    }

                    val contentType =
                        response.header(
                            "Content-Type"
                        )
                            .orEmpty()

                    if (
                        !contentType.contains(
                            "text/event-stream",
                            ignoreCase = true
                        )
                    ) {
                        throw IOException(
                            "LIAS returned an invalid SSE content type."
                        )
                    }

                    publishState(
                        generation,
                        ConnectionState.CONNECTED
                    )

                    onConnected()

                    val source =
                        response.body
                            ?.source()
                            ?: throw IOException(
                                "LIAS returned an empty event stream."
                            )

                    var eventType =
                        ""

                    var eventId:
                        Long? = null

                    val dataBuilder =
                        StringBuilder()

                    while (
                        !source.exhausted()
                    ) {

                        val line =
                            source.readUtf8Line()
                                ?: break

                        when {

                            line.isEmpty() -> {

                                if (
                                    eventType.isNotBlank() ||
                                    dataBuilder
                                        .isNotEmpty()
                                ) {

                                    deliverFrame(
                                        eventType =
                                            eventType,
                                        eventId =
                                            eventId,
                                        data =
                                            dataBuilder
                                                .toString()
                                    )
                                }

                                eventType =
                                    ""

                                eventId =
                                    null

                                dataBuilder
                                    .clear()
                            }

                            line.startsWith(
                                ":"
                            ) -> {
                                /*
                                 * SSE comment/heartbeat.
                                 */
                            }

                            line.startsWith(
                                "event:"
                            ) -> {

                                eventType =
                                    line
                                        .removePrefix(
                                            "event:"
                                        )
                                        .trimStart()
                            }

                            line.startsWith(
                                "id:"
                            ) -> {

                                eventId =
                                    line
                                        .removePrefix(
                                            "id:"
                                        )
                                        .trim()
                                        .toLongOrNull()
                            }

                            line.startsWith(
                                "data:"
                            ) -> {

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
                                        line
                                            .removePrefix(
                                                "data:"
                                            )
                                            .removePrefix(
                                                " "
                                            )
                                    )
                            }

                            line.startsWith(
                                "retry:"
                            ) -> {
                                /*
                                 * LIAS currently does not emit retry,
                                 * but parsing it is intentionally
                                 * harmless. Client backoff remains
                                 * bounded locally.
                                 */
                            }
                        }
                    }
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

    private suspend fun deliverFrame(
        eventType: String,
        eventId: Long?,
        data: String
    ) {
        if (
            eventId != null
        ) {

            val previous =
                lastDeliveredEventId
                    .get()

            if (
                eventId <=
                previous
            ) {
                /*
                 * Replay is defined as IDs strictly newer than the
                 * supplied Last-Event-ID, but suppress duplicates
                 * defensively if a proxy/server repeats a frame.
                 */
                lastEventId
                    .updateAndGet {
                        current ->

                        maxOf(
                            current,
                            eventId
                        )
                    }

                return
            }

            lastDeliveredEventId.set(
                eventId
            )

            lastEventId
                .updateAndGet {
                    current ->

                    maxOf(
                        current,
                        eventId
                    )
                }
        }

        val type =
            eventType
                .ifBlank {
                    "message"
                }

        val payload =
            if (
                data.isBlank()
            ) {
                null
            } else {
                try {
                    json.parseToJsonElement(
                        data
                    )
                } catch (
                    _: Exception
                ) {
                    null
                }
            }

        val event =
            LiasEvent(
                type =
                    type,
                timestamp =
                    "",
                deviceID =
                    extractTargetId(
                        data
                    ),
                payload =
                    payload
            )

        _events.emit(
            event
        )
    }

    private fun extractTargetId(
        jsonString: String
    ): String {
        if (
            jsonString.isBlank()
        ) {
            return ""
        }

        return try {

            val element =
                json.parseToJsonElement(
                    jsonString
                )

            if (
                element !is
                JsonObject
            ) {
                return ""
            }

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

        } catch (
            _: Exception
        ) {
            ""
        }
    }

    private fun cancelActiveTransport(
        publishDisconnected: Boolean
    ) {
        generation.incrementAndGet()

        activeCall
            ?.cancel()

        activeCall =
            null

        sseJob
            ?.cancel()

        sseJob =
            null

        if (
            publishDisconnected
        ) {
            _connectionState.value =
                ConnectionState.DISCONNECTED
        }
    }

    private fun publishState(
        streamGeneration: Long,
        state: ConnectionState
    ) {
        if (
            generation.get() ==
            streamGeneration
        ) {
            _connectionState.value =
                state
        }
    }

    private fun normalizeUrl(
        raw: String
    ): String {
        var value =
            raw.trim()

        if (
            value.isBlank()
        ) {
            return ""
        }

        if (
            !value.startsWith(
                "http://",
                ignoreCase = true
            ) &&
            !value.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            value =
                "http://$value"
        }

        return value
            .trimEnd(
                '/'
            )
    }

    private class SseHttpException(
        val statusCode: Int
    ) : IOException(
        when (statusCode) {

            401 ->
                "LIAS rejected the SSE authentication token."

            403 ->
                "LIAS denied access to the event stream."

            else ->
                "LIAS event stream returned HTTP $statusCode."
        }
    )

    private companion object {

        const val INITIAL_BACKOFF_MS =
            500L

        const val MAX_BACKOFF_MS =
            15_000L
    }
}
