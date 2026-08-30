package com.brick.earthquaketracker.ui.list

import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SortOrder
import java.time.Instant

data class ListUiState(
    val earthquakes: List<EarthquakeListing> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val emptyReason: EmptyReason? = null,
    val errorMessage: String? = null,
    val staleSince: Instant? = null,
    val locationState: LocationState = LocationState.PermissionNotRequested,
    val filter: EarthquakeFilter = EarthquakeFilter.Default,
    val sortOrder: SortOrder = SortOrder.MOST_RECENT,
)

enum class EmptyReason {
    NO_DATA,
    NO_RESULTS_FOR_FILTER,
    NO_CACHE_OFFLINE,
}
