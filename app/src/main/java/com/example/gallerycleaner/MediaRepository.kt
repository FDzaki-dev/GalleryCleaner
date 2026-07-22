package com.example.gallerycleaner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MediaRepository {

    private const val PAGE_SIZE = 400

    /** Loads all images (photos + GIFs) visible to the app from MediaStore, in
     *  one go. Kept for callers that genuinely need the full list at once;
     *  prefer [loadMediaProgressively] for populating the UI, since that
     *  surfaces the first page immediately instead of blocking until every
     *  row in a large gallery has been read. */
    fun loadAllMedia(context: Context): List<MediaItem> {
        val all = mutableListOf<MediaItem>()
        var offset = 0
        while (true) {
            val page = queryMediaPage(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, PAGE_SIZE, offset)
            if (page.isEmpty()) break
            all += page
            offset += page.size
            if (page.size < PAGE_SIZE) break
        }
        return all
    }

    /**
     * Streams the gallery in pages of [PAGE_SIZE] instead of reading every
     * row in one query. On a library with tens of thousands of photos this
     * is what lets the home screen paint its first groups almost instantly
     * — each emission is one page, and the caller decides how to accumulate
     * them (see MainActivity, which appends each page to its media list as
     * it arrives). The underlying MediaStore query is still sorted by
     * DATE_ADDED DESC throughout, so pages arrive in a stable, non-repeating
     * order and later pages never reshuffle groups already shown.
     */
    fun loadMediaProgressively(context: Context): Flow<List<MediaItem>> = flow {
        var offset = 0
        while (true) {
            val page = queryMediaPage(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, PAGE_SIZE, offset)
            if (page.isEmpty()) break
            emit(page)
            offset += page.size
            if (page.size < PAGE_SIZE) break
        }
    }

    private fun queryMediaPage(context: Context, collection: Uri, limit: Int, offset: Int): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val useRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val pathColumnName = if (useRelativePath) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.MediaColumns.DATA // deprecated but still the only path column pre-Android 10
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            pathColumnName
        )

        // ContentResolver's structured LIMIT/OFFSET query args only exist from
        // API 30 onward; below that, MediaStore (backed by SQLite) still
        // honors LIMIT/OFFSET appended directly to the sort order string —
        // a long-standing, widely-used trick for paging this provider.
        val cursor = if (Build.VERSION.SDK_INT >= 30) {
            val queryArgs = Bundle().apply {
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.DATE_ADDED)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            context.contentResolver.query(collection, projection, queryArgs, null)
        } else {
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
            context.contentResolver.query(collection, projection, null, null, sortOrder)
        }

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateModifiedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val widthCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val pathCol = it.getColumnIndexOrThrow(pathColumnName)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val dateTaken = it.getLong(dateTakenCol).let { taken ->
                    if (taken > 0) taken else it.getLong(dateAddedCol) * 1000L
                }
                val rawPath = it.getString(pathCol) ?: ""
                // On pre-Q devices this column holds the full absolute file path
                // instead of a relative folder — trim it down to just the folder.
                val folderPath = if (useRelativePath) {
                    rawPath
                } else {
                    rawPath.substringBeforeLast('/', "").substringAfter("/storage/emulated/0/", "")
                }
                result += MediaItem(
                    id = id,
                    uri = uri,
                    displayName = it.getString(nameCol) ?: "unknown",
                    dateTakenMillis = dateTaken,
                    dateModifiedMillis = it.getLong(dateModifiedCol) * 1000L,
                    sizeBytes = it.getLong(sizeCol),
                    bucketName = it.getString(bucketCol) ?: "Unknown album",
                    width = it.getInt(widthCol),
                    height = it.getInt(heightCol),
                    relativePath = folderPath
                )
            }
        }
        return result
    }

    /** Groups items either by "Month Year" or by album (bucket) name. */
    fun group(items: List<MediaItem>, mode: GroupMode, sort: SortOption): List<MediaGroup> {
        val sorted = sortItems(items, sort)
        val grouped = when (mode) {
            GroupMode.MONTH -> sorted.groupBy { monthKey(it.dateTakenMillis) }
            GroupMode.ALBUM -> sorted.groupBy { it.bucketName }
        }
        // Preserve a stable, recency-first ordering of group keys.
        //
        // MONTH used to re-scan the *entire* sorted list once per key just
        // to find a representative dateTakenMillis for that key
        // (`sorted.first { monthKey(it) == key }`) — O(keys × items). On a
        // large library (tens of thousands of photos across a couple years
        // of months) that's a lot of redundant passes over data already in
        // hand. `grouped` already has every item bucketed by key, so taking
        // the max dateTakenMillis per bucket is the same O(items) pass this
        // function is already doing, just reading from the map instead of
        // re-scanning the source list.
        val orderedKeys = when (mode) {
            GroupMode.MONTH -> grouped.keys.sortedByDescending { key ->
                grouped.getValue(key).maxOf { it.dateTakenMillis }
            }
            GroupMode.ALBUM -> grouped.keys.sorted()
        }
        return orderedKeys.map { key -> MediaGroup(key, grouped.getValue(key)) }
    }

    private fun sortItems(items: List<MediaItem>, sort: SortOption): List<MediaItem> {
        return when (sort) {
            SortOption.DATE -> items.sortedByDescending { it.dateTakenMillis }
            SortOption.SIZE -> items.sortedByDescending { it.sizeBytes }
            // Locale.getDefault() hoisted out of the lambda — sortedBy calls
            // its selector once per element, so leaving the call inline
            // meant re-resolving the default locale on every single item
            // instead of once for the whole sort.
            SortOption.NAME -> {
                val locale = Locale.getDefault()
                items.sortedBy { it.displayName.lowercase(locale) }
            }
        }
    }

    private const val LARGE_FILE_THRESHOLD_BYTES = 10L * 1024 * 1024 // 10 MB

    /** "Quick Clean" shortcuts — common starting points other cleaner apps
     *  surface, computed cheaply from data already loaded in memory. */
    fun smartCategories(items: List<MediaItem>): List<MediaGroup> {
        val result = mutableListOf<MediaGroup>()

        val screenshots = items.filter { it.bucketName.contains("screenshot", ignoreCase = true) }
        if (screenshots.isNotEmpty()) {
            result += MediaGroup("Screenshots", screenshots.sortedByDescending { it.dateTakenMillis })
        }

        val large = items.filter { it.sizeBytes >= LARGE_FILE_THRESHOLD_BYTES }
        if (large.isNotEmpty()) {
            result += MediaGroup("Large files (10MB+)", large.sortedByDescending { it.sizeBytes })
        }

        // Exact-duplicate detection now lives in findExactDuplicates() below —
        // it needs file I/O (content hashing) so it can't be part of this
        // synchronous, in-memory-only function. Callers should merge its
        // result in alongside this list (see MainActivity).

        return result
    }

    /**
     * Finds true duplicate files by content hash — not just matching file
     * size. Same size was the old heuristic; it's cheap but wrong in both
     * directions (two unrelated photos can coincidentally share a size, and
     * it says nothing about whether the *content* actually matches). This
     * confirms duplicates properly:
     *
     * 1. Group by file size first (unchanged photos that are true byte-for-
     *    byte copies always share a size, so this is a free, lossless
     *    pre-filter — it's what keeps this fast, since only items that
     *    collide on size ever get hashed at all).
     * 2. Within each size-collision group, hash the actual file contents
     *    (MD5 — collision-resistant enough for this, and faster than
     *    SHA-256, since this isn't a security context) and group by hash.
     * 3. Only hash-groups with more than one item are real duplicates.
     *
     * Does file I/O, so this must run off the main thread (Dispatchers.IO).
     */
    suspend fun findExactDuplicates(context: Context, items: List<MediaItem>): List<MediaItem> {
        val sizeCandidates = items
            .filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes }
            .values
            .filter { it.size > 1 }
        if (sizeCandidates.isEmpty()) return emptyList()

        val result = mutableListOf<MediaItem>()
        for (candidates in sizeCandidates) {
            candidates
                .mapNotNull { item -> hashContent(context, item.uri)?.let { hash -> hash to item } }
                .groupBy({ it.first }, { it.second })
                .values
                .filter { it.size > 1 }
                .forEach { result += it }
        }
        return result.sortedByDescending { it.sizeBytes }
    }

    private const val HASH_BUFFER_SIZE = 8192

    /** Streams the file through MD5 in fixed-size chunks rather than loading
     *  it fully into memory — keeps peak memory flat regardless of how large
     *  an individual photo is. Returns null (treated as "can't confirm a
     *  duplicate") if the file can't be read, e.g. it was deleted mid-scan. */
    private fun hashContent(context: Context, uri: Uri): String? = try {
        val digest = java.security.MessageDigest.getInstance("MD5")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(HASH_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }

    // SimpleDateFormat is stateful and explicitly NOT thread-safe (it mutates
    // an internal Calendar while formatting) — a plain shared instance here
    // was a real hazard, not just a style nit: `group()` runs on
    // Dispatchers.Default (a thread pool), and MainActivity's LaunchedEffect
    // re-invokes it on every single page emitted during progressive gallery
    // loading. When a new page arrives before the previous group() call's
    // coroutine has actually stopped running (cancellation is cooperative —
    // it doesn't preempt code mid-execution), two invocations calling
    // monthFormat.format(...) concurrently on different pool threads could
    // corrupt each other's in-flight formatting, producing garbled month
    // labels or occasionally throwing. ThreadLocal gives each pool thread
    // its own SimpleDateFormat instance instead of sharing one — no
    // synchronization needed, and no risk of threads stepping on each other.
    private val monthFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    private fun monthKey(millis: Long): String = monthFormat.get().format(millis)
}
