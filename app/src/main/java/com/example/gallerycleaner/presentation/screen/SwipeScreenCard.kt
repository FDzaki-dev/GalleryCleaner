package com.example.gallerycleaner

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_THRESHOLD_PX = 380f
private const val MAX_ROTATION_DEG = 12f

@Composable
internal fun SwipeCard(
    item: MediaItem,
    enabled: Boolean,
    externalDecision: SwipeDecision?,
    hapticsEnabled: Boolean,
    onExternalDecisionHandled: () -> Unit,
    onZoomRequest: () -> Unit,
    onDecision: (SwipeDecision) -> Unit
) {
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun animateAndDecide(decision: SwipeDecision) {
        // Fired right as the fling starts, not after it finishes — the tick
        // should land with the flick of the wrist that caused it, not with
        // the animation settling ~180ms later.
        if (hapticsEnabled) {
            if (decision is SwipeDecision.Keep) hapticKeep(context) else hapticDelete(context)
        }
        val target = if (decision is SwipeDecision.Keep) 1600f else -1600f
        animate(offsetX, target, animationSpec = tween(180)) { value, _ -> offsetX = value }
        onDecision(decision)
    }

    LaunchedEffect(externalDecision) {
        val decision = externalDecision
        if (decision != null) {
            animateAndDecide(decision)
            onExternalDecisionHandled()
        }
    }

    val progress = (offsetX / SWIPE_THRESHOLD_PX).coerceIn(-1f, 1f)
    val rotation = progress * MAX_ROTATION_DEG
    val washColor = when {
        progress > 0 -> lerp(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), progress)
        progress < 0 -> lerp(Color.Transparent, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), -progress)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
            .graphicsLayer {
                translationX = offsetX
                rotationZ = rotation
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled) { onZoomRequest() }
            // Skipped entirely while disabled — otherwise a physical swipe
            // could kick off a second animateAndDecide() concurrently with
            // one already in flight from a button tap, racing on the same
            // offsetX and potentially double-triggering onDecision.
            .then(
                if (enabled) {
                    Modifier.pointerInput(item.id) {
                        detectDragGestures(
                            onDragEnd = {
                                val target = offsetX
                                when {
                                    target > SWIPE_THRESHOLD_PX -> scope.launch { animateAndDecide(SwipeDecision.Keep) }
                                    target < -SWIPE_THRESHOLD_PX -> scope.launch { animateAndDecide(SwipeDecision.Delete) }
                                    else -> scope.launch {
                                        // Spring instead of a flat tween: a
                                        // fixed 200ms slide-back feels the
                                        // same whether the card was dragged
                                        // 10px or 300px, which reads as
                                        // mechanical. A spring settles
                                        // proportionally to how far it has
                                        // to travel — small releases snap
                                        // back quick and light, bigger ones
                                        // get a touch more travel/bounce —
                                        // which is what a physically
                                        // "let go" card should feel like.
                                        animate(
                                            initialValue = offsetX,
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { value, _ -> offsetX = value }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        MediaPreview(
            item = item,
            contentScale = ContentScale.Crop,
            decodeSize = SWIPE_CARD_DECODE_SIZE,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(washColor)
            )
    }
}
@Composable
internal fun FullscreenViewer(item: MediaItem, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (item.mediaType == MediaType.VIDEO) {
                // Batch121 (user bug report): videos have been queryable via
                // MediaStore.Video.Media and shown with a play-badge thumbnail
                // since Batch40 (Audit Gap P0 #1), but tapping one to inspect
                // it landed here — which, before this batch, routed every
                // MediaType through the same AsyncImage call below. Coil's
                // VideoFrameDecoder (also Batch40) can decode ONE still frame
                // from a video URI for a thumbnail; it has no concept of
                // playback at all (no player, no controls). That gap is what
                // read as "can't play/inspect it at all". VideoPlayerSurface
                // replaces this path for video items only — the AsyncImage
                // branch for photos/GIFs below is untouched.
                VideoPlayerSurface(uri = item.uri, modifier = Modifier.fillMaxSize())
                // No whole-screen clickable-to-dismiss here, unlike the photo
                // branch below — PlayerView's own tap gesture toggles its
                // playback controls, and a dismiss-on-tap layered on top of
                // that would swallow every tap meant for the player instead.
                // An explicit close button is the safe alternative; same
                // icon/circle-badge visual language already used for the
                // grid's zoom-in affordance (SwipeScreenGrid.kt).
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            // Explicit cap instead of leaving size to be inferred from
                            // layout constraints — Coil normally reads the constraints
                            // of the composable it's measured in, but that inference
                            // can fall through to the source's original resolution in
                            // edge cases (e.g. certain Dialog/window-size combos).
                            // 2400px covers every phone display with headroom; nothing
                            // is gained decoding a 12,000px sensor photo past that,
                            // it's just wasted heap.
                            .size(2400)
                            .build(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Batch121: real video playback for the fullscreen/inspect viewer, via
 * Media3 ExoPlayer (androidx.media3:media3-exoplayer + media3-ui, added
 * app/build.gradle.kts this batch — see that file for the version-pin
 * reasoning). Before this batch, FullscreenViewer routed video items
 * through the same Coil AsyncImage call used for photos, which can only
 * decode a single still frame from a video URI — not play it.
 *
 * Why this doesn't crash on a large file the way the reported bug
 * implied something was: ExoPlayer reads a content:// URI as a stream
 * (ContentDataSource → extractor → MediaCodec), pulling and decoding
 * only the buffers it needs to stay a few seconds ahead of the
 * playhead — never the whole file into one in-memory buffer the way
 * e.g. reading a file fully into a ByteArray would. File size on disk
 * doesn't translate into a proportional memory spike.
 *
 * The player is created with remember(uri) and released from
 * DisposableEffect's onDispose — this composable only exists while the
 * fullscreen Dialog is showing, so leaving it (close button, back
 * press, or the caller flipping showFullscreen off) always tears the
 * player down instead of leaking it.
 */
@Composable
private fun VideoPlayerSurface(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var playbackError by remember(uri) { mutableStateOf(false) }
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3MediaItem.fromUri(uri))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            // Playback failures (unsupported codec, corrupt file, etc.)
            // surface through this callback, not an exception PlayerView
            // itself throws — without handling it, a failed video would
            // just sit there as a black frame with working-looking
            // controls and no feedback, its own kind of dead end.
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx -> PlayerView(ctx).apply { useController = true } },
            // update (not just factory) assigns the player — factory only
            // ever runs once per PlayerView instance, so if `uri` (and thus
            // this remembered exoPlayer) ever changed without the whole
            // Dialog/composable being torn down and recreated, the view
            // would otherwise keep pointing at a released player.
            update = { view -> view.player = exoPlayer }
        )
        if (playbackError) {
            Text(
                "Can't play this video",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
internal fun FileInfoDialog(item: MediaItem, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Info") },
        text = {
            Column {
                FileInfoRow("Name", item.displayName)
                FileInfoRow("Album", item.bucketName)
                FileInfoRow("Date taken", dateFormat.format(Date(item.dateTakenMillis)))
                FileInfoRow("Size", formatBytes(item.sizeBytes))
                if (item.width > 0 && item.height > 0) {
                    FileInfoRow("Dimensions", "${item.width} × ${item.height}")
                }
                FileInfoRow("Path", item.relativePath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}


