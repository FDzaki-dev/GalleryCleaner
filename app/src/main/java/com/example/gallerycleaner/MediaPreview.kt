package com.example.gallerycleaner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Renders a photo or GIF thumbnail with one consistent API, via Coil.
 */
@Composable
fun MediaPreview(
    item: MediaItem,
    contentScale: ContentScale,
    decodeSize: Int,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri)
            .size(decodeSize)
            .crossfade(120)
            .build(),
        contentDescription = item.displayName,
        contentScale = contentScale,
        modifier = modifier
    )
}
