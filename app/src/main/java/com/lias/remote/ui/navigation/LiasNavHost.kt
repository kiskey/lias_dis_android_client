// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 20.0.0
//
// Purpose:
//   Application navigation root.
//
// Batch 20 guarantees:
//   - No Connect-screen flash while DataStore hydrates.
//   - Configured != currently connected.
//   - Server outage does not destroy navigation state.
//   - Tab stacks use saveState/restoreState.
//   - External deep links survive configuration gating.
//   - Device deep links wait for initial inventory hydration.
//   - Deleted/unknown device deep links reach DeviceDetailScreen's
//     explicit "Device Unavailable" state.
//   - Deep-link device details have Devices as logical parent.
//   - New Intents can be consumed while Activity is alive.
// ====================================================================

package com.lias.remote.ui.navigation

import android.net.Uri
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Gear
import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

sealed class LiasScreen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {

    data object Home :
        LiasScreen(
            NavigationRoutes.HOME,
            "Home",
            CupertinoIcons
                .Outlined
                .House
        )

    data object Devices :
        LiasScreen(
            NavigationRoutes.DEVICES,
            "Devices",
            CupertinoIcons
                .Outlined
                .Iphone
        )

    data object Schedules :
        LiasScreen(
            NavigationRoutes.SCHEDULES,
            "Schedules",
            CupertinoIcons
                .Outlined
                .Clock
        )

    data object Rules :
        LiasScreen(
            NavigationRoutes.RULES,
            "Rules",
            CupertinoIcons
                .Outlined
                .Shield
        )

    data object Settings :
        LiasScreen(
            NavigationRoutes.SETTINGS,
            "Settings",
            CupertinoIcons
                .Outlined
                .Gear
        )
}

private val rootTabs =
    listOf(
        LiasScreen.Home,
        LiasScreen.Devices,
        LiasScreen.Schedules,
        LiasScreen.Rules,
        LiasScreen.Settings
    )

