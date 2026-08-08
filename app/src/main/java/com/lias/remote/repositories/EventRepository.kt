// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 5.0.0
//
// Purpose:
//   Central real-time LIAS state repository.
//
// Batch 5 changes:
//
//   1. SSE lifecycle and REST synchronization are separated.
//   2. start() no longer blindly starts an SSE connection.
//   3. SettingsRepository remains the authority for server configuration.
//   4. Initial synchronization is explicitly Loading/Ready/Failed.
//   5. Failed refreshes do not destroy usable cached data.
//   6. A later refresh failure produces Stale state.
//   7. Successful synchronization records a timestamp.
//   8. Concurrent refreshAll() calls are serialized.
//   9. Connection-state collection starts exactly once.
//  10. SSE events continue to update the cached domain state.
//  11. Existing EventRepositoryActions extensions remain compatible.
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.ConcurrentHashMap

class EventRepository(
    internal val api: LiasApiClient,
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {

    // ----------------------------------------------------------------
    // Observable state
    // ----------------------------------------------------------------

    internal val _state =
        MutableStateFlow(
            UiState()
        )

    val state:
        StateFlow<UiState> =
        _state.asStateFlow()

    internal val _uiEvents =
        MutableSharedFlow<UiEvent>(
            replay = 0,
            extraBufferCapacity = 64
        )

    val uiEvents:
        SharedFlow<UiEvent> =
        _uiEvents.asSharedFlow()

    // ----------------------------------------------------------------
    // Internal runtime state
    // ----------------------------------------------------------------

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default
        )

    private val refreshMutex =
        Mutex()

    @Volatile
    private var lastSseConnectedTime:
        Long = 0L

    private val recentToastMap =
        ConcurrentHashMap<String, Long>()

    @Volatile
    private var started =
        false

    // ----------------------------------------------------------------
    // Configuration observers
    // ----------------------------------------------------------------

    init {

        scope.launch {

            settings.serverUrl.collectLatest { url ->

                api.baseUrl =
                    url

                sse.baseUrl =
                    url

                sse.disconnect()

                _state.value =
                    _state.value.copy(
                        connectionState =
                            ConnectionState.DISCONNECTED
                    )

                if (
                    url.isBlank()
                ) {

                    _state.value =
                        _state.value.copy(
                            syncState =
                                SyncState.Idle,
                            isInitialLoaded = false,
                            errorMessage = null
                        )

                    return@collectLatest
                }

                /*
                 * The URL in DataStore represents a verified/persisted
                 * connection configuration after Batch 4.
                 *
                 * Start the live stream and perform the initial REST
                 * synchronization from this single authority.
                 */
                sse.connect(
                    scope
                )

                refreshAll()
            }
        }

        scope.launch {

            settings.authToken.collectLatest { token ->

                api.authToken =
                    token

                sse.authToken =
                    token
            }
        }
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    /**
     * Starts repository collectors.
     *
     * This function is intentionally idempotent.
     *
     * The serverUrl DataStore observer is responsible for starting
     * the actual SSE connection. This prevents a duplicate connection
     * to the default localhost URL during application startup.
     */
    fun start() {

        if (started) {
            return
        }

        started =
            true

        scope.launch {
            collectSseEvents()
        }

        scope.launch {
            collectConnectionState()
        }
    }

    // ----------------------------------------------------------------
    // SSE connection state
    // ----------------------------------------------------------------

    private suspend fun collectConnectionState() {

        sse.connectionState.collect { connState ->

            val current =
                _state.value

            _state.value =
                current.copy(
                    connectionState =
                        connState
                )

            if (
                connState ==
                ConnectionState.CONNECTED
            ) {

                lastSseConnectedTime =
                    System.currentTimeMillis()

                /*
                 * If we already have cached data, a reconnect means the
                 * live stream has returned. Keep the data and, if it was
                 * stale, request a fresh primary synchronization.
                 */
                if (
                    current.syncState is
                        SyncState.Stale
                ) {

                    scope.launch {
                        refreshAll()
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Primary synchronization
    // ----------------------------------------------------------------

    internal suspend fun refreshAll() {

        refreshMutex.withLock {

            if (
                api.baseUrl.isBlank()
            ) {
                return
            }

            val previous =
                _state.value

            val hasUsableCache =
                previous.syncState.hasUsableData ||
                    previous.isInitialLoaded

            _state.value =
                previous.copy(
                    syncState =
                        SyncState.Loading,
                    errorMessage = null
                )

            try {

                coroutineScope {

                    // ------------------------------------------------
                    // Primary device inventory
                    // ------------------------------------------------

                    val devicesResult =
                        api.get<DeviceListResponse>(
                            Endpoints.DEVICES
                        )

                    val currentDevices =
                        when (
                            devicesResult
                        ) {

                            is ApiResult.Success ->
                                devicesResult
                                    .data
                                    .devices

                            is ApiResult.HttpError ->
                                throw SyncException(
                                    "Unable to load devices: HTTP ${devicesResult.code}"
                                )

                            is ApiResult.NetworkError ->
                                throw SyncException(
                                    "Unable to load devices: ${devicesResult.cause.message ?: "network error"}"
                                )

                            is ApiResult.ConflictError ->
                                throw SyncException(
                                    "Unable to load devices."
                                )
                        }

                    _state.value =
                        _state.value.copy(
                            devices =
                                currentDevices
                        )

                    // ------------------------------------------------
                    // Secondary resources in parallel
                    // ------------------------------------------------

                    val tagsDeferred =
                        async {
                            api.get<List<Tag>>(
                                Endpoints.TAGS
                            )
                        }

                    val policiesDeferred =
                        async {
                            api.get<List<Policy>>(
                                Endpoints.POLICIES
                            )
                        }

                    val schedulesDeferred =
                        async {
                            api.get<List<Schedule>>(
                                Endpoints.SCHEDULES
                            )
                        }

                    val statsDeferred =
                        async {
                            api.get<NetworkStats>(
                                Endpoints.STATS
                            )
                        }

                    val tagsResult =
                        tagsDeferred.await()

                    val policiesResult =
                        policiesDeferred.await()

                    val schedulesResult =
                        schedulesDeferred.await()

                    val statsResult =
                        statsDeferred.await()

                    /*
                     * Secondary resource failures do not invalidate
                     * already loaded primary device data.
                     *
                     * Preserve the existing cache for an individual
                     * resource rather than replacing it with empty data.
                     */
                    val loadedTags =
                        (
                            tagsResult as?
                                ApiResult.Success
                            )?.data
                            ?: previous.tags

                    val loadedPolicies =
                        (
                            policiesResult as?
                                ApiResult.Success
                            )?.data
                            ?: previous.policies

                    val loadedSchedules =
                        (
                            schedulesResult as?
                                ApiResult.Success
                            )?.data
                            ?: previous.schedules

                    val loadedStats =
                        (
                            statsResult as?
                                ApiResult.Success
                            )?.data
                            ?: previous.stats

                    _state.value =
                        _state.value.copy(
                            tags =
                                loadedTags,

                            policies =
                                loadedPolicies,

                            schedules =
                                loadedSchedules,

                            stats =
                                loadedStats
                        )

                    // ------------------------------------------------
                    // Effective status enrichment
                    // ------------------------------------------------

                    launch {

                        val deviceStatusMap =
                            mutableMapOf<
                                String,
                                EffectiveStatus
                            >()

                        currentDevices.forEach { device ->

                            if (
                                device.pdid.isBlank()
                            ) {
                                return@forEach
                            }

                            when (
                                val result =
                                    api.getDeviceEffectiveStatus(
                                        device.pdid
                                    )
                            ) {

                                is ApiResult.Success ->
                                    deviceStatusMap[
                                        device.pdid
                                    ] =
                                        result.data

                                else -> Unit
                            }
                        }

                        val tagStatusMap =
                            mutableMapOf<
                                String,
                                EffectiveStatus
                            >()

                        loadedTags.forEach { tag ->

                            when (
                                val result =
                                    api.getTagEffectiveStatus(
                                        tag.id
                                    )
                            ) {

                                is ApiResult.Success ->
                                    tagStatusMap[
                                        tag.id
                                    ] =
                                        result.data

                                else -> Unit
                            }
                        }

                        _state.value =
                            _state.value.copy(
                                deviceEffectiveStatuses =
                                    deviceStatusMap,

                                tagEffectiveStatuses =
                                    tagStatusMap
                            )
                    }
                }

                val synchronizedAt =
                    System.currentTimeMillis()

                _state.value =
                    _state.value.copy(
                        isInitialLoaded = true,

                        lastSuccessfulSyncAt =
                            synchronizedAt,

                        syncState =
                            SyncState.Ready(
                                synchronizedAt
                            ),

                        errorMessage = null
                    )

            } catch (
                error: SyncException
            ) {

                val message =
                    error.message
                        ?: "Unable to synchronize with LIAS."

                if (
                    hasUsableCache
                ) {

                    _state.value =
                        _state.value.copy(
                            syncState =
                                SyncState.Stale(
                                    synchronizedAt =
                                        previous.lastSuccessfulSyncAt,

                                    message =
                                        message
                                ),

                            errorMessage =
                                message
                        )

                } else {

                    _state.value =
                        _state.value.copy(
                            syncState =
                                SyncState.Failed(
                                    message
                                ),

                            isInitialLoaded =
                                false,

                            errorMessage =
                                message
                        )
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // SSE events
    // ----------------------------------------------------------------

    private suspend fun collectSseEvents() {

        sse.events.collect { event ->

            val now =
                System.currentTimeMillis()

            val isReplayPhase =
                (
                    now -
                        lastSseConnectedTime
                    ) < 2500L

            val toastKey =
                "${event.type}:${event.deviceID}"

            val lastToastTime =
                recentToastMap[
                    toastKey
                ] ?: 0L

            val isDuplicateToast =
                (
                    now -
                        lastToastTime
                    ) < 3000L

            val shouldShowToast =
                !isReplayPhase &&
                    !isDuplicateToast

            when (
                event.type
            ) {

                EventConstants.DEVICE_ADDED -> {

                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (
                        shouldShowToast
                    ) {

                        recentToastMap[
                            toastKey
                        ] = now

                        _uiEvents.emit(
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

                    if (
                        shouldShowToast
                    ) {

                        recentToastMap[
                            toastKey
                        ] = now

                        val confirmedBy =
                            event.payload?.let {
                                try {
                                    json.decodeFromJsonElement<
                                        DeviceEventPayload
                                    >(it)
                                        .safeConfirmedBy
                                } catch (
                                    _: Exception
                                ) {
                                    emptyList()
                                }
                            }
                                ?: emptyList()

                        val verifiedText =
                            if (
                                confirmedBy.isNotEmpty()
                            ) {
                                " · ${confirmedBy.size} sources"
                            } else {
                                ""
                            }

                        _uiEvents.emit(
                            UiEvent.ShowSnackbar(
                                "Device online$verifiedText"
                            )
                        )
                    }
                }

                EventConstants.DEVICE_OFFLINE -> {

                    refreshSingleDevice(
                        event.deviceID
                    )

                    if (
                        shouldShowToast
                    ) {

                        recentToastMap[
                            toastKey
                        ] = now

                        _uiEvents.emit(
                            UiEvent.ShowSnackbar(
                                "Device offline"
                            )
                        )
                    }
                }

                EventConstants.EFFECTIVE_STATUS_CHANGED -> {

                    refreshAll()
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

                    _state.value =
                        _state.value.copy(
                            devices =
                                _state.value
                                    .devices
                                    .filterNot {
                                        it.pdid ==
                                            event.deviceID
                                    }
                        )
                }

                EventConstants.DEVICE_REIDENTIFIED -> {

                    val payload =
                        event.payload?.let {

                            try {
                                json.decodeFromJsonElement<
                                    DeviceReidentifiedPayload
                                >(it)
                            } catch (
                                _: Exception
                            ) {
                                null
                            }
                        }

                    if (
                        payload != null
                    ) {

                        _state.value =
                            _state.value.copy(
                                devices =
                                    _state.value
                                        .devices
                                        .filterNot {
                                            it.pdid ==
                                                payload.oldPdid
                                        }
                            )

                        refreshSingleDevice(
                            payload.newPdid
                        )

                        if (
                            shouldShowToast
                        ) {

                            recentToastMap[
                                toastKey
                            ] = now

                            _uiEvents.emit(
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
                                json.decodeFromJsonElement<
                                    SecurityAlertPayload
                                >(it)
                            } catch (
                                _: Exception
                            ) {
                                null
                            }
                        }

                    val details =
                        payload?.details
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Anomaly detected"

                    _uiEvents.emit(
                        UiEvent.ShowSecurityAlert(
                            details
                        )
                    )

                    _uiEvents.emit(
                        UiEvent.ShowSnackbarError(
                            "Security alert: $details"
                        )
                    )
                }

                EventConstants.PING -> {
                    // PING is transport-level keepalive.
                    // No domain refresh is required.
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Individual device refresh
    // ----------------------------------------------------------------

    private suspend fun refreshSingleDevice(
        pdid: String
    ) {

        if (
            pdid.isBlank()
        ) {
            return
        }

        when (
            val result =
                api.get<Device>(
                    Endpoints.device(
                        pdid
                    )
                )
        ) {

            is ApiResult.Success -> {

                val updatedDevice =
                    result.data

                val currentDevices =
                    _state.value
                        .devices
                        .toMutableList()

                val index =
                    currentDevices
                        .indexOfFirst {
                            it.pdid == pdid
                        }

                if (
                    index != -1
                ) {

                    currentDevices[
                        index
                    ] =
                        updatedDevice

                } else {

                    currentDevices.add(
                        updatedDevice
                    )
                }

                _state.value =
                    _state.value.copy(
                        devices =
                            currentDevices
                    )
            }

            else -> Unit
        }
    }

    // ----------------------------------------------------------------
    // Internal exception
    // ----------------------------------------------------------------

    private class SyncException(
        message: String
    ) : Exception(
        message
    )
}
