package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.trashDataStore by preferencesDataStore(name = "gallery_cleaner_trash")

// Legacy key from before expiry tracking existed — id-only, no timestamp.
// Kept only so existing installs can be migrated the first time they're read.
private val LEGACY_TRASHED_IDS_KEY = stringSetPreferencesKey("trashed_media_ids")

// Current format: one entry per trashed item stored as "id:trashedAtEpochMillis",
// so auto-expiry can tell how long each item has actually been sitting here.
private val TRASHED_ITEMS_KEY = stringSetPreferencesKey("trashed_media_items")

/** One item currently sitting in the trash, and when it landed there. */
data class TrashedItem(val id: Long, val trashedAtMillis: Long)

/**
 * Tracks which media items the user has swiped to delete, without actually
 * deleting them from the device yet. Items sit here until the user either
 * restores them, empties the trash manually, or the item crosses the
 * auto-expiry age (see [EXPIRY_DAYS]) and gets surfaced for cleanup.
 *
 * Android's scoped storage requires an interactive system confirmation for
 * every permanent delete (there's no silent background-delete API), so
 * "auto-expiry" here means: items past the threshold are flagged via
 * [expiredItemIdsFlow] so the UI can prompt the user to confirm the delete
 * with one tap, rather than the item quietly sitting in trash forever.
 *
 * Audit Gap P0 #2 (Batch42): "Trash" here is this app's own bookkeeping —
 * an id+timestamp review queue in DataStore — not a move into MediaStore's
 * real OS-level trash (`MediaStore.createTrashRequest()`, API 30+, which
 * flags a file's `IS_TRASHED` column so it's hidden from normal queries and
 * eligible for the *system's* recovery/cleanup, without this app's local
 * bookkeeping in the loop at all). This is a deliberate choice, not an
 * oversight: it's the same "Recently Deleted" pattern most gallery/photo
 * apps use (soft-delete + countdown + restore, all app-managed) — the
 * user-facing copy this data feeds (TrashScreen's "Trash (N)"/"Empty
 * Trash"/"Delete permanently", Settings' "flag items in Trash for cleanup
 * after:") already describes exactly this and nothing more, so there's no
 * gap between what's promised and what's implemented. Adopting the real
 * MediaStore trash API instead would be a genuine architecture change, not
 * a small fix: it needs items re-queried from MediaStore with
 * `MATCH_TRASHED` to populate TrashScreen (rather than reading them from
 * here), a separate untrash flow, and a story for items trashed by some
 * *other* app showing up unexpectedly in this one's queue — worth
 * revisiting as its own dedicated batch if OS-level trash integration
 * becomes a real product requirement, not bundled into this one.
 */
class TrashStore(private val context: Context) {

    companion object {
        /** Default before the user has ever opened Settings, and the
         *  fallback value if SettingsStore's Flow hasn't emitted yet. Matches
         *  the common "auto-empty after a month" convention other gallery
         *  cleaners use. The actual retention length is user-configurable
         *  now — see SettingsStore.trashRetentionDaysFlow. */
        const val EXPIRY_DAYS = SettingsStore.DEFAULT_TRASH_RETENTION_DAYS
    }

    val trashedItemsFlow: Flow<List<TrashedItem>> =
        context.trashDataStore.data.map { prefs -> readEntries(prefs).map { (id, at) -> TrashedItem(id, at) } }

    val trashedIdsFlow: Flow<Set<Long>> =
        trashedItemsFlow.map { items -> items.map { it.id }.toSet() }

    /** Items that have been sitting in the trash longer than [retentionDays]. */
    fun expiredItemIdsFlow(retentionDays: Int): Flow<Set<Long>> =
        trashedItemsFlow.map { items ->
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            items.filter { it.trashedAtMillis < cutoff }.map { it.id }.toSet()
        }

    suspend fun addToTrash(ids: List<Long>) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        context.trashDataStore.edit { prefs ->
            val updated = readEntries(prefs) + ids.associateWith { now }
            writeEntries(prefs, updated)
        }
    }

    /** Used both for "restore" (item goes back to the active gallery) and for
     *  cleanup after a permanent delete actually succeeds. */
    suspend fun remove(ids: List<Long>) {
        if (ids.isEmpty()) return
        val removeSet = ids.toSet()
        context.trashDataStore.edit { prefs ->
            val updated = readEntries(prefs).filterKeys { it !in removeSet }
            writeEntries(prefs, updated)
        }
    }

    /** How many whole days remain before an item trashed at [trashedAtMillis]
     *  is flagged for auto-cleanup — for display in the UI (e.g. "3d left"). */
    fun daysUntilExpiry(trashedAtMillis: Long, retentionDays: Int): Int {
        val ageDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - trashedAtMillis).toInt()
        return (retentionDays - ageDays).coerceAtLeast(0)
    }

    /** Reads current entries, transparently migrating the old id-only format
     *  (timestamp-less) by treating "now" as the trash date — restarts the
     *  expiry clock for anything trashed before this feature existed, which
     *  is a reasonable default and avoids surprise deletions right after an
     *  app update. */
    private fun readEntries(prefs: Preferences): Map<Long, Long> {
        val current = prefs[TRASHED_ITEMS_KEY]
        if (current != null) {
            return current.mapNotNull(::parseEntry).associate { it.id to it.trashedAtMillis }
        }
        val now = System.currentTimeMillis()
        return (prefs[LEGACY_TRASHED_IDS_KEY] ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .associateWith { now }
    }

    private fun writeEntries(prefs: androidx.datastore.preferences.core.MutablePreferences, entries: Map<Long, Long>) {
        prefs[TRASHED_ITEMS_KEY] = entries.map { (id, at) -> "$id:$at" }.toSet()
        prefs.remove(LEGACY_TRASHED_IDS_KEY)
    }

    private fun parseEntry(raw: String): TrashedItem? {
        val parts = raw.split(":", limit = 2)
        if (parts.size != 2) return null
        val id = parts[0].toLongOrNull() ?: return null
        val at = parts[1].toLongOrNull() ?: return null
        return TrashedItem(id, at)
    }
}
