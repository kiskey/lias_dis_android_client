// ====================================================================
// File: app/src/main/java/com/lias/remote/core/AppContainer.kt
// Version: 1.0.0
// Purpose: Manual Dependency Injection graph. Replaces Hilt to save
//          ~30ms startup time and ~1.5MB APK size. Provides singletons
//          for OkHttp, DataStore, and Repositories.
// ====================================================================

package com.lias.remote.core

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    // Provides a single OkHttp instance for REST and SSE
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // Write timeout 0 required for infinite SSE streams
            .writeTimeout(0, TimeUnit.SECONDS) 
            .retryOnConnectionFailure(true)
            .build()
    }

    // Settings DataStore wrapper for zero-waste key-value persistence
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    // The main API Client (REST)
    val liasApiClient: LiasApiClient by lazy {
        LiasApiClient(okHttpClient)
    }

    // The persistent SSE Client
    val liasSseClient: LiasSseClient by lazy {
        LiasSseClient(okHttpClient)
    }

    // UI State Aggregator
    val eventRepository: EventRepository by lazy {
        EventRepository(
            liasApiClient = liasApiClient,
            liasSseClient = liasSseClient,
            settingsRepository = settingsRepository
        )
    }
}
