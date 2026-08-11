// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 34.1.1
//
// Purpose:
//   Canonical application navigation graph.
//
// Batch 25:
//   - Consumes Batch-24 external deep link.
//   - Waits for DataStore hydration before showing Connect.
//   - Deep links are deferred until configuration is available.
//   - Main tabs preserve state.
//   - Device detail remains outside tab identity.
//   - Bottom tab targets expose selectable semantics.
//   - Server connectivity is separate from configuration status.
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.lias.remote.ui.screens.identity.IdentityReviewScreen
import com.lias.remote.ui.screens.rules.RulesScreen
import com.lias.remote.ui.screens.schedules.SchedulesScreen
import com.lias.remote.ui.screens.settings.ConnectionSettingsScreen
import com.lias.remote.ui.screens.settings.SettingsScreen
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoNavigationBar
import com.slapps.cupertino.CupertinoNavigationBarItem
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.CupertinoScaffold
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.Clock
import com.slapps.cupertino.icons.outlined.Gear
import com.slapps.cupertino.icons.outlined.House
import com.slapps.cupertino.icons.outlined.Iphone
import com.slapps.cupertino.icons.outlined.Shield

sealed class LiasScreen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {

    data object Home :
        LiasScreen(
            NavigationRoutes.HOME,
            "Home",
            CupertinoIcons.Outlined.House
        )

    data object Devices :
        LiasScreen(
            NavigationRoutes.DEVICES,
            "Devices",
            CupertinoIcons.Outlined.Iphone
        )

    data object Schedules :
        LiasScreen(
            NavigationRoutes.SCHEDULES,
            "Schedules",
            CupertinoIcons.Outlined.Clock
        )

    data object Rules :
        LiasScreen(
            NavigationRoutes.RULES,
            "Rules",
            CupertinoIcons.Outlined.Shield
        )

    data object Settings :
        LiasScreen(
            NavigationRoutes.SETTINGS,
            "Settings",
            CupertinoIcons.Outlined.Gear
        )
}

