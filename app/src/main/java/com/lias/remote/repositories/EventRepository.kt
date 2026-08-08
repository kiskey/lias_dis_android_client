// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 25.0.0
//
// Purpose:
//   Canonical server-authoritative application repository.
//
// Batch 25:
//   - start() is idempotent.
//   - No duplicate SSE connect path.
//   - Persisted SettingsRepository controls endpoint lifecycle.
//   - Public refreshAll() for explicit UI refresh.
//   - Authentication/serialization/network errors propagate into state.
//   - Successful refresh clears stale repository error.
//   - EffectiveStatus remains authoritative.
//   - SSE replay reconnect behavior remains owned by LiasSseClient.
//   - Removes emoji-as-status UI events.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.diagnostics.ErrorPresentation
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class EventRepository(
    internal val api: LiasApiClient,
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {

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

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default
        )

    private val started =
        AtomicBoolean(
            false
        )

    @Volatile
    private var lastSseConnectedTime =
        0L

    private val recentToastMap =
        ConcurrentHashMap<String, Long>()

    /**
     * Repository startup is explicitly idempotent.
     *
     * Activity recreation, duplicated composable entry or repeated
     * ViewModel initialization must never create multiple SSE/event
     * collectors.
     */
    fun start() {

        if (
            !started.compareAndSet(
                false,
                true
            )
        ) {
            return
        }

        observeAuthentication()
        observeServerConfiguration()
        observeConnectionState()

        scope.launch {
            collectSseEvents()
        }
    }

    /**
     * Auth changes do not themselves decide whether a connection
     * should exist. They simply update clients.
     *
     * LiasSseClient will cancel an active call when credentials change,
     * causing its existing reconnect loop to reopen with the new token.
     */
    private fun observeAuthentication() {

        scope.launch {

            settings.authToken
                .distinctUntilChanged()
                .collect {
                    token ->

                    api.authToken =
                        token
                            ?.trim()
                            ?.ifBlank {
                                null
                            }

                    sse.authToken =
                        token
                            ?.trim()
                            ?.ifBlank {
                                null
                            }
                }
        }
    }

    /**
     * Persisted server configuration is the sole authority for the
     * existence of the live SSE connection.
     */
    private fun observeServerConfiguration() {

        scope.launch {

            settings.serverUrl
                .distinctUntilChanged()
                .collect {
                    rawUrl ->

                    val url =
                        rawUrl
                            .trim()
                            .trimEnd('/')

                    if (
                        url.isBlank()
                    ) {

                        api.baseUrl =
                            ""

                        sse.disconnect()

                        _state.value =
                            _state.value.copy(
                                connectionState =
                                    ConnectionState.DISCONNECTED,
                                isInitialLoaded =
                                    false,
                                errorMessage =
                                    null
                            )

                        return@collect
                    }

                    /*
                     * Setting baseUrl first is intentional:
                     * Batch 23 LiasSseClient resets Last-Event-ID only
                     * when the logical server actually changes.
                     */
                    api.baseUrl =
                        url

                    sse.baseUrl =
                        url

                    /*
                     * connect() itself safely replaces an old stream.
                     */
                    sse.connect(
                        scope
                    )

                    refreshAll()
                }
        }
    }

    private fun observeConnectionState() {

        scope.launch {

            sse.connectionState
                .collect {
                    connectionState ->

                    _state.value =
                        _state.value.copy(
                            connectionState =
                                connectionState
                        )

                    if (
                        connectionState ==
                        ConnectionState.CONNECTED
                    ) {

                        lastSseConnectedTime =
                            System.currentTimeMillis()
                    }
                }
        }
    }

    /**
     * Full authoritative REST synchronization.
     *
     * This is public because pull-to-refresh / retry UI should request a
     * repository refresh without reaching into networking internals.
     */
    suspend fun refreshAll() {

        if (
            api.baseUrl
                .isBlank()
        ) {
            return
        }

        coroutineScope {

            val devicesDeferred =
                async {
                    api.get<DeviceListResponse>(
                        Endpoints.DEVICES
                    )
                }

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

            val devicesResult =
                devicesDeferred.await()

            val tagsResult =
                tagsDeferred.await()

            val policiesResult =
                policiesDeferred.await()

            val schedulesResult =
                schedulesDeferred.await()

            val statsResult =
                statsDeferred.await()

            val current =
                _state.value

            val loadedDevices =
                (
                    devicesResult as?
                        ApiResult.Success
                    )
                    ?.data
                    ?.devices
                    ?: current.devices

            val loadedTags =
                (
                    tagsResult as?
                        ApiResult.Success
                    )
                    ?.data
                    ?: current.tags

            val loadedPolicies =
                (
                    policiesResult as?
                        ApiResult.Success
                    )
                    ?.data
                    ?: current.policies

            val loadedSchedules =
                (
                    schedulesResult as?
                        ApiResult.Success
                    )
                    ?.data
                    ?: current.schedules

            val loadedStats =
                (
                    statsResult as?
                        ApiResult.Success
                    )
                    ?.data
                    ?: current.stats

            val refreshFailure =
                firstFailure(
                    devicesResult,
                    tagsResult,
                    policiesResult,
                    schedulesResult,
                    statsResult
                )

            _state.value =
                current.copy(
                    devices =
                        loadedDevices,
                    tags =
                        loadedTags,
                    policies =
                        loadedPolicies,
                    schedules =
                        loadedSchedules,
                    stats =
                        loadedStats,
                    isInitialLoaded =
                        true,
                    errorMessage =
                        refreshFailure
                            ?.let {
                                ErrorPresentation
                                    .from(
                                        it
                                    )
                                    .message
                            }
                )

            /*
             * Effective status is intentionally a second phase:
             * inventory appears quickly, while authoritative access
             * state fills immediately afterward.
             */
            val deviceStatuses =
                linkedMapOf<
                    String,
                    EffectiveStatus
                >()

            for (
                device in
                loadedDevices
            ) {

                when (
                    val result =
                        api.getDeviceEffectiveStatus(
                            device.pdid
                        )
                ) {

                    is ApiResult.Success ->

                        deviceStatuses[
                            device.pdid
                        ] =
                            result.data

                    else -> {
                        /*
                         * Do not fabricate access state.
                         * Missing entry means "status unavailable".
                         */
                    }
                }
            }

            val tagStatuses =
                linkedMapOf<
                    String,
                    EffectiveStatus
                >()

            for (
                tag in
                loadedTags
            ) {

                when (
                    val result =
                        api.getTagEffectiveStatus(
                            tag.id
                        )
                ) {

                    is ApiResult.Success ->

                        tagStatuses[
                            tag.id
                        ] =
                            result.data

                    else -> {
                        // Same fail-closed presentation rule.
                    }
                }
            }

            _state.value =
                _state.value.copy(
                    deviceEffectiveStatuses =
                        deviceStatuses,
                    tagEffectiveStatuses =
                        tagStatuses
                )
        }
    }

    private fun firstFailure(
        vararg results:
            ApiResult<*>
    ): ApiResult<*>? =
        results.firstOrNull {
            it !is
                ApiResult.Success
        }

    private suspend fun collectSseEvents() {

        sse.events
            .collect {
                event ->

                val now =
                    System.currentTimeMillis()

                /*
                 * Suppress noisy banners while the Batch-23 replay
                 * cursor is recovering events after reconnect.
                 */
                val replayPhase =
                    (
                        now -
                            lastSseConnectedTime
                        ) <
                        REPLAY_QUIET_PERIOD_MS

                val toastKey =
                    "${event.type}:${event.deviceID}"

                val previousToast =
                    recentToastMap[
                        toastKey
                    ]
                        ?: 0L

                val duplicateToast =
                    (
                        now -
                            previousToast
                        ) <
                        TOAST_DEDUP_WINDOW_MS

                val mayNotify =
                    !replayPhase &&
                        !duplicateToast

                when (
                    event.type
                ) {

                    EventConstants.DEVICE_ADDED -> {

                        refreshSingleDevice(
                            event.deviceID
                        )

                        if (
                            mayNotify
                        ) {

                            recentToastMap[
                                toastKey
                            ] =
                                now

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
                            mayNotify
                        ) {

                            recentToastMap[
                                toastKey
                            ] =
                                now

                            val confirmedBy =
                                event.payload
                                    ?.let {
                                        payload ->

                                        try {

                                            json.decodeFromJsonElement<
                                                DeviceEventPayload
                                            >(
                                                payload
                                            )
                                                .safeConfirmedBy

                                        } catch (
                                            _: Exception
                                        ) {
                                            emptyList()
                                        }
                                    }
                                    .orEmpty()

                            val message =
                                if (
                                    confirmedBy
                                        .isEmpty()
                                ) {

                                    "Device is online"

                                } else {

                                    "Device is online · verified by ${confirmedBy.size} source${if (confirmedBy.size == 1) "" else "s"}"
                                }

                            _uiEvents.emit(
                                UiEvent.ShowSnackbar(
                                    message
                                )
                            )
                        }
                    }

                    EventConstants.DEVICE_OFFLINE -> {

                        refreshSingleDevice(
                            event.deviceID
                        )

                        if (
                            mayNotify
                        ) {

                            recentToastMap[
                                toastKey
                            ] =
                                now

                            _uiEvents.emit(
                                UiEvent.ShowSnackbar(
                                    "Device went offline"
                                )
                            )
                        }
                    }

                    EventConstants.EFFECTIVE_STATUS_CHANGED -> {

                        /*
                         * Policy/schedule/extension changes can affect
                         * multiple targets because precedence is global.
                         */
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
                                        },
                                deviceEffectiveStatuses =
                                    _state.value
                                        .deviceEffectiveStatuses -
                                        event.deviceID
                            )
                    }

                    EventConstants.DEVICE_REIDENTIFIED -> {

                        val payload =
                            event.payload
                                ?.let {
                                    element ->

                                    try {

                                        json.decodeFromJsonElement<
                                            DeviceReidentifiedPayload
                                        >(
                                            element
                                        )

                                    } catch (
                                        _: Exception
                                    ) {
                                        null
                                    }
                                }

                        if (
                            payload !=
                            null
                        ) {

                            _state.value =
                                _state.value.copy(
                                    devices =
                                        _state.value
                                            .devices
                                            .filterNot {
                                                it.pdid ==
                                                    payload.oldPdid
                                            },
                                    deviceEffectiveStatuses =
                                        _state.value
                                            .deviceEffectiveStatuses -
                                            payload.oldPdid
                                )

                            refreshSingleDevice(
                                payload.newPdid
                            )

                            if (
                                mayNotify
                            ) {

                                recentToastMap[
                                    toastKey
                                ] =
                                    now

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
                            event.payload
                                ?.let {
                                    element ->

                                    try {

                                        json.decodeFromJsonElement<
                                            SecurityAlertPayload
                                        >(
                                            element
                                        )

                                    } catch (
                                        _: Exception
                                    ) {
                                        null
                                    }
                                }

                        val details =
                            payload
                                ?.details
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "LIAS detected a network security anomaly."

                        _uiEvents.emit(
                            UiEvent.ShowSecurityAlert(
                                details
                            )
                        )
                    }

                    EventConstants.PING -> {
                        // Heartbeat only.
                    }
                }
            }
    }

    private suspend fun refreshSingleDevice(
        pdid: String
    ) {

        if (
            pdid.isBlank()
        ) {
            return
        }

        val deviceResult =
            api.get<Device>(
                Endpoints.device(
                    pdid
                )
            )

        if (
            deviceResult is
            ApiResult.Success
        ) {

            val updated =
                deviceResult.data

            val devices =
                _state.value
                    .devices
                    .toMutableList()

            val index =
                devices.indexOfFirst {
                    it.pdid ==
                        pdid
                }

            if (
                index >=
                0
            ) {

                devices[
                    index
                ] =
                    updated

            } else {

                devices.add(
                    updated
                )
            }

            var statuses =
                _state.value
                    .deviceEffectiveStatuses

            val statusResult =
                api.getDeviceEffectiveStatus(
                    pdid
                )

            if (
                statusResult is
                ApiResult.Success
            ) {

                statuses =
                    statuses +
                        (
                            pdid to
                                statusResult.data
                            )
            }

            _state.value =
                _state.value.copy(
                    devices =
                        devices,
                    deviceEffectiveStatuses =
                        statuses
                )
        }
    }

    companion object {

        private const val REPLAY_QUIET_PERIOD_MS =
            2_500L

        private const val TOAST_DEDUP_WINDOW_MS =
            3_000L
    }
}
