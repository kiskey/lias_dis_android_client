# ====================================================================
# File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
# ====================================================================

// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 2.3.0
// Audit Fixes:
//   1. Added SSE replay suppression (<2.5s post-connect) and duplicate event
//      toast debouncing (<3.0s window) to eliminate toast flooding.
//   2. Handled EFFECTIVE_STATUS_CHANGED event to trigger refreshAll().
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.DeviceEventPayload
import com.lias.remote.core.models.DeviceReidentifiedPayload
import com.lias.remote.core.models.NetworkStats
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.SecurityAlertPayload
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.core.network.DeviceListResponse
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.EventConstants
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.network.LiasSseClient
import com.lias.remote.core.store.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.ConcurrentHashMap

class EventRepository(
    internal val api: LiasApiClient,
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {
    internal val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    internal val _uiEvents = MutableSharedFlow<UiEvent>(replay = 0, extraBufferCapacity = 64)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var lastSseConnectedTime: Long = 0L
    private val recentToastMap = ConcurrentHashMap<String, Long>()

    init {
        scope.launch {
            settings.serverUrl.collectLatest { url ->
                api.baseUrl = url
                sse.baseUrl = url
                sse.disconnect()
                if (url.isNotBlank()) {
                    sse.connect(scope)
                    refreshAll()
                }
            }
        }
        scope.launch {
            settings.authToken.collectLatest { token ->
                api.authToken = token
                sse.authToken = token
            }
        }
    }

    fun start() {
        sse.connect(scope)
        scope.launch { collectSseEvents() }
        scope.launch { collectConnectionState() }
    }

    private fun collectConnectionState() {
        scope.launch {
            sse.connectionState.collect { connState ->
                _state.value = _state.value.copy(connectionState = connState)
                if (connState == ConnectionState.CONNECTED) {
                    lastSseConnectedTime = System.currentTimeMillis()
                }
            }
        }
    }

    internal suspend fun refreshAll() {
        if (api.baseUrl.isBlank()) return
        coroutineScope {
            val devsResult = api.get<DeviceListResponse>(Endpoints.DEVICES)
            if (devsResult is ApiResult.Success) {
                _state.value = _state.value.copy(
                    devices = devsResult.data.devices,
                    isInitialLoaded = true
                )
            }

            val tagsDeferred = async { api.get<List<Tag>>(Endpoints.TAGS) }
            val polsDeferred = async { api.get<List<Policy>>(Endpoints.POLICIES) }
            val schedsDeferred = async { api.get<List<Schedule>>(Endpoints.SCHEDULES) }
            val statsDeferred = async { api.get<NetworkStats>(Endpoints.STATS) }

            val tagsResult = tagsDeferred.await()
            val policiesResult = polsDeferred.await()
            val schedulesResult = schedsDeferred.await()
            val statsResult = statsDeferred.await()

            _state.value = _state.value.copy(
                tags = (tagsResult as? ApiResult.Success)?.data ?: _state.value.tags,
                policies = (policiesResult as? ApiResult.Success)?.data ?: _state.value.policies,
                schedules = (schedulesResult as? ApiResult.Success)?.data ?: _state.value.schedules,
                stats = (statsResult as? ApiResult.Success)?.data ?: _state.value.stats,
                isInitialLoaded = true
            )
        }
    }

    private suspend fun collectSseEvents() {
        sse.events.collect { event ->
            val now = System.currentTimeMillis()
            val isReplayPhase = (now - lastSseConnectedTime) < 2500L
            val toastKey = "${event.type}:${event.deviceID}"
            val lastToastTime = recentToastMap[toastKey] ?: 0L
            val isDuplicateToast = (now - lastToastTime) < 3000L

            val shouldShowToast = !isReplayPhase && !isDuplicateToast

            when (event.type) {
                EventConstants.DEVICE_ADDED -> {
                    refreshSingleDevice(event.deviceID)
                    if (shouldShowToast) {
                        recentToastMap[toastKey] = now
                        _uiEvents.emit(UiEvent.ShowSnackbar("✨ New Device Discovered: ${event.deviceID.takeLast(8)}"))
                    }
                }
                EventConstants.DEVICE_ONLINE -> {
                    refreshSingleDevice(event.deviceID)
                    if (shouldShowToast) {
                        recentToastMap[toastKey] = now
                        val confirmedBy = event.payload?.let {
                            try { json.decodeFromJsonElement<DeviceEventPayload>(it).safeConfirmedBy } catch (e: Exception) { emptyList() }
                        } ?: emptyList()
                        val verifiedText = if (confirmedBy.isNotEmpty()) " ✓ ${confirmedBy.size} sources" else ""
                        _uiEvents.emit(UiEvent.ShowSnackbar("🟢 Device Online: ${event.deviceID.takeLast(8)}$verifiedText"))
                    }
                }
                EventConstants.DEVICE_OFFLINE -> {
                    refreshSingleDevice(event.deviceID)
                    if (shouldShowToast) {
                        recentToastMap[toastKey] = now
                        _uiEvents.emit(UiEvent.ShowSnackbar("🔴 Device Offline: ${event.deviceID.takeLast(8)}"))
                    }
                }
                EventConstants.EFFECTIVE_STATUS_CHANGED -> {
                    refreshAll()
                }
                EventConstants.HOSTNAME_CHANGED,
                EventConstants.FINGERPRINT_UPDATED,
                EventConstants.IP_CHANGED,
                EventConstants.MAC_CHANGED -> {
                    refreshSingleDevice(event.deviceID)
                }
                EventConstants.DEVICE_REMOVED -> {
                    _state.value = _state.value.copy(
                        devices = _state.value.devices.filterNot { it.pdid == event.deviceID }
                    )
                }
                EventConstants.DEVICE_REIDENTIFIED -> {
                    val payload = event.payload?.let {
                        try { json.decodeFromJsonElement<DeviceReidentifiedPayload>(it) } catch (e: Exception) { null }
                    }
                    if (payload != null) {
                        _state.value = _state.value.copy(
                            devices = _state.value.devices.filterNot { it.pdid == payload.oldPdid }
                        )
                        refreshSingleDevice(payload.newPdid)
                        if (shouldShowToast) {
                            recentToastMap[toastKey] = now
                            _uiEvents.emit(UiEvent.ShowSnackbar("🔄 Device identified: ${payload.newPdid.takeLast(8)} (promoted from ${payload.reason})"))
                        }
                    }
                }
                EventConstants.SECURITY_ALERT -> {
                    val payload = event.payload?.let {
                        try { json.decodeFromJsonElement<SecurityAlertPayload>(it) } catch (e: Exception) { null }
                    }
                    val details = payload?.details ?: "Anomaly detected"
                    _uiEvents.emit(UiEvent.ShowSecurityAlert(details))
                    _uiEvents.emit(UiEvent.ShowSnackbarError("🚨 Security Alert: $details"))
                }
            }
        }
    }

    private suspend fun refreshSingleDevice(pdid: String) {
        if (pdid.isBlank()) return
        val result = api.get<Device>(Endpoints.device(pdid))
        if (result is ApiResult.Success) {
            val updatedDevice = result.data
            val currentDevs = _state.value.devices.toMutableList()
            val index = currentDevs.indexOfFirst { it.pdid == pdid }
            if (index != -1) {
                currentDevs[index] = updatedDevice
            } else {
                currentDevs.add(updatedDevice)
            }
            _state.value = _state.value.copy(devices = currentDevs)
        }
    }
}
