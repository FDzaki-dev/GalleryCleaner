package com.example.gallerycleaner

import android.net.Uri
import androidx.compose.runtime.Immutable

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
    val relativePath: String // folder path, e.g. "DCIM/Camera/"
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
