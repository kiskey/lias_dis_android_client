// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 1.1.0
// Purpose: Application Entry Point. Updated to construct both ViewModels
//          via manual DI and pass them directly to the NavHost.
// ====================================================================

package com.lias.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lias.remote.core.AppContainer
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge drawing under status/navigation bars
        enableEdgeToEdge()
        
        val container = (application as LiasApplication).container

        // Manual DI ViewModel Instantiation
        // We bypass ViewModelProvider for direct instantiation to save reflection overhead,
        // as our ViewModels do not need to survive process death via SavedStateHandle for v1.
        val liasViewModel = LiasViewModel(container.eventRepository)
        val settingsViewModel = SettingsViewModel(container.settingsRepository, container.liasApiClient)

        setContent {
            LiasTheme {
                LiasNavHost(
                    liasViewModel = liasViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
