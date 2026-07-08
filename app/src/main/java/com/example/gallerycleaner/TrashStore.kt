package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trashDataStore by preferencesDataStore(name = "gallery_cleaner_trash")
private val TRASHED_IDS_KEY = stringSetPreferencesKey("trashed_media_ids")

/**
 * Tracks which media items the user has swiped to delete, without actually
 * deleting them from the device yet. Items sit here until the user either
 * restores them or empties the trash (which is when the real, permanent
 * MediaStore delete request happens).
 */
class TrashStore(private val context: Context) {

    val trashedIdsFlow: Flow<Set<Long>> =
        context.trashDataStore.data.map { prefs ->
            (prefs[TRASHED_IDS_KEY] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
        }

    suspend fun addToTrash(ids: List<Long>) {
        if (ids.isEmpty()) return
        context.trashDataStore.edit { prefs ->
            val current = prefs[TRASHED_IDS_KEY] ?: emptySet()
            prefs[TRASHED_IDS_KEY] = current + ids.map { it.toString() }
        }
    }

    /** Used both for "restore" (item goes back to the active gallery) and for
     *  cleanup after a permanent delete actually succeeds. */
    suspend fun remove(ids: List<Long>) {
        if (ids.isEmpty()) return
        context.trashDataStore.edit { prefs ->
            val current = prefs[TRASHED_IDS_KEY] ?: emptySet()
            prefs[TRASHED_IDS_KEY] = current - ids.map { it.toString() }.toSet()
        }
    }
}
