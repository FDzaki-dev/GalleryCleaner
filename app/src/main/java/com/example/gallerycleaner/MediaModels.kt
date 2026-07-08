package com.example.gallerycleaner

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val bucketName: String // album name
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

data class MediaGroup(
    val key: String,       // e.g. "January 2026" or album name
    val items: List<MediaItem>
)

sealed class SwipeDecision {
    object Keep : SwipeDecision()
    object Delete : SwipeDecision()
}
