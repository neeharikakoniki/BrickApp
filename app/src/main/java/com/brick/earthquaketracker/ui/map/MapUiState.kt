package com.brick.earthquaketracker.ui.map

import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState

data class MapUiState(
    val earthquakes: List<EarthquakeListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val filter: EarthquakeFilter = EarthquakeFilter.Default,
    val locationState: LocationState = LocationState.PermissionNotRequested,
) {
    val hasLocationPermission: Boolean
        get() = locationState is LocationState.Available || locationState is LocationState.Unavailable
}
