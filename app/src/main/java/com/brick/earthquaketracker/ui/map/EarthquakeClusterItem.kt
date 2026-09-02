package com.brick.earthquaketracker.ui.map

import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class EarthquakeClusterItem(
    val listing: EarthquakeListing,
) : ClusterItem {
    private val position = LatLng(
        listing.earthquake.coordinates.latitude,
        listing.earthquake.coordinates.longitude,
    )

    override fun getPosition(): LatLng = position
    override fun getTitle(): String = listing.earthquake.place
    override fun getSnippet(): String = buildSnippet(listing)
    override fun getZIndex(): Float = listing.earthquake.magnitude?.toFloat() ?: 0f
}

private fun buildSnippet(listing: EarthquakeListing): String {
    val quake = listing.earthquake
    val mag = quake.magnitude?.let { "M%.1f".format(it) } ?: "M?"
    val depth = "%.1f km deep".format(quake.depthKm)
    val distance = listing.distanceKm?.let {
        " · %.0f km %s".format(it, listing.bearing?.label ?: "")
    } ?: ""
    return "$mag · $depth$distance"
}