@Composable
fun LiasNavHost(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel,
    externalDeepLink: String? = null,
    onExternalDeepLinkConsumed: () -> Unit = {}
) {

    val settingsState by
        settingsViewModel
            .uiState
            .collectAsState()

    /*
     * DataStore is asynchronous. Rendering Connect before hydration
     * causes an incorrect first-frame flash on already configured apps.
     */
    if (
        !settingsState
            .isConfigurationLoaded
    ) {

        ConfigurationLoadingScreen()

        return
    }

    AnimatedContent(
        targetState =
            settingsState
                .isConfigured,
        transitionSpec = {
            fadeIn(
                tween(
                    180
                )
            ) togetherWith
                fadeOut(
                    tween(
                        150
                    )
                )
        },
        label =
            "lias-configuration-root"
    ) {
        configured ->

        if (
            configured
        ) {
            ConfiguredLiasApp(
                liasViewModel =
                    liasViewModel,
                settingsViewModel =
                    settingsViewModel,
                externalDeepLink =
                    externalDeepLink,
                onExternalDeepLinkConsumed =
                    onExternalDeepLinkConsumed
            )
        } else {
            ConnectScreen(
                viewModel =
                    settingsViewModel,
                onConnected = {
                    /*
                     * DataStore changes targetState; AnimatedContent
                     * owns the root presentation transition.
                     */
                }
            )
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun ConfiguredLiasApp(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel,
    externalDeepLink: String?,
    onExternalDeepLinkConsumed: () -> Unit
) {
    val settingsState by
        settingsViewModel
            .uiState
            .collectAsState()

    val uiState by
        liasViewModel
            .state
            .collectAsState()

    val navController =
        rememberNavController()

    val tabItems =
        remember {
            listOf(
                LiasScreen.Home,
                LiasScreen.Devices,
                LiasScreen.Schedules,
                LiasScreen.Rules,
                LiasScreen.Settings
            )
        }

    val undoState by
        liasViewModel
            .undoState
            .collectAsState()

    val securityAlert by
        liasViewModel
            .pendingSecurityAlert
            .collectAsState()

    /*
     * Consume external navigation only after:
     *   1. settings have hydrated
     *   2. server configuration exists
     *   3. NavController exists
     */
    LaunchedEffect(
        externalDeepLink
    ) {

        val raw =
            externalDeepLink
                ?: return@LaunchedEffect

        val destination =
            LiasDeepLinks.parse(
                raw
            )

        if (
            destination !=
            null
        ) {

            navigateExternal(
                navController =
                    navController,
                destination =
                    destination
            )
        }

        /*
         * Invalid navigation links are consumed as well so malformed
         * intents cannot trigger endlessly after recomposition.
         */
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
                     * Existing backend security-alert event does not
                     * provide a verified block-action contract here.
                     * Do not fabricate a policy.
                     */
                    liasViewModel
                        .dismissSecurityAlert()
                },
                onTrust = {
                    /*
                     * Same rule: only dismiss until LIAS exposes a
                     * dedicated trust mutation contract.
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

    val currentRootTab =
        rootTabRoute(
            currentDestination
                ?.route
        )

    val showTabBar =
        currentRootTab !=
            null

    CupertinoScaffold(
        topBar = {

            /*
             * Each root screen already owns its large title. Keep the
             * global top area empty during normal operation and surface
             * only transient connection state here.
             */
            if (
                uiState.connectionState !=
                ConnectionState.CONNECTED
            ) {

                ConnectionBanner(
                    state =
                        uiState.connectionState
                )
            }
        },
        bottomBar = {

            if (
                showTabBar
            ) {

                CupertinoNavigationBar {

                    tabItems.forEach {
                        screen ->

                        CupertinoNavigationBarItem(
                            selected =
                                currentRootTab ==
                                    screen.route,
                            onClick = {

                                navController.navigate(
                                    screen.route
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
                            },
                            icon = {
                                CupertinoIcon(
                                    imageVector =
                                        screen.icon,
                                    contentDescription =
                                        null,
                                    /*
                                     * Slanoss CupertinoNavigationBarItem
                                     * owns a 20.dp icon slot. Keep its
                                     * layout/semantics untouched and scale
                                     * only the glyph drawing to ~24.dp.
                                     */
                                    modifier =
                                        Modifier.graphicsLayer {
                                            scaleX =
                                                1.20f
                                            scaleY =
                                                1.20f
                                        }
                                )
                            },
                            label = {
                                CupertinoText(
                                    text =
                                        screen.label
                                )
                            },
                            alwaysShowLabel =
                                true
                        )
                    }
                }
            }
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

                    if (
                        isRootTabTransition(
                            initialState.destination.route,
                            targetState.destination.route
                        )
                    ) {
                        EnterTransition.None
                    } else {
                        slideInHorizontally(
                            animationSpec =
                                tween(
                                    300
                                )
                        ) {
                            width ->
                            width
                        }
                    }
                },
                exitTransition = {

                    if (
                        isRootTabTransition(
                            initialState.destination.route,
                            targetState.destination.route
                        )
                    ) {
                        ExitTransition.None
                    } else {
                        slideOutHorizontally(
                            animationSpec =
                                tween(
                                    300
                                )
                        ) {
                            width ->
                            -width / 3
                        }
                    }
                },
                popEnterTransition = {

                    if (
                        isRootTabTransition(
                            initialState.destination.route,
                            targetState.destination.route
                        )
                    ) {
                        EnterTransition.None
                    } else {
                        slideInHorizontally(
                            animationSpec =
                                tween(
                                    300
                                )
                        ) {
                            width ->
                            -width / 3
                        }
                    }
                },
                popExitTransition = {

                    if (
                        isRootTabTransition(
                            initialState.destination.route,
                            targetState.destination.route
                        )
                    ) {
                        ExitTransition.None
                    } else {
                        slideOutHorizontally(
                            animationSpec =
                                tween(
                                    300
                                )
                        ) {
                            width ->
                            width
                        }
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

                            navController.navigate(
                                screen.route
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
                        },
                        onNavigateToDevicesForTag = {
                            tagId ->

                            navController.navigate(
                                NavigationRoutes
                                    .devicesByTag(
                                        tagId
                                    )
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
                            }
                        },
                        onNavigateToIdentityReview = {
                            navController.navigate(
                                NavigationRoutes.IDENTITY_REVIEW
                            )
                        }
                    )
                }

                composable(
                    NavigationRoutes.IDENTITY_REVIEW
                ) {
                    IdentityReviewScreen(
                        viewModel = liasViewModel,
                        onBack = {
                            navController.popBackStack()
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
                        NavigationRoutes.DEVICES_BY_TAG,
                    arguments =
                        listOf(
                            navArgument(
                                "tagId"
                            ) {
                                type =
                                    NavType.StringType
                            }
                        )
                ) {
                    entry ->

                    val tagId =
                        entry.arguments
                            ?.getString(
                                "tagId"
                            )

                    DevicesScreen(
                        viewModel =
                            liasViewModel,
                        initialTagId =
                            tagId,
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
                        NavigationRoutes.DEVICE_DETAIL,
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
                    entry ->

                    val pdid =
                        entry.arguments
                            ?.getString(
                                "pdid"
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
                            )
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
                                if (
                                    showTabBar
                                ) {
                                    HigSpec.TabBarHeight +
                                        12.dp
                                } else {
                                    16.dp
                                }
                        )
            )
        }
    }
}

@Composable
private fun ConfigurationLoadingScreen() {

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
                LiasThemeColors
                    .secondaryLabel
        )
    }
}

@Composable
private fun ConnectionBanner(
    state: ConnectionState
) {

    val text =
        when (
            state
        ) {

            ConnectionState.CONNECTING ->
                "Connecting to LIAS…"

            ConnectionState.RECONNECTING ->
                "Reconnecting to LIAS…"

            ConnectionState.DISCONNECTED ->
                "LIAS is currently unreachable"

            ConnectionState.CONNECTED ->
                ""
        }

    if (
        text.isBlank()
    ) {
        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    LiasThemeColors.orange
                        .copy(
                            alpha =
                                0.16f
                        )
                )
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        7.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        CupertinoText(
            text =
                text,
            style =
                HigTypography.subheadline,
            color =
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )
    }
}

private fun rootTabRoute(
    route: String?
): String? =
    when (
        route
    ) {
        NavigationRoutes.HOME ->
            NavigationRoutes.HOME

        NavigationRoutes.DEVICES,
        NavigationRoutes.DEVICES_BY_TAG ->
            NavigationRoutes.DEVICES

        NavigationRoutes.SCHEDULES ->
            NavigationRoutes.SCHEDULES

        NavigationRoutes.RULES ->
            NavigationRoutes.RULES

        NavigationRoutes.SETTINGS ->
            NavigationRoutes.SETTINGS

        else ->
            null
    }

private fun isRootTabTransition(
    fromRoute: String?,
    toRoute: String?
): Boolean =
    rootTabRoute(fromRoute) != null &&
        rootTabRoute(toRoute) != null

private fun navigateExternal(
    navController:
        androidx.navigation.NavHostController,
    destination:
        ExternalDestination
) {

    val route =
        when (
            destination
        ) {

            ExternalDestination.Home ->
                NavigationRoutes.HOME

            ExternalDestination.Devices ->
                NavigationRoutes.DEVICES

            ExternalDestination.Schedules ->
                NavigationRoutes.SCHEDULES

            ExternalDestination.Rules ->
                NavigationRoutes.RULES

            ExternalDestination.Settings ->
                NavigationRoutes.SETTINGS

            is ExternalDestination.Device ->
                NavigationRoutes
                    .deviceDetail(
                        destination.pdid
                    )
        }

    navController.navigate(
        route
    ) {

        launchSingleTop =
            true

        if (
            destination !is
            ExternalDestination.Device
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

            restoreState =
                true
        }
    }
}
