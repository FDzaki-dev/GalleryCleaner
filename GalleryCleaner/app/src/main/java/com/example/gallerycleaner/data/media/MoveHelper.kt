package com.example.gallerycleaner

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

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
        /** Batch62 (Audit Gap P1 #9): the file itself was confirmed moved on
         *  disk, but MediaStore's row could NOT be confirmed updated (the
         *  direct ContentResolver.update() call either matched 0 rows or
         *  threw, AND the media scanner's own follow-up scan — awaited, not
         *  fire-and-forget — still came back without a valid entry at the
         *  new location). Only reachable via [moveViaDirectFile]'s API
         *  24-28 path; [moveViaRelativePath] (API 29+) has no separate
         *  physical-move step to diverge from, so it can't land here.
         *  Callers should NOT silently treat this the same as [Success] for
         *  local state — the whole point is surfacing the ambiguity rather
         *  than repeating the bug this replaces (see PROJECT_STATE.md
         *  Batch62 for what MainActivity.kt does with it). */
        data class PartialSuccess(val newRelativePath: String) : Result()
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
     *  slash, e.g. `"Pictures/GalleryCleaner/Organized/"`.
     *
     *  Batch62: `suspend` since [moveViaDirectFile]'s ambiguous branches now
     *  await the media scanner's own confirmation (Audit Gap P1 #9) rather
     *  than guessing — both existing call sites in MainActivity.kt already
     *  run inside `scope.launch(Dispatchers.IO) { ... }`, so this needed no
     *  change at either call site. */
    suspend fun moveTo(context: Context, item: MediaItem, targetRelativePath: String): Result {
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
    private suspend fun moveViaDirectFile(context: Context, item: MediaItem, normalizedTarget: String): Result {
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
            val rows = context.contentResolver.update(item.uri, values, null, null)
            if (rows > 0) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(sourceFile.absolutePath, targetFile.absolutePath),
                    null,
                    null
                )
                Result.Success(normalizedTarget)
            } else {
                // update() didn't throw, but matched 0 rows — item.uri no
                // longer points at a row this call could touch. The file
                // itself already moved (see `moved` above), so this isn't a
                // plain Failed either; fall through to the same awaited
                // confirmation the exception branch below uses.
                confirmOrPartial(context, sourceFile, targetFile, normalizedTarget)
            }
        } catch (e: RecoverableSecurityException) {
            Result.NeedsPermission(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            Log.e("MoveHelper", "File dipindah tapi MediaStore gagal diupdate: ${item.uri}", e)
            confirmOrPartial(context, sourceFile, targetFile, normalizedTarget)
        }
    }

    /** Batch62 (Audit Gap P1 #9) — the two branches above that reach here
     *  already know the file physically moved but couldn't confirm
     *  MediaStore's row was updated. Previously both cases were reported as
     *  a plain [Result.Success] on the (undocumented) assumption that the
     *  media scanner would "eventually" catch up on its own — silently
     *  trusting that meant the in-memory list (`MainActivity.applyOrganizeResult`)
     *  could show the item in its new folder while a fresh MediaStore query
     *  (e.g. after reopening the app) still had it in the old one, exactly
     *  the "UI sementara tidak sepenuhnya sinkron" gap flagged in
     *  AUDIT_GAP.md. This now *waits* for that same scan's own callback
     *  instead of firing it and moving on: if the scanner itself confirms a
     *  valid entry at the new path, that's a real, verified [Result.Success]
     *  (the update() failure turned out to be harmless); if the scanner
     *  can't confirm it either, [Result.PartialSuccess] is returned so the
     *  caller can be honest with the user rather than repeat the guess. */
    private suspend fun confirmOrPartial(
        context: Context,
        sourceFile: File,
        targetFile: File,
        normalizedTarget: String
    ): Result {
        val confirmed = awaitScanConfirms(context, targetFile.absolutePath)
        // Old location's scan isn't part of the confirmation — just a
        // best-effort nudge so other gallery apps notice the source is
        // gone, same as the original fire-and-forget call did.
        MediaScannerConnection.scanFile(context, arrayOf(sourceFile.absolutePath), null, null)
        return if (confirmed) Result.Success(normalizedTarget) else Result.PartialSuccess(normalizedTarget)
    }

    /** Wraps `MediaScannerConnection`'s callback-based API as a one-shot
     *  suspend call — `true` iff the scanner registered a real MediaStore
     *  entry for [path] (a null [android.net.Uri] in the callback means the
     *  scan didn't find anything usable there). */
    private suspend fun awaitScanConfirms(context: Context, path: String): Boolean =
        suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, uri ->
                if (cont.isActive) cont.resume(uri != null)
            }
        }
}
