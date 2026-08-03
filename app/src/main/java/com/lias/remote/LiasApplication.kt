// ====================================================================
// File: app/src/main/java/com/lias/remote/LiasApplication.kt
// Version: 1.0.0
// Purpose: Application entry point. Initializes the manual Dependency
//          Injection container to avoid Hilt/Koin startup overhead.
// ====================================================================

package com.lias.remote

import android.app.Application
import com.lias.remote.core.AppContainer

class LiasApplication : Application() {

    // Manual DI container. Initialized once per app process.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
