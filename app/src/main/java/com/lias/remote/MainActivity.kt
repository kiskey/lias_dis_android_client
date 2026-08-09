// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 24.0.0
//
// Purpose:
//   Single-activity LIAS Remote host.
//
// Integrated contracts:
//   Batch 20:
//     - external deep links
//     - process/recreation-safe pending URI
//     - singleTop new-intent delivery
//
//   Batch 22:
//     - SettingsViewModel uses isolated LiasConnectionProbe
//
//   Batch 24:
//     - removes obsolete SettingsViewModel(LiasApiClient) constructor
// ====================================================================

package com.lias.remote

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity :
    FragmentActivity() {

    companion object {

        private const val STATE_PENDING_DEEP_LINK =
            "lias.pending_deep_link"
    }

    /**
     * Kept outside composition so onNewIntent() can update the already
     * running UI immediately.
     */
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

        val factory =
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

                        modelClass.isAssignableFrom(
                            LiasViewModel::class.java
                        ) ->

                            LiasViewModel(
                                eventRepository =
                                    container
                                        .eventRepository
                            ) as T

                        modelClass.isAssignableFrom(
                            SettingsViewModel::class.java
                        ) ->

                            SettingsViewModel(
                                settings =
                                    container
                                        .settingsRepository,
                                connectionProbe =
                                    container
                                        .liasConnectionProbe,
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
                factory
            )[
                LiasViewModel::class.java
            ]

        val settingsViewModel =
            ViewModelProvider(
                this,
                factory
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
