package com.brick.earthquaketracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.brick.earthquaketracker.domain.usecase.ObserveEarthquakeDetailUseCase
import com.brick.earthquaketracker.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EarthquakeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveEarthquakeDetailUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Detail>()

    val uiState: StateFlow<DetailUiState> = observeDetail(route.eventId)
        .map { detail ->
            if (detail != null) DetailUiState.Content(detail)
            else DetailUiState.NotFound
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading,
        )
}
