// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 3.0.0
//
// Purpose:
//   Single source of truth for LIAS Remote runtime state.
//
// Architecture:
//   REST -> Repository -> UiState
//   SSE  -> Repository -> targeted reconciliation -> UiState
//
// Stability changes:
//   1. Repository owns exactly one SSE event collector.
//   2. Repository owns exactly one connection-state collector.
//   3. Settings observers are started once.
//   4. refreshAll() is serialized to prevent overlapping full refreshes.
//   5. Device/tag effective-status requests run in bounded parallel batches.
//   6. SSE events no longer blindly trigger unbounded refresh storms.
//   7. State mutations use StateFlow.update for atomicity.
//   8. Repository exposes emitUiEvent() rather than requiring ViewModel
//      access to internal MutableSharedFlow.
//   9. Server URL changes cleanly disconnect and rebuild the live state.
//  10. Authentication and serialization errors are surfaced explicitly.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.DeviceEventPayload
import com.lias.remote.core.models.DeviceReidentifiedPayload
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.NetworkStats
import com.lias.remote.core.models.SecurityAlertPayload
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class EventRepository(
    internal val api: LiasApiClient,
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {

    private val _state =
        MutableStateFlow(UiState())

    val state: StateFlow<UiState> =
        _state.asStateFlow()

    private val _uiEvents =
        MutableSharedFlow<UiEvent>(
            replay = 0,
            extraBufferCapacity = 64
        )

    val uiEvents: SharedFlow<UiEvent> =
        _uiEvents.asSharedFlow()

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default
        )

    private val refreshMutex =
        Mutex()

    private val started =
        AtomicBoolean(false)

    private val recentToastMap =
        ConcurrentHashMap<String, Long>()

    @Volatile
    private var lastSseConnectedTime = 0L

    // Prevents rapid event bursts from causing repeated full refreshes.
    @Volatile
    private var lastEffectiveStatusRefreshTime = 0L

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            observeSettings()
        }

        scope.launch {
            collectSseEvents()
        }

        scope.launch {
            collectConnectionState()
        }
    }

    fun stop() {
        sse.disconnect()
        _state.update {
            it.copy(
                connectionState = ConnectionState.DISCONNECTED
            )
        }
    }

    // ----------------------------------------------------------------
    // Public event emission
    // ----------------------------------------------------------------

    suspend fun emitUiEvent(event: UiEvent) {
        _uiEvents.emit(event)
    }

    fun tryEmitUiEvent(event: UiEvent) {
        _uiEvents.tryEmit(event)
    }

    // ----------------------------------------------------------------
    // Settings / connection lifecycle
    // ----------------------------------------------------------------

    private suspend fun observeSettings() {
        coroutineScope {
            launch {
                settings.serverUrl.collectLatest { url ->
                    api.baseUrl = url.trim()
                    sse.baseUrl = url.trim()

                    sse.disconnect()

                    _state.update {
                        it.copy(
                            connectionState =
                                ConnectionState.DISCONNECTED,
                            isRefreshing = false
                        )
                    }

                    if (url.isNotBlank()) {
                        sse.connect(scope)
                        refreshAll()
                    } else {
                        _state.update {
                            it.copy(
                                devices = emptyList(),
                                tags = emptyList(),
                                policies = emptyList(),
                                schedules = emptyList(),
                                stats = null,
                                users = emptyList(),
                                deviceEffectiveStatuses = emptyMap(),
                                tagEffectiveStatuses = emptyMap(),
                                isInitialLoaded = false,
                                errorMessage = null
                            )
                        }
                    }
                }
            }

            launch {
                settings.authToken.collectLatest { token ->
                    api.authToken =
                        token?.trim()?.takeIf {
                            it.isNotBlank()
                        }

                    sse.authToken =
                        token?.trim()?.takeIf {
                            it.isNotBlank()
                        }
                }
            }
        }
    }

    private suspend fun collectConnectionState() {
        sse.connectionState.collect { connectionState ->
            _state.update {
                it.copy(
                    connectionState = connectionState,
                    lastConnectionError =
                        if (
                            connectionState ==
                            ConnectionState.CONNECTED
                        ) {
                            null
                        } else {
                            it.lastConnectionError
                        }
                )
            }

            if (
                connectionState ==
                ConnectionState.CONNECTED
            ) {
                lastSseConnectedTime =
                    System.currentTimeMillis()

                // A successful SSE connection means replay/live
                // delivery is now active. Reconcile once so the
                // client cannot remain stale after reconnect.
                scope.launch {
                    refreshAll()
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Full refresh
    // ----------------------------------------------------------------

    internal suspend fun refreshAll() {
        val baseUrl = api.baseUrl.trim()

        if (baseUrl.isBlank()) {
            return
        }

        refreshMutex.withLock {
            _state.update {
                it.copy(
                    isRefreshing = true,
                    errorMessage = null
                )
            }

            try {
                val devicesResult =
                    api.get<DeviceListResponse>(
                        Endpoints.DEVICES
                    )

                val devices =
                    when (devicesResult) {
                        is ApiResult.Success ->
                            devicesResult.data.devices

                        else ->
                            _state.value.devices
                    }

                _state.update {
                    it.copy(
                        devices = devices,
                        isInitialLoaded = true
                    )
                }

                val (
                    tagsResult,
                    policiesResult,
                    schedulesResult,
                    statsResult
                ) = coroutineScope {
                    listOf(
                        async {
                            api.get<List<com.lias.remote.core.models.Tag>>(
                                Endpoints.TAGS
                            )
                        },
                        async {
                            api.get<List<com.lias.remote.core.models.Policy>>(
                                Endpoints.POLICIES
                            )
                        },
                        async {
                            api.get<List<com.lias.remote.core.models.Schedule>>(
                                Endpoints.SCHEDULES
                            )
                        },
                        async {
                            api.get<NetworkStats>(
                                Endpoints.STATS
                            )
                        }
                    ).awaitAll()
                }

                val loadedTags =
                    (tagsResult as? ApiResult.Success)
                        ?.data
                        ?: _state.value.tags

                val loadedPolicies =
                    (policiesResult as? ApiResult.Success)
                        ?.data
                        ?: _state.value.policies

                val loadedSchedules =
                    (schedulesResult as? ApiResult.Success)
                        ?.data
                        ?: _state.value.schedules

                val loadedStats =
                    (statsResult as? ApiResult.Success)
                        ?.data
                        ?: _state.value.stats

                _state.update {
                    it.copy(
                        tags = loadedTags,
                        policies = loadedPolicies,
                        schedules = loadedSchedules,
                        stats = loadedStats,
                        isInitialLoaded = true
                    )
                }

                refreshEffectiveStatuses(
                    devices = devices,
                    tags = loadedTags
                )

                val refreshError =
                    firstMeaningfulError(
                        devicesResult,
                        tagsResult,
                        policiesResult,
                        schedulesResult,
                        statsResult
                    )

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = refreshError
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage =
                            error.message
                                ?: "Unable to refresh LIAS state."
                    )
                }
            }
        }
    }

    private suspend fun refreshEffectiveStatuses(
        devices: List<Device>,
        tags: List<com.lias.remote.core.models.Tag>
    ) {
        val deviceStatusMap =
            loadDeviceStatuses(devices)

        val tagStatusMap =
            loadTagStatuses(tags)

        _state.update {
            it.copy(
                deviceEffectiveStatuses =
                    deviceStatusMap,
                tagEffectiveStatuses =
                    tagStatusMap
            )
        }
    }

    /**
     * Bounded parallelism avoids the previous one-request-at-a-time
     * behavior while preventing an uncontrolled request explosion.
     */
    private suspend fun loadDeviceStatuses(
        devices: List<Device>
    ): Map<String, EffectiveStatus> {
        val result =
            mutableMapOf<String, EffectiveStatus>()

        devices
            .filter { it.pdid.isNotBlank() }
            .chunked(EFFECTIVE_STATUS_BATCH_SIZE)
            .forEach { batch ->

                val batchResults =
                    coroutineScope {
                        batch.map { device ->
                            async {
                                device.pdid to
                                    api.getDeviceEffectiveStatus(
                                        device.pdid
                                    )
                            }
                        }.awaitAll()
                    }

                batchResults.forEach { (pdid, statusResult) ->
                    if (statusResult is ApiResult.Success) {
                        result[pdid] =
                            statusResult.data
                    }
                }
            }

        return result
    }

    private suspend fun loadTagStatuses(
        tags: List<com.lias.remote.core.models.Tag>
    ): Map<String, EffectiveStatus> {
        val result =
            mutableMapOf<String, EffectiveStatus>()

        tags
            .filter { it.id.isNotBlank() }
            .chunked(EFFECTIVE_STATUS_BATCH_SIZE)
            .forEach { batch ->

                val batchResults =
                    coroutineScope {
                        batch.map { tag ->
                            async {
                                tag.id to
                                    api.getTagEffectiveStatus(
                                        tag.id
                                    )
                            }
                        }.awaitAll()
                    }

                batchResults.forEach { (tagId, statusResult) ->
                    if (statusResult is ApiResult.Success) {
                        result[tagId] =
                            statusResult.data
                    }
                }
            }

        return result
    }

    private fun firstMeaningfulError(
        vararg results: ApiResult<*>
    ): String? {
        results.forEach { result ->
            val message =
                when (result) {
                    is ApiResult.AuthenticationError ->
                        result.message

                    is ApiResult.HttpError ->
                        result.message

                    is ApiResult.NetworkError ->
                        result.cause.message

                    is ApiResult.SerializationError ->
                        "Received an invalid response from the LIAS server."

                    is ApiResult.ConflictError ->
                        result.message

                    is ApiResult.Success<*> ->
                        null
                }

            if (!message.isNullOrBlank()) {
                return message
            }
        }

        return null
    }

    // ----------------------------------------------------------------
    // SSE reconciliation
    // ----------------------------------------------------------------

    private suspend fun collectSseEvents() {
        sse.events.collect { event ->

            val now =
                System.currentTimeMillis()

            val isReplayPhase =
                lastSseConnectedTime > 0L &&
                    now - lastSseConnectedTime <
                    REPLAY_SUPPRESSION_WINDOW_MS

            val toastKey =
                "${event.type}:${event.deviceID}"

            val lastToastTime =
                recentToastMap[toastKey] ?: 0L

            val isDuplicateToast =
                now - lastToastTime <
                    DUPLICATE_TOAST_WINDOW_MS

            val shouldShowToast =
                !isReplayPhase &&
                    !isDuplicateToast

            when (event.type) {

                EventConstants.DEVICE_ADDED -> {
                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (shouldShowToast) {
                        recentToastMap[toastKey] = now

                        tryEmitUiEvent(
                            UiEvent.ShowSnackbar(
                                "New device discovered: " +
                                    event.deviceID.takeLast(8)
                            )
                        )
                    }
                }

                EventConstants.DEVICE_ONLINE -> {
                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (shouldShowToast) {
                        recentToastMap[toastKey] = now

                        val confirmedBy =
                            event.payload?.let {
                                try {
                                    json.decodeFromJsonElement<DeviceEventPayload>(
                                        it
                                    ).safeConfirmedBy
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }.orEmpty()

                        val suffix =
                            if (confirmedBy.isNotEmpty()) {
                                " · ${confirmedBy.size} sources"
                            } else {
                                ""
                            }

                        tryEmitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Device online: " +
                                    event.deviceID.takeLast(8) +
                                    suffix
                            )
                        )
                    }
                }

                EventConstants.DEVICE_OFFLINE -> {
                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (shouldShowToast) {
                        recentToastMap[toastKey] = now

                        tryEmitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Device offline: " +
                                    event.deviceID.takeLast(8)
                            )
                        )
                    }
                }

                EventConstants.EFFECTIVE_STATUS_CHANGED -> {
                    refreshEffectiveStatusIfNeeded()
                }

                EventConstants.HOSTNAME_CHANGED,
                EventConstants.FINGERPRINT_UPDATED,
                EventConstants.IP_CHANGED,
                EventConstants.MAC_CHANGED -> {
                    refreshSingleDevice(
                        event.deviceID
                    )
                }

                EventConstants.DEVICE_REMOVED -> {
                    _state.update {
                        it.copy(
                            devices =
                                it.devices.filterNot {
                                    device ->
                                    device.pdid ==
                                        event.deviceID
                                },
                            deviceEffectiveStatuses =
                                it.deviceEffectiveStatuses
                                    .filterKeys {
                                        it != event.deviceID
                                    }
                        )
                    }
                }

                EventConstants.DEVICE_REIDENTIFIED -> {
                    val payload =
                        event.payload?.let {
                            try {
                                json.decodeFromJsonElement<DeviceReidentifiedPayload>(
                                    it
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }

                    if (payload != null) {
                        _state.update {
                            it.copy(
                                devices =
                                    it.devices.filterNot { device ->
                                        device.pdid ==
                                            payload.oldPdid
                                    },
                                deviceEffectiveStatuses =
                                    it.deviceEffectiveStatuses
                                        .filterKeys {
                                            it != payload.oldPdid
                                        }
                            )
                        }

                        refreshSingleDevice(
                            payload.newPdid
                        )

                        if (shouldShowToast) {
                            recentToastMap[toastKey] = now

                            tryEmitUiEvent(
                                UiEvent.ShowSnackbar(
                                    "Device identity updated: " +
                                        payload.newPdid.takeLast(8)
                                )
                            )
                        }
                    }
                }

                EventConstants.SECURITY_ALERT -> {
                    val payload =
                        event.payload?.let {
                            try {
                                json.decodeFromJsonElement<SecurityAlertPayload>(
                                    it
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }

                    val details =
                        payload?.details
                            ?.takeIf { it.isNotBlank() }
                            ?: "A network security anomaly was detected."

                    tryEmitUiEvent(
                        UiEvent.ShowSecurityAlert(
                            details = details,
                            pdid = payload?.pdid.orEmpty()
                        )
                    )

                    tryEmitUiEvent(
                        UiEvent.ShowSnackbarError(
                            "Security alert: $details"
                        )
                    )
                }

                EventConstants.PING -> {
                    // Heartbeat has no UI-state mutation.
                }
            }
        }
    }

    private fun refreshEffectiveStatusIfNeeded() {
        val now =
            System.currentTimeMillis()

        if (
            now - lastEffectiveStatusRefreshTime <
            EFFECTIVE_STATUS_REFRESH_DEBOUNCE_MS
        ) {
            return
        }

        lastEffectiveStatusRefreshTime = now

        scope.launch {
            refreshMutex.withLock {
                refreshEffectiveStatuses(
                    devices = _state.value.devices,
                    tags = _state.value.tags
                )
            }
        }
    }

    private suspend fun refreshSingleDevice(
        pdid: String
    ) {
        if (pdid.isBlank()) {
            return
        }

        val result =
            api.get<Device>(
                Endpoints.device(pdid)
            )

        if (result is ApiResult.Success) {
            val updatedDevice =
                result.data

            _state.update { current ->
                val existing =
                    current.devices
                        .indexOfFirst {
                            it.pdid == pdid
                        }

                val devices =
                    current.devices.toMutableList()

                if (existing >= 0) {
                    devices[existing] =
                        updatedDevice
                } else {
                    devices.add(updatedDevice)
                }

                current.copy(
                    devices = devices
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // State helpers
    // ----------------------------------------------------------------

    fun clearError() {
        _state.update {
            it.copy(errorMessage = null)
        }
    }

    fun setError(message: String?) {
        _state.update {
            it.copy(errorMessage = message)
        }
    }

    private companion object {
        const val EFFECTIVE_STATUS_BATCH_SIZE = 8

        const val EFFECTIVE_STATUS_REFRESH_DEBOUNCE_MS =
            750L

        const val REPLAY_SUPPRESSION_WINDOW_MS =
            2_500L

        const val DUPLICATE_TOAST_WINDOW_MS =
            3_000L
    }
}
