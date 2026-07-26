package com.example.gallerycleaner

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Renders a photo or GIF thumbnail with one consistent API, via Coil.
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
        modifier = modifier
    )
}
