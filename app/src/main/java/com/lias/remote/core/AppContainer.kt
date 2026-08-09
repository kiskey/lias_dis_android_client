// ====================================================================
// File: app/src/main/java/com/lias/remote/core/AppContainer.kt
// Version: 24.0.0
//
// Purpose:
//   Manual dependency-injection root for LIAS Remote.
//
// Batch 24:
//   - Integrates Batch 22 isolated LiasConnectionProbe.
//   - Retains independent long-lived SSE OkHttp client.
//   - REST and connection probes share connection pooling but NOT
//     mutable LiasApiClient endpoint state.
//   - EventRepository remains the single live application repository.
// ====================================================================

package com.lias.remote.core

import android.content.Context
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.network.LiasConnectionProbe
import com.lias.remote.core.network.LiasSseClient
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class AppContainer(
    context: Context
) {

    /**
     * Ordinary finite REST requests.
     */
    val okHttpClient:
        OkHttpClient by
        lazy {

            OkHttpClient.Builder()
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
     * SSE must not inherit the ordinary 10-second read timeout.
     *
     * LIAS sends periodic heartbeat traffic, but an event stream is
     * intentionally long-lived.
     */
    val sseOkHttpClient:
        OkHttpClient by
        lazy {

            okHttpClient
                .newBuilder()
                .readTimeout(
                    0,
                    TimeUnit.SECONDS
                )
                .build()
        }

    val settingsRepository:
        SettingsRepository by
        lazy {

            SettingsRepository(
                context.applicationContext
            )
        }

    /**
     * Live REST client.
     *
     * EventRepository owns its active endpoint through persisted
     * SettingsRepository values.
     */
    val liasApiClient:
        LiasApiClient by
        lazy {

            LiasApiClient(
                okHttpClient
            )
        }

    /**
     * Batch 22 isolated candidate-server verifier.
     *
     * A connection test creates its own temporary LiasApiClient and can
     * therefore never redirect liasApiClient away from the currently
     * active server.
     */
    val liasConnectionProbe:
        LiasConnectionProbe by
        lazy {

            LiasConnectionProbe(
                okHttpClient
            )
        }

    val liasSseClient:
        LiasSseClient by
        lazy {

            LiasSseClient(
                sseOkHttpClient
            )
        }

    val eventRepository:
        EventRepository by
        lazy {

            EventRepository(
                api =
                    liasApiClient,
                sse =
                    liasSseClient,
                settings =
                    settingsRepository
            )
        }
}
