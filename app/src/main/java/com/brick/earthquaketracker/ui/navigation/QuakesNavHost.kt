package com.brick.earthquaketracker.ui.navigation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
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
    BottomNavItem("Map", Icons.Default.Map, Route.Map()),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuakesNavHost(
    navController: NavHostController,
    listViewModel: EarthquakeListViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            listViewModel.onPermissionResult(granted = true)
        } else {
            // Detect permanent denial: shouldShowRequestPermissionRationale returns false
            // when the user selected "Don't ask again" or denied twice on Android 11+.
            val activity = context as? ComponentActivity
            val canAskAgain = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) ?: true
            listViewModel.onPermissionResult(
                granted = false,
                permanentlyDenied = !canAskAgain,
            )
        }
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
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = Route.List,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                composable<Route.List> {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this@SharedTransitionLayout,
                        LocalAnimatedVisibilityScope provides this@composable,
                    ) {
                        val state by listViewModel.uiState.collectAsStateWithLifecycle()
                        EarthquakeListScreen(
                            state = state,
                            onQuakeClick = { eventId -> navController.navigate(Route.Detail(eventId)) },
                            onRefresh = listViewModel::refresh,
                            onFilterChange = listViewModel::updateFilter,
                            onSortChange = listViewModel::updateSortOrder,
                            onSearchQueryChange = listViewModel::updateSearchQuery,
                            onClearError = listViewModel::clearError,
                            onRequestLocationPermission = {
                                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                            onDismissLocationPrompt = listViewModel::dismissLocationPrompt,
                            onOpenAppSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            },
                        )
                    }
                }

                composable<Route.Map> { backStackEntry ->
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this@SharedTransitionLayout,
                        LocalAnimatedVisibilityScope provides this@composable,
                    ) {
                        val route = backStackEntry.toRoute<Route.Map>()
                        val viewModel = hiltViewModel<EarthquakeMapViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        EarthquakeMapScreen(
                            state = state,
                            onMarkerClick = { eventId -> navController.navigate(Route.Detail(eventId)) },
                            onRefresh = viewModel::refresh,
                            onFilterChange = viewModel::updateFilter,
                            focusEventId = route.focusEventId,
                            bottomBarHeight = 0,
                        )
                    }
                }

                composable<Route.Detail> {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this@SharedTransitionLayout,
                        LocalAnimatedVisibilityScope provides this@composable,
                    ) {
                        val viewModel = hiltViewModel<EarthquakeDetailViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        EarthquakeDetailScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onViewOnMap = { eventId ->
                                navController.navigate(Route.Map(focusEventId = eventId)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            },
                            onOpenUsgs = { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            },
                            onShare = { text ->
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            },
                        )
                    }
                }
            }
        }
    }
}
