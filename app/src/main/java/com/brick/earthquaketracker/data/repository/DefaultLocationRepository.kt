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

    override fun onPermissionResult(granted: Boolean) {
        _state.value = if (granted) {
            // Mark as Unavailable until refreshLocation() obtains a fix.
            // This avoids a gap where the state is still PermissionDenied
            // after the user just granted permission.
            LocationState.Unavailable
        } else {
            LocationState.PermissionDenied
        }
    }

    override suspend fun refreshLocation() {
        // Only attempt a fix if permission was granted (state is Unavailable or Available).
        if (_state.value is LocationState.PermissionNotRequested ||
            _state.value is LocationState.PermissionDenied
        ) return

        val location = locationDataSource.currentLocation()
        _state.value = if (location != null) {
            LocationState.Available(location)
        } else {
            LocationState.Unavailable
        }
    }
}
