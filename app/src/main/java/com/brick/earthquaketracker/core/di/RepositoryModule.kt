package com.brick.earthquaketracker.core.di

import com.brick.earthquaketracker.data.repository.OfflineFirstEarthquakeRepository
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindEarthquakeRepository(
        impl: OfflineFirstEarthquakeRepository,
    ): EarthquakeRepository
}
