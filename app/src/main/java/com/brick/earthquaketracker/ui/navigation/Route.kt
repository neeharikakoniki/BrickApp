package com.brick.earthquaketracker.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object List : Route
    @Serializable data class Map(val focusEventId: String? = null) : Route
    @Serializable data class Detail(val eventId: String) : Route
}
