package com.brick.earthquaketracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EarthquakeDaoTest {

    private lateinit var database: QuakesDatabase
    private lateinit var dao: EarthquakeDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuakesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.earthquakeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert inserts new event`() = runTest {
        dao.upsertAll(listOf(entity("eq1")))

        assertThat(dao.count()).isEqualTo(1)
        val all = dao.observeAll(null).first()
        assertThat(all[0].id).isEqualTo("eq1")
    }

    @Test
    fun `upsert same id replaces event`() = runTest {
        dao.upsertAll(listOf(entity("eq1", magnitude = 3.0)))
        dao.upsertAll(listOf(entity("eq1", magnitude = 5.0)))

        assertThat(dao.count()).isEqualTo(1)
        val all = dao.observeAll(null).first()
        assertThat(all[0].magnitude).isEqualTo(5.0)
    }

    @Test
    fun `syncAndPrune skips incoming event with older updatedAtMillis`() = runTest {
        val original = entity("eq1", updatedAtMillis = 2000L, magnitude = 4.0)
        dao.upsertAll(listOf(original))

        val stale = entity("eq1", updatedAtMillis = 1000L, magnitude = 9.9)
        dao.syncAndPrune(listOf(stale), cutoffMillis = 0L)

        val result = dao.observeAll(null).first()
        assertThat(result).hasSize(1)
        assertThat(result[0].magnitude).isEqualTo(4.0) // original retained
        assertThat(result[0].updatedAtMillis).isEqualTo(2000L)
    }

    @Test
    fun `syncAndPrune accepts incoming event with newer updatedAtMillis`() = runTest {
        val original = entity("eq1", updatedAtMillis = 1000L, magnitude = 4.0)
        dao.upsertAll(listOf(original))

        val revised = entity("eq1", updatedAtMillis = 2000L, magnitude = 4.3)
        dao.syncAndPrune(listOf(revised), cutoffMillis = 0L)

        val result = dao.observeAll(null).first()
        assertThat(result).hasSize(1)
        assertThat(result[0].magnitude).isEqualTo(4.3) // updated
    }

    @Test
    fun `syncAndPrune prunes events older than cutoff`() = runTest {
        val old = entity("old", occurredAtMillis = 1000L)
        val recent = entity("recent", occurredAtMillis = 5000L)
        dao.upsertAll(listOf(old, recent))

        dao.syncAndPrune(emptyList(), cutoffMillis = 3000L)

        val result = dao.observeAll(null).first()
        assertThat(result.map { it.id }).containsExactly("recent")
    }

    @Test
    fun `observeAll with minMag filters correctly`() = runTest {
        dao.upsertAll(
            listOf(
                entity("small", magnitude = 2.5),
                entity("medium", magnitude = 4.0),
                entity("large", magnitude = 6.5),
            ),
        )

        val filtered = dao.observeAll(minMag = 4.0).first()
        assertThat(filtered.map { it.id }).containsExactly("large", "medium")
    }

    @Test
    fun `observeAll with null minMag returns all`() = runTest {
        dao.upsertAll(
            listOf(
                entity("small", magnitude = 2.5),
                entity("large", magnitude = 6.5),
            ),
        )

        val all = dao.observeAll(minMag = null).first()
        assertThat(all).hasSize(2)
    }

    @Test
    fun `observeAll returns events ordered by occurredAtMillis descending`() = runTest {
        dao.upsertAll(
            listOf(
                entity("oldest", occurredAtMillis = 1000L),
                entity("newest", occurredAtMillis = 3000L),
                entity("middle", occurredAtMillis = 2000L),
            ),
        )

        val result = dao.observeAll(null).first()
        assertThat(result.map { it.id }).containsExactly("newest", "middle", "oldest").inOrder()
    }

    @Test
    fun `observeById returns matching event`() = runTest {
        dao.upsertAll(listOf(entity("eq1"), entity("eq2")))

        val result = dao.observeById("eq1").first()
        assertThat(result?.id).isEqualTo("eq1")
    }

    @Test
    fun `observeById returns null for missing id`() = runTest {
        val result = dao.observeById("nonexistent").first()
        assertThat(result).isNull()
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private fun entity(
        id: String,
        magnitude: Double? = 4.5,
        occurredAtMillis: Long = 1700000000000L,
        updatedAtMillis: Long = 1700000100000L,
    ) = EarthquakeEntity(
        id = id,
        magnitude = magnitude,
        magnitudeType = "ml",
        place = "Test Place",
        occurredAtMillis = occurredAtMillis,
        updatedAtMillis = updatedAtMillis,
        latitude = 37.0,
        longitude = -120.0,
        depthKm = 10.0,
        tsunamiWarning = false,
        significance = 200,
        alertLevel = null,
        detailsUrl = "https://earthquake.usgs.gov/earthquakes/eventpage/$id",
        eventType = "earthquake",
    )
}
