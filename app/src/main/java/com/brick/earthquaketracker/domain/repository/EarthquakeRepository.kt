package com.brick.earthquaketracker.domain.repository

import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * The earthquake data contract. Defined in the domain layer; implemented in the data layer.
 *
 * Read paths return [Flow]s backed by the local database (single source of truth).
 * [refresh] is a write operation — it fetches from the network and persists to the database;
 * the UI updates because the database changed, not because the network returned.
 */
interface EarthquakeRepository {
    fun observeEarthquakes(filter: EarthquakeFilter): Flow<List<Earthquake>>
    fun observeEarthquake(id: String): Flow<Earthquake?>
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun refresh(): AppResult<Unit>
}