@Composable
fun LiasNavHost(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel,
    externalDeepLink: String? = null,
    onExternalDeepLinkConsumed:
        () -> Unit = {}
) {

    /*
     * Keep NavController above all gates.
     *
     * A temporary configuration/connection presentation must not
     * create a second controller or discard restored tab stacks.
     */
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
     * DataStore has not hydrated yet.
     *
     * Do NOT infer "not configured" from the initial blank fields.
     */
    if (
        !settingsState
            .isConfigurationLoaded
    ) {

        LaunchLoadingScreen()

        return
    }

    /*
     * Configuration gate.
     *
     * externalDeepLink remains unconsumed here. After a successful
     * Connect, this same composable proceeds and handles it.
     */
    if (
        !settingsState
            .isConfigured
    ) {

        ConnectScreen(
            viewModel =
                settingsViewModel,
            onConnected = {
                /*
                 * No explicit navigation required.
                 *
                 * savedServerUrl changing makes isConfigured=true,
                 * revealing the already-owned NavController.
                 */
            }
        )

        return
    }

    /*
     * Handle an external URI only when configuration exists.
     *
     * For device destinations, wait until the first inventory load
     * completes. This prevents a valid device deep link from being
     * declared missing merely because REST hydration is unfinished.
     */
    LaunchedEffect(
        externalDeepLink,
        settingsState.isConfigured,
        uiState.isInitialLoaded
    ) {

        val raw =
            externalDeepLink
                ?: return@LaunchedEffect

        val destination =
            LiasDeepLinks.parse(
                raw
            )

        if (
            destination ==
            null
        ) {

            onExternalDeepLinkConsumed()

            return@LaunchedEffect
        }

        if (
            destination is
                ExternalDestination.Device &&
            !uiState.isInitialLoaded
        ) {
            return@LaunchedEffect
        }

        navigateExternalDestination(
            navController =
                navController,
            destination =
                destination
        )

        onExternalDeepLinkConsumed()
    }

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

    securityAlert
        ?.let {
            alert ->

            SecurityAlertSheet(
                alert =
                    alert,
                onDismiss = {
                    liasViewModel
                        .dismissSecurityAlert()
                },
                onBlock = {
                    /*
                     * Security response behavior remains outside
                     * navigation scope.
                     */
                    liasViewModel
                        .dismissSecurityAlert()
                },
                onTrust = {
                    liasViewModel
                        .dismissSecurityAlert()
                }
            )
        }

    val navBackStackEntry by
        navController
            .currentBackStackEntryAsState()

    val currentDestination =
        navBackStackEntry
            ?.destination

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

                    ConnectionBanner(
                        connectionState =
                            uiState.connectionState,
                        onClick = {

                            navController.navigate(
                                NavigationRoutes
                                    .CONNECTION_SETTINGS
                            ) {
                                launchSingleTop =
                                    true
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {

            LiasTabBar(
                navController =
                    navController,
                currentDestination =
                    currentDestination
            )
        }
    ) {
        innerPadding ->

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            NavHost(
                navController =
                    navController,
                startDestination =
                    NavigationRoutes.HOME,
                modifier =
                    Modifier.padding(
                        innerPadding
                    ),
                enterTransition = {

                    fadeIn(
                        tween(
                            180
                        )
                    ) +
                        slideInHorizontally(
                            tween(
                                180
                            )
                        ) {
                            it / 6
                        }
                },
                exitTransition = {

                    fadeOut(
                        tween(
                            160
                        )
                    )
                },
                popEnterTransition = {

                    fadeIn(
                        tween(
                            180
                        )
                    ) +
                        slideInHorizontally(
                            tween(
                                180
                            )
                        ) {
                            -it / 6
                        }
                },
                popExitTransition = {

                    fadeOut(
                        tween(
                            160
                        )
                    ) +
                        slideOutHorizontally(
                            tween(
                                160
                            )
                        ) {
                            it / 6
                        }
                }
            ) {

                composable(
                    NavigationRoutes.HOME
                ) {

                    HomeScreen(
                        viewModel =
                            liasViewModel,
                        onNavigateToDeviceDetail = {
                            pdid ->

                            navController.navigate(
                                NavigationRoutes
                                    .deviceDetail(
                                        pdid
                                    )
                            )
                        },
                        onNavigateToTab = {
                            screen ->

                            navigateTab(
                                navController,
                                screen.route
                            )
                        }
                    )
                }

                composable(
                    NavigationRoutes.DEVICES
                ) {

                    DevicesScreen(
                        viewModel =
                            liasViewModel,
                        onNavigateToDeviceDetail = {
                            pdid ->

                            navController.navigate(
                                NavigationRoutes
                                    .deviceDetail(
                                        pdid
                                    )
                            )
                        }
                    )
                }

                composable(
                    route =
                        NavigationRoutes
                            .DEVICE_DETAIL,
                    arguments =
                        listOf(
                            navArgument(
                                "pdid"
                            ) {
                                type =
                                    NavType.StringType
                            }
                        )
                ) {
                    backStackEntry ->

                    val pdid =
                        backStackEntry
                            .arguments
                            ?.getString(
                                "pdid"
                            )
                            ?.let {
                                Uri.decode(
                                    it
                                )
                            }
                            .orEmpty()

                    DeviceDetailScreen(
                        pdid =
                            pdid,
                        viewModel =
                            liasViewModel,
                        onBack = {

                            val popped =
                                navController
                                    .popBackStack()

                            if (
                                !popped
                            ) {

                                navigateTab(
                                    navController,
                                    NavigationRoutes.DEVICES
                                )
                            }
                        }
                    )
                }

                composable(
                    NavigationRoutes.SCHEDULES
                ) {

                    SchedulesScreen(
                        viewModel =
                            liasViewModel
                    )
                }

                composable(
                    NavigationRoutes.RULES
                ) {

                    RulesScreen(
                        viewModel =
                            liasViewModel
                    )
                }

                composable(
                    NavigationRoutes.SETTINGS
                ) {

                    SettingsScreen(
                        viewModel =
                            settingsViewModel,
                        onNavigateToConnection = {

                            navController.navigate(
                                NavigationRoutes
                                    .CONNECTION_SETTINGS
                            ) {
                                launchSingleTop =
                                    true
                            }
                        }
                    )
                }

                composable(
                    NavigationRoutes
                        .CONNECTION_SETTINGS
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
                                HigSpec
                                    .BottomNavPadding +
                                    12.dp
                        )
            )
        }
    }
}

@Composable
private fun LaunchLoadingScreen() {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    LiasThemeColors.background
                ),
        contentAlignment =
            Alignment.Center
    ) {

        CupertinoText(
            text =
                "Loading LIAS…",
            style =
                HigTypography.body,
            color =
                LiasThemeColors.secondaryLabel
        )
    }
}

