package com.brick.earthquaketracker.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SortOrderTest {

    private fun listing(
        mag: Double? = 4.0,
        distanceKm: Double? = 100.0,
        occurredAt: Instant = Instant.parse("2024-01-15T12:00:00Z"),
        id: String = "test",
    ) = EarthquakeListing(
        earthquake = Earthquake(
            id = id,
            magnitude = mag,
            magnitudeType = "ml",
            place = "Test",
            occurredAt = occurredAt,
            updatedAt = occurredAt,
            coordinates = Coordinates(0.0, 0.0),
            depthKm = 10.0,
            tsunamiWarning = false,
            significance = 100,
            alertLevel = null,
            detailsUrl = "",
            eventType = "earthquake",
        ),
        distanceKm = distanceKm,
        bearing = null,
    )

    @Test
    fun `NEAREST sorts by distance ascending`() {
        val close = listing(distanceKm = 50.0, id = "close")
        val far = listing(distanceKm = 500.0, id = "far")
        val sorted = listOf(far, close).sortedWith(SortOrder.NEAREST.comparator(hasLocation = true))
        assertThat(sorted.map { it.earthquake.id }).containsExactly("close", "far").inOrder()
    }

    @Test
    fun `NEAREST without location falls back to MOST_RECENT`() {
        val older = listing(
            occurredAt = Instant.parse("2024-01-10T00:00:00Z"),
            distanceKm = 50.0,
            id = "older",
        )
        val newer = listing(
            occurredAt = Instant.parse("2024-01-15T00:00:00Z"),
            distanceKm = 500.0,
            id = "newer",
        )
        val sorted = listOf(older, newer).sortedWith(SortOrder.NEAREST.comparator(hasLocation = false))
        assertThat(sorted.map { it.earthquake.id }).containsExactly("newer", "older").inOrder()
    }

    @Test
    fun `MOST_RECENT sorts by time descending`() {
        val older = listing(occurredAt = Instant.parse("2024-01-10T00:00:00Z"), id = "older")
        val newer = listing(occurredAt = Instant.parse("2024-01-15T00:00:00Z"), id = "newer")
        val sorted = listOf(older, newer).sortedWith(SortOrder.MOST_RECENT.comparator(hasLocation = false))
        assertThat(sorted.map { it.earthquake.id }).containsExactly("newer", "older").inOrder()
    }

    @Test
    fun `LARGEST sorts by magnitude descending`() {
        val small = listing(mag = 2.5, id = "small")
        val big = listing(mag = 6.0, id = "big")
        val sorted = listOf(small, big).sortedWith(SortOrder.LARGEST.comparator(hasLocation = false))
        assertThat(sorted.map { it.earthquake.id }).containsExactly("big", "small").inOrder()
    }

    @Test
    fun `LARGEST puts null magnitude last`() {
        val known = listing(mag = 3.0, id = "known")
        val unknown = listing(mag = null, id = "unknown")
        val sorted = listOf(unknown, known).sortedWith(SortOrder.LARGEST.comparator(hasLocation = false))
        assertThat(sorted.map { it.earthquake.id }).containsExactly("known", "unknown").inOrder()
    }

    @Test
    fun `NEAREST puts null distance last`() {
        val close = listing(distanceKm = 50.0, id = "close")
        val noDistance = listing(distanceKm = null, id = "noDistance")
        val sorted = listOf(noDistance, close).sortedWith(SortOrder.NEAREST.comparator(hasLocation = true))
        assertThat(sorted.map { it.earthquake.id }).containsExactly("close", "noDistance").inOrder()
    }
}
