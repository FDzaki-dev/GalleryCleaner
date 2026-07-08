package com.example.gallerycleaner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MediaRepository {

    /** Loads all images and videos visible to the app from MediaStore. */
    fun loadAllMedia(context: Context): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        items += queryMedia(
            context,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            isVideo = false
        )
        items += queryMedia(
            context,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            isVideo = true
        )
        return items
    }

    private fun queryMedia(context: Context, collection: Uri, isVideo: Boolean): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val dateTaken = cursor.getLong(dateTakenCol).let {
                    if (it > 0) it else cursor.getLong(dateAddedCol) * 1000L
                }
                result += MediaItem(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: "unknown",
                    dateTakenMillis = dateTaken,
                    sizeBytes = cursor.getLong(sizeCol),
                    isVideo = isVideo,
                    bucketName = cursor.getString(bucketCol) ?: "Unknown album"
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
        // Preserve a stable, recency-first ordering of group keys
        val orderedKeys = when (mode) {
            GroupMode.MONTH -> grouped.keys.sortedByDescending { key ->
                sorted.first { monthKey(it.dateTakenMillis) == key }.dateTakenMillis
            }
            GroupMode.ALBUM -> grouped.keys.sorted()
        }
        return orderedKeys.map { key -> MediaGroup(key, grouped.getValue(key)) }
    }

    private fun sortItems(items: List<MediaItem>, sort: SortOption): List<MediaItem> {
        return when (sort) {
            SortOption.DATE -> items.sortedByDescending { it.dateTakenMillis }
            SortOption.SIZE -> items.sortedByDescending { it.sizeBytes }
            SortOption.NAME -> items.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
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

        val videos = items.filter { it.isVideo }
        if (videos.isNotEmpty()) {
            result += MediaGroup("Videos", videos.sortedByDescending { it.sizeBytes })
        }

        val large = items.filter { it.sizeBytes >= LARGE_FILE_THRESHOLD_BYTES }
        if (large.isNotEmpty()) {
            result += MediaGroup("Large files (10MB+)", large.sortedByDescending { it.sizeBytes })
        }

        // Cheap, exact-duplicate heuristic: items that share both a file size
        // and media type are very likely the same file saved more than once
        // (re-saves, WhatsApp/social re-downloads, etc.). Not perfect — won't
        // catch a re-compressed copy — but zero-cost and zero false positives
        // for genuinely different files.
        val duplicates = items
            .filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes to it.isVideo }
            .values
            .filter { it.size > 1 }
            .flatten()
        if (duplicates.isNotEmpty()) {
            result += MediaGroup("Possible duplicates", duplicates.sortedByDescending { it.sizeBytes })
        }

        return result
    }

    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    private fun monthKey(millis: Long): String = monthFormat.format(millis)
}
