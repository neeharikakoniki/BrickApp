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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brick.earthquaketracker.domain.model.AlertLevel
import com.brick.earthquaketracker.domain.usecase.EarthquakeDetail
import com.brick.earthquaketracker.ui.components.MagnitudeBadge
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeDetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
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
                    DetailContent(detail = state.detail)
                }
            }
        }
    }
}

@Composable
private fun DetailContent(detail: EarthquakeDetail) {
    val quake = detail.earthquake
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a z")
        .withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MagnitudeBadge(quake.magnitude, Modifier.size(64.dp))
            Column {
                Text(
                    text = quake.place,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = dateFormatter.format(quake.occurredAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (quake.tsunamiWarning) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
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

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        DetailRow(
            icon = Icons.Outlined.Straighten,
            label = "Depth",
            value = "%.1f km".format(quake.depthKm),
        )

        quake.magnitudeType?.let {
            DetailRow(
                icon = Icons.Outlined.Straighten,
                label = "Magnitude type",
                value = it.uppercase(),
            )
        }

        quake.alertLevel?.let {
            DetailRow(
                icon = Icons.Outlined.Warning,
                label = "Alert level",
                value = it.name.lowercase().replaceFirstChar { c -> c.uppercase() },
            )
        }

        DetailRow(
            icon = Icons.Outlined.Schedule,
            label = "Updated",
            value = dateFormatter.format(quake.updatedAt),
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
            value = "%.4f, %.4f".format(quake.coordinates.latitude, quake.coordinates.longitude),
        )
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
