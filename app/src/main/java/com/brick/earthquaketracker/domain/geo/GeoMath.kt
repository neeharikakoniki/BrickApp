package com.brick.earthquaketracker.domain.geo

import com.brick.earthquaketracker.domain.model.Bearing
import com.brick.earthquaketracker.domain.model.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


object GeoMath {

    private const val EARTH_RADIUS_KM = 6_371.0

    /** Haversine distance in kilometres between two points. */
    fun haversineKm(from: Coordinates, to: Coordinates): Double {
        val dLat = (to.latitude - from.latitude).toRadians()
        val dLon = (to.longitude - from.longitude).toRadians()
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Initial bearing in degrees (0 = north, 90 = east) from [from] to [to].
     * Returns a value in [0, 360).
     */
    fun bearingDegrees(from: Coordinates, to: Coordinates): Double {
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val dLon = (to.longitude - from.longitude).toRadians()

        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val theta = atan2(x, y)
        return (theta.toDegrees() + 360) % 360
    }

    /** Converts a bearing in degrees to a 16-point compass [Bearing]. */
    fun toCardinal(degrees: Double): Bearing {
        // Each of the 16 sectors spans 22.5°. Adding 11.25° and dividing by 22.5
        // maps degrees to the nearest sector index.
        val index = ((degrees + 11.25) % 360 / 22.5).toInt()
        return Bearing.entries[index]
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
    private fun Double.toDegrees(): Double = this * 180.0 / PI
}
