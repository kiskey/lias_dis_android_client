// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 20.0.0
//
// Purpose:
//   Single-activity entry point.
//
// Batch 20:
//   - Handles cold-start external deep links.
//   - Handles deep links delivered while Activity is already alive.
//   - Preserves an unconsumed URI through Activity recreation.
//   - ViewModels remain Activity scoped.
//   - Navigation state itself remains owned by Navigation Compose.
//
// Lifecycle rule:
//   MainActivity transports an Intent URI.
//   LiasNavHost decides WHEN it is safe to consume it.
// ====================================================================

package com.lias.remote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity :
    ComponentActivity() {

    companion object {

        private const val STATE_PENDING_DEEP_LINK =
            "lias.pending_deep_link"
    }

    private val pendingDeepLink =
        mutableStateOf<String?>(
            null
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        pendingDeepLink.value =
            savedInstanceState
                ?.getString(
                    STATE_PENDING_DEEP_LINK
                )
                ?: intent
                    ?.data
                    ?.toString()

        val container =
            (
                application as
                    LiasApplication
                )
                .container

        val viewModelFactory =
            object :
                ViewModelProvider.Factory {

                @Suppress(
                    "UNCHECKED_CAST"
                )
                override fun <
                    T : ViewModel
                > create(
                    modelClass: Class<T>
                ): T {

                    return when {

                        modelClass
                            .isAssignableFrom(
                                LiasViewModel::class.java
                            ) ->

                            LiasViewModel(
                                container.eventRepository
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
                                    container
                                        .eventRepository
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
                    settingsState.themeMode
            ) {

                LiasNavHost(
                    liasViewModel =
                        liasViewModel,
                    settingsViewModel =
                        settingsViewModel,
                    externalDeepLink =
                        pendingDeepLink.value,
                    onExternalDeepLinkConsumed = {

                        pendingDeepLink.value =
                            null
                    }
                )
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(
            intent
        )

        setIntent(
            intent
        )

        pendingDeepLink.value =
            intent.data
                ?.toString()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        pendingDeepLink.value
            ?.let {
                uri ->

                outState.putString(
                    STATE_PENDING_DEEP_LINK,
                    uri
                )
            }

        super.onSaveInstanceState(
            outState
        )
    }
}
