// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasSseClient.kt
// Version: 1.2.0
// Audit Fixes: 
//   1. Hardened empty payload parsing to prevent silent exceptions on pings (GAP-E05).
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.LiasEvent
import kotlinx.coroutines.CancellationException
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.use

class LiasSseClient(
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    var baseUrl: String = "http://127.0.0.1:8081"
    var authToken: String? = null

    private val _events = MutableSharedFlow<LiasEvent>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val events: SharedFlow<LiasEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var sseJob: Job? = null

    fun connect(scope: kotlinx.coroutines.CoroutineScope) {
        sseJob?.cancel()
        sseJob = scope.launch(Dispatchers.IO) {
            var backoff = 1000L // 1 second
            val maxBackoff = 30000L // 30 seconds

            while (isActive) {
                try {
                    _connectionState.value = ConnectionState.CONNECTING
                    consumeSseStream()
                    backoff = 1000L // Reset on clean disconnect
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(maxBackoff)
                }
            }
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun disconnect() {
        sseJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private suspend fun consumeSseStream() {
        val sanitizedBase = baseUrl.trimEnd('/')
        val request = Request.Builder()
            .url("$sanitizedBase${Endpoints.EVENTS_SSE}")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .apply { authToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") } }
            .build()

        val response = client.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("SSE returned HTTP ${resp.code}")
            }
            _connectionState.value = ConnectionState.CONNECTED

            val source = resp.body?.source() ?: throw Exception("Empty SSE body")
            var eventType = ""
            var dataBuilder = StringBuilder()
            var eventId = 0L

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                when {
                    line.isEmpty() -> {
                        if (dataBuilder.isNotEmpty()) {
                            val payloadStr = dataBuilder.toString()
                            try {
                                val event = LiasEvent(
                                    type = eventType.ifEmpty { "message" },
                                    timestamp = "", 
                                    deviceID = extractDeviceId(payloadStr),
                                    // GAP-E05 Fix: Safely handle empty payloads like pings
                                    payload = if (payloadStr.isNotBlank()) json.parseToJsonElement(payloadStr) else null
                                )
                                _events.emit(event)
                            } catch (_: Exception) { /* Ignore parse error for pings */ }
                            
                            eventType = ""
                            dataBuilder = StringBuilder()
                        }
                    }
                    line.startsWith("event: ") -> eventType = line.removePrefix("event: ").trim()
                    line.startsWith("data: ") -> dataBuilder.append(line.removePrefix("data: "))
                    line.startsWith("id: ") -> eventId = line.removePrefix("id: ").trim().toLongOrNull() ?: 0L
                }
            }
            throw Exception("SSE stream closed by server") // Triggers backoff
        }
    }

    private fun extractDeviceId(jsonStr: String): String {
        val regex = """"pdid"\s*:\s*"([^"]+)"""".toRegex()
        val match = regex.find(jsonStr)
        return match?.groupValues?.get(1) ?: ""
    }
}
