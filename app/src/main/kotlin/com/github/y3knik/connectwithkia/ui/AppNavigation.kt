package com.github.y3knik.connectwithkia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.y3knik.connectwithkia.ui.credentials.CredentialsScreen
import com.github.y3knik.connectwithkia.ui.settings.SettingsScreen
import com.github.y3knik.connectwithkia.ui.status.StatusScreen

sealed class Route(val path: String, val label: String) {
    data object Status : Route("status", "Status")

    data object Credentials : Route("credentials", "Credentials")

    data object Settings : Route("settings", "Settings")
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = route == Route.Status.path,
                    onClick = { nav.navigate(Route.Status.path) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(Route.Status.label) },
                )
                NavigationBarItem(
                    selected = route == Route.Credentials.path,
                    onClick = { nav.navigate(Route.Credentials.path) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text(Route.Credentials.label) },
                )
                NavigationBarItem(
                    selected = route == Route.Settings.path,
                    onClick = { nav.navigate(Route.Settings.path) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(Route.Settings.label) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Route.Status.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Status.path) { StatusScreen() }
            composable(Route.Credentials.path) { CredentialsScreen() }
            composable(Route.Settings.path) { SettingsScreen() }
        }
    }
}
