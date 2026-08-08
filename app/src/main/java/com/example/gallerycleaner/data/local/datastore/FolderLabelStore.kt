package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.folderLabelDataStore by preferencesDataStore(name = "gallery_cleaner_folder_labels")

private const val LABEL_KEY_PREFIX = "label_"

/**
 * Lets the user give a folder/group a custom display name inside this app,
 * independent of whatever the device's own Gallery app calls it.
 *
 * Some OEM gallery apps (confirmed on Infinix/XOS, and common on other
 * skins too) let you set a "custom folder name" that's purely a cosmetic
 * alias stored in that app's own private database — it never actually
 * renames the underlying folder. Android's MediaStore only ever reports the
 * real, physical folder name via BUCKET_DISPLAY_NAME, so no third-party app
 * (this one included) can see that alias; there's no public API for it.
 * This store is the practical workaround: a rename that's guaranteed to
 * work because it lives entirely in our own data, not something dependent
 * on another app's private database.
 */
class FolderLabelStore(private val context: Context) {

    private fun keyFor(groupKey: String) = stringPreferencesKey("$LABEL_KEY_PREFIX$groupKey")

    /** Custom label for one folder, or null if the user hasn't set one. */
    fun labelFlow(groupKey: String): Flow<String?> =
        context.folderLabelDataStore.data.map { prefs -> prefs[keyFor(groupKey)] }

    /** Every custom label at once, keyed by group key — lets a whole list of
     *  folders render with one Flow collection instead of one per row. */
    val allLabelsFlow: Flow<Map<String, String>> =
        context.folderLabelDataStore.data.map { prefs ->
            prefs.asMap()
                .entries
                .filter { (key, _) -> key.name.startsWith(LABEL_KEY_PREFIX) }
                .associate { (key, value) -> key.name.removePrefix(LABEL_KEY_PREFIX) to value as String }
        }

    /** Setting an empty/blank label clears it — same result as [clearLabel],
     *  just convenient when both come from the same "Save" button. */
    suspend fun setLabel(groupKey: String, label: String) {
        val trimmed = label.trim()
        context.folderLabelDataStore.edit { prefs ->
            if (trimmed.isEmpty()) prefs.remove(keyFor(groupKey)) else prefs[keyFor(groupKey)] = trimmed
        }
    }

    suspend fun clearLabel(groupKey: String) {
        context.folderLabelDataStore.edit { prefs -> prefs.remove(keyFor(groupKey)) }
    }
}
