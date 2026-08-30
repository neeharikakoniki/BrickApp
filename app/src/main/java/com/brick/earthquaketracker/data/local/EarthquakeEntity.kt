package com.brick.earthquaketracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "earthquakes",
    indices = [
        Index("occurredAtMillis"),
        Index("magnitude"),
    ],
)
data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val magnitude: Double?,
    val magnitudeType: String?,
    val place: String,
    val occurredAtMillis: Long,
    val updatedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val depthKm: Double,
    val tsunamiWarning: Boolean,
    val significance: Int,
    val alertLevel: String?,
    val detailsUrl: String,
    val eventType: String,
)
