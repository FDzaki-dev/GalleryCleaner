package com.example.gallerycleaner

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Without this, Coil only knows how to decode still images — pointing it at
 * an animated GIF's content URI produces a garbled/frozen preview instead of
 * a real animated thumbnail. Registering the gif decoder here fixes that
 * everywhere in the app (home screen cover, swipe cards, trash grid) via the
 * one shared loader.
 *
 * Memory cache is capped to an explicit percentage rather than left at
 * Coil's default. Coil's default sizes the cache off *available* app
 * memory (via ActivityManager) — which means turning on `largeHeap` in the
 * manifest doesn't just give the JVM more headroom, it also silently lets
 * this cache grow larger, since there's more "available memory" to take a
 * percentage of. That's exactly backwards for actually fixing a memory
 * problem: it masks per-bitmap waste (duplicate cache entries, bitmaps
 * decoded at the wrong config) behind a bigger budget instead of removing
 * the waste. Pinning this to a fixed, modest percentage keeps cache
 * behavior predictable regardless of largeHeap, so real fixes (matching
 * decode sizes, RGB_565 for grids — see MediaPreview.kt) are what actually
 * reduces memory pressure, not a bigger container to hide it in.
 *
 * The disk cache is new too: content URIs are local, so this isn't about
 * avoiding network cost — it's about not re-running the decode pipeline
 * (read + resample + color-convert) every single time the user scrolls back
 * to a photo already seen this session, which is where a lot of the
 * transient allocation churn during fast swiping was coming from.
 */
class GalleryCleanerApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // fixed, not tied to largeHeap's bigger "available memory"
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
}
