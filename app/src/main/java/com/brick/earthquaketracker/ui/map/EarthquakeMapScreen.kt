package com.brick.earthquaketracker.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.ui.components.MagnitudeBadge
import com.brick.earthquaketracker.ui.components.magnitudeColor
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MAGNITUDE_THRESHOLDS = listOf(
    null to "All",
    4.0 to "M4+",
    5.0 to "M5+",
    6.0 to "M6+",
)

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun EarthquakeMapScreen(
    state: MapUiState,
    onMarkerClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onFilterChange: (EarthquakeFilter) -> Unit,
    focusEventId: String?,
    bottomBarHeight: Int,
    modifier: Modifier = Modifier,
) {
    var selectedListing by remember { mutableStateOf<EarthquakeListing?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar with title and refresh
        MapTopBar(
            filter = state.filter,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            onFilterChange = onFilterChange,
        )

        // Map fills the remaining space
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val userLatLng = (state.locationState as? LocationState.Available)?.let {
                    LatLng(it.location.coordinates.latitude, it.location.coordinates.longitude)
                }

                val cameraPositionState = rememberCameraPositionState {
                    position = CameraFraming.initial(
                        userLocation = (state.locationState as? LocationState.Available)?.location,
                        quakes = state.earthquakes.map { it.earthquake },
                    )
                }

                // Animate to focused event when arriving from detail screen
                LaunchedEffect(focusEventId) {
                    if (focusEventId != null) {
                        val target = state.earthquakes.firstOrNull {
                            it.earthquake.id == focusEventId
                        }
                        target?.let {
                            val pos = LatLng(
                                it.earthquake.coordinates.latitude,
                                it.earthquake.coordinates.longitude,
                            )
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(pos, 8f),
                            )
                            selectedListing = it
                        }
                    }
                }

                val clusterItems = remember(state.earthquakes) {
                    state.earthquakes.map { EarthquakeClusterItem(it) }
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = state.hasLocationPermission,
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                    ),
                    contentPadding = PaddingValues(bottom = bottomBarHeight.dp),
                ) {
                    Clustering(
                        items = clusterItems,
                        onClusterItemClick = { item ->
                            selectedListing = item.listing
                            true
                        },
                        onClusterClick = { cluster ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        cluster.position,
                                        cameraPositionState.position.zoom + 2f,
                                    ),
                                )
                            }
                            true
                        },
                        clusterContent = { cluster ->
                            ClusterBubble(cluster)
                        },
                        clusterItemContent = { item ->
                            MarkerBubble(item)
                        },
                    )
                }

                // Recenter FAB — only when location is available
                if (userLatLng != null) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(userLatLng, 5f),
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = (16 + bottomBarHeight).dp),
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center on my location")
                    }
                }
            }

            // Bottom sheet for selected earthquake
            if (selectedListing != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedListing = null },
                    sheetState = sheetState,
                ) {
                    selectedListing?.let { listing ->
                        QuakeBottomSheetContent(
                            listing = listing,
                            onViewDetails = {
                                onMarkerClick(listing.earthquake.id)
                                selectedListing = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTopBar(
    filter: EarthquakeFilter,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onFilterChange: (EarthquakeFilter) -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column {
            TopAppBar(
                title = { Text("Seismic Map") },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MAGNITUDE_THRESHOLDS.forEach { (threshold, label) ->
                    FilterChip(
                        selected = filter.minMagnitude == threshold,
                        onClick = { onFilterChange(EarthquakeFilter(minMagnitude = threshold)) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

/**
 * Cluster bubble: circle with a white ring border and "×" prefix on count.
 * Visually distinct from individual markers (no border, magnitude number).
 */
@Composable
private fun ClusterBubble(cluster: Cluster<EarthquakeClusterItem>) {
    val maxMagnitude = cluster.items.maxOfOrNull { it.listing.earthquake.magnitude ?: 0.0 }
    val color = magnitudeColor(maxMagnitude)
    val count = cluster.size

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(width = 3.dp, color = Color.White, shape = CircleShape)
            .background(color, CircleShape)
            .semantics { contentDescription = "$count earthquakes in this area" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×$count",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Individual marker: teardrop-shaped pin with magnitude number.
 */
@Composable
private fun MarkerBubble(item: EarthquakeClusterItem) {
    val magnitude = item.listing.earthquake.magnitude
    val color = magnitudeColor(magnitude)
    val display = magnitude?.let { "%.1f".format(it) } ?: "?"
    val place = item.listing.earthquake.place
    val a11yLabel = magnitude?.let { "Magnitude $display earthquake, $place" }
        ?: "Earthquake, $place"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { contentDescription = a11yLabel },
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color, RoundedCornerShape(topStart = 50f, topEnd = 50f, bottomEnd = 50f, bottomStart = 10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = display,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        // Pin tail
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 6.dp)
                .background(color),
        )
    }
}

@Composable
private fun QuakeBottomSheetContent(
    listing: EarthquakeListing,
    onViewDetails: () -> Unit,
) {
    val quake = listing.earthquake
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
        .withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MagnitudeBadge(quake.magnitude, Modifier.size(56.dp))
            Column {
                Text(
                    text = quake.place,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = dateFormatter.format(quake.occurredAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DetailChip(icon = Icons.Outlined.Straighten, text = "%.1f km deep".format(quake.depthKm))
            listing.distanceKm?.let { km ->
                DetailChip(
                    icon = Icons.Outlined.LocationOn,
                    text = "%.0f km %s".format(km, listing.bearing?.label ?: ""),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = onViewDetails,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("View details")
        }
    }
}

@Composable
private fun DetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
