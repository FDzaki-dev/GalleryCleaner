package com.example.gallerycleaner

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Loading/success/failed — lets the UI show a spinner only while genuinely
 *  still working, instead of an indefinite blank for files that fail. */
sealed class VideoFrameState {
    object Loading : VideoFrameState()
    data class Success(val bitmap: Bitmap) : VideoFrameState()
    object Failed : VideoFrameState()
}

/**
 * Grabs a representative frame from a video. Coil's coil-video extension
 * functions (videoFrameMillis etc.) proved unreliable to import correctly
 * across versions, so this uses MediaMetadataRetriever directly — a stable,
 * long-standing Android API — instead.
 */
object VideoThumbnail {

    private const val MAX_DIMENSION = 720 // plenty for any preview/thumbnail; keeps decode fast+light
    private const val EXTRACTION_TIMEOUT_MS = 8_000L

    suspend fun extractFrame(context: Context, uri: Uri): Bitmap? =
        withTimeoutOrNull(EXTRACTION_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)

                    val durationMs = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L

                    // Large/4K videos can be slow or flaky to decode at a single fixed
                    // timestamp (some codecs have no keyframe there, or the file is
                    // long enough that 10s is still black). Try a few candidate
                    // points — first successful frame wins.
                    val candidateTimesUs = buildList {
                        if (durationMs > 10_000) add(10_000_000L)      // 10s in
                        if (durationMs > 0) add(durationMs * 500L)      // midpoint (ms -> us, /2)
                        add(1_000_000L)                                  // 1s in
                        add(0L)                                          // first frame, last resort
                    }.distinct()

                    var result: Bitmap? = null
                    for (timeUs in candidateTimesUs) {
                        result = decodeFrame(retriever, timeUs)
                        if (result != null) break
                    }
                    result
                } catch (e: Exception) {
                    null
                } finally {
                    try { retriever.release() } catch (e: Exception) { /* no-op */ }
                }
            }
        } // null on timeout too — same "failed" outcome the UI already handles

    private fun decodeFrame(retriever: MediaMetadataRetriever, timeUs: Long): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 27) {
            // Decodes directly at a small target size — far faster and much
            // less memory than pulling a full-resolution (e.g. 4K) frame.
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                MAX_DIMENSION,
                MAX_DIMENSION
            )
        } else {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
        }
    } catch (e: Exception) {
        null
    }
}

/** Composable helper: loads and remembers a video's preview frame for the given uri. */
@Composable
fun rememberVideoFrameState(context: Context, uri: Uri): VideoFrameState {
    val state = produceState<VideoFrameState>(initialValue = VideoFrameState.Loading, key1 = uri) {
        value = VideoThumbnail.extractFrame(context, uri)
            ?.let { VideoFrameState.Success(it) }
            ?: VideoFrameState.Failed
    }
    return state.value
}

