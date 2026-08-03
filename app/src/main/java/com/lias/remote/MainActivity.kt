// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 1.0.0
// Purpose: Application Entry Point. Initializes Edge-to-Edge display,
//          binds the manual DI container to the ViewModel, and renders
//          the root Compose hierarchy.
// ====================================================================

package com.lias.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lias.remote.core.AppContainer
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge drawing under status/navigation bars
        enableEdgeToEdge()
        
        val container = (application as LiasApplication).container

        // Manual DI ViewModel Factory
        val viewModelFactory = viewModelFactory {
            initializer {
                LiasViewModel(
                    eventRepository = container.eventRepository
                )
            }
        }
        
        // Unused in this exact file, but demonstrates how to fetch it if needed
        // val viewModel: LiasViewModel = ViewModelProvider(this, viewModelFactory)[LiasViewModel::class.java]

        setContent {
            LiasTheme {
                LiasNavHost()
            }
        }
    }
}
