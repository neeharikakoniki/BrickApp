package com.brick.earthquaketracker.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.domain.repository.LocationRepository
import com.brick.earthquaketracker.domain.usecase.ObserveEarthquakesUseCase
import com.brick.earthquaketracker.domain.usecase.RefreshEarthquakesUseCase
import com.brick.earthquaketracker.ui.filter.FilterStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EarthquakeMapViewModel @Inject constructor(
    observeEarthquakes: ObserveEarthquakesUseCase,
    private val refreshEarthquakes: RefreshEarthquakesUseCase,
    private val locationRepository: LocationRepository,
    private val filterStateHolder: FilterStateHolder,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<MapUiState> = combine(
        filterStateHolder.filter
            .flatMapLatest { filter ->
                observeEarthquakes(filter, SortOrder.MOST_RECENT)
            },
        filterStateHolder.filter,
        locationRepository.observeLocationState(),
        _isRefreshing,
    ) { earthquakes, filter, locationState, isRefreshing ->
        MapUiState(
            earthquakes = earthquakes,
            isLoading = false,
            isRefreshing = isRefreshing,
            filter = filter,
            locationState = locationState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshEarthquakes()
            _isRefreshing.value = false
        }
    }

    fun updateFilter(filter: EarthquakeFilter) {
        filterStateHolder.updateFilter(filter)
    }
}
