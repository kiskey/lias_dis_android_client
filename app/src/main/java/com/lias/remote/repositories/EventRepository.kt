// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepository.kt
// Version: 13.0.0
//
// Purpose:
//   Single source of truth for LIAS REST + SSE state.
//
// Transport corrections:
//   - start() is idempotent.
//   - Repository configuration no longer opens SSE twice.
//   - URL and token are combined before configuring SSE.
//   - Token changes restart the authenticated stream.
//   - Server changes reset SSE replay state in LiasSseClient.
//   - Background state intentionally closes SSE.
//   - Foreground resumes SSE and performs authoritative REST reconcile.
//   - Network loss pauses reconnect loop.
//   - Network restoration immediately reconnects + reconciles.
//   - effective.status_changed refreshes only the indicated target when
//     possible instead of blindly refreshing the entire application.
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
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.core.network.DeviceListResponse
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.EventConstants
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.network.LiasSseClient
import com.lias.remote.core.network.NetworkMonitor
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class EventRepository(
    internal val api:
        LiasApiClient,
    private val sse:
        LiasSseClient,
    private val settings:
        SettingsRepository,
    private val networkMonitor:
        NetworkMonitor
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

    private val refreshInProgress =
        AtomicBoolean(
            false
        )

    @Volatile
    private var lastSseConnectedTime =
        0L

    private val recentToastMap =
        ConcurrentHashMap<
            String,
            Long
        >()

    fun start() {
        if (
            !started.compareAndSet(
                false,
                true
            )
        ) {
            return
        }

        networkMonitor.start()

        scope.launch {
            observeConfiguration()
        }

        scope.launch {
            observeNetwork()
        }

        scope.launch {
            collectConnectionState()
        }

        scope.launch {
            collectTransportErrors()
        }

        scope.launch {
            collectSseEvents()
        }
    }

    /**
     * Called by MainActivity lifecycle.
     *
     * The SSE socket is foreground-only. On resume, Last-Event-ID
     * replays buffered events and REST reconciliation closes any gap
     * larger than the server's replay history.
     */
    fun setAppForeground(
        foreground: Boolean
    ) {
        val previous =
            _state.value
                .isAppForeground

        if (
            previous ==
            foreground
        ) {
            return
        }

        _state.value =
            _state.value.copy(
                isAppForeground =
                    foreground
            )

        if (
            !foreground
        ) {

            sse.disconnect(
                preserveReplayPosition =
                    true
            )

            return
        }

        if (
            _state.value
                .isNetworkAvailable
        ) {

            sse.connect(
                scope
            )

            scope.launch {
                refreshAll()
            }
        }
    }

    private suspend fun observeConfiguration() {

        combine(
            settings.serverUrl,
            settings.authToken
        ) {
                serverUrl,
                token ->

            serverUrl to
                token
        }
            .collect {
                    (
                        serverUrl,
                        token
                    ) ->

                api.baseUrl =
                    serverUrl

                api.authToken =
                    token

                /*
                 * LiasSseClient determines whether the server or only
                 * credentials changed and applies the correct replay
                 * policy.
                 */
                sse.configure(
                    baseUrl =
                        serverUrl,
                    authToken =
                        token
                )

                if (
                    serverUrl.isBlank()
                ) {

                    sse.disconnect(
                        preserveReplayPosition =
                            false
                    )

                    _state.value =
                        _state.value.copy(
                            connectionState =
                                ConnectionState.DISCONNECTED,
                            syncState =
                                SyncState.Idle,
                            transportError =
                                null
                        )

                    return@collect
                }

                /*
                 * A saved connection mutation is authoritative.
                 *
                 * If foregrounded, restart immediately with the new
                 * credentials. If backgrounded, connect on next resume.
                 */
                if (
                    _state.value
                        .isAppForeground &&
                    _state.value
                        .isNetworkAvailable
                ) {

                    sse.connect(
                        scope
                    )

                    refreshAll()
                }
            }
    }

    private suspend fun observeNetwork() {

        networkMonitor.available
            .collect { available ->

                val previous =
                    _state.value
                        .isNetworkAvailable

                _state.value =
                    _state.value.copy(
                        isNetworkAvailable =
                            available
                    )

                sse.setNetworkAvailable(
                    available
                )

                if (
                    !available
                ) {

                    val current =
                        _state.value

                    _state.value =
                        current.copy(
                            syncState =
                                if (
                                    current.isInitialLoaded
                                ) {
                                    SyncState.Stale(
                                        message =
                                            "Network unavailable · showing last known data.",
                                        lastSuccessfulSyncMs =
                                            current.lastSuccessfulSyncMs
                                    )
                                } else {
                                    current.syncState
                                }
                        )

                    return@collect
                }

                if (
                    !previous &&
                    _state.value
                        .isAppForeground &&
                    api.baseUrl
                        .isNotBlank()
                ) {

                    /*
                     * Connectivity transition: reconnect immediately
                     * rather than waiting for an old exponential
                     * backoff delay.
                     */
                    sse.reconnect(
                        scope =
                            scope,
                        preserveReplayPosition =
                            true
                    )

                    refreshAll()
                }
            }
    }

    private suspend fun collectConnectionState() {

        sse.connectionState
            .collect { connection ->

                val previous =
                    _state.value
                        .connectionState

                if (
                    connection ==
                    ConnectionState.CONNECTED
                ) {

                    lastSseConnectedTime =
                        System.currentTimeMillis()
                }

                _state.value =
                    _state.value.copy(
                        connectionState =
                            connection,
                        transportError =
                            if (
                                connection ==
                                ConnectionState.CONNECTED
                            ) {
                                null
                            } else {
                                _state.value
                                    .transportError
                            }
                    )

                /*
                 * Every genuine reconnection gets a REST reconciliation.
                 *
                 * SSE replay handles buffered events; REST handles cases
                 * where background/network loss exceeded broker history.
                 */
                if (
                    connection ==
                        ConnectionState.CONNECTED &&
                    previous !=
                        ConnectionState.CONNECTED &&
                    _state.value
                        .isAppForeground
                ) {

                    scope.launch {
                        refreshAll()
                    }
                }
            }
    }

    private suspend fun collectTransportErrors() {

        sse.lastError
            .collect { error ->

                if (
                    error != null
                ) {

                    val current =
                        _state.value

                    _state.value =
                        current.copy(
                            transportError =
                                error,
                            syncState =
                                if (
                                    current.isInitialLoaded
                                ) {
                                    SyncState.Stale(
                                        message =
                                            "Live updates interrupted · showing last known data.",
                                        lastSuccessfulSyncMs =
                                            current.lastSuccessfulSyncMs
                                    )
                                } else {
                                    current.syncState
                                }
                        )
                }
            }
    }

    internal suspend fun refreshAll() {

        if (
            api.baseUrl
                .isBlank()
        ) {
            return
        }

        if (
            !_state.value
                .isNetworkAvailable
        ) {
            return
        }

        if (
            !refreshInProgress
                .compareAndSet(
                    false,
                    true
                )
        ) {
            return
        }

        val hadUsableData =
            _state.value
                .isInitialLoaded

        _state.value =
            _state.value.copy(
                isRefreshing =
                    true,
                syncState =
                    if (
                        hadUsableData
                    ) {
                        _state.value
                            .syncState
                    } else {
                        SyncState.Loading
                    }
            )

        try {

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

                val usersDeferred =
                    async {
                        api.get<List<User>>(
                            Endpoints.USERS
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

                val usersResult =
                    usersDeferred.await()

                val primaryFailure =
                    listOf(
                        devicesResult,
                        tagsResult,
                        policiesResult,
                        schedulesResult
                    )
                        .firstOrNull {
                            it !is
                                ApiResult.Success<*>
                        }

                if (
                    primaryFailure !=
                    null
                ) {

                    val message =
                        resultMessage(
                            primaryFailure,
                            "Unable to synchronize LIAS."
                        )

                    val current =
                        _state.value

                    _state.value =
                        current.copy(
                            isRefreshing =
                                false,
                            errorMessage =
                                message,
                            syncState =
                                if (
                                    current.isInitialLoaded
                                ) {
                                    SyncState.Stale(
                                        message =
                                            message,
                                        lastSuccessfulSyncMs =
                                            current.lastSuccessfulSyncMs
                                    )
                                } else {
                                    SyncState.Failed(
                                        message
                                    )
                                }
                        )

                    return@coroutineScope
                }

                val devices =
                    (
                        devicesResult
                            as ApiResult.Success
                        )
                        .data
                        .devices

                val tags =
                    (
                        tagsResult
                            as ApiResult.Success
                        )
                        .data

                val policies =
                    (
                        policiesResult
                            as ApiResult.Success
                        )
                        .data

                val schedules =
                    (
                        schedulesResult
                            as ApiResult.Success
                        )
                        .data

                val stats =
                    (
                        statsResult
                            as? ApiResult.Success
                        )
                        ?.data

                val users =
                    (
                        usersResult
                            as? ApiResult.Success
                        )
                        ?.data
                        ?: _state.value
                            .users

                /*
                 * Fetch effective statuses concurrently.
                 */
                val deviceStatusDeferred =
                    devices.associate {
                            device ->

                        device.pdid to
                            async {
                                api.getDeviceEffectiveStatus(
                                    device.pdid
                                )
                            }
                    }

                val tagStatusDeferred =
                    tags.associate {
                            tag ->

                        tag.id to
                            async {
                                api.getTagEffectiveStatus(
                                    tag.id
                                )
                            }
                    }

                val deviceStatuses =
                    buildMap {

                        deviceStatusDeferred
                            .forEach {
                                    (
                                        pdid,
                                        deferred
                                    ) ->

                                val result =
                                    deferred.await()

                                if (
                                    result is
                                    ApiResult.Success
                                ) {
                                    put(
                                        pdid,
                                        result.data
                                    )
                                }
                            }
                    }

                val tagStatuses =
                    buildMap {

                        tagStatusDeferred
                            .forEach {
                                    (
                                        tagId,
                                        deferred
                                    ) ->

                                val result =
                                    deferred.await()

                                if (
                                    result is
                                    ApiResult.Success
                                ) {
                                    put(
                                        tagId,
                                        result.data
                                    )
                                }
                            }
                    }

                val now =
                    System.currentTimeMillis()

                _state.value =
                    _state.value.copy(
                        devices =
                            devices,
                        tags =
                            tags,
                        policies =
                            policies,
                        schedules =
                            schedules,
                        stats =
                            stats,
                        users =
                            users,
                        deviceEffectiveStatuses =
                            deviceStatuses,
                        tagEffectiveStatuses =
                            tagStatuses,
                        isInitialLoaded =
                            true,
                        isRefreshing =
                            false,
                        lastSuccessfulSyncMs =
                            now,
                        errorMessage =
                            null,
                        syncState =
                            SyncState.Ready(
                                syncedAtMs =
                                    now
                            )
                    )
            }

        } finally {

            refreshInProgress.set(
                false
            )

            if (
                _state.value
                    .isRefreshing
            ) {
                _state.value =
                    _state.value.copy(
                        isRefreshing =
                            false
                    )
            }
        }
    }

    private suspend fun collectSseEvents() {

        sse.events
            .collect { event ->

                if (
                    event.type ==
                    EventConstants.PING
                ) {
                    return@collect
                }

                val now =
                    System.currentTimeMillis()

                /*
                 * Suppress user-facing replay noise immediately after
                 * reconnect. State reconciliation still occurs.
                 */
                val isReplayPhase =
                    (
                        now -
                            lastSseConnectedTime
                        ) <
                        REPLAY_TOAST_SUPPRESSION_MS

                val toastKey =
                    "${event.type}:${event.deviceID}"

                val lastToastTime =
                    recentToastMap[
                        toastKey
                    ] ?: 0L

                val duplicateToast =
                    (
                        now -
                            lastToastTime
                        ) <
                        DUPLICATE_TOAST_WINDOW_MS

                val showToast =
                    !isReplayPhase &&
                        !duplicateToast

                when (
                    event.type
                ) {

                    EventConstants.DEVICE_ADDED -> {

                        refreshSingleDevice(
                            event.deviceID
                        )

                        if (
                            showToast
                        ) {

                            rememberToast(
                                toastKey,
                                now
                            )

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
                            showToast
                        ) {

                            rememberToast(
                                toastKey,
                                now
                            )

                            val payload =
                                event.payload
                                    ?.let {
                                        try {
                                            json.decodeFromJsonElement<
                                                DeviceEventPayload
                                            >(
                                                it
                                            )
                                        } catch (
                                            _: Exception
                                        ) {
                                            null
                                        }
                                    }

                            val verified =
                                payload
                                    ?.safeConfirmedBy
                                    .orEmpty()

                            _uiEvents.emit(
                                UiEvent.ShowSnackbar(
                                    if (
                                        verified.isNotEmpty()
                                    ) {
                                        "Device online · ${verified.size} discovery sources"
                                    } else {
                                        "Device online"
                                    }
                                )
                            )
                        }
                    }

                    EventConstants.DEVICE_OFFLINE -> {

                        refreshSingleDevice(
                            event.deviceID
                        )

                        if (
                            showToast
                        ) {

                            rememberToast(
                                toastKey,
                                now
                            )

                            _uiEvents.emit(
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

                    EventConstants.DEVICE_REMOVED -> {

                        val pdid =
                            event.deviceID

                        if (
                            pdid.isNotBlank()
                        ) {

                            _state.value =
                                _state.value.copy(
                                    devices =
                                        _state.value
                                            .devices
                                            .filterNot {
                                                it.pdid ==
                                                    pdid
                                            },
                                    deviceEffectiveStatuses =
                                        _state.value
                                            .deviceEffectiveStatuses -
                                            pdid
                                )
                        }
                    }

                    EventConstants.DEVICE_REIDENTIFIED -> {

                        val payload =
                            event.payload
                                ?.let {
                                    try {
                                        json.decodeFromJsonElement<
                                            DeviceReidentifiedPayload
                                        >(
                                            it
                                        )
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
                                showToast
                            ) {

                                rememberToast(
                                    toastKey,
                                    now
                                )

                                _uiEvents.emit(
                                    UiEvent.ShowSnackbar(
                                        "Device identity updated"
                                    )
                                )
                            }
                        }
                    }

                    EventConstants.EFFECTIVE_STATUS_CHANGED -> {

                        refreshEffectiveStatusForEvent(
                            event
                        )
                    }

                    EventConstants.SECURITY_ALERT -> {

                        val payload =
                            event.payload
                                ?.let {
                                    try {
                                        json.decodeFromJsonElement<
                                            SecurityAlertPayload
                                        >(
                                            it
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
                                ?: "Network identity anomaly detected."

                        _uiEvents.emit(
                            UiEvent.ShowSecurityAlert(
                                details =
                                    details,
                                pdid =
                                    payload
                                        ?.pdid
                                        .orEmpty()
                            )
                        )

                        if (
                            showToast
                        ) {

                            rememberToast(
                                toastKey,
                                now
                            )

                            _uiEvents.emit(
                                UiEvent.ShowSnackbarError(
                                    "Security alert · $details"
                                )
                            )
                        }
                    }
                }
            }
    }

    private suspend fun refreshEffectiveStatusForEvent(
        event:
            com.lias.remote.core.models.LiasEvent
    ) {

        val objectPayload =
            event.payload
                ?.runCatching {
                    jsonObject
                }
                ?.getOrNull()

        val targetType =
            objectPayload
                ?.get(
                    "target_type"
                )
                ?.jsonPrimitive
                ?.contentOrNull

        val targetId =
            objectPayload
                ?.get(
                    "target_id"
                )
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: event.deviceID
                    .takeIf {
                        it.isNotBlank()
                    }

        if (
            targetId == null
        ) {

            refreshAll()

            return
        }

        when (
            targetType
                ?.lowercase()
        ) {

            "tag" ->
                refreshSingleTagStatus(
                    targetId
                )

            "device" ->
                refreshSingleDeviceStatus(
                    targetId
                )

            else -> {

                /*
                 * Older server payload: infer from current inventory.
                 */
                if (
                    _state.value
                        .tags
                        .any {
                            it.id ==
                                targetId
                        }
                ) {

                    refreshSingleTagStatus(
                        targetId
                    )

                } else {

                    refreshSingleDeviceStatus(
                        targetId
                    )
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

        when (
            val result =
                api.get<Device>(
                    Endpoints.device(
                        pdid
                    )
                )
        ) {

            is ApiResult.Success -> {

                val updated =
                    result.data

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
                    index >= 0
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

                _state.value =
                    _state.value.copy(
                        devices =
                            devices
                    )

                refreshSingleDeviceStatus(
                    pdid
                )
            }

            else ->
                Unit
        }
    }

    private suspend fun refreshSingleDeviceStatus(
        pdid: String
    ) {
        if (
            pdid.isBlank()
        ) {
            return
        }

        when (
            val result =
                api.getDeviceEffectiveStatus(
                    pdid
                )
        ) {

            is ApiResult.Success -> {

                _state.value =
                    _state.value.copy(
                        deviceEffectiveStatuses =
                            _state.value
                                .deviceEffectiveStatuses +
                                (
                                    pdid to
                                        result.data
                                    )
                    )
            }

            else ->
                Unit
        }
    }

    private suspend fun refreshSingleTagStatus(
        tagId: String
    ) {
        if (
            tagId.isBlank()
        ) {
            return
        }

        when (
            val result =
                api.getTagEffectiveStatus(
                    tagId
                )
        ) {

            is ApiResult.Success -> {

                _state.value =
                    _state.value.copy(
                        tagEffectiveStatuses =
                            _state.value
                                .tagEffectiveStatuses +
                                (
                                    tagId to
                                        result.data
                                    )
                    )
            }

            else ->
                Unit
        }
    }

    fun clearError() {

        _state.value =
            _state.value.copy(
                errorMessage =
                    null
            )
    }

    suspend fun emitUiEvent(
        event: UiEvent
    ) {

        _uiEvents.emit(
            event
        )
    }

    private fun rememberToast(
        key: String,
        now: Long
    ) {

        recentToastMap[
            key
        ] =
            now

        /*
         * Prevent unbounded growth after long-running sessions.
         */
        if (
            recentToastMap.size >
            256
        ) {

            val cutoff =
                now -
                    TOAST_CACHE_RETENTION_MS

            recentToastMap
                .entries
                .removeIf {
                    it.value <
                        cutoff
                }
        }
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
                result.cause
                    .message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: fallback

            is ApiResult.SerializationError ->
                "LIAS returned an invalid response."
        }

    private companion object {

        const val REPLAY_TOAST_SUPPRESSION_MS =
            3_000L

        const val DUPLICATE_TOAST_WINDOW_MS =
            3_000L

        const val TOAST_CACHE_RETENTION_MS =
            60_000L
    }
}
