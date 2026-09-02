package com.brick.earthquaketracker.domain.repository

import com.brick.earthquaketracker.domain.model.LocationState
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocationState(): Flow<LocationState>
    suspend fun refreshLocation()
    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false)
}
