package com.brick.earthquaketracker.domain.usecase

import app.cash.turbine.test
import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.domain.model.Bearing
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.domain.model.SyncStatus
import com.brick.earthquaketracker.domain.model.UserLocation
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import com.brick.earthquaketracker.domain.repository.LocationRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class ObserveEarthquakesUseCaseTest {

    private val earthquakeFlow = MutableStateFlow<List<Earthquake>>(emptyList())
    private val locationFlow = MutableStateFlow<LocationState>(LocationState.PermissionNotRequested)

    private val fakeEarthquakeRepo = object : EarthquakeRepository {
        override fun observeEarthquakes(filter: EarthquakeFilter) = earthquakeFlow
        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            MutableStateFlow(null)
        override fun observeTotalCount(): Flow<Int> = MutableStateFlow(0)
        override fun observeSyncStatus(): Flow<SyncStatus> =
            MutableStateFlow(SyncStatus(lastSyncAt = null, inFlight = false))
        override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val fakeLocationRepo = object : LocationRepository {
        override fun observeLocationState(): Flow<LocationState> = locationFlow
        override suspend fun refreshLocation() {}
        override fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {}
    }

    private val useCase = ObserveEarthquakesUseCase(fakeEarthquakeRepo, fakeLocationRepo)

    // -----------------------------------------------------------------------
    // The headline test: grant location mid-session
    // -----------------------------------------------------------------------

    @Test
    fun `granting location mid-session adds distances and re-sorts to nearest-first`() = runTest {
        val tokyo = quake("tokyo", Coordinates(35.68, 139.69), Instant.parse("2024-01-15T10:00:00Z"))
        val london = quake("london", Coordinates(51.51, -0.13), Instant.parse("2024-01-15T12:00:00Z"))
        val nyc = quake("nyc", Coordinates(40.71, -74.01), Instant.parse("2024-01-15T11:00:00Z"))

        earthquakeFlow.value = listOf(tokyo, london, nyc)
        locationFlow.value = LocationState.PermissionDenied

        useCase(EarthquakeFilter.Default, SortOrder.NEAREST).test {
            // --- No location: NEAREST falls back to MOST_RECENT (descending) ---
            val denied = awaitItem()
            assertThat(denied).hasSize(3)
            assertThat(denied.map { it.earthquake.id })
                .containsExactly("london", "nyc", "tokyo").inOrder()
            assertThat(denied.all { it.distanceKm == null }).isTrue()
            assertThat(denied.all { it.bearing == null }).isTrue()

            // --- Grant location: user is in NYC ---
            val userInNyc = UserLocation(
                coordinates = Coordinates(40.71, -74.01),
                capturedAt = Instant.parse("2024-01-15T12:30:00Z"),
            )
            locationFlow.value = LocationState.Available(userInNyc)

            // --- Same flow re-emits with distances and nearest-first ordering ---
            val granted = awaitItem()
            assertThat(granted).hasSize(3)
            // NYC is closest (≈0 km), London next (~5,500 km), Tokyo farthest (~10,800 km)
            assertThat(granted.map { it.earthquake.id })
                .containsExactly("nyc", "london", "tokyo").inOrder()
            // Distances are populated
            assertThat(granted.all { it.distanceKm != null }).isTrue()
            assertThat(granted.all { it.bearing != null }).isTrue()
            // NYC distance ≈ 0
            assertThat(granted[0].distanceKm!!).isWithin(1.0).of(0.0)
            // London distance ≈ 5,570 km
            assertThat(granted[1].distanceKm!!).isWithin(100.0).of(5_570.0)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // MOST_RECENT ordering without location
    // -----------------------------------------------------------------------

    @Test
    fun `MOST_RECENT sorts by time descending regardless of location`() = runTest {
        val older = quake("older", occurredAt = Instant.parse("2024-01-10T00:00:00Z"))
        val newer = quake("newer", occurredAt = Instant.parse("2024-01-15T00:00:00Z"))

        earthquakeFlow.value = listOf(older, newer)

        useCase(EarthquakeFilter.Default, SortOrder.MOST_RECENT).test {
            val items = awaitItem()
            assertThat(items.map { it.earthquake.id })
                .containsExactly("newer", "older").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // LARGEST ordering — null magnitude sorts last
    // -----------------------------------------------------------------------

    @Test
    fun `LARGEST sorts by magnitude descending with null last`() = runTest {
        val big = quake("big", magnitude = 6.5)
        val small = quake("small", magnitude = 2.5)
        val unknown = quake("unknown", magnitude = null)

        earthquakeFlow.value = listOf(unknown, small, big)

        useCase(EarthquakeFilter.Default, SortOrder.LARGEST).test {
            val items = awaitItem()
            assertThat(items.map { it.earthquake.id })
                .containsExactly("big", "small", "unknown").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Empty earthquake list
    // -----------------------------------------------------------------------

    @Test
    fun `empty earthquake list emits empty listings`() = runTest {
        earthquakeFlow.value = emptyList()

        useCase(EarthquakeFilter.Default, SortOrder.MOST_RECENT).test {
            val items = awaitItem()
            assertThat(items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // NEAREST without location falls back to MOST_RECENT
    // -----------------------------------------------------------------------

    @Test
    fun `NEAREST without location falls back to MOST_RECENT`() = runTest {
        val older = quake("older", occurredAt = Instant.parse("2024-01-10T00:00:00Z"))
        val newer = quake("newer", occurredAt = Instant.parse("2024-01-15T00:00:00Z"))

        earthquakeFlow.value = listOf(older, newer)
        locationFlow.value = LocationState.PermissionDenied

        useCase(EarthquakeFilter.Default, SortOrder.NEAREST).test {
            val items = awaitItem()
            assertThat(items.map { it.earthquake.id })
                .containsExactly("newer", "older").inOrder()
            assertThat(items.all { it.distanceKm == null }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Bearings are correct
    // -----------------------------------------------------------------------

    @Test
    fun `bearing is computed when location is available`() = runTest {
        // Event is due east of the user (same latitude, greater longitude)
        val east = quake("east", Coordinates(0.0, 10.0))
        earthquakeFlow.value = listOf(east)
        locationFlow.value = LocationState.Available(
            UserLocation(Coordinates(0.0, 0.0), Instant.now()),
        )

        useCase(EarthquakeFilter.Default, SortOrder.MOST_RECENT).test {
            val items = awaitItem()
            assertThat(items[0].bearing).isEqualTo(Bearing.E)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun quake(
        id: String,
        coordinates: Coordinates = Coordinates(0.0, 0.0),
        occurredAt: Instant = Instant.parse("2024-01-15T12:00:00Z"),
        magnitude: Double? = 4.0,
    ) = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = "ml",
        place = "Test",
        occurredAt = occurredAt,
        updatedAt = occurredAt,
        coordinates = coordinates,
        depthKm = 10.0,
        tsunamiWarning = false,
        significance = 100,
        alertLevel = null,
        detailsUrl = "",
        eventType = "earthquake",
    )
}
