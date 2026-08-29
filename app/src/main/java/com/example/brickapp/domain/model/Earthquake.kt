package com.example.brickapp.domain.model

import java.time.Instant

data class Earthquake(
    val id: String,
    val magnitude: Double?,
    val magnitudeType: String?,
    val place: String,
    val occurredAt: Instant,
    val updatedAt: Instant,
    val coordinates: Coordinates,
    val depthKm: Double,
    val tsunamiWarning: Boolean,
    val significance: Int,
    val alertLevel: AlertLevel?,
    val detailsUrl: String,
    val eventType: String,
)

enum class AlertLevel { GREEN, YELLOW, ORANGE, RED }
