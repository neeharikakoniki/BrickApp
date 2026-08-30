package com.brick.earthquaketracker.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun EarthquakeMapScreen(
    state: MapUiState,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                ) {
                    state.earthquakes.forEach { listing ->
                        val quake = listing.earthquake
                        val position = LatLng(
                            quake.coordinates.latitude,
                            quake.coordinates.longitude,
                        )
                        val hue = remember(quake.magnitude) { magnitudeHue(quake.magnitude) }

                        val markerState = rememberMarkerState(
                            key = quake.id,
                            position = position,
                        )

                        Marker(
                            state = markerState,
                            title = quake.place,
                            snippet = buildSnippet(listing),
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                it.showInfoWindow()
                                true
                            },
                            onInfoWindowClick = {
                                onMarkerClick(quake.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun magnitudeHue(magnitude: Double?): Float = when {
    magnitude == null -> BitmapDescriptorFactory.HUE_VIOLET
    magnitude >= 7.0 -> BitmapDescriptorFactory.HUE_RED
    magnitude >= 5.0 -> BitmapDescriptorFactory.HUE_ORANGE
    magnitude >= 4.0 -> BitmapDescriptorFactory.HUE_YELLOW
    magnitude >= 3.0 -> BitmapDescriptorFactory.HUE_GREEN
    else -> BitmapDescriptorFactory.HUE_AZURE
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
