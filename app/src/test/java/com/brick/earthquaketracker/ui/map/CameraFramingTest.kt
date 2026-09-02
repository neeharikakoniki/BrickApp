package com.brick.earthquaketracker.ui.map

import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.UserLocation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class CameraFramingTest {

    @Test
    fun `with user location centers on user at zoom 5`() {
        val location = UserLocation(
            coordinates = Coordinates(latitude = 30.0, longitude = -97.0),
            capturedAt = Instant.now(),
        )

        val result = CameraFraming.initial(location, emptyList())

        assertThat(result.target.latitude).isWithin(0.001).of(30.0)
        assertThat(result.target.longitude).isWithin(0.001).of(-97.0)
        assertThat(result.zoom).isEqualTo(5f)
    }

    @Test
    fun `without location centers on quake centroid at zoom 3`() {
        val quakes = listOf(
            testQuake("q1", lat = 35.0, lng = 139.0),
            testQuake("q2", lat = 37.0, lng = -122.0),
        )

        val result = CameraFraming.initial(null, quakes)

        assertThat(result.target.latitude).isWithin(0.001).of(36.0)
        assertThat(result.target.longitude).isWithin(0.001).of(8.5)
        assertThat(result.zoom).isEqualTo(3f)
    }

    @Test
    fun `empty list and no location shows world view`() {
        val result = CameraFraming.initial(null, emptyList())

        assertThat(result.target.latitude).isWithin(0.001).of(20.0)
        assertThat(result.target.longitude).isWithin(0.001).of(0.0)
        assertThat(result.zoom).isEqualTo(2f)
    }

    @Test
    fun `user location takes priority over quakes`() {
        val location = UserLocation(
            coordinates = Coordinates(latitude = 30.0, longitude = -97.0),
            capturedAt = Instant.now(),
        )
        val quakes = listOf(testQuake("q1", lat = 35.0, lng = 139.0))

        val result = CameraFraming.initial(location, quakes)

        assertThat(result.target.latitude).isWithin(0.001).of(30.0)
        assertThat(result.zoom).isEqualTo(5f)
    }

    private fun testQuake(id: String, lat: Double, lng: Double) = Earthquake(
        id = id,
        magnitude = 4.5,
        magnitudeType = "ml",
        place = "Test",
        occurredAt = Instant.now(),
        updatedAt = Instant.now(),
        coordinates = Coordinates(latitude = lat, longitude = lng),
        depthKm = 10.0,
        tsunamiWarning = false,
        significance = 200,
        alertLevel = null,
        detailsUrl = "https://example.com/$id",
        eventType = "earthquake",
    )
}
