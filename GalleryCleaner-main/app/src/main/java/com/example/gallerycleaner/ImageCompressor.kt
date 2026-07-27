package com.example.gallerycleaner

import android.app.RecoverableSecurityException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * Re-encodes a photo in place at a lower JPEG quality — same photo, same
 * dimensions, smaller file. Deliberately does NOT downscale resolution:
 * that would be a visible, permanent quality loss the person didn't ask
 * for. Quality-only recompression is the same trade every "compress"
 * button in a photo app makes.
 *
 * Mirrors DeleteHelper's exact scoped-storage dance for a reason: writing
 * to an existing MediaStore entry the app doesn't own hits the same
 * Android 10+ permission wall as deleting one does, and Android exposes
 * the same two-step "try, and if denied, ask via a system prompt" pattern
 * for both.
 */
object ImageCompressor {

    /** Result of one compression attempt. */
    sealed class Result {
        /** [savedBytes] is the difference between the old and new size —
         *  always >= 0; if recompressing wouldn't shrink the file the item
         *  is left untouched and reported as Skipped instead. */
        data class Success(val savedBytes: Long) : Result()
        object Skipped : Result()
        object Failed : Result()

        /** Android 10+: the app doesn't hold write access to this
         *  particular file yet. [recoverySender] is what MainActivity
         *  needs to launch a system consent prompt and retry afterwards —
         *  identical in shape to RecoverableSecurityException handling in
         *  MainActivity.performPermanentDeletion(). */
        data class NeedsPermission(val recoverySender: android.content.IntentSender) : Result()
    }

    /** JPEG only — PNG is lossless-by-format (re-encoding at "quality"
     *  doesn't shrink it the way it shrinks a JPEG) and GIFs are almost
     *  always small already. Re-compressing formats compression can't
     *  meaningfully help would just burn battery decoding+encoding for a
     *  file that comes out the same size or larger. */
    private fun isCompressible(item: MediaItem): Boolean {
        val name = item.displayName.lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
    }

    /** [quality] is a JPEG quality 0-100; callers should offer this as a
     *  single "balanced" default rather than a per-photo dial — matching
     *  every other bulk action in this app (bulk delete, bulk trash),
     *  which act on a whole selection at once rather than one dial per
     *  item. */
    fun compressInPlace(context: Context, item: MediaItem, quality: Int = 80): Result {
        if (!isCompressible(item)) return Result.Skipped

        val bitmap = try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e("ImageCompressor", "Gagal decode: ${item.uri}", e)
            null
        } ?: return Result.Failed

        return try {
            val newBytes = writeCompressed(context, item.uri, bitmap, quality)
            when {
                newBytes == null -> Result.Failed
                newBytes >= item.sizeBytes -> Result.Skipped // recompression didn't actually shrink it — leave the original untouched rather than write a same-or-larger "compressed" file
                else -> Result.Success(item.sizeBytes - newBytes)
            }
        } catch (e: RecoverableSecurityException) {
            Result.NeedsPermission(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            Log.e("ImageCompressor", "Gagal menulis: ${item.uri}", e)
            Result.Failed
        } finally {
            bitmap.recycle()
        }
    }

    /** Returns the new file size in bytes, or null if the write failed for
     *  a reason other than a recoverable permission gap (that case is
     *  thrown, not returned — see the catch block above). */
    private fun writeCompressed(context: Context, uri: Uri, bitmap: Bitmap, quality: Int): Long? {
        // "wt" = write, truncate — replaces the existing file's contents in
        // place rather than appending, so a shorter re-encoded JPEG doesn't
        // leave old trailing bytes behind.
        val opened = context.contentResolver.openOutputStream(uri, "wt") ?: return null
        opened.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) return null
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() }
        } catch (e: Exception) {
            null
        }
    }

    /** True on API levels where MediaStore.createWriteRequest() exists —
     *  the batched, single-consent-dialog path MainActivity should prefer,
     *  same cutoff as MediaStore.createDeleteRequest(). Below this, a
     *  RecoverableSecurityException thrown per-item (caught above) is the
     *  only mechanism available, same as DeleteHelper's fallback path. */
    fun supportsBatchWriteRequest(): Boolean = Build.VERSION.SDK_INT >= 30
}
