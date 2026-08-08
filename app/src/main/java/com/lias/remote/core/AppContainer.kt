// ====================================================================
// File: app/src/main/java/com/lias/remote/core/AppContainer.kt
// Version: 13.0.0
//
// Purpose:
//   Lightweight manual dependency container.
//
// Batch 13:
//   - Adds NetworkMonitor.
//   - Keeps dedicated infinite-read-timeout SSE OkHttpClient.
//   - EventRepository owns transport coordination.
// ====================================================================

package com.lias.remote.core

import android.content.Context
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.network.LiasSseClient
import com.lias.remote.core.network.NetworkMonitor
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class AppContainer(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    val okHttpClient:
        OkHttpClient
        by lazy {

            OkHttpClient
                .Builder()
                .connectTimeout(
                    10,
                    TimeUnit.SECONDS
                )
                .readTimeout(
                    10,
                    TimeUnit.SECONDS
                )
                .writeTimeout(
                    10,
                    TimeUnit.SECONDS
                )
                .retryOnConnectionFailure(
                    true
                )
                .build()
        }

    /**
     * SSE must not have a finite read timeout.
     *
     * LIAS emits heartbeat events every 15 seconds, but network or
     * scheduling jitter should not make OkHttp itself kill an otherwise
     * valid persistent response.
     */
    val sseOkHttpClient:
        OkHttpClient
        by lazy {

            okHttpClient
                .newBuilder()
                .readTimeout(
                    0,
                    TimeUnit.SECONDS
                )
                .build()
        }

    val settingsRepository:
        SettingsRepository
        by lazy {

            SettingsRepository(
                applicationContext
            )
        }

    val networkMonitor:
        NetworkMonitor
        by lazy {

            NetworkMonitor(
                applicationContext
            )
        }

    val liasApiClient:
        LiasApiClient
        by lazy {

            LiasApiClient(
                okHttpClient
            )
        }

    val liasSseClient:
        LiasSseClient
        by lazy {

            LiasSseClient(
                sseOkHttpClient
            )
        }

    val eventRepository:
        EventRepository
        by lazy {

            EventRepository(
                api =
                    liasApiClient,
                sse =
                    liasSseClient,
                settings =
                    settingsRepository,
                networkMonitor =
                    networkMonitor
            )
                .also {
                    /*
                     * Repository startup belongs to the application
                     * dependency graph, not to a ViewModel constructor.
                     *
                     * start() is idempotent regardless.
                     */
                    it.start()
                }
        }
}
