// ====================================================================
// File: app/src/main/java/com/lias/remote/core/AppContainer.kt
// Version: 2.0.0
//
// Purpose:
//   Lightweight application-scoped dependency container.
//
// Design:
//   Manual DI is intentionally retained. The supplied project uses
//   this approach to avoid unnecessary dependency-injection startup
//   overhead.
//
// Network:
//   REST client has finite request timeouts.
//   SSE client has an infinite read timeout because it is a long-lived
//   HTTP stream.
// ====================================================================

package com.lias.remote.core

import android.content.Context
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.network.LiasSseClient
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(
    context: Context
) {

    val okHttpClient:
        OkHttpClient by lazy {

        OkHttpClient.Builder()
            .connectTimeout(
                10,
                TimeUnit.SECONDS
            )
            .readTimeout(
                15,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                15,
                TimeUnit.SECONDS
            )
            .callTimeout(
                30,
                TimeUnit.SECONDS
            )
            .retryOnConnectionFailure(
                true
            )
            .build()
    }

    val sseOkHttpClient:
        OkHttpClient by lazy {

        okHttpClient.newBuilder()
            .readTimeout(
                0,
                TimeUnit.SECONDS
            )
            .callTimeout(
                0,
                TimeUnit.SECONDS
            )
            .build()
    }

    val settingsRepository:
        SettingsRepository by lazy {
            SettingsRepository(
                context.applicationContext
            )
        }

    val liasApiClient:
        LiasApiClient by lazy {
            LiasApiClient(
                okHttpClient
            )
        }

    val liasSseClient:
        LiasSseClient by lazy {
            LiasSseClient(
                sseOkHttpClient
            )
        }

    val eventRepository:
        EventRepository by lazy {
            EventRepository(
                api = liasApiClient,
                sse = liasSseClient,
                settings = settingsRepository
            )
        }
}
