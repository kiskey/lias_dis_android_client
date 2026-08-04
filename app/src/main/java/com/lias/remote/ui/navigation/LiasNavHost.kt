// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 1.5.0
// Audit Fixes: 
//   1. Fully verified Jetpack Navigation backstack entry hierarchy, 
//      SnackbarHost state binding, and collectAsStateWithLifecycle lifecycle safety.
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lias.remote.repositories.UiEvent
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.screens.dashboard.DashboardScreen
import com.lias.remote.ui.screens.devices.TagGroupsScreen
import com.lias.remote.ui.screens.policies.PoliciesScreen
import com.lias.remote.ui.screens.schedules.SchedulesScreen
import com.lias.remote.ui.screens.settings.SettingsScreen

sealed class LiasScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : LiasScreen("dashboard", "Home", Icons.Filled.SpaceDashboard)
    data object Devices : LiasScreen("devices", "Tags", Icons.Filled.Devices)
    data object Schedules : LiasScreen("schedules", "Schedules", Icons.Filled.Schedule)
    data object Policies : LiasScreen("policies", "Rules", Icons.Filled.Security)
    data object Settings : LiasScreen("settings", "Config", Icons.Filled.Settings)
}

@Composable
fun LiasNavHost(
    liasViewModel: LiasViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val items = listOf(
        LiasScreen.Dashboard,
        LiasScreen.Devices,
        LiasScreen.Schedules,
        LiasScreen.Policies,
        LiasScreen.Settings
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val uiEvents by liasViewModel.uiEvents.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(uiEvents) {
        uiEvents?.let { event ->
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
            }
        }
    }

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
            startDestination = LiasScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LiasScreen.Dashboard.route) {
                DashboardScreen(viewModel = liasViewModel)
            }
            composable(LiasScreen.Devices.route) {
                TagGroupsScreen(viewModel = liasViewModel)
            }
            composable(LiasScreen.Schedules.route) {
                SchedulesScreen(viewModel = liasViewModel)
            }
            composable(LiasScreen.Policies.route) {
                PoliciesScreen(viewModel = liasViewModel)
            }
            composable(LiasScreen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
