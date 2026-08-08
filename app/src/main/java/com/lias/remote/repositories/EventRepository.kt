// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 6.0.0
//
// Purpose:
//   Application-scoped source of truth for LIAS runtime state.
//
// Guarantees:
//   - Exactly one settings observer.
//   - Exactly one SSE event collector.
//   - Exactly one SSE connection-state collector.
//   - REST sync and SSE transport state remain independent.
//   - Initial failure is distinct from stale cached data.
//   - Full refreshes are serialized.
//   - Effective-status requests use bounded parallelism.
//   - SSE event bursts cannot create uncontrolled refresh storms.
//   - All ApiResult variants introduced in Batch 1 are handled.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.DeviceEventPayload
import com.lias.remote.core.models.DeviceReidentifiedPayload
import com.lias.remote.core.models.EffectiveStatus
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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

class EventRepository(
    internal val api: LiasApiClient,
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {

    internal val _state =
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
    private var lastSseConnectedTime =
        0L

    @Volatile
    private var lastEffectiveStatusRefreshTime =
        0L

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            observeConfiguration()
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
                connectionState =
                    ConnectionState.DISCONNECTED
            )
        }
    }

    // ----------------------------------------------------------------
    // UI events
    // ----------------------------------------------------------------

    suspend fun emitUiEvent(
        event: UiEvent
    ) {
        _uiEvents.emit(event)
    }

    fun tryEmitUiEvent(
        event: UiEvent
    ) {
        _uiEvents.tryEmit(event)
    }

    // ----------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------

    private suspend fun observeConfiguration() {
        coroutineScope {

            launch {
                settings.serverUrl.collectLatest { rawUrl ->

                    val url =
                        rawUrl.trim()

                    api.baseUrl =
                        url

                    sse.baseUrl =
                        url

                    sse.disconnect()

                    _state.update { current ->
                        current.copy(
                            connectionState =
                                ConnectionState.DISCONNECTED
                        )
                    }

                    if (url.isBlank()) {
                        clearRuntimeState()
                        return@collectLatest
                    }

                    sse.connect(scope)
                    refreshAll()
                }
            }

            launch {
                settings.authToken.collectLatest { rawToken ->

                    val token =
                        rawToken
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }

                    api.authToken =
                        token

                    sse.authToken =
                        token
                }
            }
        }
    }

    private fun clearRuntimeState() {
        _state.value =
            UiState(
                connectionState =
                    ConnectionState.DISCONNECTED,
                syncState =
                    SyncState.Idle
            )
    }

    // ----------------------------------------------------------------
    // SSE transport
    // ----------------------------------------------------------------

    private suspend fun collectConnectionState() {
        sse.connectionState.collect { connectionState ->

            _state.update { current ->
                current.copy(
                    connectionState =
                        connectionState
                )
            }

            if (
                connectionState ==
                ConnectionState.CONNECTED
            ) {
                lastSseConnectedTime =
                    System.currentTimeMillis()

                if (
                    _state.value.syncState
                    is SyncState.Stale
                ) {
                    scope.launch {
                        refreshAll()
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Authoritative full synchronization
    // ----------------------------------------------------------------

    suspend fun refreshAll() {
        if (api.baseUrl.trim().isBlank()) {
            return
        }

        refreshMutex.withLock {

            val before =
                _state.value

            val previouslyUsable =
                before.isInitialLoaded ||
                    before.syncState.hasUsableData

            _state.update {
                it.copy(
                    syncState =
                        SyncState.Loading,
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
                            throw SyncFailure(
                                resultMessage(
                                    devicesResult,
                                    "Unable to load devices."
                                )
                            )
                    }

                val (
                    tagsResult,
                    policiesResult,
                    schedulesResult,
                    statsResult
                ) = coroutineScope {

                    val tags =
                        async {
                            api.get<List<Tag>>(
                                Endpoints.TAGS
                            )
                        }

                    val policies =
                        async {
                            api.get<List<Policy>>(
                                Endpoints.POLICIES
                            )
                        }

                    val schedules =
                        async {
                            api.get<List<Schedule>>(
                                Endpoints.SCHEDULES
                            )
                        }

                    val stats =
                        async {
                            api.get<NetworkStats>(
                                Endpoints.STATS
                            )
                        }

                    listOf(
                        tags.await(),
                        policies.await(),
                        schedules.await(),
                        stats.await()
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val loadedTags =
                    (tagsResult as? ApiResult.Success<List<Tag>>)
                        ?.data
                        ?: before.tags

                @Suppress("UNCHECKED_CAST")
                val loadedPolicies =
                    (policiesResult as? ApiResult.Success<List<Policy>>)
                        ?.data
                        ?: before.policies

                @Suppress("UNCHECKED_CAST")
                val loadedSchedules =
                    (schedulesResult as? ApiResult.Success<List<Schedule>>)
                        ?.data
                        ?: before.schedules

                @Suppress("UNCHECKED_CAST")
                val loadedStats =
                    (statsResult as? ApiResult.Success<NetworkStats>)
                        ?.data
                        ?: before.stats

                _state.update {
                    it.copy(
                        devices = devices,
                        tags = loadedTags,
                        policies = loadedPolicies,
                        schedules = loadedSchedules,
                        stats = loadedStats
                    )
                }

                refreshEffectiveStatuses(
                    devices = devices,
                    tags = loadedTags
                )

                val synchronizedAt =
                    System.currentTimeMillis()

                val partialFailure =
                    firstMeaningfulError(
                        tagsResult,
                        policiesResult,
                        schedulesResult,
                        statsResult
                    )

                if (partialFailure == null) {
                    _state.update {
                        it.copy(
                            syncState =
                                SyncState.Ready(
                                    synchronizedAt
                                ),
                            isInitialLoaded = true,
                            isRefreshing = false,
                            lastSuccessfulSyncAt =
                                synchronizedAt,
                            errorMessage = null
                        )
                    }
                } else {
                    /*
                     * Devices were successfully loaded, so the app has
                     * usable state. Secondary-resource failure is
                     * represented as stale/partial rather than a blank
                     * fatal screen.
                     */
                    _state.update {
                        it.copy(
                            syncState =
                                SyncState.Stale(
                                    synchronizedAt =
                                        synchronizedAt,
                                    message =
                                        partialFailure
                                ),
                            isInitialLoaded = true,
                            isRefreshing = false,
                            lastSuccessfulSyncAt =
                                synchronizedAt,
                            errorMessage =
                                partialFailure
                        )
                    }
                }

            } catch (failure: SyncFailure) {

                val message =
                    failure.message
                        ?: "Unable to synchronize with LIAS."

                _state.update { current ->

                    if (previouslyUsable) {
                        current.copy(
                            syncState =
                                SyncState.Stale(
                                    synchronizedAt =
                                        before.lastSuccessfulSyncAt,
                                    message = message
                                ),
                            isRefreshing = false,
                            errorMessage = message
                        )
                    } else {
                        current.copy(
                            syncState =
                                SyncState.Failed(
                                    message
                                ),
                            isInitialLoaded = false,
                            isRefreshing = false,
                            errorMessage = message
                        )
                    }
                }

            } catch (error: Exception) {

                val message =
                    error.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to synchronize with LIAS."

                _state.update { current ->

                    if (previouslyUsable) {
                        current.copy(
                            syncState =
                                SyncState.Stale(
                                    synchronizedAt =
                                        before.lastSuccessfulSyncAt,
                                    message = message
                                ),
                            isRefreshing = false,
                            errorMessage = message
                        )
                    } else {
                        current.copy(
                            syncState =
                                SyncState.Failed(
                                    message
                                ),
                            isInitialLoaded = false,
                            isRefreshing = false,
                            errorMessage = message
                        )
                    }
                }
            }
        }
    }

    private suspend fun refreshEffectiveStatuses(
        devices: List<Device>,
        tags: List<Tag>
    ) {
        val deviceStatuses =
            mutableMapOf<String, EffectiveStatus>()

        devices
            .filter {
                it.pdid.isNotBlank()
            }
            .chunked(EFFECTIVE_STATUS_BATCH_SIZE)
            .forEach { batch ->

                val results =
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

                results.forEach { (pdid, result) ->
                    if (
                        result
                        is ApiResult.Success
                    ) {
                        deviceStatuses[pdid] =
                            result.data
                    }
                }
            }

        val tagStatuses =
            mutableMapOf<String, EffectiveStatus>()

        tags
            .filter {
                it.id.isNotBlank()
            }
            .chunked(EFFECTIVE_STATUS_BATCH_SIZE)
            .forEach { batch ->

                val results =
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

                results.forEach { (tagId, result) ->
                    if (
                        result
                        is ApiResult.Success
                    ) {
                        tagStatuses[tagId] =
                            result.data
                    }
                }
            }

        _state.update {
            it.copy(
                deviceEffectiveStatuses =
                    deviceStatuses,
                tagEffectiveStatuses =
                    tagStatuses
            )
        }
    }

    // ----------------------------------------------------------------
    // SSE reconciliation
    // ----------------------------------------------------------------

    private suspend fun collectSseEvents() {
        sse.events.collect { event ->

            val now =
                System.currentTimeMillis()

            val replayPhase =
                lastSseConnectedTime > 0 &&
                    now - lastSseConnectedTime <
                    REPLAY_SUPPRESSION_WINDOW_MS

            val toastKey =
                "${event.type}:${event.deviceID}"

            val lastToast =
                recentToastMap[toastKey]
                    ?: 0L

            val duplicateToast =
                now - lastToast <
                    DUPLICATE_TOAST_WINDOW_MS

            val showToast =
                !replayPhase &&
                    !duplicateToast

            when (event.type) {

                EventConstants.DEVICE_ADDED -> {
                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (showToast) {
                        recentToastMap[toastKey] =
                            now

                        tryEmitUiEvent(
                            UiEvent.ShowSnackbar(
                                "New device discovered"
                            )
                        )
                    }
                }

                EventConstants.DEVICE_ONLINE -> {
                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (showToast) {
                        recentToastMap[toastKey] =
                            now

                        val confirmations =
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
                            if (
                                confirmations.isNotEmpty()
                            ) {
                                " · ${confirmations.size} sources"
                            } else {
                                ""
                            }

                        tryEmitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Device online$suffix"
                            )
                        )
                    }
                }

                EventConstants.DEVICE_OFFLINE -> {
                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (showToast) {
                        recentToastMap[toastKey] =
                            now

                        tryEmitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Device offline"
                            )
                        )
                    }
                }

                EventConstants.HOSTNAME_CHANGED,
                EventConstants.FINGERPRINT_UPDATED,
                EventConstants.IP_CHANGED,
                EventConstants.MAC_CHANGED -> {
                    refreshSingleDevice(
                        event.deviceID
                    )
                }

                EventConstants.EFFECTIVE_STATUS_CHANGED -> {
                    scheduleEffectiveStatusRefresh()
                }

                EventConstants.DEVICE_REMOVED -> {
                    _state.update { current ->
                        current.copy(
                            devices =
                                current.devices
                                    .filterNot {
                                        it.pdid ==
                                            event.deviceID
                                    },
                            deviceEffectiveStatuses =
                                current
                                    .deviceEffectiveStatuses
                                    .filterKeys {
                                        it !=
                                            event.deviceID
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
                        _state.update { current ->
                            current.copy(
                                devices =
                                    current.devices
                                        .filterNot {
                                            it.pdid ==
                                                payload.oldPdid
                                        },
                                deviceEffectiveStatuses =
                                    current
                                        .deviceEffectiveStatuses
                                        .filterKeys {
                                            it !=
                                                payload.oldPdid
                                        }
                            )
                        }

                        refreshSingleDevice(
                            payload.newPdid
                        )

                        if (showToast) {
                            recentToastMap[toastKey] =
                                now

                            tryEmitUiEvent(
                                UiEvent.ShowSnackbar(
                                    "Device identity updated"
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
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "A network security anomaly was detected."

                    tryEmitUiEvent(
                        UiEvent.ShowSecurityAlert(
                            details = details,
                            pdid =
                                payload?.pdid.orEmpty()
                        )
                    )

                    tryEmitUiEvent(
                        UiEvent.ShowSnackbarError(
                            "Security alert: $details"
                        )
                    )
                }

                EventConstants.PING -> Unit
            }
        }
    }

    private fun scheduleEffectiveStatusRefresh() {
        val now =
            System.currentTimeMillis()

        if (
            now -
                lastEffectiveStatusRefreshTime <
            EFFECTIVE_STATUS_REFRESH_DEBOUNCE_MS
        ) {
            return
        }

        lastEffectiveStatusRefreshTime =
            now

        scope.launch {
            val snapshot =
                _state.value

            refreshEffectiveStatuses(
                devices =
                    snapshot.devices,
                tags =
                    snapshot.tags
            )
        }
    }

    private suspend fun refreshSingleDevice(
        pdid: String
    ) {
        if (pdid.isBlank()) {
            return
        }

        when (
            val result =
                api.get<Device>(
                    Endpoints.device(pdid)
                )
        ) {
            is ApiResult.Success -> {
                _state.update { current ->

                    val devices =
                        current.devices
                            .toMutableList()

                    val index =
                        devices.indexOfFirst {
                            it.pdid == pdid
                        }

                    if (index >= 0) {
                        devices[index] =
                            result.data
                    } else {
                        devices.add(
                            result.data
                        )
                    }

                    current.copy(
                        devices = devices
                    )
                }
            }

            else -> Unit
        }
    }

    // ----------------------------------------------------------------
    // State helpers
    // ----------------------------------------------------------------

    fun clearError() {
        _state.update {
            it.copy(
                errorMessage = null
            )
        }
    }

    fun setError(
        message: String?
    ) {
        _state.update {
            it.copy(
                errorMessage = message
            )
        }
    }

    private fun firstMeaningfulError(
        vararg results: ApiResult<*>
    ): String? {
        results.forEach { result ->

            if (
                result !is ApiResult.Success
            ) {
                return resultMessage(
                    result,
                    "Some LIAS data could not be refreshed."
                )
            }
        }

        return null
    }

    private fun resultMessage(
        result: ApiResult<*>,
        fallback: String
    ): String =
        when (result) {
            is ApiResult.Success<*> ->
                fallback

            is ApiResult.AuthenticationError ->
                result.message

            is ApiResult.HttpError ->
                result.message

            is ApiResult.ConflictError ->
                result.message

            is ApiResult.NetworkError ->
                result.cause.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: fallback

            is ApiResult.SerializationError ->
                "The LIAS server returned an invalid response."
        }

    private class SyncFailure(
        message: String
    ) : Exception(message)

    private companion object {

        const val EFFECTIVE_STATUS_BATCH_SIZE =
            8

        const val EFFECTIVE_STATUS_REFRESH_DEBOUNCE_MS =
            750L

        const val REPLAY_SUPPRESSION_WINDOW_MS =
            2_500L

        const val DUPLICATE_TOAST_WINDOW_MS =
            3_000L
    }
}
