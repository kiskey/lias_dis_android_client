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
    data object Home : LiasScreen("home", "Home", CupertinoIcons.Outlined.House)
    data object Devices : LiasScreen("devices", "Devices", CupertinoIcons.Outlined.Iphone)
    data object Schedules : LiasScreen("schedules", "Schedules", CupertinoIcons.Outlined.Clock)
    data object Rules : LiasScreen("rules", "Rules", CupertinoIcons.Outlined.Shield)
    data object Settings : LiasScreen("settings", "Settings", CupertinoIcons.Outlined.Gear)
}

@Composable
fun LiasNavHost(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val items = listOf(LiasScreen.Home, LiasScreen.Devices, LiasScreen.Schedules, LiasScreen.Rules, LiasScreen.Settings)
    val settingsState by settingsViewModel.uiState.collectAsState()
    val uiState by liasViewModel.state.collectAsState()
    val isConnected = settingsState.savedServerUrl.isNotBlank()

    val undoState by liasViewModel.undoState.collectAsState()
    val securityAlert by liasViewModel.pendingSecurityAlert.collectAsState()

    if (!isConnected) {
        ConnectScreen(viewModel = settingsViewModel, onConnected = {})
        return
    }

    if (!settingsState.isOnboarded) {
        OnboardingSheet(onComplete = { settingsViewModel.completeOnboarding() })
    }

    securityAlert?.let { alert ->
        SecurityAlertSheet(
            alert = alert,
            onDismiss = { liasViewModel.dismissSecurityAlert() },
            onBlock = { liasViewModel.dismissSecurityAlert() },
            onTrust = { liasViewModel.dismissSecurityAlert() }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    CupertinoScaffold(
        topBar = {
            Column {
                CupertinoTopAppBar(
                    title = { CupertinoText("LIAS Remote — HIG Redesign") }
                )
                // Connection Banner
                if (uiState.connectionState != ConnectionState.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LiasThemeColors.orange)
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CupertinoText(
                            text = when (uiState.connectionState) {
                                ConnectionState.CONNECTING -> "Connecting to LIAS Server…"
                                ConnectionState.RECONNECTING -> "Reconnecting to LIAS Server…"
                                else -> "Disconnected from LIAS Server"
                            },
                            style = HigTypography.subheadline,
                            color = LiasThemeColors.label,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HigSpec.TabBarHeight)
                    .background(LiasThemeColors.secondaryBackground)
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val color = if (isSelected) LiasThemeColors.blue else LiasThemeColors.tertiaryLabel
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CupertinoIcon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            tint = color,
                            modifier = Modifier.size(HigSpec.IconSizeM)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CupertinoText(
                            text = screen.label,
                            style = HigTypography.tabLabel,
                            color = color
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = LiasScreen.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 4 } },
                exitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 } },
                popEnterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 4 } },
                popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 } }
            ) {
                composable(LiasScreen.Home.route) {
                    HomeScreen(
                        viewModel = liasViewModel,
                        onNavigateToDeviceDetail = { pdid -> navController.navigate("device_detail/$pdid") },
                        onNavigateToTab = { screen ->
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(LiasScreen.Devices.route) {
                    DevicesScreen(
                        viewModel = liasViewModel,
                        onNavigateToDeviceDetail = { pdid -> navController.navigate("device_detail/$pdid") }
                    )
                }
                composable(
                    route = "device_detail/{pdid}",
                    arguments = listOf(navArgument("pdid") { type = NavType.StringType })
                ) { backStackEntry ->
                    val pdid = backStackEntry.arguments?.getString("pdid") ?: ""
                    DeviceDetailScreen(
                        pdid = pdid,
                        viewModel = liasViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(LiasScreen.Schedules.route) { SchedulesScreen(viewModel = liasViewModel) }
                composable(LiasScreen.Rules.route) { RulesScreen(viewModel = liasViewModel) }
                composable(LiasScreen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToConnection = { navController.navigate("connection_settings") }
                    )
                }
                composable("connection_settings") {
                    ConnectionSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Transient Action Undo Banner
            UndoToast(
                undoState = undoState,
                onDismiss = { liasViewModel.clearUndo() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = HigSpec.BottomNavPadding + 12.dp)
            )
        }
    }
}
