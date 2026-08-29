package com.example.brickapp.domain.model

/**
 * Sort orders for the earthquake list.
 *
 * [NEAREST] degrades to [MOST_RECENT] when no user location is available,
 * rather than producing an arbitrary order. Null magnitudes sort last under [LARGEST].
 */
enum class SortOrder {
    NEAREST,
    MOST_RECENT,
    LARGEST;

    /**
     * Returns a comparator for [EarthquakeListing] items.
     *
     * @param hasLocation whether a user location is available. When false,
     *   [NEAREST] falls back to [MOST_RECENT] rather than producing arbitrary order.
     */
    fun comparator(hasLocation: Boolean): Comparator<EarthquakeListing> = when {
        this == NEAREST && hasLocation -> compareBy(nullsLast()) { it.distanceKm }
        this == NEAREST -> MOST_RECENT.comparator(false) // fallback: no location → recency
        this == MOST_RECENT -> compareByDescending { it.earthquake.occurredAt }
        // compareByDescending reverses the inner comparator: nullsFirst().reversed() = descending + nulls last.
        else -> compareByDescending(nullsFirst()) { it.earthquake.magnitude }
    }
}
