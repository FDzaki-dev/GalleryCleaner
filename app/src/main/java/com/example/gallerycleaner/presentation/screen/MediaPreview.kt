package com.example.gallerycleaner

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Renders a photo, GIF, or video thumbnail with one consistent API, via
 * Coil (the video frame decoder is registered once, app-wide, in
 * GalleryCleanerApp's shared ImageLoader — this function doesn't need to
 * know the difference between a photo URI and a video URI to load either).
 *
 * Every screen that shows a MediaItem — home covers, trash grid, filmstrip,
 * swipe card, fullscreen viewer — goes through this one function, which is
 * why the play-badge added below for [MediaType.VIDEO] (Batch40, Audit Gap
 * P0 #1) shows up everywhere a video thumbnail can appear without needing
 * to touch each of those screens individually.
 *
 * @param lowMemory Two things change together for high-volume, fast-scrolling
 *   contexts (home covers, trash grid, filmstrip) — both are no-ops for a
 *   card you look at one at a time (swipe card, fullscreen viewer), where
 *   they'd only hurt quality or add pointless overhead:
 *   - Decodes as RGB_565 (2 bytes/pixel) instead of the default ARGB_8888
 *     (4 bytes/pixel) — half the memory per bitmap, with no visible quality
 *     loss at small thumbnail sizes since there's no alpha channel to lose
 *     (photos are opaque) and the color banding RGB_565 can introduce is
 *     imperceptible once an image is downscaled this far anyway.
 *     IMPORTANT: `bitmapConfig()` alone does nothing on API 26+ — Coil
 *     defaults every request to a hardware bitmap, and hardware bitmaps
 *     ignore any custom Bitmap.Config entirely. `allowHardware(false)` has
 *     to be set alongside it or the RGB_565 request is silently discarded.
 *   - Skips the crossfade. A 120ms fade-in is barely noticeable on a single
 *     photo you're deliberately looking at, but during a fast fling through
 *     a grid or filmstrip, dozens of thumbnails start fading in at once as
 *     they scroll into view — that's real animation overhead stacking up
 *     exactly when the list is already busy laying out and decoding, and a
 *     common, concrete cause of scroll stutter in image-heavy lists.
 */
@Composable
fun MediaPreview(
    item: MediaItem,
    contentScale: ContentScale,
    decodeSize: Int,
    modifier: Modifier = Modifier,
    lowMemory: Boolean = false
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .size(decodeSize)
                .apply {
                    if (lowMemory) {
                        allowHardware(false)
                        bitmapConfig(Bitmap.Config.RGB_565)
                    } else {
                        crossfade(120)
                    }
                }
                .build(),
            contentDescription = item.displayName,
            contentScale = contentScale,
            modifier = Modifier.matchParentSize()
        )
        // Batch40 (Audit Gap P0 #1): the only visual cue that an item is a
        // video rather than a still photo — MediaPreview otherwise renders
        // both identically (a static decoded frame). Duration label / tap-
        // to-play are deliberately NOT part of this batch's scope; see
        // PROJECT_STATE.md Batch40 for what's deferred and why.
        if (item.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(18.dp)
                )
            }
        }
    }
}
