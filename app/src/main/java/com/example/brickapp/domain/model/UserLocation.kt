package com.example.brickapp.domain.model

import java.time.Instant

data class UserLocation(
    val coordinates: Coordinates,
    val capturedAt: Instant,
)
