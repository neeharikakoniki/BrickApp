package com.brick.earthquaketracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brick.earthquaketracker.domain.model.AlertLevel
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.usecase.EarthquakeDetail
import com.brick.earthquaketracker.ui.components.MagnitudeBadge
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeDetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onViewOnMap: (String) -> Unit,
    onOpenUsgs: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                DetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                DetailUiState.NotFound -> {
                    Text(
                        text = "Earthquake not found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is DetailUiState.Content -> {
                    DetailContent(
                        detail = state.detail,
                        onViewOnMap = { onViewOnMap(state.detail.earthquake.id) },
                        onOpenUsgs = { onOpenUsgs(state.detail.earthquake.detailsUrl) },
                        onShare = { onShare(buildShareText(state.detail)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: EarthquakeDetail,
    onViewOnMap: () -> Unit,
    onOpenUsgs: () -> Unit,
    onShare: () -> Unit,
) {
    val quake = detail.earthquake
    val absoluteFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a z")
        .withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header: magnitude badge + place + time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MagnitudeBadge(
                magnitude = quake.magnitude,
                modifier = Modifier.size(64.dp),
                sharedElementKey = "magnitude_badge_${quake.id}",
            )
            Column {
                Text(
                    text = quake.place,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatRelativeTime(quake.occurredAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = absoluteFormatter.format(quake.occurredAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Tsunami warning card
        if (quake.tsunamiWarning) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Tsunami warning issued",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Alert level card
        quake.alertLevel?.let { alert ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = alertColor(alert).copy(alpha = 0.12f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = "Alert",
                        tint = alertColor(alert),
                    )
                    Text(
                        text = "PAGER alert: ${alert.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = alertColor(alert),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Action buttons
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onViewOnMap,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Map")
            }
            FilledTonalButton(
                onClick = onOpenUsgs,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("USGS")
            }
            FilledTonalButton(
                onClick = onShare,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Share")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Detail rows
        DetailRow(
            icon = Icons.Outlined.Straighten,
            label = "Depth",
            value = formatDepth(quake.depthKm),
        )

        detail.distanceKm?.let { km ->
            DetailRow(
                icon = Icons.Outlined.LocationOn,
                label = "Distance from you",
                value = "%.0f km %s".format(km, detail.bearing?.label ?: ""),
            )
        }

        DetailRow(
            icon = Icons.Outlined.LocationOn,
            label = "Coordinates",
            value = formatCoordinates(quake.coordinates.latitude, quake.coordinates.longitude),
        )

        quake.magnitudeType?.let {
            DetailRow(
                icon = Icons.Outlined.Straighten,
                label = "Magnitude type",
                value = it.uppercase(),
            )
        }

        DetailRow(
            icon = Icons.Outlined.Info,
            label = "Event type",
            value = quake.eventType.replaceFirstChar { it.uppercase() },
        )

        DetailRow(
            icon = Icons.Outlined.Schedule,
            label = "Updated",
            value = absoluteFormatter.format(quake.updatedAt),
        )
    }
}

private fun formatDepth(depthKm: Double): String = when {
    depthKm < 0 -> "%.1f km above sea level".format(-depthKm)
    else -> "%.1f km".format(depthKm)
}

private fun formatCoordinates(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return "%.4f°%s, %.4f°%s".format(
        kotlin.math.abs(lat), latDir,
        kotlin.math.abs(lon), lonDir,
    )
}

private fun formatRelativeTime(instant: Instant): String {
    val duration = Duration.between(instant, Instant.now())
    return when {
        duration.toDays() > 0 -> "${duration.toDays()}d ago"
        duration.toHours() > 0 -> "${duration.toHours()}h ago"
        duration.toMinutes() > 0 -> "${duration.toMinutes()}m ago"
        else -> "just now"
    }
}

private fun alertColor(alert: AlertLevel): Color = when (alert) {
    AlertLevel.GREEN -> Color(0xFF388E3C)
    AlertLevel.YELLOW -> Color(0xFFFBC02D)
    AlertLevel.ORANGE -> Color(0xFFF57C00)
    AlertLevel.RED -> Color(0xFFD32F2F)
}

private fun buildShareText(detail: EarthquakeDetail): String {
    val quake = detail.earthquake
    val mag = quake.magnitude?.let { "M%.1f".format(it) } ?: "M?"
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z")
        .withZone(ZoneId.systemDefault())
    return buildString {
        append("$mag earthquake — ${quake.place}\n")
        append(dateFormatter.format(quake.occurredAt))
        append("\nDepth: ${formatDepth(quake.depthKm)}")
        detail.distanceKm?.let { km ->
            append("\n%.0f km %s from me".format(km, detail.bearing?.label ?: ""))
        }
        if (quake.tsunamiWarning) append("\n⚠️ Tsunami warning issued")
        append("\n\n${quake.detailsUrl}")
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
