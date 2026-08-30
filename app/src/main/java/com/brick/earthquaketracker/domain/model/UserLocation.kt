package com.brick.earthquaketracker.domain.model

import java.time.Instant

data class UserLocation(
    val coordinates: Coordinates,
    val capturedAt: Instant,
)
