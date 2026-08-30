package com.brick.earthquaketracker.core.di

import android.content.Context
import com.brick.earthquaketracker.data.location.FusedLocationDataSource
import com.brick.earthquaketracker.data.location.LocationDataSource
import com.brick.earthquaketracker.data.repository.DefaultLocationRepository
import com.brick.earthquaketracker.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    abstract fun bindLocationDataSource(
        impl: FusedLocationDataSource,
    ): LocationDataSource

    @Binds
    abstract fun bindLocationRepository(
        impl: DefaultLocationRepository,
    ): LocationRepository

    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationClient(
            @ApplicationContext context: Context,
        ): FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
    }
}
