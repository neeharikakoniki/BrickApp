package com.brick.earthquaketracker.ui.list

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.core.common.DataError
import com.brick.earthquaketracker.data.local.SyncMetadataStore
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SyncStatus
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import com.brick.earthquaketracker.domain.repository.LocationRepository
import com.brick.earthquaketracker.domain.usecase.ObserveEarthquakesUseCase
import com.brick.earthquaketracker.domain.usecase.RefreshEarthquakesUseCase
import com.brick.earthquaketracker.ui.filter.FilterStateHolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class EarthquakeListViewModelTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val earthquakesFlow = MutableStateFlow<List<Earthquake>>(emptyList())
    private val syncStatusFlow = MutableStateFlow(SyncStatus(lastSyncAt = null, inFlight = false))
    private val locationFlow = MutableStateFlow<LocationState>(LocationState.PermissionNotRequested)
    private var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    private val clock = Clock.fixed(Instant.parse("2024-01-15T00:00:00Z"), ZoneOffset.UTC)

    private val fakeEarthquakeRepo = object : EarthquakeRepository {
        override fun observeEarthquakes(filter: EarthquakeFilter): Flow<List<Earthquake>> =
            earthquakesFlow

        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            MutableStateFlow(null)

        override fun observeTotalCount(): Flow<Int> =
            earthquakesFlow.map { it.size }

        override fun observeSyncStatus(): Flow<SyncStatus> = syncStatusFlow

        override suspend fun refresh(): AppResult<Unit> = refreshResult
    }

    private val fakeLocationRepo = object : LocationRepository {
        var lastPermissionResult: Boolean? = null
        override fun observeLocationState(): Flow<LocationState> = locationFlow
        override suspend fun refreshLocation() {}
        override fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
            lastPermissionResult = granted
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSyncMetadataStore() = SyncMetadataStore(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(testDispatcher + SupervisorJob()),
        ) {
            tempFolder.newFile("test_vm.preferences_pb")
        },
    )

    private fun createViewModel(
        syncMetadataStore: SyncMetadataStore = createSyncMetadataStore(),
    ) = EarthquakeListViewModel(
        observeEarthquakes = ObserveEarthquakesUseCase(fakeEarthquakeRepo, fakeLocationRepo),
        refreshEarthquakes = RefreshEarthquakesUseCase(fakeEarthquakeRepo),
        earthquakeRepository = fakeEarthquakeRepo,
        locationRepository = fakeLocationRepo,
        filterStateHolder = FilterStateHolder(),
        syncMetadataStore = syncMetadataStore,
        clock = clock,
    )

    @Test
    fun `initial state shows loading when no sync has occurred`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = null, inFlight = true)

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().isInitialLoading).isTrue()
        }
    }

    @Test
    fun `shows earthquakes after successful refresh`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(testQuake("q1"), testQuake("q2"))

        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.earthquakes).hasSize(2)
            assertThat(state.isInitialLoading).isFalse()
            assertThat(state.emptyReason).isNull()
        }
    }

    @Test
    fun `failure over cache shows error message but keeps cached data`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(testQuake("cached"))
        refreshResult = AppResult.Failure(DataError.NoConnectivity)

        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.earthquakes).hasSize(1)
            assertThat(state.errorMessage).contains("No internet")
        }
    }

    @Test
    fun `filter with no results shows NO_RESULTS_FOR_FILTER`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = emptyList()

        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1) // initial emission
            vm.updateFilter(EarthquakeFilter(minMagnitude = 9.0))
            assertThat(awaitItem().emptyReason).isEqualTo(EmptyReason.NO_RESULTS_FOR_FILTER)
        }
    }

    @Test
    fun `empty list with default filter shows NO_DATA`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = emptyList()

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().emptyReason).isEqualTo(EmptyReason.NO_DATA)
        }
    }

    @Test
    fun `no cache and no sync shows NO_CACHE_OFFLINE`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = null, inFlight = false)
        earthquakesFlow.value = emptyList()

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().emptyReason).isEqualTo(EmptyReason.NO_CACHE_OFFLINE)
        }
    }

    @Test
    fun `stale data banner appears when last sync exceeds threshold`() = runTest {
        val twoHoursAgo = clock.instant().minus(Duration.ofHours(2))
        syncStatusFlow.value = SyncStatus(lastSyncAt = twoHoursAgo, inFlight = false)
        earthquakesFlow.value = listOf(testQuake("q1"))

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().staleSince).isEqualTo(twoHoursAgo)
        }
    }

    @Test
    fun `onPermissionResult delegates to location repository`() = runTest {
        val vm = createViewModel()

        vm.onPermissionResult(true)

        assertThat(fakeLocationRepo.lastPermissionResult).isTrue()
    }


    @Test
    fun `search filters earthquakes by place name`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(
            testQuake("q1", place = "10km NW of Tokyo, Japan"),
            testQuake("q2", place = "5km SE of Los Angeles, CA"),
            testQuake("q3", place = "20km E of Osaka, Japan"),
        )

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().earthquakes).hasSize(3)

            vm.updateSearchQuery("Japan")
            val filtered = awaitItem()
            assertThat(filtered.earthquakes).hasSize(2)
            assertThat(filtered.earthquakes.map { it.earthquake.id }).containsExactly("q1", "q3")
            assertThat(filtered.searchQuery).isEqualTo("Japan")
        }
    }

    @Test
    fun `search is case insensitive`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(
            testQuake("q1", place = "10km NW of TOKYO, Japan"),
            testQuake("q2", place = "5km SE of Los Angeles, CA"),
        )

        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            vm.updateSearchQuery("tokyo")
            assertThat(awaitItem().earthquakes).hasSize(1)
        }
    }

    @Test
    fun `search with no matches shows NO_RESULTS_FOR_FILTER`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(testQuake("q1", place = "Tokyo, Japan"))

        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            vm.updateSearchQuery("zzz_no_match")
            val state = awaitItem()
            assertThat(state.earthquakes).isEmpty()
            assertThat(state.emptyReason).isEqualTo(EmptyReason.NO_RESULTS_FOR_FILTER)
        }
    }

    @Test
    fun `clearing search query restores full list`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(
            testQuake("q1", place = "Tokyo, Japan"),
            testQuake("q2", place = "Los Angeles, CA"),
        )

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().earthquakes).hasSize(2)

            vm.updateSearchQuery("Tokyo")
            assertThat(awaitItem().earthquakes).hasSize(1)

            vm.updateSearchQuery("")
            assertThat(awaitItem().earthquakes).hasSize(2)
        }
    }

    @Test
    fun `search query is reflected in isFiltered`() = runTest {
        syncStatusFlow.value = SyncStatus(lastSyncAt = Instant.now(), inFlight = false)
        earthquakesFlow.value = listOf(testQuake("q1"))

        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().isFiltered).isFalse()

            vm.updateSearchQuery("test")
            assertThat(awaitItem().isFiltered).isTrue()

            vm.updateSearchQuery("")
            assertThat(awaitItem().isFiltered).isFalse()
        }
    }

    private fun testQuake(id: String, magnitude: Double? = 4.5, place: String = "Test Place") = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = "ml",
        place = place,
        occurredAt = Instant.parse("2024-01-14T12:00:00Z"),
        updatedAt = Instant.parse("2024-01-14T12:05:00Z"),
        coordinates = Coordinates(latitude = 37.0, longitude = -120.0),
        depthKm = 10.0,
        tsunamiWarning = false,
        significance = 200,
        alertLevel = null,
        detailsUrl = "https://example.com/$id",
        eventType = "earthquake",
    )
}
