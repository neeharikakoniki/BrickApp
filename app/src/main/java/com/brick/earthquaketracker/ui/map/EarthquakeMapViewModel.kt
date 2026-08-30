package com.brick.earthquaketracker.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.domain.usecase.ObserveEarthquakesUseCase
import com.brick.earthquaketracker.ui.filter.FilterStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EarthquakeMapViewModel @Inject constructor(
    observeEarthquakes: ObserveEarthquakesUseCase,
    private val filterStateHolder: FilterStateHolder,
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = combine(
        filterStateHolder.filter
            .flatMapLatest { filter ->
                observeEarthquakes(filter, SortOrder.MOST_RECENT)
            },
        filterStateHolder.filter,
    ) { earthquakes, filter ->
        MapUiState(
            earthquakes = earthquakes,
            isLoading = false,
            filter = filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )
}
