package com.brick.earthquaketracker.data.repository

import com.brick.earthquaketracker.data.location.LocationDataSource
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of location state. The Activity's permission callback pushes into
 * [onPermissionResult], both ViewModels observe the same flow, and the use case
 * combines it with the earthquake stream.
 */
@Singleton
class DefaultLocationRepository @Inject constructor(
    private val locationDataSource: LocationDataSource,
) : LocationRepository {

    private val _state = MutableStateFlow<LocationState>(LocationState.PermissionNotRequested)

    override fun observeLocationState(): Flow<LocationState> = _state.asStateFlow()

    override fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _state.value = when {
            granted -> LocationState.Unavailable
            permanentlyDenied -> LocationState.PermanentlyDenied
            else -> LocationState.PermissionDenied
        }
    }

    override suspend fun refreshLocation() {
        if (_state.value is LocationState.PermissionNotRequested ||
            _state.value is LocationState.PermissionDenied ||
            _state.value is LocationState.PermanentlyDenied
        ) return

        val location = locationDataSource.currentLocation()
        _state.value = if (location != null) {
            LocationState.Available(location)
        } else {
            LocationState.Unavailable
        }
    }
}
