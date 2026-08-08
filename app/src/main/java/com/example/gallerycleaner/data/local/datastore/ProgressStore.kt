package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gallery_cleaner_progress")

class ProgressStore(private val context: Context) {

    private fun keyFor(groupKey: String) = intPreferencesKey("progress_$groupKey")

    /** Emits the last saved index (how many items have been swiped through) for a group. */
    fun progressFlow(groupKey: String): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[keyFor(groupKey)] ?: 0 }

    suspend fun saveProgress(groupKey: String, index: Int) {
        context.dataStore.edit { prefs -> prefs[keyFor(groupKey)] = index }
    }

    suspend fun resetProgress(groupKey: String) {
        context.dataStore.edit { prefs -> prefs.remove(keyFor(groupKey)) }
    }
}
