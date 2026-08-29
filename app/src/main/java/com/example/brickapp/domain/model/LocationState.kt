package com.example.brickapp.domain.model

/**
 * Four distinct states for location availability. Using a sealed interface rather than
 * a nullable [UserLocation] lets the UI distinguish "not asked yet" from "denied" from
 * "granted but no GPS fix" — each gets different copy and affordances.
 */
sealed interface LocationState {
    /** Permission has not been requested yet. */
    data object PermissionNotRequested : LocationState

    /** User denied location permission. */
    data object PermissionDenied : LocationState

    /** Permission granted, but the device has no location fix (e.g. cold GPS). */
    data object Unavailable : LocationState

    /** Permission granted and a location is available. */
    data class Available(val location: UserLocation) : LocationState
}
