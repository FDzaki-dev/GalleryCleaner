package com.example.gallerycleaner

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder

/**
 * Without this, Coil only knows how to decode still images — pointing it at
 * a video's or an animated GIF's content URI (which happens for every video
 * and GIF in the gallery) produces a garbled/blank preview instead of a real
 * thumbnail, and wastes CPU/IO trying. Registering the video + gif decoders
 * here fixes that everywhere in the app (home screen cover, swipe cards,
 * trash grid) via the one shared loader.
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
                add(VideoFrameDecoder.Factory())
            }
            .build()
}
