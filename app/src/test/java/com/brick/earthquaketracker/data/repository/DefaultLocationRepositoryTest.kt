package com.brick.earthquaketracker.data.repository

import com.brick.earthquaketracker.data.location.LocationDataSource
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.LocationState
import com.brick.earthquaketracker.domain.model.UserLocation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class DefaultLocationRepositoryTest {

    private var fakeLocation: UserLocation? = null

    private val fakeDataSource = object : LocationDataSource {
        override suspend fun currentLocation(): UserLocation? = fakeLocation
    }

    private val repository = DefaultLocationRepository(fakeDataSource)

    @Test
    fun `initial state is PermissionNotRequested`() = runTest {
        assertThat(repository.observeLocationState().first())
            .isEqualTo(LocationState.PermissionNotRequested)
    }

    @Test
    fun `onPermissionResult denied transitions to PermissionDenied`() = runTest {
        repository.onPermissionResult(granted = false)

        assertThat(repository.observeLocationState().first())
            .isEqualTo(LocationState.PermissionDenied)
    }

    @Test
    fun `onPermissionResult granted transitions to Unavailable`() = runTest {
        repository.onPermissionResult(granted = true)

        assertThat(repository.observeLocationState().first())
            .isEqualTo(LocationState.Unavailable)
    }

    @Test
    fun `refreshLocation after grant with fix transitions to Available`() = runTest {
        val location = UserLocation(
            coordinates = Coordinates(40.71, -74.01),
            capturedAt = Instant.parse("2024-01-15T12:00:00Z"),
        )
        fakeLocation = location

        repository.onPermissionResult(granted = true)
        repository.refreshLocation()

        val state = repository.observeLocationState().first()
        assertThat(state).isInstanceOf(LocationState.Available::class.java)
        assertThat((state as LocationState.Available).location).isEqualTo(location)
    }

    @Test
    fun `refreshLocation after grant with no fix stays Unavailable`() = runTest {
        fakeLocation = null

        repository.onPermissionResult(granted = true)
        repository.refreshLocation()

        assertThat(repository.observeLocationState().first())
            .isEqualTo(LocationState.Unavailable)
    }

    @Test
    fun `refreshLocation without permission does nothing`() = runTest {
        fakeLocation = UserLocation(
            coordinates = Coordinates(40.71, -74.01),
            capturedAt = Instant.parse("2024-01-15T12:00:00Z"),
        )

        // Never granted permission — refreshLocation should be a no-op
        repository.refreshLocation()

        assertThat(repository.observeLocationState().first())
            .isEqualTo(LocationState.PermissionNotRequested)
    }

    @Test
    fun `refreshLocation with denied permission does nothing`() = runTest {
        fakeLocation = UserLocation(
            coordinates = Coordinates(40.71, -74.01),
            capturedAt = Instant.parse("2024-01-15T12:00:00Z"),
        )

        repository.onPermissionResult(granted = false)
        repository.refreshLocation()

        assertThat(repository.observeLocationState().first())
            .isEqualTo(LocationState.PermissionDenied)
    }
}
