package com.example.brickapp.domain.repository

import com.example.brickapp.domain.model.LocationState
import kotlinx.coroutines.flow.Flow

/**
 * Abstracts location access so the domain layer never touches Android's
 * FusedLocationProviderClient or ActivityCompat. The interface also makes
 * location trivially fakeable in tests.
 */
interface LocationRepository {
    fun observeLocationState(): Flow<LocationState>
    suspend fun refreshLocation()
    fun onPermissionResult(granted: Boolean)
}
