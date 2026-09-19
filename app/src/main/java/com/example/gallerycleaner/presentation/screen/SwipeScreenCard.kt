package com.example.gallerycleaner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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
// [Batch125, superseded by Batch126 below — kept, not deleted, per project
// convention] Diagnosed `LocalContext.current` inside a Compose `Dialog` as
// a `ContextThemeWrapper`, not the Activity, and added findActivity() to
// unwrap it. That diagnosis for THAT symptom was correct, but Batch126
// removes the Dialog entirely (see FullscreenViewer below) — so as of this
// batch there is no ContextThemeWrapper here to unwrap in the first place.
// findActivity() is kept anyway: harmless, still correct (an Activity
// passed straight in resolves on the first branch), and cheap insurance
// against any future context-wrapping layer.
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Batch126 (user re-report — clipping AND rotation STILL broken after TWO
// rounds of fixes, Batch122 + Batch125, both applied from inside the
// Dialog code path): root cause traced to a documented Compose limitation,
// not another OEM/device quirk to chase — a Compose `Dialog` with
// `usePlatformDefaultWidth = false` opens its own separate `Window`, and
// public reports of the identical setup describe the exact symptom seen
// here: `WindowInsets` read as zero inside that dialog's own content
// regardless of `decorFitsSystemWindows`, "as if the Dialog is oblivious to
// the navigation bar" (public Compose issue tracker, same DialogProperties
// combo as this file used). Batch125's SideEffect+DialogWindowProvider
// reinforcement pushed on the window property from the outside, but the
// window property was never the missing piece — Compose's own insets
// plumbing not reliably reaching that window's content is. Separately,
// multiple public reports of this same DialogProperties combo also
// describe rotation-time breakage ("the ui breaks sometimes" on orientation
// change) — a second, independent Window on top of the Activity's own is
// an unnecessary extra moving part for something that must rotate reliably
// (this app's `configChanges`+SENSOR override, Batch122, lives on the
// Activity's window, not this one).
// Fix: remove Dialog/DialogProperties/DialogWindowProvider entirely.
// FullscreenViewer is now a plain full-screen overlay, composed directly
// into the SAME window as the rest of the app — one window, one set of
// insets, one orientation, nothing Dialog-specific left to fight. Both call
// sites (SwipeScreen.kt, SwipeScreenGrid.kt) already place this as a
// sibling to their own Scaffold rather than nested inside its content slot
// — SwipeScreenGrid.kt's zoom trigger is now `onZoomRequest` handed up to
// SwipeScreen.kt for exactly this reason (see that file's Batch126 comment)
// — so it measures against the full window size the same way the Dialog's
// separate window used to, minus that window's own bugs. MainActivity's
// window is NOT edge-to-edge (grep project-wide: `setDecorFitsSystemWindows`
// appeared nowhere before this batch, and nowhere for the Activity's own
// window now either) — same as every other screen in this app, none of
// which have ever needed manual systemBars padding — so the OS itself
// already keeps this content clear of the status/gesture-nav bars, exactly
// like it does for the rest of the app. No windowInsetsPadding needed here.
@Composable
internal fun FullscreenViewer(item: MediaItem, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    // [Batch134] Hoisted up from VideoPlayerSurface (was private/internal to
    // it, remember(uri)-keyed) so the SAME auto-hide state/timer that
    // already existed there (Batch127, 3s idle) can also drive the Close+
    // filename row below — one shared source of truth, not a second
    // independent timer. remember(item.uri) matches the exact key the
    // internal version used, same reset-per-video behavior as before.
    // Meaningless for the photo/GIF branch below, which never reads it.
    var controlsVisible by remember(item.uri) { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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
            VideoPlayerSurface(
                uri = item.uri,
                controlsVisible = controlsVisible,
                onControlsVisibleChange = { controlsVisible = it },
                modifier = Modifier.fillMaxSize()
            )
            // No whole-screen clickable-to-dismiss here, unlike the photo
            // branch below — VideoPlayerSurface's own AndroidView already
            // has a tap-to-toggle-controls clickable (Batch127, replacing
            // PlayerView's bundled controller and the tap gesture that used
            // to come with it), and a dismiss-on-tap layered on top of that
            // here would swallow every tap meant for toggling controls
            // instead. An explicit close button is the safe alternative; same
            // icon/circle-badge visual language already used for the
            // grid's zoom-in affordance (SwipeScreenGrid.kt).
            // [Batch129] user device-test feedback on Batch127's video
            // viewer — close button existed but nothing next to it ever
            // showed WHAT is playing, unlike FileInfoDialog (name/date/size)
            // already available for photos from this same Box's sibling
            // affordances elsewhere in the app. Genuinely missing, not a
            // styling call: added inline, same top-start row as Close so it
            // shares that button's existing tap target/scrim treatment
            // rather than introducing a second visual language.
            // [Batch134] Now wrapped in AnimatedVisibility on the hoisted
            // controlsVisible above instead of always shown — user request:
            // ALL video-player nav (this row + VideoControlBar, which
            // already reused this same state before this batch) must fade
            // out on idle, not just the bottom control bar.
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        item.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
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
 * fullscreen viewer is showing (a plain overlay since Batch126, was a
 * Dialog before that), so leaving it (close button, back press, or the
 * caller flipping showFullscreen off) always tears the player down
 * instead of leaking it.
 *
 * [Batch122, user bug report] Two fixes on top of Batch121:
 * 1. onPlayerError used to just flip a Boolean and show one hardcoded
 *    string, discarding the actual PlaybackException — impossible to tell
 *    "corrupt file" from "unsupported codec" from "stripped by R8" from
 *    that alone. Now shows error.errorCodeName + message, and hides the
 *    (non-functional, confusingly overlapping) PlayerView controller
 *    underneath instead of leaving it visible over the error text.
 * 2. Rotating the device did nothing because MainActivity requests no
 *    particular orientation and (before this batch) tore itself down on
 *    every rotation anyway (see AndroidManifest.xml Batch122 comment) —
 *    fatal combination for a video player. This composable now requests
 *    SCREEN_ORIENTATION_SENSOR for as long as it's on screen (restored to
 *    UNSPECIFIED on dispose), so turning the phone actually rotates the
 *    video to landscape, independent of the system's own auto-rotate
 *    toggle — the same override every mainstream video player does, and
 *    only possible without losing playback state now that the manifest
 *    change stops the Activity from recreating on that rotation.
 *
 * [Batch123, user re-report with the Batch122 error text now visible]
 * The Batch122 proguard-rules.pro guess (R8 stripping something Media3
 * needs) is now DISPROVEN, not just unconfirmed — if that were the cause,
 * MediaCodecVideoRenderer couldn't have been constructed at all. Instead
 * the user's screenshot shows a specific, well-formed failure:
 * "ERROR_CODE_DECODER_INIT_FAILED ... format=[3840, 2160, 59.9986 ...
 * video/avc ...], format_supported=NO_EXCEEDS_CAPABILITIES" — the file is
 * 4K@60fps H.264. That combination is a real device-hardware ceiling on
 * many phones (most AVC decoders top out around 4K@30; 4K@60 is commonly
 * HEVC/VP9-only in hardware) — MediaCodecUtil found an avc decoder, tried
 * it, and that decoder's own capability check rejected this exact
 * resolution/frame-rate. ExoPlayer.Builder(context) alone (this file's
 * previous code, Batch121) uses MediaCodecSelector.DEFAULT with decoder
 * fallback OFF — the moment the first-choice decoder rejects the format,
 * playback fails outright with no attempt at any other matching decoder,
 * even if one exists that could have handled it (e.g. a device's
 * secondary/software avc decoder). Root cause + fix confirmed against a
 * public report with the identical "format_supported=NO_EXCEEDS_CAPABILITIES"
 * signature (androidx/media#1311) resolved the same way: build the player
 * with DefaultRenderersFactory(context).setEnableDecoderFallback(true)
 * instead of the no-arg ExoPlayer.Builder(context), so a capability
 * rejection tries the next matching video/avc decoder instead of failing
 * immediately. Honest limit, stated up front: if this device genuinely
 * has no decoder — hardware or software — capable of 4K@60 avc, fallback
 * has nothing left to try and playback will still fail; that would be a
 * hardware ceiling no app-level code can route around. To make that case
 * distinguishable from an actually-fixable bug without another
 * screenshot round-trip, onPlayerError below now also shows a plain-
 * language hint specifically when errorCode is DECODER_INIT_FAILED and
 * the message contains EXCEEDS_CAPABILITIES — the raw diagnostic line
 * from Batch122 (errorCodeName + message) is kept as-is underneath it,
 * nothing removed.
 */
@Composable
private fun VideoPlayerSurface(
    uri: Uri,
    // [Batch134] Hoisted from an internal remember(uri) here up to
    // FullscreenViewer, which also has UI (the Close/filename row) that
    // needs to react to the same auto-hide timer — see that function's
    // Batch134 comment. Everything below that reads/writes controlsVisible
    // is otherwise unchanged from before this batch.
    controlsVisible: Boolean,
    onControlsVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // [Batch129] Constructed locally rather than threaded down as a
    // parameter from SwipeScreen.kt/SwipeScreenGrid.kt (both current
    // FullscreenViewer call sites) — this is the ONLY place in this file a
    // setting is read, and SettingsStore's DataStore-backed flows are cheap/
    // safe to collect straight from a Composable (no ViewModel scoping
    // needed for a single boolean read). Threading a parameter through 2
    // extra files for 1 boolean would widen this batch's blast radius for
    // no real benefit — see SettingsStore.kt's videoSoundEnabledFlow doc.
    val settingsStore = remember { SettingsStore(context) }
    // [Batch132] initial=true, not false — this is the actual regression
    // fix. ExoPlayer.Builder().build().apply{} below reads
    // videoSoundEnabled on the VERY FIRST composition, before the real
    // DataStore value has had a chance to arrive; `initial=false` meant
    // brand-new/cold-started playback briefly (and on a slow disk, not
    // so briefly) built the player MUTED even for users whose real,
    // saved preference is sound-on. See SettingsStore.kt's
    // videoSoundEnabledFlow doc for the full regression writeup.
    val videoSoundEnabled by settingsStore.videoSoundEnabledFlow.collectAsState(initial = true)
    var playbackError by remember(uri) { mutableStateOf(false) }
    // errorDetail is independent of `uri` on purpose — it's UI-only text
    // derived from playbackError, no need to reset/rebuild it per item.
    var errorDetail by remember { mutableStateOf("") }
    // Batch123: plain-language line shown only for the specific
    // "decoder exists but rejects this resolution/frame-rate" failure —
    // errorDetail above is untouched/still raw for triage, this is additive.
    var errorHint by remember { mutableStateOf("") }
    // Batch127 (user request — custom controls instead of media3-ui's
    // bundled PlayerView controller, which only exposes what its own XML
    // layout/attrs allow to tweak): PlayerView below now has
    // useController = false, and everything under this comment drives a
    // fully custom Compose control bar instead. isPlaying/isBuffering/
    // durationMs all mirror ExoPlayer's own Player.Listener callbacks
    // (single source of truth, pushed — not polled). positionMs is the one
    // value ExoPlayer doesn't push change events for on its own, so it's
    // polled on a fixed tick below, only while this composable is part of
    // the composition.
    var isPlaying by remember(uri) { mutableStateOf(true) }
    // True until the first onPlaybackStateChanged callback — matches
    // reality (prepare()+playWhenReady=true, called just below, always
    // buffers at least briefly before the first frame).
    var isBuffering by remember(uri) { mutableStateOf(true) }
    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    // True while the user has a finger on the seek bar's thumb — the
    // polling loop below skips writing positionMs while this is true, so
    // the drag gesture and the poll tick don't fight over the same value
    // and make the thumb stutter/jump under the finger.
    var isSeeking by remember(uri) { mutableStateOf(false) }
    // Batch123: decoder fallback ON (see doc comment above) — lets ExoPlayer
    // try the next matching video/avc decoder if the first one rejects the
    // format as exceeding its capabilities, instead of failing immediately.
    val exoPlayer = remember(uri) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setMediaItem(Media3MediaItem.fromUri(uri))
            volume = if (videoSoundEnabled) 1f else 0f
            playWhenReady = true
            prepare()
        }
    }
    // [Batch133] In-player mute button (VideoControlBar) — null means "no
    // manual override yet, keep following the Settings default", exactly
    // the behavior that existed before this batch. A non-null value means
    // the user tapped mute/unmute on THIS video's control bar, which wins
    // over the Settings value until a different uri opens (remember(uri)
    // below resets it back to null, i.e. back to following Settings, per
    // video — a quick in-context override rather than a permanent change,
    // same as the mute button in any basic video player). This is purely
    // additive on top of Batch132's fix above — that fix is about what
    // collectAsState(initial=...) resolves to on first read, this is a
    // separate layer that only ever reacts to the resolved value, never
    // re-reads DataStore itself.
    var manualMuteOverride by remember(uri) { mutableStateOf<Boolean?>(null) }
    val isMuted = manualMuteOverride ?: !videoSoundEnabled
    // [Batch136] user report: the right-half drag gesture (Batch134) was
    // "cosmetic" — it only fed exoPlayer.volume, an in-app gain on this
    // one player instance, and never touched the device's actual master
    // (STREAM_MUSIC) volume, so it had zero effect outside this player
    // and didn't persist like a real volume control should.
    // Fix: mirror brightnessLevel further down this same composable — a
    // real, physical/device setting, not a per-video one — instead of a
    // fake per-video gain.
    // Plain remember{} (no uri key) on purpose: like brightness, this must
    // NOT reset when swiping to a new video, since that would forcibly
    // reset the user's actual system volume on every video switch.
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolumeSteps = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var manualVolume by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                .toFloat() / maxVolumeSteps
        )
    }
    // exoPlayer plays at full gain (or 0 when muted) — real loudness now
    // comes from the actual system stream, set by the gesture below, the
    // same as any standard video player. Separate from the remember(uri)
    // block above on purpose: that block only re-runs when `uri` changes,
    // so it alone would miss the Settings default being flipped while a
    // video is already open/prepared, or the mute button being tapped.
    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            // Batch127: pushed state for the custom control bar below —
            // added to this SAME listener object rather than a second one,
            // so there's still exactly one addListener/removeListener pair
            // for this player.
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            // Playback failures (unsupported codec, corrupt file, etc.)
            // surface through this callback, not an exception PlayerView
            // itself throws — without handling it, a failed video would
            // just sit there as a black frame with working-looking
            // controls and no feedback, its own kind of dead end.
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                // errorCodeName is a stable string like
                // "ERROR_CODE_IO_FILE_NOT_FOUND"/"ERROR_CODE_DECODER_INIT_FAILED"
                // — the detail that was completely discarded before this
                // batch, and the difference between a guess and a diagnosis
                // if this still fails after the Batch122 proguard-rules.pro
                // mitigation.
                errorDetail = "${error.errorCodeName}: ${error.message ?: "nggak ada detail lain"}"
                // Batch123: this specific errorCode+message pairing means a
                // matching decoder exists but every one tried rejects this
                // exact resolution/frame-rate as beyond what it can init —
                // a device-hardware ceiling (e.g. 4K@60), not a generic
                // "corrupt file"/"unsupported container" failure. Told apart
                // from other DECODER_INIT_FAILED causes by checking for the
                // EXCEEDS_CAPABILITIES substring too, not errorCode alone.
                errorHint = if (
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED &&
                    error.message?.contains("EXCEEDS_CAPABILITIES") == true
                ) {
                    "Hardware hp ini nggak sanggup nge-decode kombinasi resolusi/frame-rate video ini."
                } else {
                    ""
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    // Batch127: positionMs is the one piece of playback state ExoPlayer has
    // no change-callback for — polled on a fixed tick instead. Plain
    // LaunchedEffect(exoPlayer), same cancellation guarantee the player
    // release itself relies on above: leaving composition cancels this
    // coroutine automatically, no separate cleanup needed.
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isSeeking) positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(300)
        }
    }
    // Batch127: auto-hide, same convention as every mainstream video player
    // — but never while paused/buffering/erroring, where the controls are
    // the only way to resume/seek/close at all.
    LaunchedEffect(controlsVisible, isPlaying, isBuffering, playbackError) {
        if (controlsVisible && isPlaying && !isBuffering && !playbackError) {
            delay(3000)
            onControlsVisibleChange(false)
        }
    }
    // Batch122: allow this screen specifically to rotate to landscape for
    // as long as a video is open, regardless of the system's own
    // auto-rotate toggle (same override every mainstream video player
    // does) — restored to the app's normal unspecified/no-lock behavior
    // the moment this composable leaves composition. Requires
    // AndroidManifest.xml's MainActivity configChanges (Batch122) to not
    // destroy the Activity — and the player along with it — the instant
    // the OS honors this and actually rotates the display.
    // Batch125 found `context as? Activity` always null here (Dialog's
    // ContextThemeWrapper), fixed with findActivity(). Batch126 removed
    // that Dialog entirely (see FullscreenViewer's Batch126 comment) — this
    // is a direct Activity context now, no wrapper left to unwrap — but
    // findActivity() stays, harmless and still correct either way.
    val activity = context.findActivity()
    // [Batch133] Split in two: this half now only captures+restores the
    // orientation that existed before the video viewer ever touched it.
    // remember{} (no key) runs during composition, before any effect body
    // — same timing the old `val previousOrientation = activity?.
    // requestedOrientation` line had inside DisposableEffect(Unit), so this
    // still captures the true pre-viewer value exactly once, not once per
    // video switched to within an already-open viewer.
    val previousOrientation = remember { activity?.requestedOrientation }
    // [Batch134] Left-half vertical-drag gesture below controls this same
    // Activity's window brightness override — a physical-display setting,
    // not a per-video one, so it deliberately does NOT use remember(uri)
    // like most state in this composable: it persists across switching to
    // a different video within one fullscreen-viewer session, and only
    // resets (in the SAME onDispose below that already restores
    // orientation) when the viewer itself closes.
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness
                ?.takeIf { it in 0f..1f } ?: 0.5f
        )
    }
    LaunchedEffect(activity, brightnessLevel) {
        val window = activity?.window ?: return@LaunchedEffect
        val params = window.attributes
        params.screenBrightness = brightnessLevel
        window.attributes = params
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // [Batch134] Brightness override above restored to system
            // default (WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            // -1f) the same moment orientation is — not left stuck at the
            // last dragged value after the viewer closes.
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    // [Batch133] New rotation-mode button in VideoControlBar. Batch122's
    // SENSOR-while-open default (free rotation by physically turning the
    // phone, independent of the system's own auto-rotate toggle) is exactly
    // what still runs when this is false — the branch below is unchanged
    // from before. Tapping the new icon forces SENSOR_LANDSCAPE instead:
    // landscape without physically turning the phone, the same override
    // every basic video player's fullscreen/rotate button does (still
    // sensor-driven within the landscape pair, so normal vs
    // reverse-landscape still follows however the phone happens to be
    // held). remember(uri), matching every other per-item state in this
    // composable, resets this back to auto/SENSOR whenever a different
    // video opens rather than carrying a manual landscape lock over
    // between videos.
    var manualLandscape by remember(uri) { mutableStateOf(false) }
    LaunchedEffect(manualLandscape) {
        activity?.requestedOrientation = if (manualLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }
    Box(modifier = modifier) {
        if (!playbackError) {
            AndroidView(
                // [Batch135] Tap-to-toggle used to live here as a .clickable
                // — removed because it was DEAD CODE since Batch134: the new
                // gesture-zone Row below is declared after this AndroidView
                // (drawn on top) and covers the exact same area, and per
                // Compose's documented hit-testing, only the highest-z-index
                // composable in a hit region ever receives pointer events —
                // siblings underneath (this AndroidView included) get NONE
                // for that touch, consumed or not. That's the root cause of
                // the Batch134 bug report ("nav won't come back after
                // fadeout, only exiting/re-entering the video fixes it") —
                // tapping here silently never reached this clickable at all.
                // Tap-to-toggle is now handled directly in the gesture-zone
                // Boxes themselves (see their Batch135 comment below).
                modifier = Modifier.matchParentSize(),
                factory = { ctx -> PlayerView(ctx).apply { useController = false } },
                // update (not just factory) assigns the player — factory only
                // ever runs once per PlayerView instance, so if `uri` (and thus
                // this remembered exoPlayer) ever changed without the whole
                // viewer/composable being torn down and recreated, the view
                // would otherwise keep pointing at a released player.
                update = { view -> view.player = exoPlayer }
            )
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            // [Batch135 hotfix] `currentControlsVisible` — Batch134's gesture
            // zones below read `controlsVisible` inside detectTapGestures'
            // onTap, but that pointerInput coroutine is keyed on `uri`, not
            // on `controlsVisible` itself, so it's long-lived across every
            // show/hide toggle and every 3s auto-hide — reading the raw
            // parameter there would close over whatever value was current
            // the moment the coroutine launched and never see it change
            // again (classic Compose stale-closure gotcha). rememberUpdatedState
            // gives a stable holder that always reads the latest value even
            // from inside that same never-restarted coroutine.
            val currentControlsVisible by rememberUpdatedState(controlsVisible)
            // [Batch134, hit-testing bug fixed Batch135] YouTube/ReVanced-
            // style gesture: vertical drag on the left half of the video
            // surface = brightness, right half = volume. Two sibling Boxes
            // layered ABOVE the AndroidView in this same Box (declared
            // after it, so Compose hit-tests them first) rather than one
            // full-width zone with internal math — a hard 50/50 split with
            // zero extra per-event branching.
            // [Batch135] Batch134's comment here claimed a plain tap would
            // fall through unconsumed to the AndroidView's clickable below
            // — WRONG, confirmed against Compose's own docs (Event
            // dispatching and hit-testing, developer.android.com/develop/
            // ui/compose/touch-input/pointer-input/understand-gestures):
            // "when there are multiple eligible composables on the same
            // level of the tree, only the composable with the highest
            // z-index is hit... Composables that are not in the chain
            // never receive pointer events, even when the pointer is
            // inside of their bounds." Once these zones exist on top, the
            // AndroidView below gets NOTHING for that touch, tap or drag,
            // consumed or not — that's why nav stopped coming back after
            // fadeout (the old clickable, now removed above, was silently
            // unreachable dead code). Fix: tap-to-toggle now lives directly
            // on these same zones via a SECOND, separate .pointerInput —
            // exactly the pattern the same official doc recommends for
            // combining a tap detector and a drag detector on one
            // composable ("use separate pointerInput modifier instances
            // instead" — a single detectTapGestures call blocks forever
            // and a second detector chained after it in the SAME
            // pointerInput block would never run).
            // Always active (not gated on controlsVisible), same as every
            // mainstream player's gesture zones — dimmed nav shouldn't
            // disable the gesture itself.
            Row(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(uri) {
                            detectTapGestures(
                                onTap = { onControlsVisibleChange(!currentControlsVisible) }
                            )
                        }
                        .pointerInput(uri) {
                            detectVerticalDragGestures(
                                onDragStart = { showBrightnessIndicator = true },
                                onDragEnd = { showBrightnessIndicator = false },
                                onDragCancel = { showBrightnessIndicator = false },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Full container height dragged = full
                                    // 0f..1f range; negated because Compose
                                    // Y grows downward but dragging UP
                                    // should INCREASE brightness.
                                    val delta = -dragAmount / size.height.toFloat()
                                    brightnessLevel = (brightnessLevel + delta).coerceIn(0f, 1f)
                                }
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(uri) {
                            detectTapGestures(
                                onTap = { onControlsVisibleChange(!currentControlsVisible) }
                            )
                        }
                        .pointerInput(uri) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    showVolumeIndicator = true
                                    // Starting a volume gesture is an
                                    // explicit signal the user wants audio
                                    // control — same quick, session-only
                                    // override the mute button already does
                                    // (Batch133), not written to DataStore.
                                    manualMuteOverride = false
                                },
                                onDragEnd = { showVolumeIndicator = false },
                                onDragCancel = { showVolumeIndicator = false },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / size.height.toFloat()
                                    val newLevel = (manualVolume + delta).coerceIn(0f, 1f)
                                    manualVolume = newLevel
                                    // [Batch136] Actually move the device's
                                    // master (STREAM_MUSIC) volume — flags=0
                                    // so the OS's own volume UI doesn't pop
                                    // up on top of this app's own
                                    // GestureLevelIndicator below.
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (newLevel * maxVolumeSteps).roundToInt()
                                            .coerceIn(0, maxVolumeSteps),
                                        0
                                    )
                                }
                            )
                        }
                )
            }
            if (showBrightnessIndicator) {
                GestureLevelIndicator(
                    // [Batch135] user report: with no label, brightness vs
                    // volume was ambiguous while dragging — bar+percent
                    // alone didn't say which one was being adjusted.
                    label = "Kecerahan",
                    level = brightnessLevel,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                )
            }
            if (showVolumeIndicator) {
                GestureLevelIndicator(
                    label = "Volume",
                    level = if (isMuted) 0f else manualVolume,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                )
            }
            // [Batch134] AnimatedVisibility instead of a plain `if` — user
            // request: nav must FADE out on idle, not just disappear/appear
            // abruptly the way it did before this batch.
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                VideoControlBar(
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isMuted = isMuted,
                    onToggleMute = { manualMuteOverride = !isMuted },
                    isLandscapeMode = manualLandscape,
                    onToggleRotation = { manualLandscape = !manualLandscape },
                    onPlayPause = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            // Replay from the start instead of a no-op tap
                            // on Play once the video has actually finished
                            // — same reasoning as the rest of this file:
                            // no dead-end controls.
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.play()
                        }
                    },
                    onRewind = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                    },
                    onForward = {
                        exoPlayer.seekTo(
                            (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration.coerceAtLeast(0))
                        )
                    },
                    onSeekChange = { isSeeking = true; positionMs = it },
                    onSeekFinished = { exoPlayer.seekTo(positionMs); isSeeking = false }
                )
            }
        } else {
            // Batch122 found the bundled PlayerView controller (rewind/
            // play/ff, scrubber) staying visible and overlapping the error
            // text underneath it, and fixed it by swapping the whole
            // AndroidView out for the error text once playbackError is
            // true. Batch127 replaced that bundled controller with the
            // custom one above, gated on the same `!playbackError` branch
            // — so that specific overlap can't recur structurally anymore
            // — but the swap itself stays: a player that failed to decode
            // has nothing useful behind the error text (dead/black surface
            // at best), and hiding it removes any stray-tap surface too.
            // Batch126: this branch has no element covering the full area
            // (just a centered Column) — while FullscreenViewer was a
            // Dialog, that didn't matter, its own separate Window caught
            // every tap regardless. Now that it's a plain overlay in the
            // same window (see FullscreenViewer's Batch126 comment), a tap
            // landing outside the Column's bounds could fall through to
            // the SwipeCard/grid underneath. No-op clickable, indication
            // off (it's an invisible full-size layer, a ripple here would
            // just look like a stray flash) — swallows it instead.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            )
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Video ini nggak bisa diputar",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    errorDetail,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                // Batch123: additive, not a replacement — errorDetail above
                // still shows the raw errorCodeName+message untouched.
                if (errorHint.isNotEmpty()) {
                    Text(
                        errorHint,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Batch134: shared visual for the brightness (left) and volume (right)
 * gesture indicators above — a rounded pill with a proportional fill bar
 * plus a percentage label. Deliberately icon-free: this file has a
 * documented history (Batch110/113/128) of CI failures from guessed
 * Material icon names, and every icon already used here (mute/rotate/
 * playback) was individually verified before being added — a text+bar
 * indicator needs no new icon glyph at all, so that whole risk class
 * simply doesn't apply to this batch.
 * [Batch135] `label` added — user report: bar+percent alone didn't say
 * which control (brightness vs volume) was being dragged. Plain text,
 * same reasoning as the rest of this indicator: 0 new icon risk.
 */
@Composable
private fun GestureLevelIndicator(label: String, level: Float, modifier: Modifier = Modifier) {
    val clamped = level.coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.3f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clamped)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "${(clamped * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Batch127: custom playback control bar, replacing the media3-ui bundled
 * PlayerView controller (user request — a controller built from Compose
 * primitives here can be extended/restyled freely, unlike the bundled one
 * which only exposes what its XML layout/attrs allow). Deliberately plain
 * (white on a black gradient scrim, MaterialTheme typography) rather than
 * branched per AppTheme/MaterialStyle — the rest of FullscreenViewer (close
 * button, error text) has never branched on theme either, so this matches
 * existing precedent instead of introducing a new one unasked.
 */
@Composable
private fun VideoControlBar(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    // [Batch133] Mute + rotation-mode buttons — see VideoPlayerSurface's
    // Batch133 comments for how isMuted/isLandscapeMode are derived and
    // what onToggleMute/onToggleRotation do to that state.
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isLandscapeMode: Boolean,
    onToggleRotation: () -> Unit,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp, top = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatPlaybackTime(positionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(40.dp)
            )
            Slider(
                value = positionMs.toFloat(),
                // coerceAtLeast avoids a zero-width 0f..0f range (Slider
                // requires start < end) during the brief window before
                // onPlaybackStateChanged(STATE_READY) reports the real
                // duration.
                valueRange = 0f..durationMs.coerceAtLeast(1000L).toFloat(),
                onValueChange = { onSeekChange(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                formatPlaybackTime(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(40.dp)
            )
        }
        // [Batch133] SpaceBetween instead of the old Center — mute (start)
        // and rotation-mode (end) are new, transport controls stay in a
        // nested Row so they keep grouping/centering as a unit between the
        // two. Three-zone layout (mute — transport — rotate) requested by
        // user as "like basic apps" — NOT cross-checked against Sponge's
        // own video player specifically (ROADMAP.md's Sponge notes cover
        // Settings/Cleaning-Options/Home-dashboard gaps, not its video
        // player's control-bar layout), so this is a generic basic-video-
        // player convention, not a Sponge-matching claim.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleMute) {
                Icon(
                    if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Nyalain suara" else "Bisukan",
                    tint = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRewind) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Mundur 10 detik", tint = Color.White)
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onForward) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Maju 10 detik", tint = Color.White)
                }
            }
            IconButton(onClick = onToggleRotation) {
                Icon(
                    if (isLandscapeMode) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                    contentDescription = if (isLandscapeMode) "Balik ke rotasi otomatis" else "Putar ke landscape",
                    tint = Color.White
                )
            }
        }
    }
}

/** Batch127: `m:ss` — matches the timestamp style already used everywhere
 *  else time is shown to the user in this app (plain digits, no locale-
 *  sensitive date library needed for a duration this short). */
private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
internal fun FileInfoDialog(item: MediaItem, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Info File") },
        text = {
            Column {
                FileInfoRow("Nama", item.displayName)
                FileInfoRow("Album", item.bucketName)
                FileInfoRow("Tanggal diambil", dateFormat.format(Date(item.dateTakenMillis)))
                FileInfoRow("Ukuran", formatBytes(item.sizeBytes))
                if (item.width > 0 && item.height > 0) {
                    FileInfoRow("Dimensi", "${item.width} × ${item.height}")
                }
                FileInfoRow("Lokasi", item.relativePath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Oke") }
        }
    )
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.width(112.dp).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}


