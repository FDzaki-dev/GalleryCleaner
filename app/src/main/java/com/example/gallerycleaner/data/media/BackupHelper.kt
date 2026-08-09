package com.example.gallerycleaner

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log

/**
 * ROADMAP Fase B item 7 — "Backup-before-permanent-delete opsional".
 *
 * Copies each item to a local backup folder BEFORE the actual MediaStore
 * delete happens — the only point at which the source `uri` is guaranteed
 * to still be readable. Called from `MainActivity.performPermanentDeletion`
 * (see comment there), so it runs even if the user later cancels the
 * system delete confirmation on API 30+: an extra backup copy sitting
 * unused is a harmless tradeoff, whereas backing up only after a confirmed
 * delete is not possible (the source is already gone by then). Opt-in,
 * defaults off (`SettingsStore.backupBeforeDeleteEnabledFlow`) — this is
 * expected disk-usage behavior the person deliberately turns on, not a
 * surprise background copy.
 *
 * Destination mirrors CrashLogger's approach — MediaStore insert (API 29+,
 * no legacy WRITE_EXTERNAL_STORAGE permission) into a public, user-visible
 * folder rather than internal app storage, so the backup survives an
 * uninstall and the person can browse it with any gallery/file app:
 * `Pictures/GalleryCleaner/Backup/` for images, `Movies/GalleryCleaner/Backup/`
 * for videos. Below API 29, falls back to direct `File` I/O in the
 * equivalent public directory, same fallback pattern `CrashLogger` uses.
 *
 * Best-effort by design: one item failing to back up (corrupt file,
 * storage full, permission edge case) must never block or fail the
 * deletion the person actually asked for — every step is wrapped so a
 * single failure is skipped, not thrown.
 */
object BackupHelper {

    private const val APP_FOLDER = "GalleryCleaner"
    private const val BACKUP_SUBDIR = "Backup"

    /** Attempts to copy every item in [items] to the backup folder.
     *  Returns the count actually backed up (for optional caller feedback,
     *  e.g. a snackbar) — never throws, so callers can fire-and-forget. */
    fun backupBeforeDelete(context: Context, items: List<MediaItem>): Int {
        var backedUp = 0
        for (item in items) {
            try {
                if (copyOne(context, item)) backedUp++
            } catch (e: Exception) {
                Log.e("BackupHelper", "Gagal backup: ${item.displayName}", e)
                // Intentionally swallowed per-item — see class doc: a single
                // failed backup must never block the deletion itself.
            }
        }
        return backedUp
    }

    private fun copyOne(context: Context, item: MediaItem): Boolean {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(item.uri) ?: "application/octet-stream"
        val isVideo = mimeType.startsWith("video/")

        val collection: Uri
        val baseDir: String
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            baseDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val relativePath = "$baseDir/$APP_FOLDER/$BACKUP_SUBDIR"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            val destUri = resolver.insert(collection, values) ?: return false
            return copyBytes(resolver, item.uri, destUri)
        } else {
            // API 24-28 fallback — RELATIVE_PATH needs API 29+, same
            // exception CrashLogger documents for its own legacy path.
            baseDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val publicDir = Environment.getExternalStoragePublicDirectory(baseDir)
            val backupDir = java.io.File(publicDir, "$APP_FOLDER/$BACKUP_SUBDIR")
            if (!backupDir.exists()) backupDir.mkdirs()
            val destFile = java.io.File(backupDir, item.displayName)
            resolver.openInputStream(item.uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            return true
        }
    }

    private fun copyBytes(resolver: android.content.ContentResolver, source: Uri, dest: Uri): Boolean {
        val input = resolver.openInputStream(source) ?: return false
        input.use { inStream ->
            val output = resolver.openOutputStream(dest) ?: return false
            output.use { outStream -> inStream.copyTo(outStream) }
        }
        return true
    }
}
