// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Implemented ViewModelProvider.Factory to properly retain ViewModels
//      across configuration changes (screen rotations) instead of direct
//      instantiation, fixing the lifecycle violation.
// ====================================================================

package com.lias.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lias.remote.core.AppContainer
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        val container = (application as LiasApplication).container

        // FIX 2.1: Proper ViewModel lifecycle management via ViewModelProvider.Factory
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LiasViewModel::class.java)) {
                    return LiasViewModel(container.eventRepository) as T
                }
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    return SettingsViewModel(container.settingsRepository, container.liasApiClient) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val liasViewModel = ViewModelProvider(this, viewModelFactory)[LiasViewModel::class.java]
        val settingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]

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
