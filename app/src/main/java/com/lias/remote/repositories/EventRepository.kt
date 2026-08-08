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
import com.lias.remote.core.models.User
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

/**
 * Canonical server-authoritative repository.
 *
 * Mutation ownership is split into focused files, all coordinated by
 * [mutations].
 *
 * Bulk refreshes are revision-guarded so an older REST snapshot cannot
 * overwrite a newer mutation or SSE-driven state change.
 */
class EventRepository(
    internal val api: LiasApiClient,
    private val sse: LiasSseClient,
    private val settings: SettingsRepository
) {

    internal val _state =
        MutableStateFlow(
            UiState()
        )

    val state: StateFlow<UiState> =
        _state.asStateFlow()

    internal val _uiEvents =
        MutableSharedFlow<UiEvent>(
            replay = 0,
            extraBufferCapacity = 64
        )

    val uiEvents: SharedFlow<UiEvent> =
        _uiEvents.asSharedFlow()

    /**
     * Shared serialization + revision authority used by:
     *
     * - PolicyScheduleMutations
     * - TagMutations
     * - GlobalControlMutations
     * - TemporaryAccessRepository
     * - EventRepositoryActions
     */
    internal val mutations =
        MutationCoordinator()

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

    private fun observeAuthentication() {

        scope.launch {

            settings.authToken
                .distinctUntilChanged()
                .collect {
                    token ->

                    val normalized =
                        token
                            ?.trim()
                            ?.ifBlank {
                                null
                            }

                    api.authToken =
                        normalized

                    sse.authToken =
                        normalized
                }
        }
    }

    /**
     * Persisted configuration is the sole authority deciding whether
     * the live LIAS connection exists.
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

                    api.baseUrl =
                        url

                    /*
                     * Batch-23 LiasSseClient resets its replay cursor
                     * only when this logical server actually changes.
                     */
                    sse.baseUrl =
                        url

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
     * Complete authoritative synchronization from LIAS.
     *
     * A revision snapshot prevents this REST request from overwriting a
     * mutation or SSE event that happens while the requests are in
     * flight.
     */
    suspend fun refreshAll() {

        if (
            api.baseUrl
                .isBlank()
        ) {
            return
        }

        val snapshotRevision =
            mutations.revision()

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

            /*
             * Important:
             * UiState contains users and DeviceDetail consumes them.
             *
             * Without loading USERS here, existing server users vanish
             * after a fresh process start until one is locally created.
             */
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

            if (
                !mutations.snapshotIsCurrent(
                    snapshotRevision
                )
            ) {
                return@coroutineScope
            }

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

            val loadedUsers =
                (
                    usersResult as?
                        ApiResult.Success
                    )
                    ?.data
                    ?: current.users

            val refreshFailure =
                firstFailure(
                    devicesResult,
                    tagsResult,
                    policiesResult,
                    schedulesResult,
                    statsResult,
                    usersResult
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
                    users =
                        loadedUsers,
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
             * EffectiveStatus is intentionally fetched after the
             * inventory snapshot.
             *
             * Missing status never becomes fabricated Allow/Block.
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

                    else ->
                        Unit
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

                    else ->
                        Unit
                }
            }

            /*
             * A mutation may have happened during EffectiveStatus
             * retrieval too.
             */
            if (
                !mutations.snapshotIsCurrent(
                    snapshotRevision
                )
            ) {
                return@coroutineScope
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
        vararg results: ApiResult<*>
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

                val replayPhase =
                    now -
                        lastSseConnectedTime <
                        REPLAY_QUIET_PERIOD_MS

                val toastKey =
                    "${event.type}:${event.deviceID}"

                val previousToast =
                    recentToastMap[
                        toastKey
                    ]
                        ?: 0L

                val duplicateToast =
                    now -
                        previousToast <
                        TOAST_DEDUP_WINDOW_MS

                val mayNotify =
                    !replayPhase &&
                        !duplicateToast

                /*
                 * Every meaningful server event invalidates REST
                 * snapshots that may currently be in flight.
                 */
                if (
                    event.type !=
                    EventConstants.PING
                ) {
                    mutations.markExternalChange()
                }

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
                                    confirmedBy.isEmpty()
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

                    EventConstants.EFFECTIVE_STATUS_CHANGED ->

                        refreshAll()

                    EventConstants.HOSTNAME_CHANGED,
                    EventConstants.FINGERPRINT_UPDATED,
                    EventConstants.IP_CHANGED,
                    EventConstants.MAC_CHANGED ->

                        refreshSingleDevice(
                            event.deviceID
                        )

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

                    EventConstants.PING ->
                        Unit
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
            deviceResult !is
            ApiResult.Success
        ) {
            return
        }

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

        when (
            val statusResult =
                api.getDeviceEffectiveStatus(
                    pdid
                )
        ) {

            is ApiResult.Success ->

                statuses =
                    statuses +
                        (
                            pdid to
                                statusResult.data
                            )

            else ->
                Unit
        }

        _state.value =
            _state.value.copy(
                devices =
                    devices,
                deviceEffectiveStatuses =
                    statuses
            )
    }

    /**
     * Canonical local upsert helpers used only after successful server
     * mutations.
     */
    internal fun upsertPolicy(
        policy: Policy
    ) {

        val current =
            _state.value

        _state.value =
            current.copy(
                policies =
                    current.policies
                        .filterNot {
                            it.id ==
                                policy.id
                        } +
                        policy
            )
    }

    internal fun upsertSchedule(
        schedule: Schedule
    ) {

        val current =
            _state.value

        _state.value =
            current.copy(
                schedules =
                    current.schedules
                        .filterNot {
                            it.id ==
                                schedule.id
                        } +
                        schedule
            )
    }

    internal fun upsertTag(
        tag: Tag
    ) {

        val current =
            _state.value

        _state.value =
            current.copy(
                tags =
                    current.tags
                        .filterNot {
                            it.id ==
                                tag.id
                        } +
                        tag
            )
    }

    internal suspend fun emitUiEvent(
        event: UiEvent
    ) {

        _uiEvents.emit(
            event
        )
    }

    companion object {

        private const val REPLAY_QUIET_PERIOD_MS =
            2_500L

        private const val TOAST_DEDUP_WINDOW_MS =
            3_000L
    }
}
