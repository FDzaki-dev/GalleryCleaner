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

/**
 * Raw MediaStore access — paging queries only, no grouping/sorting/analysis
 * logic (that lives in [MediaRepository] / [MediaScanner]). Split out of
 * the old monolithic MediaRepository.kt so the "how do we talk to
 * MediaStore" concern is isolated from "how do we present/analyze what we
 * got back".
 */
object MediaDataSource {

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
}
