package com.brick.earthquaketracker.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.ui.components.EmptyState
import com.brick.earthquaketracker.ui.components.MagnitudeBadge
import com.brick.earthquaketracker.ui.components.StaleDataBanner
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MAGNITUDE_THRESHOLDS = listOf(
    null to "All",
    4.0 to "M4+",
    5.0 to "M5+",
    6.0 to "M6+",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeListScreen(
    state: ListUiState,
    onQuakeClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onFilterChange: (EarthquakeFilter) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onClearError: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDismissLocationPrompt: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isRefreshing) {
        if (wasRefreshing && !state.isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        wasRefreshing = state.isRefreshing
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ListTopBar(
                sortOrder = state.sortOrder,
                filter = state.filter,
                onSortChange = onSortChange,
                onFilterChange = onFilterChange,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AnimatedVisibility(visible = state.staleSince != null) {
                state.staleSince?.let { StaleDataBanner(it) }
            }

            when {
                state.isInitialLoading -> {
                    ShimmerList()
                }
                state.emptyReason != null -> {
                    EmptyState(
                        reason = state.emptyReason,
                        onClearFilter = if (state.emptyReason == EmptyReason.NO_RESULTS_FOR_FILTER) {
                            { onFilterChange(EarthquakeFilter.Default) }
                        } else null,
                        onRetry = if (state.emptyReason == EmptyReason.NO_CACHE_OFFLINE) {
                            onRefresh
                        } else null,
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            item(key = "location_banner") {
                                LocationBanner(
                                    locationState = state.locationState,
                                    dismissed = state.locationPromptDismissed,
                                    onEnable = onRequestLocationPermission,
                                    onDismiss = onDismissLocationPrompt,
                                    onOpenSettings = onOpenAppSettings,
                                )
                            }

                            item(key = "summary_header") {
                                SummaryHeader(
                                    count = state.earthquakes.size,
                                    totalCount = state.totalCount,
                                    isFiltered = state.isFiltered,
                                )
                            }

                            items(
                                items = state.earthquakes,
                                key = { it.earthquake.id },
                            ) { listing ->
                                Column(modifier = Modifier.animateItem()) {
                                    EarthquakeRow(
                                        listing = listing,
                                        onClick = { onQuakeClick(listing.earthquake.id) },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Summary header
// ---------------------------------------------------------------------------

@Composable
private fun SummaryHeader(
    count: Int,
    totalCount: Int,
    isFiltered: Boolean,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val label = if (isFiltered) {
        "${numberFormat.format(count)} of ${numberFormat.format(totalCount)} events"
    } else {
        "${numberFormat.format(count)} events"
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}

// ---------------------------------------------------------------------------
// Location banner — distinct per state
// ---------------------------------------------------------------------------

@Composable
private fun LocationBanner(
    locationState: LocationState,
    dismissed: Boolean,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (locationState) {
        LocationState.PermissionNotRequested -> {
            if (!dismissed) {
                LocationPromptRow(
                    icon = Icons.Outlined.LocationOn,
                    text = "See how far these are from you",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionLabel = "Enable",
                    onAction = onEnable,
                    onDismiss = onDismiss,
                )
            }
        }

        LocationState.PermissionDenied -> {
            if (!dismissed) {
                LocationPromptRow(
                    icon = Icons.Outlined.LocationOff,
                    text = "Distances need location access",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionLabel = "Try again",
                    onAction = onEnable,
                    onDismiss = onDismiss,
                )
            }
        }

        LocationState.PermanentlyDenied -> {
            if (!dismissed) {
                LocationPromptRow(
                    icon = Icons.Outlined.Settings,
                    text = "Location denied — enable in Settings to see distances",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionLabel = "Settings",
                    onAction = onOpenSettings,
                    onDismiss = onDismiss,
                )
            }
        }

        LocationState.Unavailable -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Locating…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is LocationState.Available -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Showing distances from your location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LocationPromptRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Earthquake row
// ---------------------------------------------------------------------------

@Composable
private fun EarthquakeRow(listing: EarthquakeListing, onClick: () -> Unit) {
    val quake = listing.earthquake
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MagnitudeBadge(quake.magnitude)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quake.place,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildSupportingText(quake.occurredAt, quake.depthKm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (quake.tsunamiWarning) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Tsunami warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        listing.distanceKm?.let { km ->
            val bearingLabel = listing.bearing?.label?.let { " $it" } ?: ""
            Text(
                text = "${numberFormat.format(km.toLong())} km$bearingLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 52.dp, top = 4.dp),
            )
        }
    }
}

private fun buildSupportingText(occurredAt: Instant, depthKm: Double): String {
    val relativeTime = formatRelativeTime(occurredAt)
    val depthText = when {
        depthKm < 0 -> "${"%.1f".format(-depthKm)} km above sea level"
        else -> "${depthKm.toLong()} km deep"
    }
    return "$relativeTime · $depthText"
}

// ---------------------------------------------------------------------------
// Shimmer skeleton
// ---------------------------------------------------------------------------

@Composable
private fun ShimmerList() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Column(modifier = Modifier.padding(top = 8.dp)) {
        repeat(8) {
            ShimmerRow(shimmerColor)
            if (it < 7) HorizontalDivider()
        }
    }
}

@Composable
private fun ShimmerRow(color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(
    sortOrder: SortOrder,
    filter: EarthquakeFilter,
    onSortChange: (SortOrder) -> Unit,
    onFilterChange: (EarthquakeFilter) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text("Quakes") },
            actions = {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.displayName(),
                                    color = if (order == sortOrder) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                onSortChange(order)
                                showSortMenu = false
                            },
                        )
                    }
                }
            },
        )
        MagnitudeFilterChips(filter = filter, onFilterChange = onFilterChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MagnitudeFilterChips(
    filter: EarthquakeFilter,
    onFilterChange: (EarthquakeFilter) -> Unit,
) {
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

private fun SortOrder.displayName(): String = when (this) {
    SortOrder.NEAREST -> "Nearest"
    SortOrder.MOST_RECENT -> "Most recent"
    SortOrder.LARGEST -> "Largest"
}

private fun formatRelativeTime(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)
    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toHours() < 1 -> "${duration.toMinutes()}m ago"
        duration.toDays() < 1 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        else -> DateTimeFormatter.ofPattern("MMM d")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}
