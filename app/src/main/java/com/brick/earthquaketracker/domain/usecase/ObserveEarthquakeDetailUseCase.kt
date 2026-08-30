package com.brick.earthquaketracker.domain.usecase

import com.brick.earthquaketracker.domain.geo.GeoMath
import com.brick.earthquaketracker.domain.model.Bearing
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import com.brick.earthquaketracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Observes a single earthquake by ID, enriched with distance and bearing
 * from the user's current location.
 */
class ObserveEarthquakeDetailUseCase @Inject constructor(
    private val earthquakeRepository: EarthquakeRepository,
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(eventId: String): Flow<EarthquakeDetail?> =
        combine(
            earthquakeRepository.observeEarthquake(eventId),
            locationRepository.observeLocationState(),
        ) { earthquake, locationState ->
            earthquake?.let {
                val origin =
                    (locationState as? LocationState.Available)?.location?.coordinates
                val distance = origin?.let { o -> GeoMath.haversineKm(o, it.coordinates) }
                val bearing = origin?.let { o ->
                    GeoMath.toCardinal(GeoMath.bearingDegrees(o, it.coordinates))
                }
                EarthquakeDetail(
                    earthquake = it,
                    distanceKm = distance,
                    bearing = bearing,
                )
            }
        }
}

data class EarthquakeDetail(
    val earthquake: Earthquake,
    val distanceKm: Double?,
    val bearing: Bearing?,
)