@Composable
private fun ConnectionBanner(
    connectionState: ConnectionState,
    onClick: () -> Unit
) {

    val message =
        when (
            connectionState
        ) {

            ConnectionState.CONNECTING ->
                "Connecting to LIAS…"

            ConnectionState.RECONNECTING ->
                "Connection interrupted · Reconnecting…"

            ConnectionState.DISCONNECTED ->
                "LIAS is offline · Tap for connection settings"

            ConnectionState.CONNECTED ->
                ""
        }

    if (
        message.isBlank()
    ) {
        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    LiasThemeColors.orange
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 7.dp,
                    horizontal = 16.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        CupertinoText(
            text =
                message,
            style =
                HigTypography.subheadline,
            color =
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun LiasTabBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {

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

        rootTabs.forEach {
            screen ->

            val selected =
                currentDestination
                    ?.hierarchy
                    ?.any {
                        destination ->

                        destination.route ==
                            screen.route
                    } ==
                    true

            val tint =
                if (
                    selected
                ) {
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
                        .semantics {
                            role =
                                Role.Tab
                        }
                        .clickable(
                            interactionSource =
                                interactionSource,
                            indication =
                                null
                        ) {

                            navigateTab(
                                navController,
                                screen.route
                            )
                        }
                        .padding(
                            vertical = 4.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {

                CupertinoIcon(
                    imageVector =
                        screen.icon,
                    contentDescription =
                        screen.label,
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
                        screen.label,
                    style =
                        HigTypography.tabLabel,
                    color =
                        tint
                )
            }
        }
    }
}

private fun navigateTab(
    navController: NavHostController,
    route: String
) {

    navController.navigate(
        route
    ) {

        popUpTo(
            navController.graph
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

private fun navigateExternalDestination(
    navController: NavHostController,
    destination: ExternalDestination
) {

    when (
        destination
    ) {

        ExternalDestination.Home ->

            navigateTab(
                navController,
                NavigationRoutes.HOME
            )

        ExternalDestination.Devices ->

            navigateTab(
                navController,
                NavigationRoutes.DEVICES
            )

        ExternalDestination.Schedules ->

            navigateTab(
                navController,
                NavigationRoutes.SCHEDULES
            )

        ExternalDestination.Rules ->

            navigateTab(
                navController,
                NavigationRoutes.RULES
            )

        ExternalDestination.Settings ->

            navigateTab(
                navController,
                NavigationRoutes.SETTINGS
            )

        is ExternalDestination.Device -> {

            /*
             * External device navigation gets a predictable parent:
             *
             * Devices
             *    ↓
             * Device Detail
             *
             * Back therefore leads to inventory rather than whatever
             * arbitrary tab happened to be visible previously.
             */
            navigateTab(
                navController,
                NavigationRoutes.DEVICES
            )

            navController.navigate(
                NavigationRoutes
                    .deviceDetail(
                        destination.pdid
                    )
            ) {
                launchSingleTop =
                    true
            }
        }
    }
}
