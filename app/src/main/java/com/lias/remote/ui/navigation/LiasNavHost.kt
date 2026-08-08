// ====================================================================
// File: LiasNavHost.kt
// Version: 3.2.0 (Cupertino Refactor)
// Purpose: Refactored to use CupertinoScaffold, CupertinoTopAppBar,
//          CupertinoTabBar. Fixed slide animations. Implemented strict
//          layout design requirements.
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.lias.remote.repositories.UiEvent
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.UndoToast
import com.lias.remote.ui.screens.devices.DeviceDetailScreen
import com.lias.remote.ui.screens.devices.DevicesScreen
import com.lias.remote.ui.screens.home.HomeScreen
import com.lias.remote.ui.screens.onboarding.OnboardingSheet
import com.lias.remote.ui.screens.rules.RulesScreen
import com.lias.remote.ui.screens.schedules.SchedulesScreen
import com.lias.remote.ui.screens.security.SecurityAlertSheet
import com.lias.remote.ui.screens.settings.ConnectionSettingsScreen
import com.lias.remote.ui.screens.settings.SettingsScreen
import com.lias.remote.ui.theme.HigSpec
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoTabBar
import io.github.alexzhirkevich.cupertino.CupertinoTabBarItem
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import kotlinx.coroutines.launch

sealed class LiasScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : LiasScreen("home", "Home", Icons.Filled.Home)
    data object Devices : LiasScreen("devices", "Devices", Icons.Filled.Devices)
    data object Schedules : LiasScreen("schedules", "Schedules", Icons.Filled.Schedule)
    data object Rules : LiasScreen("rules", "Rules", Icons.Filled.Security)
    data object Settings : LiasScreen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun LiasNavHost(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val items = listOf(LiasScreen.Home, LiasScreen.Devices, LiasScreen.Schedules, LiasScreen.Rules, LiasScreen.Settings)
    val settingsState by settingsViewModel.uiState.collectAsState()
    val isConnected = settingsState.savedServerUrl.isNotBlank()

    val undoState by liasViewModel.undoState.collectAsState()
    val securityAlert by liasViewModel.pendingSecurityAlert.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        liasViewModel.uiEvents.collect { event ->
            if (event is UiEvent.ShowSnackbar) {
                // In a real app, you might use a CupertinoSnackbar equivalent
            }
        }
    }

    if (!isConnected) {
        com.lias.remote.ui.screens.connect.ConnectScreen(viewModel = settingsViewModel, onConnected = {})
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
                // Top Status Row / Label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Reconnecting to LIAS Server…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        bottomBar = {
            CupertinoTabBar {
                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    CupertinoTabBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                        label = { CupertinoText(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = LiasScreen.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 4 }
                },
                exitTransition = {
                    fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }
                },
                popEnterTransition = {
                    fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 4 }
                },
                popExitTransition = {
                    fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }
                }
            ) {
                composable(LiasScreen.Home.route) {
                    HomeScreen(
                        viewModel = liasViewModel,
                        onNavigateToDeviceDetail = { pdid -> navController.navigate("device_detail/$pdid") }
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

            // Banner Action Component
            UndoToast(
                undoState = undoState,
                onDismiss = { liasViewModel.clearUndo() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
    }
}
