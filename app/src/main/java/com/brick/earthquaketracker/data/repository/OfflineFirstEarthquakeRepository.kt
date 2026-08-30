package com.brick.earthquaketracker.data.repository

import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.core.common.DataError
import com.brick.earthquaketracker.core.common.IoDispatcher
import com.brick.earthquaketracker.data.local.EarthquakeDao
import com.brick.earthquaketracker.data.local.SyncMetadataStore
import com.brick.earthquaketracker.data.mapper.toDomain
import com.brick.earthquaketracker.data.mapper.toEntity
import com.brick.earthquaketracker.data.remote.UsgsApi
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.SyncStatus
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first repository: reads always come from Room (single source of truth).
 * [refresh] fetches from the network, maps to entities, and upserts into Room;
 * the UI updates because the database changed, not because the network returned.
 *
 * A failed refresh is **non-destructive by construction** — cached rows survive
 * any network error. This is the headline property the integration test verifies.
 */
@Singleton
class OfflineFirstEarthquakeRepository @Inject constructor(
    private val api: UsgsApi,
    private val dao: EarthquakeDao,
    private val syncMetadataStore: SyncMetadataStore,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EarthquakeRepository {

    private val refreshMutex = Mutex()
    private val _inFlight = MutableStateFlow(false)

    /**
     * Holds the in-flight refresh result so concurrent callers can await
     * the real outcome instead of getting a fabricated Success.
     */
    @Volatile
    private var inFlightDeferred: CompletableDeferred<AppResult<Unit>>? = null

    override fun observeEarthquakes(filter: EarthquakeFilter): Flow<List<Earthquake>> =
        dao.observeAll(filter.minMagnitude)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeEarthquake(id: String): Flow<Earthquake?> =
        dao.observeById(id)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeTotalCount(): Flow<Int> = dao.observeCount()

    override fun observeSyncStatus(): Flow<SyncStatus> =
        combine(syncMetadataStore.lastSyncAt, _inFlight) { lastSync, inFlight ->
            SyncStatus(
                lastSyncAt = lastSync?.let { Instant.ofEpochMilli(it) },
                inFlight = inFlight,
            )
        }

    /**
     * Single-flight refresh: concurrent callers share the same in-flight result
     * via a [CompletableDeferred], so the second caller gets the real outcome
     * (success or failure) rather than a fabricated response.
     */
    override suspend fun refresh(): AppResult<Unit> {
        // Fast path: if a refresh is already in-flight, await its real result.
        inFlightDeferred?.let { existing ->
            if (existing.isActive) return existing.await()
        }

        return refreshMutex.withLock {
            // Re-check inside the lock — another coroutine may have completed
            // between the fast-path check and acquiring the lock.
            inFlightDeferred?.let { existing ->
                if (existing.isActive) return@withLock existing.await()
            }

            val deferred = CompletableDeferred<AppResult<Unit>>()
            inFlightDeferred = deferred
            _inFlight.value = true

            try {
                val result = doRefresh()
                deferred.complete(result)
                result
            } catch (e: Throwable) {
                val failure = AppResult.Failure(DataError.Unknown(e))
                deferred.complete(failure)
                failure
            } finally {
                _inFlight.value = false
                inFlightDeferred = null // Clear so the next refresh hits the network
            }
        }
    }

    private suspend fun doRefresh(): AppResult<Unit> = try {
        withContext(ioDispatcher) {
            val dto = api.getWeeklySummary()
            val entities = dto.features.mapNotNull { it.toEntity() }
            val cutoff = clock.instant().minus(RETENTION_PERIOD).toEpochMilli()
            dao.syncAndPrune(entities, cutoff)
            syncMetadataStore.setLastSyncAt(clock.instant().toEpochMilli())
        }
        AppResult.Success(Unit)
    } catch (e: SocketTimeoutException) {
        AppResult.Failure(DataError.Timeout)
    } catch (e: java.io.IOException) {
        AppResult.Failure(DataError.NoConnectivity)
    } catch (e: HttpException) {
        AppResult.Failure(DataError.Server(e.code()))
    } catch (e: SerializationException) {
        AppResult.Failure(DataError.Serialization)
    } catch (e: Exception) {
        AppResult.Failure(DataError.Unknown(e))
    }

    private companion object {
        /**
         * Events older than this are pruned on each successful sync.
         * 8 days = the 7-day feed window + 1 day buffer, so events near
         * the trailing edge aren't prematurely removed between refreshes.
         */
        val RETENTION_PERIOD: Duration = Duration.ofDays(8)
    }
}
