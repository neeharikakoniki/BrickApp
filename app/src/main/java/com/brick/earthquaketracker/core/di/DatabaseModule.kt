package com.brick.earthquaketracker.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.brick.earthquaketracker.data.local.EarthquakeDao
import com.brick.earthquaketracker.data.local.QuakesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuakesDatabase =
        Room.databaseBuilder(context, QuakesDatabase::class.java, "quakes.db")
            .build()

    @Provides
    fun provideEarthquakeDao(database: QuakesDatabase): EarthquakeDao =
        database.earthquakeDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("sync_metadata")
        }
}
