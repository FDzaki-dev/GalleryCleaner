package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.hashCacheDataStore by preferencesDataStore(name = "gallery_cleaner_hash_cache")
private val HASH_CACHE_KEY = stringPreferencesKey("exact_dup_hash_cache_v1")

/**
 * Persistent cache of MD5 content hashes, so MediaScanner.findExactDuplicates
 * doesn't re-read and re-hash a file it already hashed in a previous scan.
 * Added Batch45 (Audit Gap P1 #6, stage 1 — "belum terlihat: persistent hash
 * cache" was the first of five gaps that finding listed; incremental
 * progress/resume/finer-grained cancellation are separate, larger UI-facing
 * changes and are queued, not part of this stage — see PROJECT_STATE).
 *
 * Whole-cache read/write, not one DataStore key per media item: [loadAll] is
 * called once at the start of a scan and [saveAll] once at the end, never
 * per item. DataStore's edit{} rewrites its entire backing file on every
 * call, so a per-item write on a library of thousands would just relocate
 * the exact O(n) repeated-write cost Batch44 fixed for allMedia into this
 * store instead — a single JSON blob under one key avoids that entirely.
 *
 * Cache key is the media id; each entry also carries the size and
 * dateModifiedMillis that were true *when it was hashed*. A cache hit only
 * counts if both still match the item's current values — that's what
 * makes this safe against a file being edited/replaced in place (same id,
 * new content): a changed size or mtime forces a real re-hash rather than
 * silently trusting stale data.
 */
class HashCacheStore(private val context: Context) {

    data class Entry(val sizeBytes: Long, val dateModifiedMillis: Long, val hash: String)

    /** One-shot read of the whole cache, keyed by media id. A corrupt or
     *  unreadable blob (e.g. hand-edited prefs file, future schema change)
     *  is treated as a cold cache rather than a crash — worst case is a
     *  slower scan, never a broken one. */
    suspend fun loadAll(): Map<Long, Entry> {
        val raw = context.hashCacheDataStore.data.first()[HASH_CACHE_KEY] ?: return emptyMap()
        return try {
            val array = JSONArray(raw)
            val map = HashMap<Long, Entry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                map[obj.getLong("id")] = Entry(
                    sizeBytes = obj.getLong("size"),
                    dateModifiedMillis = obj.getLong("mod"),
                    hash = obj.getString("hash")
                )
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** One-shot write of the whole cache. Callers should pass the complete
     *  replacement map (cache hits carried forward + fresh hashes merged
     *  in) — whatever isn't included here is dropped, which is what prunes
     *  entries for items that no longer exist or no longer collide on size
     *  (see findExactDuplicates: only size-candidates are ever hashed, so
     *  only they ever belong in this cache). */
    suspend fun saveAll(entries: Map<Long, Entry>) {
        val array = JSONArray()
        entries.forEach { (id, entry) ->
            array.put(
                JSONObject().apply {
                    put("id", id)
                    put("size", entry.sizeBytes)
                    put("mod", entry.dateModifiedMillis)
                    put("hash", entry.hash)
                }
            )
        }
        context.hashCacheDataStore.edit { prefs -> prefs[HASH_CACHE_KEY] = array.toString() }
    }
}
