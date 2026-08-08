// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasSseClient.kt
// Version: 2.0.0
//
// Purpose:
//   Production-oriented Server-Sent Events client for LIAS Remote.
//
// Contract:
//   Mirrors the LIAS backend StreamEvents implementation.
//
// Audit / Stability Changes:
//   1. Correctly preserves Last-Event-ID replay semantics.
//   2. Parses SSE id separately from payload.
//   3. Converts the backend nanosecond event ID into the LiasEvent
//      timestamp field when possible.
//   4. Supports multi-line SSE data fields.
//   5. Ignores SSE comments/heartbeats safely.
//   6. Resets event framing state after each dispatched event.
//   7. Avoids reconnecting indefinitely on a clean client disconnect.
//   8. Preserves the existing exponential reconnect strategy.
//   9. Treats HTTP failures as reconnectable while exposing the
//      connection state to the UI.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.LiasEvent
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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Volatile
    var baseUrl: String = "http://127.0.0.1:8081"

    @Volatile
    var authToken: String? = null

    private val _events =
        MutableSharedFlow<LiasEvent>(
            replay = 0,
            extraBufferCapacity = 128
        )

    val events: SharedFlow<LiasEvent> =
        _events.asSharedFlow()

    private val _connectionState =
        MutableStateFlow(
            ConnectionState.DISCONNECTED
        )

    val connectionState: StateFlow<ConnectionState> =
        _connectionState.asStateFlow()

    @Volatile
    private var sseJob: Job? = null

    @Volatile
    private var activeCall: Call? = null

    /**
     * Backend event IDs are Unix nanoseconds.
     *
     * Long is sufficient for the timestamp range used by the backend.
     */
    @Volatile
    private var lastEventId: Long = 0L

    fun connect(scope: CoroutineScope) {
        disconnect()

        sseJob = scope.launch(Dispatchers.IO) {
            var backoff = INITIAL_BACKOFF_MS

            while (isActive) {
                try {
                    _connectionState.value =
                        if (backoff == INITIAL_BACKOFF_MS) {
                            ConnectionState.CONNECTING
                        } else {
                            ConnectionState.RECONNECTING
                        }

                    consumeSseStream()

                    // A stream ending normally is treated as a
                    // reconnect condition unless the coroutine was
                    // cancelled.
                    if (isActive) {
                        _connectionState.value =
                            ConnectionState.RECONNECTING

                        delay(backoff)

                        backoff =
                            (backoff * 2L)
                                .coerceAtMost(MAX_BACKOFF_MS)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    if (!isActive) {
                        break
                    }

                    _connectionState.value =
                        ConnectionState.RECONNECTING

                    delay(backoff)

                    backoff =
                        (backoff * 2L)
                            .coerceAtMost(MAX_BACKOFF_MS)
                }
            }

            _connectionState.value =
                ConnectionState.DISCONNECTED
        }
    }

    fun disconnect() {
        activeCall?.cancel()
        activeCall = null

        sseJob?.cancel()
        sseJob = null

        _connectionState.value =
            ConnectionState.DISCONNECTED
    }

    private fun normalizeUrl(raw: String): String {
        var url = raw.trim()

        if (url.isBlank()) {
            return ""
        }

        if (
            !url.startsWith("http://") &&
            !url.startsWith("https://")
        ) {
            url = "http://$url"
        }

        return url.trimEnd('/')
    }

    private suspend fun consumeSseStream() {
        val sanitizedBase =
            normalizeUrl(baseUrl)

        require(sanitizedBase.isNotBlank()) {
            "LIAS server URL is not configured."
        }

        val requestBuilder =
            Request.Builder()
                .url(
                    "$sanitizedBase${Endpoints.EVENTS_SSE}"
                )
                .header(
                    "Accept",
                    "text/event-stream"
                )
                .header(
                    "Cache-Control",
                    "no-cache"
                )
                .header(
                    "Connection",
                    "keep-alive"
                )

        authToken
            ?.takeIf { it.isNotBlank() }
            ?.let { token ->
                requestBuilder.header(
                    "Authorization",
                    "Bearer $token"
                )
            }

        if (lastEventId > 0L) {
            requestBuilder.header(
                "Last-Event-ID",
                lastEventId.toString()
            )
        }

        val call =
            client.newCall(
                requestBuilder.build()
            )

        activeCall = call

        call.execute().use { response ->

            if (!response.isSuccessful) {
                throw SseHttpException(
                    response.code
                )
            }

            _connectionState.value =
                ConnectionState.CONNECTED

            val source =
                response.body?.source()
                    ?: throw IllegalStateException(
                        "LIAS SSE response has no body."
                    )

            var eventType: String? = null
            var eventId: Long? = null
            val dataBuilder =
                StringBuilder()

            while (!source.exhausted()) {
                val line =
                    source.readUtf8Line()
                        ?: break

                when {
                    line.isEmpty() -> {
                        dispatchEvent(
                            eventType = eventType,
                            eventId = eventId,
                            data = dataBuilder.toString()
                        )

                        eventType = null
                        eventId = null
                        dataBuilder.clear()
                    }

                    line.startsWith(":") -> {
                        // SSE comment / heartbeat.
                        // It intentionally has no application meaning.
                    }

                    line.startsWith("event:") -> {
                        eventType =
                            parseFieldValue(line)
                                .takeIf { it.isNotBlank() }
                    }

                    line.startsWith("id:") -> {
                        val parsedId =
                            parseFieldValue(line)
                                .toLongOrNull()

                        if (parsedId != null) {
                            eventId = parsedId
                            lastEventId = parsedId
                        }
                    }

                    line.startsWith("data:") -> {
                        if (dataBuilder.isNotEmpty()) {
                            dataBuilder.append('\n')
                        }

                        dataBuilder.append(
                            parseFieldValue(line)
                        )
                    }

                    // retry: is intentionally ignored.
                    // The LIAS client owns its reconnect policy so a
                    // server-supplied retry value cannot unexpectedly
                    // create an aggressive reconnect loop.
                    line.startsWith("retry:") -> Unit

                    else -> {
                        // Unknown SSE field. Ignore per SSE semantics.
                    }
                }
            }

            // The server normally terminates events with a blank line,
            // but dispatch a final buffered event defensively.
            if (
                dataBuilder.isNotEmpty() ||
                eventType != null ||
                eventId != null
            ) {
                dispatchEvent(
                    eventType = eventType,
                    eventId = eventId,
                    data = dataBuilder.toString()
                )
            }

            throw IllegalStateException(
                "LIAS SSE stream closed by server."
            )
        }
    }

    private suspend fun dispatchEvent(
        eventType: String?,
        eventId: Long?,
        data: String
    ) {
        if (data.isBlank()) {
            return
        }

        val type =
            eventType
                ?.takeIf { it.isNotBlank() }
                ?: "message"

        val payload =
            try {
                json.parseToJsonElement(data)
            } catch (_: Exception) {
                null
            }

        val deviceId =
            extractDeviceId(data)

        val timestamp =
            eventId
                ?.let(::timestampFromEventId)
                ?: extractTimestamp(data)

        val event =
            LiasEvent(
                type = type,
                timestamp = timestamp,
                deviceID = deviceId,
                payload = payload
            )

        _events.emit(event)
    }

    private fun parseFieldValue(
        line: String
    ): String {
        val separatorIndex =
            line.indexOf(':')

        if (separatorIndex < 0) {
            return ""
        }

        var value =
            line.substring(separatorIndex + 1)

        if (value.startsWith(" ")) {
            value = value.drop(1)
        }

        return value
    }

    private fun timestampFromEventId(
        eventId: Long
    ): String {
        return try {
            java.time.Instant
                .ofEpochSecond(
                    eventId / 1_000_000_000L,
                    eventId % 1_000_000_000L
                )
                .toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractTimestamp(
        jsonString: String
    ): String {
        if (jsonString.isBlank()) {
            return ""
        }

        return try {
            val element =
                json.parseToJsonElement(
                    jsonString
                )

            if (element is JsonObject) {
                element["timestamp"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    .orEmpty()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractDeviceId(
        jsonString: String
    ): String {
        if (jsonString.isBlank()) {
            return ""
        }

        return try {
            val element =
                json.parseToJsonElement(
                    jsonString
                )

            if (element is JsonObject) {
                element["pdid"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?: element["device_id"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                    ?: element["new_pdid"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                    ?: element["old_pdid"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                    ?: ""
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private class SseHttpException(
        val statusCode: Int
    ) : Exception(
        "LIAS SSE endpoint returned HTTP $statusCode."
    )

    private companion object {
        const val INITIAL_BACKOFF_MS = 500L
        const val MAX_BACKOFF_MS = 10_000L
    }
}
