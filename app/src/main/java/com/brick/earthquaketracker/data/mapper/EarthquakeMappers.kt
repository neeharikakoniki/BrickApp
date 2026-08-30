package com.brick.earthquaketracker.data.mapper

import com.brick.earthquaketracker.data.local.EarthquakeEntity
import com.brick.earthquaketracker.data.remote.dto.FeatureDto
import com.brick.earthquaketracker.domain.model.AlertLevel
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.Earthquake
import java.time.Instant

//Returns null when the feature has incomplete coordinates
fun FeatureDto.toEntity(): EarthquakeEntity? {
    if (geometry.coordinates.size < 3) return null

    return EarthquakeEntity(
        id = id,
        magnitude = properties.mag,
        magnitudeType = properties.magType,
        place = properties.place ?: properties.title ?: "Unknown location",
        occurredAtMillis = properties.time,
        updatedAtMillis = properties.updated,
        // GeoJSON: [longitude, latitude, depth]
        longitude = geometry.coordinates[0],
        latitude = geometry.coordinates[1],
        depthKm = geometry.coordinates[2],
        tsunamiWarning = properties.tsunami == 1,
        significance = properties.sig,
        alertLevel = properties.alert,
        detailsUrl = properties.url,
        eventType = properties.type,
    )
}

fun EarthquakeEntity.toDomain(): Earthquake = Earthquake(
    id = id,
    magnitude = magnitude,
    magnitudeType = magnitudeType,
    place = place,
    occurredAt = Instant.ofEpochMilli(occurredAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    depthKm = depthKm,
    tsunamiWarning = tsunamiWarning,
    significance = significance,
    alertLevel = alertLevel?.toAlertLevel(),
    detailsUrl = detailsUrl,
    eventType = eventType,
)

private fun String.toAlertLevel(): AlertLevel? = when (lowercase()) {
    "green" -> AlertLevel.GREEN
    "yellow" -> AlertLevel.YELLOW
    "orange" -> AlertLevel.ORANGE
    "red" -> AlertLevel.RED
    else -> null
}
