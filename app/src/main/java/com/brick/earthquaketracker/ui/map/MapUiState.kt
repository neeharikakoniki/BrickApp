package com.brick.earthquaketracker.ui.map

import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing

data class MapUiState(
    val earthquakes: List<EarthquakeListing> = emptyList(),
    val isLoading: Boolean = true,
    val filter: EarthquakeFilter = EarthquakeFilter.Default,
)
