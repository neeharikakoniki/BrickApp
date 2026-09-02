package com.brick.earthquaketracker.ui.map

import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.UserLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

object CameraFraming {

    private val WORLD_VIEW = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)

    fun initial(
        userLocation: UserLocation?,
        quakes: List<Earthquake>,
    ): CameraPosition = when {
        userLocation != null -> CameraPosition.fromLatLngZoom(
            LatLng(userLocation.coordinates.latitude, userLocation.coordinates.longitude),
            5f,
        )
        quakes.isNotEmpty() -> {
            val avgLat = quakes.sumOf { it.coordinates.latitude } / quakes.size
            val avgLng = quakes.sumOf { it.coordinates.longitude } / quakes.size
            CameraPosition.fromLatLngZoom(LatLng(avgLat, avgLng), 3f)
        }
        else -> WORLD_VIEW
    }
}
