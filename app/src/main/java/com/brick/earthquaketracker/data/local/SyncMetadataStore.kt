package com.brick.earthquaketracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores lightweight sync metadata and UI preferences in DataStore Preferences.
 * Injecting [DataStore] directly keeps this class testable — tests provide a
 * DataStore backed by a temp file.
 */
@Singleton
class SyncMetadataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val lastSyncAt: Flow<Long?> = dataStore.data.map { it[LAST_SYNC_AT] }

    suspend fun setLastSyncAt(millis: Long) {
        dataStore.edit { it[LAST_SYNC_AT] = millis }
    }

    val locationPromptDismissed: Flow<Boolean> =
        dataStore.data.map { it[LOCATION_PROMPT_DISMISSED] ?: false }

    suspend fun setLocationPromptDismissed(dismissed: Boolean) {
        dataStore.edit { it[LOCATION_PROMPT_DISMISSED] = dismissed }
    }

    private companion object {
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LOCATION_PROMPT_DISMISSED = booleanPreferencesKey("location_prompt_dismissed")
    }
}
