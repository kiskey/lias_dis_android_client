// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 3.0.0
//
// Purpose:
//   Primary Cupertino navigation host.
//
// Changes:
//   - Uses canonical LiasRoute definitions.
//   - Uses canonical LiasTab definitions.
//   - Device routes are generated through LiasRoute.DeviceDetail.create().
//   - Dynamic PDID arguments are URI-safe.
//   - Deep links are handled at the navigation boundary.
//   - Connection state no longer requires a fake navigation destination.
//   - Cupertino navigation remains the visual system.
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
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
        settingsViewModel.uiState.collectAsState()

    val uiState by
        liasViewModel.state.collectAsState()

    val undoState by
        liasViewModel.undoState.collectAsState()

    val securityAlert by
        liasViewModel.pendingSecurityAlert.collectAsState()

    val isConnected =
        settingsState.savedServerUrl.isNotBlank()

    /*
     * Connection is configuration state, not a navigation destination.
     *
     * This prevents the previous architecture where the NavController
     * could contain a stale "main" destination while the application
     * was actually showing ConnectScreen.
     */
    if (!isConnected) {
        ConnectScreen(
            viewModel = settingsViewModel,
            onConnected = {}
        )
        return
    }

    LaunchedEffect(
        pendingDeepLink
    ) {
        when (
            val deepLink =
                pendingDeepLink
        ) {
            is LiasDeepLink.Device -> {
                navController.navigate(
                    LiasRoute.DeviceDetail.create(
                        deepLink.pdid
                    )
                ) {
                    launchSingleTop = true
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

            null -> Unit
        }
    }

    if (!settingsState.isOnboarded) {
        OnboardingSheet(
            onComplete = {
                settingsViewModel.completeOnboarding()
            }
        )
    }

    securityAlert?.let { alert ->

        SecurityAlertSheet(
            alert = alert,
            onDismiss = {
                liasViewModel.dismissSecurityAlert()
            },
            onBlock = {
                liasViewModel.dismissSecurityAlert()
            },
            onTrust = {
                liasViewModel.dismissSecurityAlert()
            }
        )
    }

    val navBackStackEntry by
        navController.currentBackStackEntryAsState()

    val currentDestination =
        navBackStackEntry?.destination

    CupertinoScaffold(

        topBar = {
            Column {

                CupertinoTopAppBar(
                    title = {
                        CupertinoText(
                            "LIAS Remote"
                        )
                    }
                )

                if (
                    uiState.connectionState !=
                    ConnectionState.CONNECTED
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    LiasThemeColors.orange
                                )
                                .padding(
                                    vertical = 6.dp,
                                    horizontal = 16.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        CupertinoText(
                            text =
                                connectionLabel(
                                    uiState.connectionState
                                ),
                            style =
                                HigTypography.subheadline,
                            color =
                                LiasThemeColors.label,
                            textAlign =
                                TextAlign.Center
                        )
                    }
                }
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

                    val isSelected =
                        currentDestination
                            ?.hierarchy
                            ?.any {
                                it.route ==
                                    tab.route
                            } == true

                    val color =
                        if (isSelected) {
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
                                .weight(1f)
                                .clickable(
                                    interactionSource =
                                        interactionSource,
                                    indication = null
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
                            tint = color,
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
                            text = tab.label,
                            style =
                                HigTypography.tabLabel,
                            color = color
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
                        tween(180)
                    ) +
                        slideInHorizontally(
                            tween(180)
                        ) {
                            it / 5
                        }
                },

                exitTransition = {
                    fadeOut(
                        tween(150)
                    ) +
                        slideOutHorizontally(
                            tween(150)
                        ) {
                            -it / 5
                        }
                },

                popEnterTransition = {
                    fadeIn(
                        tween(180)
                    ) +
                        slideInHorizontally(
                            tween(180)
                        ) {
                            -it / 5
                        }
                },

                popExitTransition = {
                    fadeOut(
                        tween(150)
                    ) +
                        slideOutHorizontally(
                            tween(150)
                        ) {
                            it / 5
                        }
                }

            ) {

                composable(
                    route =
                        LiasRoute.Home.route
                ) {
                    HomeScreen(
                        viewModel =
                            liasViewModel,

                        onNavigateToDeviceDetail = {
                            pdid ->
                            navController.navigate(
                                LiasRoute.DeviceDetail.create(
                                    pdid
                                )
                            )
                        },

                        onNavigateToTab = {
                            tab ->
                            navigateToTab(
                                navController,
                                tab
                            )
                        }
                    )
                }

                composable(
                    route =
                        LiasRoute.Devices.route
                ) {
                    DevicesScreen(
                        viewModel =
                            liasViewModel,

                        onNavigateToDeviceDetail = {
                            pdid ->
                            navController.navigate(
                                LiasRoute.DeviceDetail.create(
                                    pdid
                                )
                            )
                        }
                    )
                }

                composable(
                    route =
                        LiasRoute.DeviceDetail.route,

                    arguments =
                        listOf(
                            navArgument(
                                LiasRoute.DeviceDetail.ARG_PDID
                            ) {
                                type =
                                    NavType.StringType
                            }
                        )
                ) { backStackEntry ->

                    val pdid =
                        backStackEntry
                            .arguments
                            ?.getString(
                                LiasRoute.DeviceDetail.ARG_PDID
                            )
                            .orEmpty()

                    DeviceDetailScreen(
                        pdid = pdid,
                        viewModel =
                            liasViewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route =
                        LiasRoute.Schedules.route
                ) {
                    SchedulesScreen(
                        viewModel =
                            liasViewModel
                    )
                }

                composable(
                    route =
                        LiasRoute.Rules.route
                ) {
                    RulesScreen(
                        viewModel =
                            liasViewModel
                    )
                }

                composable(
                    route =
                        LiasRoute.Settings.route
                ) {
                    SettingsScreen(
                        viewModel =
                            settingsViewModel,
                        onNavigateToConnection = {
                            navController.navigate(
                                LiasRoute.ConnectionSettings.route
                            )
                        }
                    )
                }

                composable(
                    route =
                        LiasRoute.ConnectionSettings.route
                ) {
                    ConnectionSettingsScreen(
                        viewModel =
                            settingsViewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            UndoToast(
                undoState =
                    undoState,

                onDismiss = {
                    liasViewModel.clearUndo()
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
    navController:
        androidx.navigation.NavHostController,
    tab: LiasTab
) {
    navController.navigate(
        tab.route
    ) {
        popUpTo(
            navController.graph
                .findStartDestination()
                .id
        ) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}

private fun connectionLabel(
    state: ConnectionState
): String =
    when (state) {

        ConnectionState.CONNECTING ->
            "Connecting to Server…"

        ConnectionState.RECONNECTING ->
            "Reconnecting to Server…"

        ConnectionState.CONNECTED ->
            ""

        ConnectionState.DISCONNECTED ->
            "Server Disconnected"
    }
