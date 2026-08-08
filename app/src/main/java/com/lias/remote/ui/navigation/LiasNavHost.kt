// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 7.0.0
//
// Purpose:
//   Primary application navigation shell.
//
// Integration:
//   - Batch 3 typed routes.
//   - Batch 4 verified connection gate.
//   - Batch 5/7 transport + synchronization banner.
//   - Batch 6 Home/Devices state-aware screens.
//   - Device-detail deep-link safety.
//   - Tab state restoration.
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.ConnectionStatusBanner
import com.lias.remote.ui.components.UndoToast
import com.lias.remote.ui.screens.OnboardingSheet
import com.lias.remote.ui.screens.SecurityAlertSheet
import com.lias.remote.ui.screens.connect.ConnectScreen
import com.lias.remote.ui.screens.devices.DeviceDetailScreen
import com.lias.remote.ui.screens.devices.DevicesScreen
import com.lias.remote.ui.screens.home.HomeScreen
import com.lias.remote.ui.screens.rules.RulesScreen
import com.lias.remote.ui.screens.schedules.SchedulesScreen
import com.lias.remote.ui.screens.settings.ConnectionSettingsScreen
import com.lias.remote.ui.screens.settings.SettingsScreen
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar

@Composable
fun LiasNavHost(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel,
    pendingDeepLink: LiasDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController =
        rememberNavController()

    val settingsState by
        settingsViewModel
            .uiState
            .collectAsState()

    val uiState by
        liasViewModel
            .state
            .collectAsState()

    val undoState by
        liasViewModel
            .undoState
            .collectAsState()

    val securityAlert by
        liasViewModel
            .pendingSecurityAlert
            .collectAsState()

    /*
     * Since Batch 4, savedServerUrl means a configuration that has
     * successfully passed the /health verification flow.
     */
    val hasConfiguration =
        settingsState
            .savedServerUrl
            .isNotBlank()

    if (!hasConfiguration) {
        ConnectScreen(
            viewModel =
                settingsViewModel,
            onConnected = {}
        )

        return
    }

    // ----------------------------------------------------------------
    // External navigation
    // ----------------------------------------------------------------

    LaunchedEffect(
        pendingDeepLink
    ) {
        when (
            val link =
                pendingDeepLink
        ) {

            is LiasDeepLink.Device -> {
                navController.navigate(
                    LiasRoute
                        .DeviceDetail
                        .create(
                            link.pdid
                        )
                ) {
                    launchSingleTop =
                        true
                }

                onDeepLinkConsumed()
            }

            LiasDeepLink.Home -> {
                navigateToTab(
                    navController,
                    LiasTab.HOME
                )

                onDeepLinkConsumed()
            }

            LiasDeepLink.Devices -> {
                navigateToTab(
                    navController,
                    LiasTab.DEVICES
                )

                onDeepLinkConsumed()
            }

            null ->
                Unit
        }
    }

    // ----------------------------------------------------------------
    // Onboarding
    // ----------------------------------------------------------------

    if (
        !settingsState
            .isOnboarded
    ) {
        OnboardingSheet(
            onComplete = {
                settingsViewModel
                    .completeOnboarding()
            }
        )
    }

    // ----------------------------------------------------------------
    // Security alert
    // ----------------------------------------------------------------

    securityAlert?.let { alert ->

        SecurityAlertSheet(
            alert =
                alert,
            onDismiss = {
                liasViewModel
                    .dismissSecurityAlert()
            },
            onBlock = {
                /*
                 * No backend security-alert disposition endpoint exists
                 * in the supplied API contract, so do not fabricate a
                 * network mutation here.
                 */
                liasViewModel
                    .dismissSecurityAlert()
            },
            onTrust = {
                /*
                 * Same reasoning as onBlock: presentation only until
                 * the backend exposes an authoritative disposition API.
                 */
                liasViewModel
                    .dismissSecurityAlert()
            }
        )
    }

    val backStackEntry by
        navController
            .currentBackStackEntryAsState()

    val currentDestination =
        backStackEntry
            ?.destination

    CupertinoScaffold(

        topBar = {
            Column {

                CupertinoTopAppBar(
                    title = {
                        CupertinoText(
                            text =
                                "LIAS Remote"
                        )
                    }
                )

                ConnectionStatusBanner(
                    connectionState =
                        uiState.connectionState,
                    syncState =
                        uiState.syncState
                )
            }
        },

        bottomBar = {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            HigSpec.TabBarHeight
                        )
                        .background(
                            LiasThemeColors.secondaryBackground
                        )
                        .padding(
                            top = 4.dp,
                            bottom = 4.dp
                        ),
                horizontalArrangement =
                    Arrangement.SpaceAround,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                LiasTab.all.forEach { tab ->

                    val selected =
                        currentDestination
                            ?.hierarchy
                            ?.any {
                                it.route ==
                                    tab.route
                            } == true

                    val tint =
                        if (selected) {
                            LiasThemeColors.blue
                        } else {
                            LiasThemeColors.tertiaryLabel
                        }

                    val interactionSource =
                        remember {
                            MutableInteractionSource()
                        }

                    Column(
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .clickable(
                                    interactionSource =
                                        interactionSource,
                                    indication =
                                        null
                                ) {
                                    navigateToTab(
                                        navController,
                                        tab
                                    )
                                },
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        CupertinoIcon(
                            imageVector =
                                tab.icon,
                            contentDescription =
                                tab.label,
                            tint =
                                tint,
                            modifier =
                                Modifier.size(
                                    HigSpec.IconSizeM
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )

                        CupertinoText(
                            text =
                                tab.label,
                            style =
                                HigTypography.tabLabel,
                            color =
                                tint
                        )
                    }
                }
            }
        }

    ) { innerPadding ->

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            NavHost(
                navController =
                    navController,
                startDestination =
                    LiasRoute.Home.route,
                modifier =
                    Modifier.padding(
                        innerPadding
                    ),

                enterTransition = {
                    fadeIn(
                        tween(
                            durationMillis = 180
                        )
                    ) +
                        slideInHorizontally(
                            tween(
                                durationMillis = 180
                            )
                        ) {
                            it / 5
                        }
                },

                exitTransition = {
                    fadeOut(
                        tween(
                            durationMillis = 150
                        )
                    ) +
                        slideOutHorizontally(
                            tween(
                                durationMillis = 150
                            )
                        ) {
                            -it / 5
                        }
                },

                popEnterTransition = {
                    fadeIn(
                        tween(
                            durationMillis = 180
                        )
                    ) +
                        slideInHorizontally(
                            tween(
                                durationMillis = 180
                            )
                        ) {
                            -it / 5
                        }
                },

                popExitTransition = {
                    fadeOut(
                        tween(
                            durationMillis = 150
                        )
                    ) +
                        slideOutHorizontally(
                            tween(
                                durationMillis = 150
                            )
                        ) {
                            it / 5
                        }
                }

            ) {

                // ----------------------------------------------------
                // Home
                // ----------------------------------------------------

                composable(
                    route =
                        LiasRoute.Home.route
                ) {
                    HomeScreen(
                        viewModel =
                            liasViewModel,

                        onNavigateToDeviceDetail = { pdid ->
                            navController.navigate(
                                LiasRoute
                                    .DeviceDetail
                                    .create(
                                        pdid
                                    )
                            )
                        },

                        onNavigateToTab = { tab ->
                            navigateToTab(
                                navController,
                                tab
                            )
                        }
                    )
                }

                // ----------------------------------------------------
                // Devices
                // ----------------------------------------------------

                composable(
                    route =
                        LiasRoute.Devices.route
                ) {
                    DevicesScreen(
                        viewModel =
                            liasViewModel,

                        onNavigateToDeviceDetail = { pdid ->
                            navController.navigate(
                                LiasRoute
                                    .DeviceDetail
                                    .create(
                                        pdid
                                    )
                            )
                        }
                    )
                }

                composable(
                    route =
                        LiasRoute
                            .DeviceDetail
                            .route,

                    arguments =
                        listOf(
                            navArgument(
                                LiasRoute
                                    .DeviceDetail
                                    .ARG_PDID
                            ) {
                                type =
                                    NavType.StringType
                            }
                        )
                ) { destination ->

                    val pdid =
                        destination
                            .arguments
                            ?.getString(
                                LiasRoute
                                    .DeviceDetail
                                    .ARG_PDID
                            )
                            .orEmpty()

                    DeviceDetailScreen(
                        pdid =
                            pdid,
                        viewModel =
                            liasViewModel,
                        onBack = {
                            navController
                                .popBackStack()
                        }
                    )
                }

                // ----------------------------------------------------
                // Schedules
                // ----------------------------------------------------

                composable(
                    route =
                        LiasRoute.Schedules.route
                ) {
                    SchedulesScreen(
                        viewModel =
                            liasViewModel
                    )
                }

                // ----------------------------------------------------
                // Rules
                // ----------------------------------------------------

                composable(
                    route =
                        LiasRoute.Rules.route
                ) {
                    RulesScreen(
                        viewModel =
                            liasViewModel
                    )
                }

                // ----------------------------------------------------
                // Settings
                // ----------------------------------------------------

                composable(
                    route =
                        LiasRoute.Settings.route
                ) {
                    SettingsScreen(
                        viewModel =
                            settingsViewModel,
                        onNavigateToConnection = {
                            navController.navigate(
                                LiasRoute
                                    .ConnectionSettings
                                    .route
                            )
                        }
                    )
                }

                composable(
                    route =
                        LiasRoute
                            .ConnectionSettings
                            .route
                ) {
                    ConnectionSettingsScreen(
                        viewModel =
                            settingsViewModel,
                        onBack = {
                            navController
                                .popBackStack()
                        }
                    )
                }
            }

            UndoToast(
                undoState =
                    undoState,
                onDismiss = {
                    liasViewModel
                        .clearUndo()
                },
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            bottom =
                                HigSpec.BottomNavPadding +
                                    12.dp
                        )
            )
        }
    }
}

private fun navigateToTab(
    navController: NavHostController,
    tab: LiasTab
) {
    navController.navigate(
        tab.route
    ) {

        popUpTo(
            navController
                .graph
                .findStartDestination()
                .id
        ) {
            saveState =
                true
        }

        launchSingleTop =
            true

        restoreState =
            true
    }
}
