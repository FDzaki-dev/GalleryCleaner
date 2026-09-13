package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.statsDataStore by preferencesDataStore(name = "gallery_cleaner_stats")
private val TOTAL_FREED_BYTES_KEY = longPreferencesKey("total_freed_bytes")
private val TOTAL_DELETED_COUNT_KEY = intPreferencesKey("total_deleted_count")

/** Tracks all-time cleanup stats across sessions — the running "space freed"
 *  and "items cleaned" totals shown on the home screen. */
class StatsStore(private val context: Context) {

    val totalFreedBytesFlow: Flow<Long> =
        context.statsDataStore.data.map { it[TOTAL_FREED_BYTES_KEY] ?: 0L }

    val totalDeletedCountFlow: Flow<Int> =
        context.statsDataStore.data.map { it[TOTAL_DELETED_COUNT_KEY] ?: 0 }

    suspend fun recordDeletion(bytesFreed: Long, count: Int) {
        if (count <= 0) return
        context.statsDataStore.edit { prefs ->
            prefs[TOTAL_FREED_BYTES_KEY] = (prefs[TOTAL_FREED_BYTES_KEY] ?: 0L) + bytesFreed
            prefs[TOTAL_DELETED_COUNT_KEY] = (prefs[TOTAL_DELETED_COUNT_KEY] ?: 0) + count
        }
    }
}
