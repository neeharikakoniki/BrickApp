package com.example.brickapp.domain.model

/**
 * An [Earthquake] enriched with distance and bearing relative to the user.
 * Produced by the use case, not the ViewModel — the enrichment is business logic.
 * [distanceKm] and [bearing] are null when no user location is available.
 */
data class EarthquakeListing(
    val earthquake: Earthquake,
    val distanceKm: Double?,
    val bearing: Bearing?,
)

/** 16-point compass bearing. */
enum class Bearing(val label: String) {
    N("N"), NNE("NNE"), NE("NE"), ENE("ENE"),
    E("E"), ESE("ESE"), SE("SE"), SSE("SSE"),
    S("S"), SSW("SSW"), SW("SW"), WSW("WSW"),
    W("W"), WNW("WNW"), NW("NW"), NNW("NNW"),
}
