// ====================================================================
// File: LiasNavHost.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Adaptive 5-tab navigation. HIG-compliant 49dp bottom bar.
//          Spring physics transitions. Global undo/snackbar host.
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.lias.remote.repositories.UiEvent
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.screens.devices.DeviceDetailScreen
import com.lias.remote.ui.screens.devices.DevicesScreen
import com.lias.remote.ui.screens.home.HomeScreen
import com.lias.remote.ui.screens.rules.RulesScreen
import com.lias.remote.ui.screens.schedules.SchedulesScreen
import com.lias.remote.ui.screens.settings.ConnectionSettingsScreen
import com.lias.remote.ui.screens.settings.SettingsScreen
import com.lias.remote.ui.theme.HigSpec

sealed class LiasScreen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

    val snackbarHostState = remember { SnackbarHostState() }

    // Global UI Events (Snackbars, Security Alerts)
    LaunchedEffect(Unit) {
        liasViewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowSnackbarError -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowSecurityAlert -> snackbarHostState.showSnackbar("🚨 Security Alert: ${event.details}")
            }
        }
    }

    if (!isConnected) {
        com.lias.remote.ui.screens.connect.ConnectScreen(viewModel = settingsViewModel, onConnected = {})
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // HIG Tab Bar: 49dp + NavigationBars padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(HigSpec.TabBarHeight)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val activeColor = MaterialTheme.colorScheme.primary
                        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                tint = if (isSelected) activeColor else inactiveColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                color = if (isSelected) activeColor else inactiveColor
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LiasScreen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200)) },
            exitTransition = { fadeOut(tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) }
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
    }
}
