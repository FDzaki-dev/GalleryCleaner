package com.example.gallerycleaner

import android.net.Uri
import androidx.compose.runtime.Immutable

// Batch40 (Audit Gap P0 #1): distinguishes what MediaDataSource actually
// queried the item from (MediaStore.Images.Media vs MediaStore.Video.Media)
// — not derived from file extension/MIME sniffing, since the collection
// queried is already an unambiguous, zero-cost source of truth.
enum class MediaType { IMAGE, VIDEO }

// @Immutable is a promise to the Compose compiler, not just documentation.
// MediaItem carries an android.net.Uri field — a platform class the
// compiler can't see inside, so its default (conservative) stability
// inference marks the *whole* MediaItem as unstable, and that instability
// is contagious: List<MediaItem> and MediaGroup (which wraps that list)
// become unstable too. An unstable parameter means Compose can never skip
// recomposing a composable that receives it, even when nothing it actually
// reads has changed — which is exactly what's on the hot path of every
// scrollable list in this app (GroupRow, SmartCategoryRow, the swipe
// filmstrip, the Swipe/Trash grids). @Immutable overrides the inference:
// it tells the compiler "trust that this never mutates after construction"
// (true here — every MediaItem/MediaGroup instance is built fresh by
// MediaRepository and never mutated in place), which restores
// recomposition-skipping everywhere these types flow, not just one screen.
@Immutable
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTakenMillis: Long,
    val dateModifiedMillis: Long,
    val sizeBytes: Long,
    val bucketName: String, // album name
    val width: Int,
    val height: Int,
    val relativePath: String, // folder path, e.g. "DCIM/Camera/"
    // Batch40 (Audit Gap P0 #1): defaulted, not a new required param — the
    // one real construction site (MediaDataSource.queryMediaPage) always
    // passes it explicitly with a named arg, same as every other field
    // here; the default just means this doesn't become a source-breaking
    // change for any future call site (test fixtures, previews, etc.) that
    // only cares about images.
    val mediaType: MediaType = MediaType.IMAGE,
    // 0 for images (and for videos where MediaStore couldn't report a
    // duration) — never null, so callers can treat "0" as the one
    // "unknown/not applicable" case instead of null-checking everywhere.
    val durationMillis: Long = 0L
)

enum class SortOption(val label: String) {
    DATE("Date"),
    SIZE("Size"),
    NAME("Name")
}

enum class GroupMode(val label: String) {
    MONTH("Month"),
    ALBUM("Album")
}

@Immutable
data class MediaGroup(
    val key: String,       // e.g. "January 2026" or album name
    val items: List<MediaItem>
)

sealed class SwipeDecision {
    object Keep : SwipeDecision()
    object Delete : SwipeDecision()
}

/** State for an on-demand scan (blur detection, near-duplicate detection)
 *  — separate from the automatic smartGroups/onThisDay pipeline because
 *  these require real per-photo bitmap decoding, too costly to run
 *  silently on every load. [T] is List<MediaItem> for blur results, or
 *  List<MediaGroup> for near-duplicate clusters. */
sealed class ScanState<out T> {
    object Idle : ScanState<Nothing>()
    object Scanning : ScanState<Nothing>()
    data class Done<T>(val result: T) : ScanState<T>()
}
