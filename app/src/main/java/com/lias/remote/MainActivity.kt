// ====================================================================
// File: app/src/main/java/com/lias/remote/MainActivity.kt
// Version: 3.0.0
//
// Purpose:
//   Android activity entry point.
//
// Changes:
//   - Owns Android Intent -> DeepLinkResolver boundary.
//   - Supports cold-start deep links.
//   - Supports warm-start deep links.
//   - Keeps ViewModels activity-scoped.
//   - Does not allow individual screens to inspect Intents.
// ====================================================================

package com.lias.remote

import android.content.Intent
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
import com.lias.remote.ui.navigation.DeepLinkResolver
import com.lias.remote.ui.navigation.LiasDeepLink
import com.lias.remote.ui.navigation.LiasNavHost
import com.lias.remote.ui.theme.LiasTheme

class MainActivity : ComponentActivity() {

    private var pendingDeepLink:
        LiasDeepLink? = null

    private var liasViewModel:
        LiasViewModel? = null

    private var settingsViewModel:
        SettingsViewModel? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        val container =
            (application as LiasApplication)
                .container

        val viewModelFactory =
            object :
                ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel>
                    create(
                        modelClass: Class<T>
                    ): T {

                    if (
                        modelClass.isAssignableFrom(
                            LiasViewModel::class.java
                        )
                    ) {
                        return LiasViewModel(
                            container.eventRepository
                        ) as T
                    }

                    if (
                        modelClass.isAssignableFrom(
                            SettingsViewModel::class.java
                        )
                    ) {
                        return SettingsViewModel(
                            container.settingsRepository,
                            container.liasApiClient,
                            container.eventRepository
                        ) as T
                    }

                    throw IllegalArgumentException(
                        "Unknown ViewModel class: " +
                            modelClass.name
                    )
                }
            }

        liasViewModel =
            ViewModelProvider(
                this,
                viewModelFactory
            )[LiasViewModel::class.java]

        settingsViewModel =
            ViewModelProvider(
                this,
                viewModelFactory
            )[SettingsViewModel::class.java]

        pendingDeepLink =
            DeepLinkResolver.resolve(
                intent
            )

        setContent {

            val settingsState by
                settingsViewModel!!
                    .uiState
                    .collectAsState()

            LiasTheme(
                themeMode =
                    settingsState.themeMode
            ) {
                LiasNavHost(
                    liasViewModel =
                        liasViewModel!!,

                    settingsViewModel =
                        settingsViewModel!!,

                    pendingDeepLink =
                        pendingDeepLink,

                    onDeepLinkConsumed = {
                        pendingDeepLink =
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

        pendingDeepLink =
            DeepLinkResolver.resolve(
                intent
            )
    }
}
