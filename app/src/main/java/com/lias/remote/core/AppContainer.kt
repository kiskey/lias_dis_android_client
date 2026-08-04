// ====================================================================
// File: app/src/main/java/com/lias/remote/core/AppContainer.kt
// Version: 1.1.0
// Audit Fixes: 
//   1. Added missing imports for DI components.
//   2. Removed unused okhttp logging import.
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
            .writeTimeout(0, TimeUnit.SECONDS) 
            .retryOnConnectionFailure(true)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val liasApiClient: LiasApiClient by lazy {
        LiasApiClient(okHttpClient)
    }

    val liasSseClient: LiasSseClient by lazy {
        LiasSseClient(okHttpClient)
    }

    val eventRepository: EventRepository by lazy {
        EventRepository(
            api = liasApiClient,
            sse = liasSseClient,
            settings = settingsRepository
        )
    }
}
