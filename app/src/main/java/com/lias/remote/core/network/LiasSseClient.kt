// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasSseClient.kt
// Version: 1.6.0
// Audit Fixes:
//   1. Verified volatile fields and OkHttp active call socket cancellation logic.
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
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use

class LiasSseClient(
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    var baseUrl: String = "http://127.0.0.1:8081"

    @Volatile
    var authToken: String? = null

    private val _events = MutableSharedFlow<LiasEvent>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val events: SharedFlow<LiasEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var sseJob: Job? = null
    
    @Volatile
    private var activeCall: Call? = null
    
    private var lastEventId: Long = 0L

    fun connect(scope: kotlinx.coroutines.CoroutineScope) {
        disconnect()
        sseJob = scope.launch(Dispatchers.IO) {
            var backoff = 1000L
            val maxBackoff = 30000L

            while (isActive) {
                try {
                    _connectionState.value = ConnectionState.CONNECTING
                    consumeSseStream()
                    backoff = 1000L
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
        activeCall?.cancel()
        activeCall = null
        sseJob?.cancel()
        sseJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun normalizeUrl(raw: String): String {
        var url = raw.trim()
        if (url.isBlank()) return ""
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url.trimEnd('/')
    }

    private suspend fun consumeSseStream() {
        val sanitizedBase = normalizeUrl(baseUrl)
        val request = Request.Builder()
            .url("$sanitizedBase${Endpoints.EVENTS_SSE}")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .apply { 
                authToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
                if (lastEventId > 0) {
                    header("Last-Event-ID", lastEventId.toString())
                }
            }
            .build()

        val call = client.newCall(request)
        activeCall = call
        val response = call.execute()

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("SSE returned HTTP ${resp.code}")
            }
            _connectionState.value = ConnectionState.CONNECTED

            val source = resp.body?.source() ?: throw Exception("Empty SSE body")
            var eventType = ""
            val dataBuilder = StringBuilder()

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
                                    payload = if (payloadStr.isNotBlank()) json.parseToJsonElement(payloadStr) else null
                                )
                                _events.emit(event)
                            } catch (_: Exception) { }
                            
                            eventType = ""
                            dataBuilder.clear()
                        }
                    }
                    line.startsWith("event: ") -> eventType = line.removePrefix("event: ").trim()
                    line.startsWith("data: ") -> dataBuilder.append(line.removePrefix("data: "))
                    line.startsWith("id: ") -> {
                        lastEventId = line.removePrefix("id: ").trim().toLongOrNull() ?: lastEventId
                    }
                }
            }
            throw Exception("SSE stream closed by server")
        }
    }

    private fun extractDeviceId(jsonStr: String): String {
        val regex = """"pdid"\s*:\s*"([^"]+)"""".toRegex()
        val match = regex.find(jsonStr)
        return match?.groupValues?.get(1) ?: ""
    }
}
