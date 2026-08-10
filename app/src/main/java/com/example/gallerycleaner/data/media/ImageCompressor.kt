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
     *  item.
     *
     *  Bug fix (crash log `crash_20260810_134626`): this used to call
     *  `BitmapFactory.decodeStream(stream)` with zero options — full
     *  resolution, default `ARGB_8888` (4 bytes/pixel), no cap. A 108MP
     *  photo (common on MediaTek/Transsion camera stacks, confirmed by the
     *  crash's device model) is ~12000x9000 — that's one ~430MB
     *  allocation, on its own big enough to exhaust a 512MB `largeHeap`.
     *  The crash's own stack trace (MediaTek `BoostFwk`, unrelated 32-byte
     *  allocation) wasn't the cause — it's just whatever tiny allocation
     *  happened to run right after the heap was already full from this.
     *  Confirmed by the `OutOfMemoryError` never being caught here either:
     *  it `extends Error`, not `Exception`, so the old `catch (e:
     *  Exception)` below let it fall straight through and kill the app —
     *  same failure mode the crash log shows.
     *
     *  Fix keeps the documented "never downscale resolution" promise for
     *  the vast majority of photos, and only changes decode *bit depth*
     *  (not dimensions) for the rare oversized outlier: bounds are read
     *  first (cheap — no pixel buffer allocated), and only pixel counts
     *  above [LARGE_IMAGE_PIXEL_THRESHOLD] fall back to `RGB_565` (2
     *  bytes/pixel, half the memory) instead of `ARGB_8888`. JPEGs have no
     *  alpha channel to lose either way, and `RGB_565`'s minor color-depth
     *  reduction is imperceptible once re-encoded as JPEG at quality 80 —
     *  a fully reasonable trade against actually crashing the app.
     *  `OutOfMemoryError` is now also caught explicitly as a last resort
     *  (a device with an even smaller heap, or a genuinely corrupt/bogus
     *  image header, can still exceed this) — reported as [Result.Failed]
     *  for that one photo instead of taking the whole app down. */
    fun compressInPlace(context: Context, item: MediaItem, quality: Int = 80): Result {
        if (!isCompressible(item)) return Result.Skipped

        val bitmap = try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val pixelCount = bounds.outWidth.toLong() * bounds.outHeight.toLong()
            val options = BitmapFactory.Options().apply {
                if (pixelCount > LARGE_IMAGE_PIXEL_THRESHOLD) {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            }
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: OutOfMemoryError) {
            Log.e("ImageCompressor", "OOM decoding ${item.uri} (${item.displayName})", e)
            null
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
        } catch (e: OutOfMemoryError) {
            Log.e("ImageCompressor", "OOM writing ${item.uri}", e)
            Result.Failed
        } catch (e: Exception) {
            Log.e("ImageCompressor", "Gagal menulis: ${item.uri}", e)
            Result.Failed
        } finally {
            bitmap.recycle()
        }
    }

    /** ~24 megapixels — comfortably above what the large majority of phone
     *  cameras shoot (most flagships top out around 12-50MP), but well
     *  below the 100MP+ sensors some MediaTek/Transsion devices ship,
     *  which is exactly the case that crashed. At `ARGB_8888` that's
     *  ~96MB for one bitmap — still hefty but survivable; the 108MP case
     *  above this threshold would otherwise be ~430MB. */
    private const val LARGE_IMAGE_PIXEL_THRESHOLD = 24_000_000L

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
