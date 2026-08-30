package com.brick.earthquaketracker.data.location

import android.annotation.SuppressLint
import android.location.Location
import com.brick.earthquaketracker.domain.model.Coordinates
import com.brick.earthquaketracker.domain.model.UserLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import javax.inject.Inject

/**
 * Production [LocationDataSource] backed by Play Services' FusedLocationProviderClient.
 *
 * Uses [getCurrentLocation][FusedLocationProviderClient.getCurrentLocation] rather than
 * `lastLocation` — `lastLocation` returns null on a device that hasn't recently fixed
 * a position (fresh emulator, cold GPS), which produces confusing "it works on my phone" bugs.
 */
class FusedLocationDataSource @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
    private val clock: Clock,
) : LocationDataSource {

    @SuppressLint("MissingPermission") // Caller is responsible for checking permission
    override suspend fun currentLocation(): UserLocation? =
        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            val cancellation = CancellationTokenSource()
            try {
                val location: Location? = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellation.token,
                ).await()
                location?.toUserLocation()
            } finally {
                cancellation.cancel()
            }
        }

    private fun Location.toUserLocation() = UserLocation(
        coordinates = Coordinates(
            latitude = latitude,
            longitude = longitude,
        ),
        capturedAt = clock.instant(),
    )

    private companion object {
        const val LOCATION_TIMEOUT_MS = 5_000L
    }
}
