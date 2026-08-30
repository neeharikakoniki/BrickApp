package com.brick.earthquaketracker.ui.detail

import com.brick.earthquaketracker.domain.usecase.EarthquakeDetail

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: EarthquakeDetail) : DetailUiState
    data object NotFound : DetailUiState
}
