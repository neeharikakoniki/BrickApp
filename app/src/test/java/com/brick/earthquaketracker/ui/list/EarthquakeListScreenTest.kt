package com.brick.earthquaketracker.ui.list

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.Earthquake
import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.EarthquakeListing
import com.brick.earthquaketracker.domain.model.LocationState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EarthquakeListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `displays earthquake cards from state`() {
        val listings = listOf(
            testListing("q1", place = "10km NW of Tokyo, Japan", magnitude = 5.2),
            testListing("q2", place = "5km SE of Los Angeles, CA", magnitude = 4.1),
        )
        val state = testState(earthquakes = listings)

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("10km NW of Tokyo, Japan").assertIsDisplayed()
        composeTestRule.onNodeWithText("5km SE of Los Angeles, CA").assertIsDisplayed()
    }

    @Test
    fun `tapping earthquake card invokes onQuakeClick with correct id`() {
        var clickedId: String? = null
        val listings = listOf(
            testListing("quake_42", place = "Near Fiji Islands", magnitude = 6.0),
        )
        val state = testState(earthquakes = listings)

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = { clickedId = it },
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("Near Fiji Islands").performClick()

        assertThat(clickedId).isEqualTo("quake_42")
    }

    @Test
    fun `shows empty state when no results for filter`() {
        val state = testState(
            earthquakes = emptyList(),
            emptyReason = EmptyReason.NO_RESULTS_FOR_FILTER,
        )

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("No matching events").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear filters").assertIsDisplayed()
    }

    @Test
    fun `shows offline empty state with retry button`() {
        val state = testState(
            earthquakes = emptyList(),
            emptyReason = EmptyReason.NO_CACHE_OFFLINE,
        )

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("Unable to load earthquakes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun `shows location prompt when permission not requested`() {
        val state = testState(
            earthquakes = listOf(testListing("q1")),
            locationState = LocationState.PermissionNotRequested,
            locationPromptDismissed = false,
        )

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("See how far these are from you").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable").assertIsDisplayed()
    }

    @Test
    fun `hides location prompt when dismissed`() {
        val state = testState(
            earthquakes = listOf(testListing("q1")),
            locationState = LocationState.PermissionNotRequested,
            locationPromptDismissed = true,
        )

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("See how far these are from you").assertDoesNotExist()
    }

    @Test
    fun `clear filters button invokes onFilterChange with default`() {
        var filterReceived: EarthquakeFilter? = null
        val state = testState(
            earthquakes = emptyList(),
            emptyReason = EmptyReason.NO_RESULTS_FOR_FILTER,
        )

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = { filterReceived = it },
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("Clear filters").performClick()

        assertThat(filterReceived).isEqualTo(EarthquakeFilter.Default)
    }

    @Test
    fun `shows shimmer loading skeleton during initial load`() {
        val state = testState(isInitialLoading = true)

        composeTestRule.setContent {
            EarthquakeListScreen(
                state = state,
                onQuakeClick = {},
                onRefresh = {},
                onFilterChange = {},
                onSortChange = {},
                onSearchQueryChange = {},
                onClearError = {},
                onRequestLocationPermission = {},
                onDismissLocationPrompt = {},
                onOpenAppSettings = {},
            )
        }

        composeTestRule.onNodeWithText("Earthquake Tracker").assertIsDisplayed()
        // Earthquake cards should not be visible during loading
        composeTestRule.onNodeWithText("Test Place").assertDoesNotExist()
    }

    // --- Helpers ---

    private fun testState(
        earthquakes: List<EarthquakeListing> = emptyList(),
        totalCount: Int = earthquakes.size,
        isInitialLoading: Boolean = false,
        isRefreshing: Boolean = false,
        emptyReason: EmptyReason? = null,
        locationState: LocationState = LocationState.PermissionNotRequested,
        locationPromptDismissed: Boolean = true,
    ) = ListUiState(
        earthquakes = earthquakes,
        totalCount = totalCount,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        emptyReason = emptyReason,
        locationState = locationState,
        locationPromptDismissed = locationPromptDismissed,
    )

    private fun testListing(
        id: String = "test_id",
        place: String = "Test Place",
        magnitude: Double = 4.5,
    ) = EarthquakeListing(
        earthquake = Earthquake(
            id = id,
            magnitude = magnitude,
            magnitudeType = "ml",
            place = place,
            occurredAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3500),
            coordinates = Coordinates(latitude = 37.0, longitude = -120.0),
            depthKm = 10.0,
            tsunamiWarning = false,
            significance = 200,
            alertLevel = null,
            detailsUrl = "https://example.com/$id",
            eventType = "earthquake",
        ),
        distanceKm = null,
        bearing = null,
    )
}
