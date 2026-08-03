// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Added missing `async` and `coroutineScope` imports.
//   2. Changed `_state`, `api`, and `refreshAll()` from `private` to `internal` 
//      to allow EventRepositoryActions.kt extension functions access.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EventRepository(
    internal val api: LiasApiClient, // Changed to internal
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {
    internal val _state = MutableStateFlow(UiState()) // Changed to internal
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            settings.serverUrl.collectLatest { url ->
                api.baseUrl = url
                sse.baseUrl = url
                refreshAll()
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
            }
        }
    }

    internal suspend fun refreshAll() { // Changed to internal
        coroutineScope {
            val devs = async { api.get<DeviceListResponse>(Endpoints.DEVICES) }
            val tags = async { api.get<List<Tag>>(Endpoints.TAGS) }
            val pols = async { api.get<List<Policy>>(Endpoints.POLICIES) }
            val scheds = async { api.get<List<Schedule>>(Endpoints.SCHEDULES) }

            val devicesResult = devs.await()
            val tagsResult = tags.await()
            val policiesResult = pols.await()
            val schedulesResult = scheds.await()

            _state.value = _state.value.copy(
                devices = (devicesResult as? ApiResult.Success)?.data?.devices ?: emptyList(),
                tags = (tagsResult as? ApiResult.Success)?.data ?: emptyList(),
                policies = (policiesResult as? ApiResult.Success)?.data ?: emptyList(),
                schedules = (schedulesResult as? ApiResult.Success)?.data ?: emptyList(),
                isInitialLoaded = true
            )
        }
    }

    private suspend fun collectSseEvents() {
        sse.events.collect { event ->
            when (event.type) {
                EventConstants.DEVICE_ADDED,
                EventConstants.DEVICE_ONLINE,
                EventConstants.DEVICE_OFFLINE,
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
