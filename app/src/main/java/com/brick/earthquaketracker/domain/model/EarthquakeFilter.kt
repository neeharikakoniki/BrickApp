package com.brick.earthquaketracker.domain.model

data class EarthquakeFilter(
    val minMagnitude: Double? = null,
) {
    companion object {
        val Default = EarthquakeFilter()
    }
}
