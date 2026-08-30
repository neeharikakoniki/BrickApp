package com.brick.earthquaketracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.core.common.DataError
import com.brick.earthquaketracker.data.local.EarthquakeEntity
import com.brick.earthquaketracker.data.local.QuakesDatabase
import com.brick.earthquaketracker.data.local.SyncMetadataStore
import com.brick.earthquaketracker.data.remote.UsgsApi
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class OfflineFirstEarthquakeRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: QuakesDatabase
    private lateinit var repository: OfflineFirstEarthquakeRepository

    private val testDispatcher = UnconfinedTestDispatcher()
    // Must be close to the fixture event timestamps (1700000000000 = 2023-11-14T22:13:20Z)
    // so events fall within the 8-day retention window.
    private val fixedClock = Clock.fixed(Instant.parse("2023-11-15T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuakesDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(UsgsApi::class.java)
        val dataStore = PreferenceDataStoreFactory.create {
            tempFolder.newFile("test_sync.preferences_pb")
        }
        val syncStore = SyncMetadataStore(dataStore)

        repository = OfflineFirstEarthquakeRepository(
            api = api,
            dao = database.earthquakeDao(),
            syncMetadataStore = syncStore,
            clock = fixedClock,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        database.close()
        mockWebServer.shutdown()
    }

    // -----------------------------------------------------------------------
    // The headline test: failure over populated cache preserves cached data
    // -----------------------------------------------------------------------

    @Test
    fun `refresh failure over populated cache returns Failure and preserves cached data`() = runTest {
        // Seed the database directly
        val dao = database.earthquakeDao()
        dao.upsertAll(listOf(seedEntity("cached-eq")))

        // Verify seed is visible
        val before = repository.observeEarthquakes(EarthquakeFilter.Default).first()
        assertThat(before).hasSize(1)
        assertThat(before[0].id).isEqualTo("cached-eq")

        // Enqueue a server error
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        // Refresh — should fail
        val result = repository.refresh()
        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isEqualTo(DataError.Server(500))

        // Cached data is STILL available, untouched
        val after = repository.observeEarthquakes(EarthquakeFilter.Default).first()
        assertThat(after).hasSize(1)
        assertThat(after[0].id).isEqualTo("cached-eq")
    }

    // -----------------------------------------------------------------------
    // Successful refresh persists and emits
    // -----------------------------------------------------------------------

    @Test
    fun `successful refresh persists events from network`() = runTest {
        val fixture = fixtureJson()
        mockWebServer.enqueue(MockResponse().setBody(fixture).setResponseCode(200))

        val result = repository.refresh()
        assertThat(result).isInstanceOf(AppResult.Success::class.java)

        val earthquakes = repository.observeEarthquakes(EarthquakeFilter.Default).first()
        assertThat(earthquakes).hasSize(3)

        // Verify the Tokyo event has correct coordinate ordering
        val tokyo = earthquakes.first { it.id == "us7000abcd" }
        assertThat(tokyo.coordinates.longitude).isEqualTo(136.8)
        assertThat(tokyo.coordinates.latitude).isEqualTo(34.2)
        assertThat(tokyo.magnitude).isEqualTo(5.2)
    }

    // -----------------------------------------------------------------------
    // Malformed JSON returns Serialization error, cache untouched
    // -----------------------------------------------------------------------

    @Test
    fun `malformed JSON returns Serialization error and preserves cache`() = runTest {
        // Seed
        val dao = database.earthquakeDao()
        dao.upsertAll(listOf(seedEntity("cached-eq")))

        // Enqueue garbage
        mockWebServer.enqueue(
            MockResponse()
                .setBody("{not valid json at all")
                .setResponseCode(200),
        )

        val result = repository.refresh()
        assertThat(result).isInstanceOf(AppResult.Failure::class.java)

        // Cache survives
        val after = repository.observeEarthquakes(EarthquakeFilter.Default).first()
        assertThat(after).hasSize(1)
        assertThat(after[0].id).isEqualTo("cached-eq")
    }

    // -----------------------------------------------------------------------
    // Concurrent refresh issues a single network request
    // -----------------------------------------------------------------------

    @Test
    fun `concurrent refreshes issue only one network request`() = runTest {
        val fixture = fixtureJson()
        // Add a body delay so the first refresh is still in-flight when the second starts
        mockWebServer.enqueue(
            MockResponse()
                .setBody(fixture)
                .setResponseCode(200)
                .setBodyDelay(200, TimeUnit.MILLISECONDS),
        )

        // Launch both concurrently — the first acquires the mutex, the second skips
        val deferred1 = async { repository.refresh() }
        val deferred2 = async { repository.refresh() }
        val result1 = deferred1.await()
        val result2 = deferred2.await()

        // Both succeed (second is a no-op), but only one request was made
        assertThat(result1).isInstanceOf(AppResult.Success::class.java)
        assertThat(result2).isInstanceOf(AppResult.Success::class.java)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    // -----------------------------------------------------------------------
    // Deferred is cleared after completion — subsequent refresh hits network
    // -----------------------------------------------------------------------

    @Test
    fun `refresh after a failure makes a new network request`() = runTest {
        // First refresh fails
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val firstResult = repository.refresh()
        assertThat(firstResult).isInstanceOf(AppResult.Failure::class.java)

        // Second refresh succeeds — must actually hit the network, not return stale deferred
        mockWebServer.enqueue(MockResponse().setBody(fixtureJson()).setResponseCode(200))
        val secondResult = repository.refresh()
        assertThat(secondResult).isInstanceOf(AppResult.Success::class.java)

        // Two distinct network requests were made
        assertThat(mockWebServer.requestCount).isEqualTo(2)

        // Data from the second refresh is persisted
        val earthquakes = repository.observeEarthquakes(EarthquakeFilter.Default).first()
        assertThat(earthquakes).hasSize(3)
    }

    // -----------------------------------------------------------------------
    // Empty features array is a legitimate success, not an error
    // -----------------------------------------------------------------------

    @Test
    fun `empty features array is treated as success not error`() = runTest {
        val emptyFeed = """{"type":"FeatureCollection","features":[]}"""
        mockWebServer.enqueue(MockResponse().setBody(emptyFeed).setResponseCode(200))

        val result = repository.refresh()
        assertThat(result).isInstanceOf(AppResult.Success::class.java)

        val earthquakes = repository.observeEarthquakes(EarthquakeFilter.Default).first()
        assertThat(earthquakes).isEmpty()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun seedEntity(id: String) = EarthquakeEntity(
        id = id,
        magnitude = 4.5,
        magnitudeType = "ml",
        place = "Test Place",
        occurredAtMillis = fixedClock.instant().toEpochMilli(),
        updatedAtMillis = fixedClock.instant().toEpochMilli(),
        latitude = 37.0,
        longitude = -120.0,
        depthKm = 10.0,
        tsunamiWarning = false,
        significance = 200,
        alertLevel = null,
        detailsUrl = "https://earthquake.usgs.gov/earthquakes/eventpage/$id",
        eventType = "earthquake",
    )

    private fun fixtureJson(): String =
        javaClass.classLoader!!.getResourceAsStream("usgs_2.5_week.json")!!
            .bufferedReader()
            .readText()
}
