package com.example.gallerycleaner

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Orchestration facade — grouping/sorting logic plus thin delegating
 * wrappers around [MediaDataSource] (raw MediaStore I/O) and [MediaScanner]
 * (analytical scans). Split out of a single 517-line God File per the
 * structure audit; kept as a facade specifically so every existing caller
 * (MainActivity, CleaningReminderWorker) keeps calling `MediaRepository.xxx`
 * completely unchanged — this refactor moves *implementation*, not the
 * public API.
 */
object MediaRepository {

    // ---- Delegated to MediaDataSource (raw MediaStore I/O) ----
    fun loadAllMedia(context: Context): List<MediaItem> =
        MediaDataSource.loadAllMedia(context)

    fun loadMediaProgressively(context: Context): Flow<List<MediaItem>> =
        MediaDataSource.loadMediaProgressively(context)

    // ---- Delegated to MediaScanner (analytical / CPU-heavy scans) ----
    fun smartCategories(items: List<MediaItem>): List<MediaGroup> =
        MediaScanner.smartCategories(items)

    fun onThisDay(items: List<MediaItem>): List<MediaItem> =
        MediaScanner.onThisDay(items)

    suspend fun findExactDuplicates(context: Context, items: List<MediaItem>, onProgress: (checked: Int, total: Int) -> Unit = {}): List<MediaItem> =
        MediaScanner.findExactDuplicates(context, items, onProgress)

    suspend fun findBlurryPhotos(context: Context, items: List<MediaItem>): List<MediaItem> =
        MediaScanner.findBlurryPhotos(context, items)

    suspend fun findNearDuplicates(context: Context, items: List<MediaItem>, threshold: Int = 5): List<MediaGroup> =
        MediaScanner.findNearDuplicates(context, items, threshold)

    // ---- Grouping/sorting — stays here, this IS the orchestration layer ----

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

    // Was private — made public (Batch20) so SwipeScreen's in-session sort
    // control (ROADMAP Fase A item 4) reuses this exact logic instead of a
    // second implementation that could drift from Home's.
    fun sortItems(items: List<MediaItem>, sort: SortOption): List<MediaItem> {
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
