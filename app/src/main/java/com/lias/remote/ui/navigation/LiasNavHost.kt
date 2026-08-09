// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 27.2.0
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

    val uiState by
        liasViewModel
            .state
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

    if (
        !settingsState
            .isConfigured
    ) {

        ConnectScreen(
            viewModel =
                settingsViewModel,
            onConnected = {
                /*
                 * Settings state updates through DataStore and this
                 * composable naturally transitions to the app graph.
                 */
            }
        )

        return
    }

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

    val showTabBar =
        currentDestination
            ?.route !=
            NavigationRoutes.DEVICE_DETAIL &&
            currentDestination
                ?.route !=
            NavigationRoutes.CONNECTION_SETTINGS

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

                /*
                 * Configured != Connected.
                 *
                 * Users stay inside the app during temporary SSE loss.
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
            }
        },
        bottomBar = {

            if (
                showTabBar
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                HigSpec.TabBarHeight
                            )
                            .background(
                                LiasThemeColors
                                    .secondaryBackground
                            )
                            .padding(
                                top =
                                    4.dp,
                                bottom =
                                    4.dp
                            ),
                    horizontalArrangement =
                        Arrangement.SpaceAround,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    tabItems.forEach {
                        screen ->

                        val selected =
                            currentDestination
                                ?.hierarchy
                                ?.any {
                                    destination ->

                                    destination.route ==
                                        screen.route
                                }
                                ?: false

                        TabItem(
                            screen =
                                screen,
                            selected =
                                selected,
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
                            }
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
                            it /
                                5
                        }
                },
                exitTransition = {

                    fadeOut(
                        tween(
                            150
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
                            -it /
                                5
                        }
                },
                popExitTransition = {

                    fadeOut(
                        tween(
                            150
                        )
                    ) +
                        slideOutHorizontally(
                            tween(
                                180
                            )
                        ) {
                            it /
                                5
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
                                    HigSpec.BottomNavPadding +
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

@Composable
private fun RowScope.TabItem(
    screen: LiasScreen,
    selected: Boolean,
    onClick: () -> Unit
) {

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val color =
        if (
            selected
        ) {
            LiasThemeColors.blue
        } else {
            LiasThemeColors
                .tertiaryLabel
        }

    Column(
        modifier =
            Modifier
                .weight(
                    1f
                )
                .semantics(
                    mergeDescendants =
                        true
                ) {

                    role =
                        Role.Tab

                    this.selected =
                        selected
                }
                .clickable(
                    interactionSource =
                        interactionSource,
                    indication =
                        null,
                    onClick =
                        onClick
                )
                .padding(
                    vertical =
                        4.dp
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
                null,
            tint =
                color,
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
                color
        )
    }
}

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
