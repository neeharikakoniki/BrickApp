package com.brick.earthquaketracker.data.mapper

import com.brick.earthquaketracker.data.local.EarthquakeEntity
import com.brick.earthquaketracker.data.remote.dto.FeatureDto
import com.brick.earthquaketracker.data.remote.dto.GeometryDto
import com.brick.earthquaketracker.data.remote.dto.PropertiesDto
import com.brick.earthquaketracker.domain.model.AlertLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class EarthquakeMappersTest {

    @Test
    fun `toEntity maps GeoJSON coordinates as longitude-latitude-depth`() {
        val dto = feature(
            id = "us7000abcd",
            coordinates = listOf(136.8, 34.2, 35.0),
        )

        val entity = dto.toEntity()

        assertThat(entity!!.longitude).isEqualTo(136.8)
        assertThat(entity.latitude).isEqualTo(34.2)
        assertThat(entity.depthKm).isEqualTo(35.0)
    }

    @Test
    fun `toEntity handles null magnitude`() {
        val entity = feature(mag = null).toEntity()

        assertThat(entity!!.magnitude).isNull()
    }

    @Test
    fun `toEntity falls back to title when place is null`() {
        val entity = feature(place = null, title = "M ? - Unknown Region").toEntity()

        assertThat(entity!!.place).isEqualTo("M ? - Unknown Region")
    }

    @Test
    fun `toEntity falls back to Unknown location when both place and title are null`() {
        val entity = feature(place = null, title = null).toEntity()

        assertThat(entity!!.place).isEqualTo("Unknown location")
    }

    @Test
    fun `toEntity preserves negative depth`() {
        val entity = feature(coordinates = listOf(-120.5, 37.8, -2.5)).toEntity()

        assertThat(entity!!.depthKm).isEqualTo(-2.5)
    }

    @Test
    fun `toEntity maps tsunami flag from integer`() {
        val withTsunami = feature(tsunami = 1).toEntity()
        val withoutTsunami = feature(tsunami = 0).toEntity()

        assertThat(withTsunami!!.tsunamiWarning).isTrue()
        assertThat(withoutTsunami!!.tsunamiWarning).isFalse()
    }

    @Test
    fun `toEntity maps epoch millis directly`() {
        val entity = feature(time = 1700000000000L, updated = 1700000100000L).toEntity()

        assertThat(entity!!.occurredAtMillis).isEqualTo(1700000000000L)
        assertThat(entity.updatedAtMillis).isEqualTo(1700000100000L)
    }

    @Test
    fun `toEntity returns null when coordinates are incomplete`() {
        val entity = feature(coordinates = listOf(10.0)).toEntity()

        assertThat(entity).isNull()
    }

    @Test
    fun `toDomain converts epoch millis to Instant`() {
        val domain = entity(
            occurredAtMillis = 1700000000000L,
            updatedAtMillis = 1700000100000L,
        ).toDomain()

        assertThat(domain.occurredAt).isEqualTo(Instant.ofEpochMilli(1700000000000L))
        assertThat(domain.updatedAt).isEqualTo(Instant.ofEpochMilli(1700000100000L))
    }

    @Test
    fun `toDomain maps alert level string to enum`() {
        assertThat(entity(alertLevel = "green").toDomain().alertLevel).isEqualTo(AlertLevel.GREEN)
        assertThat(entity(alertLevel = "yellow").toDomain().alertLevel).isEqualTo(AlertLevel.YELLOW)
        assertThat(entity(alertLevel = "orange").toDomain().alertLevel).isEqualTo(AlertLevel.ORANGE)
        assertThat(entity(alertLevel = "red").toDomain().alertLevel).isEqualTo(AlertLevel.RED)
    }

    @Test
    fun `toDomain maps null alert level to null`() {
        assertThat(entity(alertLevel = null).toDomain().alertLevel).isNull()
    }

    @Test
    fun `toDomain maps unknown alert level string to null`() {
        assertThat(entity(alertLevel = "purple").toDomain().alertLevel).isNull()
    }

    @Test
    fun `toDomain maps coordinates correctly`() {
        val domain = entity(latitude = 34.2, longitude = 136.8).toDomain()

        assertThat(domain.coordinates.latitude).isEqualTo(34.2)
        assertThat(domain.coordinates.longitude).isEqualTo(136.8)
    }

    private fun feature(
        id: String = "test-id",
        mag: Double? = 4.5,
        place: String? = "Test Place",
        title: String? = "M 4.5 - Test Place",
        time: Long = 1700000000000L,
        updated: Long = 1700000100000L,
        coordinates: List<Double> = listOf(-120.0, 37.0, 10.0),
        tsunami: Int = 0,
        sig: Int = 200,
        alert: String? = null,
        magType: String? = "ml",
        eventType: String = "earthquake",
    ) = FeatureDto(
        type = "Feature",
        id = id,
        properties = PropertiesDto(
            mag = mag,
            place = place,
            time = time,
            updated = updated,
            url = "https://earthquake.usgs.gov/earthquakes/eventpage/$id",
            detail = null,
            status = "reviewed",
            tsunami = tsunami,
            sig = sig,
            type = eventType,
            title = title,
            alert = alert,
            magType = magType,
        ),
        geometry = GeometryDto(
            type = "Point",
            coordinates = coordinates,
        ),
    )

    private fun entity(
        id: String = "test-id",
        magnitude: Double? = 4.5,
        place: String = "Test Place",
        occurredAtMillis: Long = 1700000000000L,
        updatedAtMillis: Long = 1700000100000L,
        latitude: Double = 37.0,
        longitude: Double = -120.0,
        depthKm: Double = 10.0,
        alertLevel: String? = null,
    ) = EarthquakeEntity(
        id = id,
        magnitude = magnitude,
        magnitudeType = "ml",
        place = place,
        occurredAtMillis = occurredAtMillis,
        updatedAtMillis = updatedAtMillis,
        latitude = latitude,
        longitude = longitude,
        depthKm = depthKm,
        tsunamiWarning = false,
        significance = 200,
        alertLevel = alertLevel,
        detailsUrl = "https://earthquake.usgs.gov/earthquakes/eventpage/$id",
        eventType = "earthquake",
    )
}
