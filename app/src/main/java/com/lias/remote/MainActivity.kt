// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 13.0.0
//
// Purpose:
//   LIAS Remote activity + application foreground lifecycle boundary.
//
// Batch 13:
//   - Marks repository foreground in onStart().
//   - Disconnects foreground-only SSE in onStop().
//   - ViewModels no longer own transport lifetime.
//   - Configuration changes remain safe because EventRepository is
//     application-scoped through AppContainer.
// ====================================================================

package com.lias.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity :
    ComponentActivity() {

    private val container:
        com.lias.remote.core.AppContainer
        get() =
            (
                application
                    as LiasApplication
                )
                .container

    override fun onCreate(
        savedInstanceState:
            Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        /*
         * Ensure application-scoped EventRepository exists before the
         * activity reaches onStart().
         */
        val repository =
            container.eventRepository

        val viewModelFactory =
            object :
                ViewModelProvider.Factory {

                @Suppress(
                    "UNCHECKED_CAST"
                )
                override fun <
                    T : ViewModel
                > create(
                    modelClass:
                        Class<T>
                ): T {

                    return when {

                        modelClass
                            .isAssignableFrom(
                                LiasViewModel::class.java
                            ) ->

                            LiasViewModel(
                                repository
                            ) as T

                        modelClass
                            .isAssignableFrom(
                                SettingsViewModel::class.java
                            ) ->

                            SettingsViewModel(
                                settings =
                                    container
                                        .settingsRepository,
                                api =
                                    container
                                        .liasApiClient,
                                eventRepository =
                                    repository
                            ) as T

                        else ->
                            throw IllegalArgumentException(
                                "Unknown ViewModel class: ${modelClass.name}"
                            )
                    }
                }
            }

        val liasViewModel =
            ViewModelProvider(
                this,
                viewModelFactory
            )[
                LiasViewModel::class.java
            ]

        val settingsViewModel =
            ViewModelProvider(
                this,
                viewModelFactory
            )[
                SettingsViewModel::class.java
            ]

        setContent {

            val settingsState by
                settingsViewModel
                    .uiState
                    .collectAsState()

            LiasTheme(
                themeMode =
                    settingsState
                        .themeMode
            ) {

                LiasNavHost(
                    liasViewModel =
                        liasViewModel,
                    settingsViewModel =
                        settingsViewModel
                )
            }
        }
    }

    override fun onStart() {

        super.onStart()

        container
            .eventRepository
            .setAppForeground(
                true
            )
    }

    override fun onStop() {

        container
            .eventRepository
            .setAppForeground(
                false
            )

        super.onStop()
    }
}
