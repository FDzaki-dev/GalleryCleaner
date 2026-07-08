package com.example.gallerycleaner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Renders a photo or video thumbnail with one consistent API. Videos are
 * decoded via MediaMetadataRetriever (see VideoThumbnail.kt) so previews
 * don't land on the black/transparent fade-in most clips open on; photos go
 * through Coil as usual. Shows a spinner while a video frame is still being
 * decoded — large files can take a moment — instead of a silent blank box,
 * and a broken-image icon if decoding genuinely failed.
 */
@Composable
fun MediaPreview(
    item: MediaItem,
    contentScale: ContentScale,
    decodeSize: Int,
    modifier: Modifier = Modifier
) {
    if (item.isVideo) {
        val context = LocalContext.current
        val frameState = rememberVideoFrameState(context, item.uri)
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            when (frameState) {
                is VideoFrameState.Success -> Image(
                    bitmap = frameState.bitmap.asImageBitmap(),
                    contentDescription = item.displayName,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
                VideoFrameState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VideoFrameState.Failed -> Text(
                    "No preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
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
}
