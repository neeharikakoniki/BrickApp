package com.brick.earthquaketracker.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.SortOrder
import com.brick.earthquaketracker.ui.components.EmptyState
import com.brick.earthquaketracker.ui.components.MagnitudeBadge
import com.brick.earthquaketracker.ui.components.StaleDataBanner
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
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
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (state.locationState is LocationState.PermissionNotRequested) {
                                item(key = "location_prompt") {
                                    LocationPromptRow(
                                        onEnable = onRequestLocationPermission,
                                    )
                                }
                            }

                            if (state.isFiltered) {
                                item(key = "result_count") {
                                    ResultCountRow(
                                        showing = state.earthquakes.size,
                                        total = state.totalCount,
                                    )
                                }
                            }

                            items(
                                items = state.earthquakes,
                                key = { it.earthquake.id },
                            ) { listing ->
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

@Composable
private fun LocationPromptRow(onEnable: () -> Unit) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (dismissed) return

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "See how far these are from you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEnable) {
                Text("Enable")
            }
            TextButton(onClick = { dismissed = true }) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun ResultCountRow(showing: Int, total: Int) {
    Text(
        text = "Showing $showing of $total events",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun EarthquakeRow(listing: EarthquakeListing, onClick: () -> Unit) {
    val quake = listing.earthquake

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { MagnitudeBadge(quake.magnitude) },
        headlineContent = {
            Text(
                text = quake.place,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row {
                Text(
                    text = formatRelativeTime(quake.occurredAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listing.distanceKm?.let { km ->
                    Text(
                        text = " · %.0f km %s".format(km, listing.bearing?.label ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            if (quake.tsunamiWarning) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Tsunami warning",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

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
