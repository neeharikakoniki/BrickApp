package com.brick.earthquaketracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EarthquakeDao {

    @Query(
        """
        SELECT * FROM earthquakes
        WHERE (:minMag IS NULL OR magnitude >= :minMag)
        ORDER BY occurredAtMillis DESC
        """,
    )
    abstract fun observeAll(minMag: Double?): Flow<List<EarthquakeEntity>>

    @Query("SELECT * FROM earthquakes WHERE id = :id")
    abstract fun observeById(id: String): Flow<EarthquakeEntity?>

    @Query("SELECT * FROM earthquakes")
    abstract suspend fun getAll(): List<EarthquakeEntity>

    @Upsert
    abstract suspend fun upsertAll(entities: List<EarthquakeEntity>)

    @Query("DELETE FROM earthquakes WHERE occurredAtMillis < :cutoffMillis")
    abstract suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM earthquakes")
    abstract suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM earthquakes")
    abstract fun observeCount(): Flow<Int>

    /**
     * Atomic sync: upsert incoming events (skipping any whose `updatedAtMillis`
     * is older than what is already stored), then prune events older than the
     * retention cutoff.
     */
    @Transaction
    open suspend fun syncAndPrune(incoming: List<EarthquakeEntity>, cutoffMillis: Long) {
        val existing = getAll().associateBy { it.id }
        val toUpsert = incoming.filter { entity ->
            val current = existing[entity.id]
            current == null || entity.updatedAtMillis >= current.updatedAtMillis
        }
        if (toUpsert.isNotEmpty()) {
            upsertAll(toUpsert)
        }
        deleteOlderThan(cutoffMillis)
    }
}
