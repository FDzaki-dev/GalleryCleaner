package com.example.gallerycleaner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
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

    /** Photos taken on today's month+day in a past year — a fully-offline
     *  "memories" nudge, no network/API needed since dateTakenMillis is
     *  already in hand for every item. Cheap, synchronous, in-memory —
     *  rides along with smartCategories() rather than needing its own
     *  dispatcher hop or debounce.
     *
     *  The scratch `cal` below is a *local* variable — a fresh one on every
     *  call's own stack, never shared across calls or threads — which is
     *  what makes reusing it across the loop safe. That's a different
     *  situation from the old `monthFormat` bug elsewhere in this file:
     *  that one was a single instance stored as a class-level field and
     *  reused across *concurrent* calls, which is what made it unsafe.
     *  Reusing a local inside one function's own execution has no such
     *  hazard — nothing else can ever be holding a reference to it. */
    fun onThisDay(items: List<MediaItem>): List<MediaItem> {
        val now = Calendar.getInstance()
        val todayMonth = now.get(Calendar.MONTH)
        val todayDay = now.get(Calendar.DAY_OF_MONTH)
        val todayYear = now.get(Calendar.YEAR)

        val cal = Calendar.getInstance()
        return items.filter { item ->
            cal.timeInMillis = item.dateTakenMillis
            cal.get(Calendar.MONTH) == todayMonth &&
                cal.get(Calendar.DAY_OF_MONTH) == todayDay &&
                cal.get(Calendar.YEAR) < todayYear
        }.sortedByDescending { it.dateTakenMillis }
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

    /**
     * Decodes [uri] at roughly [maxDimension] instead of full resolution,
     * using BitmapFactory's inSampleSize (a cheap power-of-2 downscale done
     * *during* decode, not after) — this is what keeps blur/near-duplicate
     * scanning from having to fully decode every original photo just to
     * throw most of its resolution away a moment later.
     */
    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? = try {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, bounds)
        }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) {
            null
        } else {
            var sample = 1
            while (w / sample > maxDimension * 2 || h / sample > maxDimension * 2) sample *= 2
            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Flags likely-blurry photos via Laplacian variance — a standard,
     * well-established local-only blur metric: sharp edges produce a wide
     * spread of values under a Laplacian (edge-detection) kernel, so their
     * variance is high; a blurry image's edges are soft, so the spread (and
     * therefore the variance) is low. No ML model, no network — just pixel
     * math on a small downsampled grayscale copy of each photo.
     *
     * [BLUR_VARIANCE_THRESHOLD] is a heuristic cutoff, not a precise
     * measurement — like any blur detector, it can misjudge a deliberately
     * soft-focus or minimalist shot as "blurry". Framed as a suggestion
     * list to review, the same way Quick Clean's other categories are,
     * rather than something the app acts on unsupervised.
     *
     * User-triggered, not automatic: decoding + processing every photo in
     * a library is real, unavoidable CPU work (unlike the cheap in-memory
     * filters in smartCategories()/onThisDay()), so this only runs when the
     * person explicitly asks for a scan — never silently in the background.
     * `yield()` every [YIELD_EVERY] photos keeps the scan cancellable: a
     * suspend function with no suspension points in its loop body won't
     * respond to the caller navigating away and cancelling the coroutine
     * until the *entire* loop finishes, which would waste battery running
     * a scan nobody's watching anymore.
     */
    suspend fun findBlurryPhotos(context: Context, items: List<MediaItem>): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        items.forEachIndexed { index, item ->
            if (index % YIELD_EVERY == 0) kotlinx.coroutines.yield()
            val variance = laplacianVariance(context, item.uri) ?: return@forEachIndexed
            if (variance < BLUR_VARIANCE_THRESHOLD) result += item
        }
        return result.sortedBy { it.dateTakenMillis }
    }

    private const val BLUR_SAMPLE_MAX_DIMENSION = 240
    private const val BLUR_VARIANCE_THRESHOLD = 60.0
    private const val YIELD_EVERY = 20

    private fun laplacianVariance(context: Context, uri: Uri): Double? {
        val bitmap = decodeSampledBitmap(context, uri, BLUR_SAMPLE_MAX_DIMENSION) ?: return null
        try {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 3 || height < 3) return null
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val gray = IntArray(width * height) { i ->
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                (r * 299 + g * 587 + b * 114) / 1000 // standard luma weights
            }
            // 3x3 Laplacian kernel [[0,1,0],[1,-4,1],[0,1,0]]; a 1px border
            // is skipped rather than specially handled — negligible effect
            // on the variance of what's otherwise thousands of interior
            // samples even at this small decode size.
            val values = DoubleArray((width - 2) * (height - 2))
            var idx = 0
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val center = gray[y * width + x]
                    val up = gray[(y - 1) * width + x]
                    val down = gray[(y + 1) * width + x]
                    val left = gray[y * width + (x - 1)]
                    val right = gray[y * width + (x + 1)]
                    values[idx++] = (up + down + left + right - 4 * center).toDouble()
                }
            }
            if (values.isEmpty()) return null
            val mean = values.average()
            return values.sumOf { (it - mean) * (it - mean) } / values.size
        } catch (e: Exception) {
            return null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Groups visually-similar photos — burst shots, near-identical retakes
     * — using average hashing (aHash), not byte-identical content like
     * findExactDuplicates(). Each photo is shrunk to an 8x8 grayscale
     * thumbnail, and each of the 64 pixels becomes one bit of a 64-bit
     * fingerprint (1 if that pixel is at-or-above the thumbnail's average
     * brightness, 0 otherwise). Two photos that look alike produce
     * fingerprints that differ in only a handful of bits — measured as
     * Hamming distance (XOR, then count the 1 bits) — even though their
     * *exact* pixels differ. Pure bit arithmetic, no ML model.
     *
     * [threshold] is the max Hamming distance (out of 64 bits) for two
     * photos to be clustered together; 5 is a commonly-cited aHash cutoff
     * that catches near-retakes/burst shots without also catching two
     * unrelated photos that just happen to share similar lighting/framing.
     *
     * Clustering itself is a simple greedy pass (compare each unclustered
     * photo against every later one), which is O(n²) comparisons — but
     * each comparison is just a 64-bit XOR + popcount, essentially free;
     * the real cost is the O(n) bitmap decodes beforehand, same as
     * findBlurryPhotos() above, which is why this is also user-triggered
     * rather than automatic, and also yields periodically to stay
     * cancellable.
     */
    suspend fun findNearDuplicates(context: Context, items: List<MediaItem>, threshold: Int = 5): List<MediaGroup> {
        val hashed = mutableListOf<Pair<MediaItem, Long>>()
        items.forEachIndexed { index, item ->
            if (index % YIELD_EVERY == 0) kotlinx.coroutines.yield()
            averageHash(context, item.uri)?.let { hashed += item to it }
        }

        val used = HashSet<Long>()
        val clusters = mutableListOf<MutableList<MediaItem>>()
        for (i in hashed.indices) {
            val (itemA, hashA) = hashed[i]
            if (itemA.id in used) continue
            val cluster = mutableListOf(itemA)
            for (j in i + 1 until hashed.size) {
                val (itemB, hashB) = hashed[j]
                if (itemB.id in used) continue
                if (java.lang.Long.bitCount(hashA xor hashB) <= threshold) {
                    cluster += itemB
                    used += itemB.id
                }
            }
            if (cluster.size > 1) {
                used += itemA.id
                clusters += cluster
            }
        }
        return clusters.mapIndexed { idx, cluster ->
            MediaGroup("Similar photos (${cluster.size})".let { if (clusters.size > 1) "$it \u2013 ${idx + 1}" else it },
                cluster.sortedByDescending { it.dateTakenMillis })
        }
    }

    private fun averageHash(context: Context, uri: Uri): Long? {
        val source = decodeSampledBitmap(context, uri, 8) ?: return null
        try {
            val scaled = Bitmap.createScaledBitmap(source, 8, 8, true)
            try {
                val pixels = IntArray(64)
                scaled.getPixels(pixels, 0, 8, 0, 0, 8, 8)
                val gray = IntArray(64) { i ->
                    val p = pixels[i]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    (r * 299 + g * 587 + b * 114) / 1000
                }
                val avg = gray.average()
                var hash = 0L
                for (i in 0 until 64) {
                    if (gray[i] >= avg) hash = hash or (1L shl i)
                }
                return hash
            } finally {
                if (scaled !== source) scaled.recycle()
            }
        } catch (e: Exception) {
            return null
        } finally {
            source.recycle()
        }
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
