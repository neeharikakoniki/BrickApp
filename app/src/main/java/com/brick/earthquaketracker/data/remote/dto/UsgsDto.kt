package com.brick.earthquaketracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureCollectionDto(
    val type: String,
    val features: List<FeatureDto>,
)

@Serializable
data class FeatureDto(
    val type: String,
    val properties: PropertiesDto,
    val geometry: GeometryDto,
    val id: String,
)

@Serializable
data class PropertiesDto(
    val mag: Double? = null,
    val place: String? = null,
    val time: Long,
    val updated: Long,
    val url: String,
    val detail: String? = null,
    val status: String? = null,
    val tsunami: Int = 0,
    val sig: Int = 0,
    val type: String = "earthquake",
    val title: String? = null,
    val alert: String? = null,
    @SerialName("magType") val magType: String? = null,
)

@Serializable
data class GeometryDto(
    val type: String,
    val coordinates: List<Double>,
)
