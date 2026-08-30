package com.brick.earthquaketracker.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.brick.earthquaketracker.ui.detail.EarthquakeDetailScreen
import com.brick.earthquaketracker.ui.detail.EarthquakeDetailViewModel
import com.brick.earthquaketracker.ui.list.EarthquakeListScreen
import com.brick.earthquaketracker.ui.list.EarthquakeListViewModel
import com.brick.earthquaketracker.ui.map.EarthquakeMapScreen
import com.brick.earthquaketracker.ui.map.EarthquakeMapViewModel

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Route,
)

private val bottomNavItems = listOf(
    BottomNavItem("List", Icons.AutoMirrored.Filled.List, Route.List),
    BottomNavItem("Map", Icons.Default.Map, Route.Map),
)

@Composable
fun QuakesNavHost(
    navController: NavHostController,
    listViewModel: EarthquakeListViewModel,
    modifier: Modifier = Modifier,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        listViewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        listViewModel.requestLocationPermission.collect {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hasRoute(item.route::class) == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.List,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Route.List> {
                val state by listViewModel.uiState.collectAsStateWithLifecycle()
                EarthquakeListScreen(
                    state = state,
                    onQuakeClick = { eventId -> navController.navigate(Route.Detail(eventId)) },
                    onRefresh = listViewModel::refresh,
                    onFilterChange = listViewModel::updateFilter,
                    onSortChange = listViewModel::updateSortOrder,
                    onClearError = listViewModel::clearError,
                    onRequestLocationPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                )
            }

            composable<Route.Map> {
                val viewModel = hiltViewModel<EarthquakeMapViewModel>()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                EarthquakeMapScreen(
                    state = state,
                    onMarkerClick = { eventId -> navController.navigate(Route.Detail(eventId)) },
                )
            }

            composable<Route.Detail> {
                val viewModel = hiltViewModel<EarthquakeDetailViewModel>()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                EarthquakeDetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
