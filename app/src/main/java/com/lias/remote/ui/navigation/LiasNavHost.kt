// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt
// Version: 1.0.0
// Purpose: Navigation graph and scaffold wrapper. Implements the 5-tab
//          bottom navigation bar for phones. Renders placeholders for
//          screens which will be populated in subsequent batches.
// ====================================================================

package com.lias.remote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class LiasScreen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : LiasScreen("dashboard", "Home", Icons.Filled.SpaceDashboard)
    data object Devices : LiasScreen("devices", "Tags", Icons.Filled.Devices)
    data object Schedules : LiasScreen("schedules", "Schedules", Icons.Filled.Schedule)
    data object Policies : LiasScreen("policies", "Rules", Icons.Filled.Security)
    data object Settings : LiasScreen("settings", "Config", Icons.Filled.AppSettings)
}

@Composable
fun LiasNavHost() {
    val navController = rememberNavController()
    val items = listOf(
        LiasScreen.Dashboard,
        LiasScreen.Devices,
        LiasScreen.Schedules,
        LiasScreen.Policies,
        LiasScreen.Settings
    )

    Scaffold(
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
                // Placeholder - Will be replaced by DashboardScreen
                Text("Dashboard Screen - Batch 7")
            }
            composable(LiasScreen.Devices.route) {
                Text("Tag Groups Screen - Batch 7")
            }
            composable(LiasScreen.Schedules.route) {
                Text("Schedules Screen - Batch 7")
            }
            composable(LiasScreen.Policies.route) {
                Text("Policies Screen - Batch 7")
            }
            composable(LiasScreen.Settings.route) {
                Text("Settings Screen - Batch 7")
            }
        }
    }
}
