package com.brick.earthquaketracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EarthquakeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class QuakesDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao
}
