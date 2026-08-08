package com.example.gallerycleaner

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Built-in crash logger. Writes each uncaught exception to
 * `Documents/GalleryCleaner/logs/crash_<yyyyMMdd_HHmmss>_<UUID>.txt` via
 * MediaStore (API 29+) — no legacy WRITE_EXTERNAL_STORAGE permission
 * needed. Every write is wrapped so a failure here can never mask or
 * replace the real crash: the original default handler always still runs.
 *
 * FIFO retention: keeps at most [MAX_LOGS] files, oldest deleted first.
 */
object CrashLogger {

    private const val APP_FOLDER = "GalleryCleaner"
    private const val LOG_SUBDIR = "logs"
    private const val MAX_LOGS = 50
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Fail-safe: never let a logging failure swallow the crash itself.
            try {
                writeCrashLog(appContext, thread, throwable)
            } catch (_: Throwable) {
                // Intentionally swallowed — logging must never block the
                // real crash from reaching the previous/default handler.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val now = Date()
        val fileName = "crash_${fileNameFormat.format(now)}_${UUID.randomUUID()}.txt"
        val content = buildLogContent(context, thread, throwable, now)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, fileName, content)
        } else {
            writeViaLegacyFile(fileName, content)
        }
    }

    private fun writeViaMediaStore(context: Context, fileName: String, content: String) {
        val relativePath = "${android.os.Environment.DIRECTORY_DOCUMENTS}/$APP_FOLDER/$LOG_SUBDIR"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return
        resolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }

        enforceFifoRetentionMediaStore(context, relativePath)
    }

    /** API 24-28 fallback — MediaStore's RELATIVE_PATH column requires API 29+. */
    private fun writeViaLegacyFile(fileName: String, content: String) {
        val docsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOCUMENTS
        )
        val logDir = java.io.File(docsDir, "$APP_FOLDER/$LOG_SUBDIR")
        if (!logDir.exists()) logDir.mkdirs()
        java.io.File(logDir, fileName).writeText(content, Charsets.UTF_8)
        enforceFifoRetentionLegacy(logDir)
    }

    private fun buildLogContent(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        now: Date
    ): String {
        val versionName = try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
        } catch (_: Exception) {
            "unknown"
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now)

        return buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Timestamp: $timestamp")
            appendLine("App Version: $versionName")
            appendLine("OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine("--- Stack Trace ---")
            appendLine(throwable.stackTraceToString())
        }
    }

    /**
     * Keeps only the most recent [MAX_LOGS] crash files under the app's log
     * folder, deleting older ones first (FIFO). Queried by
     * DATE_ADDED so this works purely through MediaStore, no filesystem
     * path traversal / legacy permission needed.
     */
    private fun enforceFifoRetentionMediaStore(context: Context, relativePath: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$relativePath/", "crash_%.txt")

        resolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            var index = 0
            while (cursor.moveToNext()) {
                index++
                if (index > MAX_LOGS) {
                    val id = cursor.getLong(idColumn)
                    val itemUri = android.content.ContentUris.withAppendedId(collection, id)
                    try {
                        resolver.delete(itemUri, null, null)
                    } catch (_: Exception) {
                        // best-effort cleanup — a stuck delete shouldn't crash the crash logger
                    }
                }
            }
        }
    }

    /** API 24-28 FIFO fallback — plain file listing sorted by lastModified. */
    private fun enforceFifoRetentionLegacy(logDir: java.io.File) {
        val files = logDir.listFiles { f -> f.name.startsWith("crash_") && f.name.endsWith(".txt") }
            ?: return
        if (files.size <= MAX_LOGS) return
        files.sortedByDescending { it.lastModified() }
            .drop(MAX_LOGS)
            .forEach { old ->
                try {
                    old.delete()
                } catch (_: Exception) {
                    // best-effort cleanup
                }
            }
    }
}
