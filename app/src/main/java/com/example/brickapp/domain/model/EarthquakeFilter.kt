package com.example.brickapp.domain.model

/**
 * Client-side filter applied over the cached earthquake superset.
 * Filtering locally means filters keep working offline.
 */
data class EarthquakeFilter(
    val minMagnitude: Double? = null,
) {
    companion object {
        val Default = EarthquakeFilter()
    }
}
