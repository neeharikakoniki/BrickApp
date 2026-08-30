package com.brick.earthquaketracker.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object List : Route
    @Serializable data object Map : Route
    @Serializable data class Detail(val eventId: String) : Route
}
