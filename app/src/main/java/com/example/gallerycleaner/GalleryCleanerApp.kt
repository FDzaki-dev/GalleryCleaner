package com.example.gallerycleaner

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * Without this, Coil only knows how to decode still images — pointing it at
 * an animated GIF's content URI produces a garbled/frozen preview instead of
 * a real animated thumbnail. Registering the gif decoder here fixes that
 * everywhere in the app (home screen cover, swipe cards, trash grid) via the
 * one shared loader.
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
            .build()
}
