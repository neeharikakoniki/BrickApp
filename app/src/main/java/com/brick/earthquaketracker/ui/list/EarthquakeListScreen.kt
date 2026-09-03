package com.brick.earthquaketracker.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EarthquakeListScreen(
    state: ListUiState,
    onQuakeClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onFilterChange: (EarthquakeFilter) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
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
                searchQuery = state.searchQuery,
                onSortChange = onSortChange,
                onFilterChange = onFilterChange,
                onSearchQueryChange = onSearchQueryChange,
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
                    val grouped = remember(state.earthquakes) {
                        groupByTimePeriod(state.earthquakes)
                    }

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = 12.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
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

                            item(key = "stats_banner") {
                                QuickStatsBanner(
                                    earthquakes = state.earthquakes,
                                    totalCount = state.totalCount,
                                    isFiltered = state.isFiltered,
                                )
                            }

                            grouped.forEach { (period, listings) ->
                                stickyHeader(key = "header_${period.name}") {
                                    TimeGroupHeader(period)
                                }
                                items(
                                    items = listings,
                                    key = { it.earthquake.id },
                                ) { listing ->
                                    EarthquakeCard(
                                        listing = listing,
                                        onClick = { onQuakeClick(listing.earthquake.id) },
                                        modifier = Modifier.animateItem(),
                                    )
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
// Quick-stats banner
// ---------------------------------------------------------------------------

@Composable
private fun QuickStatsBanner(
    earthquakes: List<EarthquakeListing>,
    totalCount: Int,
    isFiltered: Boolean,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    val countLabel = if (isFiltered) {
        "${numberFormat.format(earthquakes.size)} of ${numberFormat.format(totalCount)}"
    } else {
        numberFormat.format(earthquakes.size)
    }

    val lastDayCount = remember(earthquakes) {
        val oneDayAgo = Instant.now().minus(Duration.ofDays(1))
        earthquakes.count { it.earthquake.occurredAt.isAfter(oneDayAgo) }
    }

    val largest = remember(earthquakes) {
        earthquakes.maxByOrNull { it.earthquake.magnitude ?: 0.0 }
    }

    val nearest = remember(earthquakes) {
        earthquakes.filter { it.distanceKm != null }.minByOrNull { it.distanceKm!! }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatPill(
            label = "Events",
            value = countLabel,
            modifier = Modifier.weight(1f),
        )
        StatPill(
            label = "24h",
            value = numberFormat.format(lastDayCount),
            modifier = Modifier.weight(1f),
        )
        largest?.earthquake?.magnitude?.let { mag ->
            StatPill(
                label = "Largest",
                value = "M${"%.1f".format(mag)}",
                modifier = Modifier.weight(1f),
            )
        }
        nearest?.distanceKm?.let { km ->
            StatPill(
                label = "Nearest",
                value = "${numberFormat.format(km.toLong())} km",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
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
// Earthquake card
// ---------------------------------------------------------------------------

@Composable
private fun EarthquakeCard(
    listing: EarthquakeListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quake = listing.earthquake
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isRecent = Duration.between(quake.occurredAt, Instant.now()).toHours() < 1
                MagnitudeBadge(
                    magnitude = quake.magnitude,
                    isRecent = isRecent,
                    sharedElementKey = "magnitude_badge_${quake.id}",
                )
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
// Time-grouped sections
// ---------------------------------------------------------------------------

private enum class TimePeriod(val label: String) {
    LAST_HOUR("Last hour"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This week"),
    OLDER("Older"),
}

private fun classifyTimePeriod(occurredAt: Instant): TimePeriod {
    val now = Instant.now()
    val duration = Duration.between(occurredAt, now)
    return when {
        duration.toHours() < 1 -> TimePeriod.LAST_HOUR
        duration.toHours() < 24 -> TimePeriod.TODAY
        duration.toHours() < 48 -> TimePeriod.YESTERDAY
        duration.toDays() < 7 -> TimePeriod.THIS_WEEK
        else -> TimePeriod.OLDER
    }
}

private fun groupByTimePeriod(
    listings: List<EarthquakeListing>,
): List<Pair<TimePeriod, List<EarthquakeListing>>> {
    val grouped = listings.groupBy { classifyTimePeriod(it.earthquake.occurredAt) }
    return TimePeriod.entries.mapNotNull { period ->
        grouped[period]?.let { period to it }
    }
}

@Composable
private fun TimeGroupHeader(period: TimePeriod) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Text(
            text = period.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
        )
    }
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

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(8) {
            ShimmerCard(shimmerColor)
        }
    }
}

@Composable
private fun ShimmerCard(color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(
    sortOrder: SortOrder,
    filter: EarthquakeFilter,
    searchQuery: String,
    onSortChange: (SortOrder) -> Unit,
    onFilterChange: (EarthquakeFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) focusRequester.requestFocus()
    }

    Column {
        TopAppBar(
            title = {
                AnimatedVisibility(
                    visible = searchExpanded,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by location…") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
                if (!searchExpanded) {
                    Text("Earthquake Tracker")
                }
            },
            navigationIcon = {
                if (searchExpanded) {
                    IconButton(onClick = {
                        searchExpanded = false
                        onSearchQueryChange("")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                    }
                }
            },
            actions = {
                if (!searchExpanded) {
                    IconButton(onClick = { searchExpanded = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
                if (!searchExpanded) {
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
                }
            },
        )
        if (!searchExpanded) {
            MagnitudeFilterChips(filter = filter, onFilterChange = onFilterChange)
        }
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
