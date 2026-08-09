package com.example.gallerycleaner

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Moves a MediaStore item into a different folder — the backend for
 * ROADMAP.md Fase A item 2 ("Organize" swipe action / 3rd action button).
 *
 * Mirrors ImageCompressor's exact scoped-storage dance for the same reason
 * DeleteHelper does: writing to a MediaStore entry the app doesn't own hits
 * the Android 10+ permission wall, and Android exposes the same "try, and
 * if denied, ask via a system prompt" two-step for updates as for deletes.
 *
 * Correction (Batch16 audit): ROADMAP.md previously claimed a `moveTo`
 * primitive already existed in `MediaDataSource`. That was wrong — grep
 * only ever matched `Cursor.moveToNext()`, an unrelated API. No move
 * primitive existed before this file. Built from scratch here.
 */
object MoveHelper {

    sealed class Result {
        /** [newRelativePath] is what to reflect in local app state (e.g.
         *  removing the item from the currently-open folder's group). */
        data class Success(val newRelativePath: String) : Result()
        /** Destination is the same folder the item is already in — nothing
         *  to do, not an error. */
        object AlreadyThere : Result()
        object Failed : Result()
        data class NeedsPermission(val recoverySender: android.content.IntentSender) : Result()
    }

    /** True on API levels where `MediaStore.createWriteRequest()` exists —
     *  same batched single-consent-dialog path used for compression, and
     *  the one MainActivity should prefer for a multi-select "Organize"
     *  from grid mode. Below this, per-item `RecoverableSecurityException`
     *  handling (caught below) is the only mechanism available. */
    fun supportsBatchWriteRequest(): Boolean = Build.VERSION.SDK_INT >= 30

    /** [targetRelativePath] must match the shape `MediaItem.relativePath`
     *  already uses elsewhere in this app: a folder path with a trailing
     *  slash, e.g. `"Pictures/GalleryCleaner/Organized/"`. */
    fun moveTo(context: Context, item: MediaItem, targetRelativePath: String): Result {
        val normalizedTarget = if (targetRelativePath.endsWith("/")) targetRelativePath else "$targetRelativePath/"
        if (normalizedTarget == item.relativePath) return Result.AlreadyThere

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            moveViaRelativePath(context, item, normalizedTarget)
        } else {
            moveViaDirectFile(context, item, normalizedTarget)
        }
    }

    /** API 29+: updating RELATIVE_PATH through the provider physically
     *  relocates the underlying file on disk — this is the documented,
     *  intended way to "move" a MediaStore entry under scoped storage,
     *  not just a metadata change. */
    private fun moveViaRelativePath(context: Context, item: MediaItem, normalizedTarget: String): Result {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, normalizedTarget)
        }
        return try {
            val rows = context.contentResolver.update(item.uri, values, null, null)
            if (rows > 0) Result.Success(normalizedTarget) else Result.Failed
        } catch (e: RecoverableSecurityException) {
            Result.NeedsPermission(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            Log.e("MoveHelper", "Gagal memindahkan (RELATIVE_PATH): ${item.uri}", e)
            Result.Failed
        }
    }

    /** API 24-28: no RELATIVE_PATH column and no scoped-storage write wall
     *  either — WRITE_EXTERNAL_STORAGE (already declared, maxSdkVersion=28
     *  in the manifest) is enough to move the file directly, then the
     *  MediaStore row is updated to match and the media scanner is nudged
     *  so gallery apps notice immediately instead of after a manual scan. */
    @Suppress("DEPRECATION")
    private fun moveViaDirectFile(context: Context, item: MediaItem, normalizedTarget: String): Result {
        val root = Environment.getExternalStorageDirectory()
        val sourceFile = File(root, item.relativePath + item.displayName)
        val targetDir = File(root, normalizedTarget)
        val targetFile = File(targetDir, item.displayName)

        if (!sourceFile.exists()) return Result.Failed
        if (!targetDir.exists() && !targetDir.mkdirs()) return Result.Failed

        val moved = try {
            sourceFile.renameTo(targetFile) || run {
                // renameTo can fail across some filesystem boundaries even
                // within external storage — fall back to copy+delete rather
                // than give up, matching the resilience DeleteHelper shows
                // for its own failure cases.
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                targetFile.exists()
            }
        } catch (e: Exception) {
            Log.e("MoveHelper", "Gagal memindahkan (direct file): ${sourceFile.path}", e)
            false
        }
        if (!moved) return Result.Failed

        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, targetFile.absolutePath)
            }
            context.contentResolver.update(item.uri, values, null, null)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(sourceFile.absolutePath, targetFile.absolutePath),
                null,
                null
            )
            Result.Success(normalizedTarget)
        } catch (e: RecoverableSecurityException) {
            Result.NeedsPermission(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            Log.e("MoveHelper", "File dipindah tapi MediaStore gagal diupdate: ${item.uri}", e)
            // The file itself did move — still a partial success from the
            // user's point of view (media scanner below will eventually
            // pick up the new location on its own even if this row update
            // failed), so this is reported as Success rather than Failed.
            MediaScannerConnection.scanFile(
                context,
                arrayOf(sourceFile.absolutePath, targetFile.absolutePath),
                null,
                null
            )
            Result.Success(normalizedTarget)
        }
    }
}
