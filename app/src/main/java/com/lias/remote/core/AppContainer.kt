// ====================================================================
// File: app/src/main/java/com/lias/remote/core/AppContainer.kt
// Version: 1.2.0
// Audit Fixes:
//   1. Provided dedicated `sseOkHttpClient` instance with `readTimeout(0, TimeUnit.SECONDS)`
//      to prevent OkHttp from killing long-lived SSE connections between 15s server pings.
// ====================================================================

package com.lias.remote.core

import android.content.Context
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.network.LiasSseClient
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Dedicated SSE OkHttpClient with infinite readTimeout (0) for long-lived streams
    val sseOkHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val liasApiClient: LiasApiClient by lazy {
        LiasApiClient(okHttpClient)
    }

    val liasSseClient: LiasSseClient by lazy {
        LiasSseClient(sseOkHttpClient)
    }

    val eventRepository: EventRepository by lazy {
        EventRepository(
            api = liasApiClient,
            sse = liasSseClient,
            settings = settingsRepository
        )
    }
}
