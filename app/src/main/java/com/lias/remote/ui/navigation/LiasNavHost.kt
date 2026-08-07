// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 2.3.0
// Audit Fixes:
//   1. Removed duplicate `navController.navigate` call in ConnectScreen callback
//      to resolve ConnectScreen crash on Connect tap (CRASH-01).
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lias.remote.repositories.UiEvent
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.screens.connect.ConnectScreen
import com.lias.remote.ui.screens.devices.DeviceDetailScreen
import com.lias.remote.ui.screens.devices.DevicesScreen
import com.lias.remote.ui.screens.home.HomeScreen
import com.lias.remote.ui.screens.rules.RulesScreen
import com.lias.remote.ui.screens.schedules.SchedulesScreen
import com.lias.remote.ui.screens.settings.ConnectionSettingsScreen
import com.lias.remote.ui.screens.settings.SettingsScreen

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
    val items = listOf(
        LiasScreen.Home,
        LiasScreen.Devices,
        LiasScreen.Schedules,
        LiasScreen.Rules,
        LiasScreen.Settings
    )

    val settingsState by settingsViewModel.uiState.collectAsState()
    val isConnected = settingsState.savedServerUrl.isNotBlank()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        liasViewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.ShowSnackbarError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is UiEvent.ShowSecurityAlert -> {
                    snackbarHostState.showSnackbar(
                        message = "🚨 Security Alert: ${event.details}",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    if (!isConnected) {
        ConnectScreen(
            viewModel = settingsViewModel,
            onConnected = {
                // Compositional state switch handles navigation cleanly without race conditions
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = LiasScreen.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(150)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) }
            ) {
                composable(LiasScreen.Home.route) {
                    HomeScreen(
                        viewModel = liasViewModel,
                        onNavigateToDeviceDetail = { pdid ->
                            navController.navigate("device_detail/$pdid")
                        }
                    )
                }
                composable(LiasScreen.Devices.route) {
                    DevicesScreen(
                        viewModel = liasViewModel,
                        onNavigateToDeviceDetail = { pdid ->
                            navController.navigate("device_detail/$pdid")
                        }
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
                composable(LiasScreen.Schedules.route) {
                    SchedulesScreen(viewModel = liasViewModel)
                }
                composable(LiasScreen.Rules.route) {
                    RulesScreen(viewModel = liasViewModel)
                }
                composable(LiasScreen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToConnection = {
                            navController.navigate("connection_settings")
                        }
                    )
                }
                composable("connection_settings") {
                    ConnectionSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
