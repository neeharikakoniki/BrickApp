package com.brick.earthquaketracker.data.remote

import com.brick.earthquaketracker.data.remote.dto.FeatureCollectionDto
import retrofit2.http.GET

interface UsgsApi {

    @GET("earthquakes/feed/v1.0/summary/2.5_week.geojson")
    suspend fun getWeeklySummary(): FeatureCollectionDto
}
