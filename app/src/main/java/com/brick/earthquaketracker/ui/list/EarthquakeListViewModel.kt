package com.brick.earthquaketracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.core.common.DataError
import com.brick.earthquaketracker.data.local.SyncMetadataStore
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import com.brick.earthquaketracker.domain.repository.LocationRepository
import com.brick.earthquaketracker.domain.usecase.ObserveEarthquakesUseCase
import com.brick.earthquaketracker.domain.usecase.RefreshEarthquakesUseCase
import com.brick.earthquaketracker.ui.filter.FilterStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EarthquakeListViewModel @Inject constructor(
    private val observeEarthquakes: ObserveEarthquakesUseCase,
    private val refreshEarthquakes: RefreshEarthquakesUseCase,
    private val earthquakeRepository: EarthquakeRepository,
    private val locationRepository: LocationRepository,
    private val filterStateHolder: FilterStateHolder,
    private val syncMetadataStore: SyncMetadataStore,
    private val clock: java.time.Clock,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _requestLocationPermission = MutableSharedFlow<Unit>()
    val requestLocationPermission: SharedFlow<Unit> = _requestLocationPermission.asSharedFlow()

    private val filterSort = combine(
        filterStateHolder.filter, filterStateHolder.sortOrder,
    ) { f, s -> f to s }

    val uiState: StateFlow<ListUiState> = combine(
        filterSort.flatMapLatest { (filter, sort) -> observeEarthquakes(filter, sort) },
        earthquakeRepository.observeSyncStatus(),
        combine(
            locationRepository.observeLocationState(),
            earthquakeRepository.observeTotalCount(),
            syncMetadataStore.locationPromptDismissed,
        ) { loc, count, dismissed -> Triple(loc, count, dismissed) },
        filterSort,
        _errorMessage,
    ) { earthquakes, syncStatus, (locationState, totalCount, promptDismissed), (filter, sortOrder), errorMsg ->
        val isInitialLoading = syncStatus.lastSyncAt == null && syncStatus.inFlight
        val emptyReason = when {
            earthquakes.isNotEmpty() -> null
            isInitialLoading -> null
            syncStatus.lastSyncAt == null -> EmptyReason.NO_CACHE_OFFLINE
            filter != EarthquakeFilter.Default -> EmptyReason.NO_RESULTS_FOR_FILTER
            else -> EmptyReason.NO_DATA
        }
        val staleSince = syncStatus.lastSyncAt?.takeIf {
            Duration.between(it, clock.instant()) > STALE_THRESHOLD
        }

        ListUiState(
            earthquakes = earthquakes,
            totalCount = totalCount,
            isInitialLoading = isInitialLoading,
            isRefreshing = syncStatus.inFlight && !isInitialLoading,
            emptyReason = emptyReason,
            errorMessage = errorMsg,
            staleSince = staleSince,
            locationState = locationState,
            locationPromptDismissed = promptDismissed,
            filter = filter,
            sortOrder = sortOrder,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = refreshEarthquakes()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _errorMessage.value = result.error.toMessage()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateFilter(filter: EarthquakeFilter) {
        filterStateHolder.updateFilter(filter)
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        if (sortOrder == SortOrder.NEAREST &&
            uiState.value.locationState is LocationState.PermissionNotRequested
        ) {
            viewModelScope.launch { _requestLocationPermission.emit(Unit) }
        }
        filterStateHolder.updateSortOrder(sortOrder)
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        locationRepository.onPermissionResult(granted, permanentlyDenied)
        if (granted) {
            viewModelScope.launch { locationRepository.refreshLocation() }
        }
    }

    fun dismissLocationPrompt() {
        viewModelScope.launch {
            syncMetadataStore.setLocationPromptDismissed(true)
        }
    }

    private companion object {
        val STALE_THRESHOLD: Duration = Duration.ofMinutes(30)
    }
}

private fun DataError.toMessage(): String = when (this) {
    DataError.NoConnectivity -> "No internet connection. Showing cached data."
    DataError.Timeout -> "Request timed out. Showing cached data."
    is DataError.Server -> "Server error ($code). Showing cached data."
    DataError.Serialization -> "Unexpected response format. Showing cached data."
    is DataError.Unknown -> "Something went wrong. Showing cached data."
}
