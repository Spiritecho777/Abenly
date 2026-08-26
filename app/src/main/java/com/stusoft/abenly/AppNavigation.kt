package com.stusoft.abenly

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stusoft.abenly.ui.components.AbenlyTopAppBar
import com.stusoft.abenly.ui.screens.CarMaintenanceScreen
import com.stusoft.abenly.ui.screens.MainScreen
import com.stusoft.abenly.ui.screens.SubscriptionsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Titre dynamique de la TopAppBar
    val titleRes = when (currentRoute) {
        "car_maintenance" -> R.string.btn_car_maintenance
        "subscriptions" -> R.string.btn_subscriptions
        else -> R.string.app_name
    }

    Scaffold(
        topBar = {
            AbenlyTopAppBar(
                titleRes = titleRes,
                canNavigateBack = currentRoute != "main",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(padding)
        ) {
            composable("main") {
                MainScreen(
                    onNavigateToCarMaintenance = { navController.navigate("car_maintenance") },
                    onNavigateToSubscriptions = { navController.navigate("subscriptions") }
                )
            }
            composable("car_maintenance") {
                CarMaintenanceScreen()
            }
            composable("subscriptions") {
                SubscriptionsScreen()
            }
        }
    }
}