package com.brick.earthquaketracker.domain.model


sealed interface LocationState {
    /** Permission has not been requested yet. */
    data object PermissionNotRequested : LocationState

    /** User denied location permission but can be asked again. */
    data object PermissionDenied : LocationState

    /** User permanently denied location permission ("Don't ask again"). */
    data object PermanentlyDenied : LocationState

    /** Permission granted, but the device has no location fix (e.g. cold GPS). */
    data object Unavailable : LocationState

    /** Permission granted and a location is available. */
    data class Available(val location: UserLocation) : LocationState
}
