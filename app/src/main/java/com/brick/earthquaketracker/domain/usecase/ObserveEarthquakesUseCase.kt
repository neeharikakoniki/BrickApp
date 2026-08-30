package com.brick.earthquaketracker.domain.usecase

import com.brick.earthquaketracker.domain.geo.GeoMath
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import com.brick.earthquaketracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The core use case: combines the earthquake stream with the location stream,
 * enriches each event with distance and bearing, then applies sort order.
 *
 * Grant location mid-session → the flow re-emits with distances populated and
 * the list re-sorted to nearest-first. No refetch, no manual invalidation.
 */
class ObserveEarthquakesUseCase @Inject constructor(
    private val earthquakeRepository: EarthquakeRepository,
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(
        filter: EarthquakeFilter,
        sortOrder: SortOrder,
    ): Flow<List<EarthquakeListing>> =
        combine(
            earthquakeRepository.observeEarthquakes(filter),
            locationRepository.observeLocationState(),
        ) { quakes, locationState ->
            val origin = (locationState as? LocationState.Available)?.location?.coordinates
            quakes
                .map { it.toListing(origin) }
                .sortedWith(sortOrder.comparator(hasLocation = origin != null))
        }
}

/**
 * Enriches an [Earthquake] with distance and bearing relative to [origin].
 * Both are null when no user location is available.
 */
private fun Earthquake.toListing(origin: Coordinates?): EarthquakeListing {
    val distance = origin?.let { GeoMath.haversineKm(it, coordinates) }
    val bearing = origin?.let {
        GeoMath.toCardinal(GeoMath.bearingDegrees(it, coordinates))
    }
    return EarthquakeListing(
        earthquake = this,
        distanceKm = distance,
        bearing = bearing,
    )
}
