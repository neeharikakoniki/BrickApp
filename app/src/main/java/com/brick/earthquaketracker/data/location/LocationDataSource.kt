package com.brick.earthquaketracker.data.location

import com.brick.earthquaketracker.domain.model.UserLocation

interface LocationDataSource {
    /**
     * Returns the device's current location, or null if a fix
     * could not be obtained within the timeout.
     */
    suspend fun currentLocation(): UserLocation?
}
