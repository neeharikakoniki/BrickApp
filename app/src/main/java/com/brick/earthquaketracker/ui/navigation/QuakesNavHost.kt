package com.brick.earthquaketracker.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.brick.earthquaketracker.ui.detail.EarthquakeDetailScreen
import com.brick.earthquaketracker.ui.detail.EarthquakeDetailViewModel
import com.brick.earthquaketracker.ui.list.EarthquakeListScreen
import com.brick.earthquaketracker.ui.list.EarthquakeListViewModel

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

    NavHost(
        navController = navController,
        startDestination = Route.List,
        modifier = modifier,
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
