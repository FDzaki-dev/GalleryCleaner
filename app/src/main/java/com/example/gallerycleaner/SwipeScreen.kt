package com.example.gallerycleaner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_THRESHOLD_PX = 380f
private const val MAX_ROTATION_DEG = 12f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    group: MediaGroup,
    progressStore: ProgressStore,
    onBack: () -> Unit,
    onFinishWithDeletions: (List<MediaItem>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var index by remember(group.key) { mutableIntStateOf(0) }
    val pendingDeletes = remember(group.key) { mutableStateListOf<MediaItem>() }
    var restored by remember(group.key) { mutableStateOf(false) }
    // Single-level undo — remembers only the most recent decision, which
    // covers the common "oops, wrong direction" case without the complexity
    // (and memory) of a full history stack.
    var lastDecision by remember(group.key) { mutableStateOf<Pair<MediaItem, SwipeDecision>?>(null) }
    // Set by the bottom action buttons; SwipeCard watches this and plays the
    // same fly-off animation a drag gesture would trigger.
    var buttonDecision by remember(group.key) { mutableStateOf<SwipeDecision?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(group.key) {
        index = progressStore.progressFlow(group.key).first().coerceIn(0, group.items.size)
        restored = true
    }

    // Quietly warm the image cache for the next couple of photos so the swipe
    // never has to wait on a fresh decode mid-gesture.
    LaunchedEffect(index, group.key) {
        val loader = context.imageLoader
        (index + 1..index + 2).forEach { i ->
            group.items.getOrNull(i)?.let { item ->
                loader.enqueue(
                    ImageRequest.Builder(context)
                        .data(item.uri)
                        .size(900)
                        .build()
                )
            }
        }
    }

    fun finishAndExit() {
        scope.launch { progressStore.saveProgress(group.key, index) }
        onFinishWithDeletions(pendingDeletes.toList())
        onBack()
    }

    // Makes sure the back gesture/button goes through the same save-and-exit
    // path as the toolbar's back arrow, instead of just closing the app.
    BackHandler { finishAndExit() }

    val currentItem = group.items.getOrNull(index)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.key) },
                navigationIcon = {
                    IconButton(onClick = { finishAndExit() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentItem != null) {
                        IconButton(onClick = { showInfo = true }) {
                            Text("ⓘ", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (lastDecision != null) {
                        IconButton(onClick = {
                            val (item, decision) = lastDecision!!
                            if (decision is SwipeDecision.Delete) pendingDeletes.remove(item)
                            index = (index - 1).coerceAtLeast(0)
                            lastDecision = null
                            scope.launch { progressStore.saveProgress(group.key, index) }
                        }) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo last swipe")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!restored) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Column
            }

            Filmstrip(
                items = group.items,
                currentIndex = index,
                onSelect = { tapped ->
                    // Manual tap on a thumbnail jumps straight to that photo,
                    // same as if the user had swiped/skipped their way there.
                    index = tapped
                    scope.launch { progressStore.saveProgress(group.key, index) }
                }
            )

            if (currentItem != null) {
                InfoBar(item = currentItem, position = index + 1, total = group.items.size)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (currentItem == null) {
                    FinishedPanel(
                        deletedCount = pendingDeletes.size,
                        reviewedCount = group.items.size,
                        onDone = { finishAndExit() }
                    )
                } else {
                    // A second card peeking behind the top one — makes the stack read as
                    // a deck rather than a single flat image, at near-zero extra cost
                    // since it's just a static, non-animated background layer.
                    group.items.getOrNull(index + 1)?.let { nextItem ->
                        MediaPreview(
                            item = nextItem,
                            contentScale = ContentScale.FillBounds,
                            decodeSize = 300,
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .aspectRatio(1f)
                                .graphicsLayer { scaleX = 0.96f; scaleY = 0.96f; alpha = 0.7f }
                        )
                    }
                    SwipeCard(
                        item = currentItem,
                        externalDecision = buttonDecision,
                        onExternalDecisionHandled = { buttonDecision = null },
                        onZoomRequest = { showFullscreen = true },
                        onDecision = { decision ->
                            if (decision is SwipeDecision.Delete) pendingDeletes.add(currentItem)
                            lastDecision = currentItem to decision
                            index += 1
                            scope.launch { progressStore.saveProgress(group.key, index) }
                        }
                    )
                }
            }

            if (currentItem != null) {
                ActionButtonRow(
                    onDelete = { buttonDecision = SwipeDecision.Delete },
                    onSkip = {
                        // Skip moves on without judging the item either way —
                        // it's neither kept-via-swipe nor deleted, just passed
                        // over. No fly-off animation, just an instant advance.
                        lastDecision = currentItem to SwipeDecision.Keep
                        index += 1
                        scope.launch { progressStore.saveProgress(group.key, index) }
                    },
                    onKeep = { buttonDecision = SwipeDecision.Keep }
                )
            }
        }
    }

    if (showFullscreen && currentItem != null) {
        FullscreenViewer(item = currentItem, onDismiss = { showFullscreen = false })
    }
    if (showInfo && currentItem != null) {
        FileInfoDialog(item = currentItem, onDismiss = { showInfo = false })
    }
}

/** Small horizontal strip of upcoming/reviewed thumbnails, auto-scrolling to
 *  keep the current item in view. Tapping any thumbnail jumps straight to
 *  that photo — a manual alternative to swiping through one by one. */
@Composable
private fun Filmstrip(items: List<MediaItem>, currentIndex: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items.size) { i ->
            val item = items[i]
            val isCurrent = i == currentIndex
            val isReviewed = i < currentIndex
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isCurrent) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier
                    )
                    .padding(if (isCurrent) 2.dp else 0.dp)
                    // Whole thumbnail is the tap target — jumps directly to
                    // that item instead of only allowing sequential swipes.
                    .clickable(onClick = { onSelect(i) })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    MediaPreview(
                        item = item,
                        // Fit (not Crop/FillBounds) preserves the photo's own
                        // aspect ratio so thumbnails never look stretched or
                        // sliced — same proportions as the original file,
                        // just scaled down to the row's size.
                        contentScale = ContentScale.Fit,
                        decodeSize = 100,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isReviewed) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Compact strip of file facts — format, size, position — plus the info-dialog trigger. */
@Composable
private fun InfoBar(item: MediaItem, position: Int, total: Int) {
    val format = item.displayName.substringAfterLast('.', "").uppercase().ifEmpty { "?" }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfoChip(formatBytes(item.sizeBytes))
        InfoChip(format)
        InfoChip("$position/$total")
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** Delete / Skip / Keep — an explicit, discoverable alternative to swiping,
 *  since not everyone realizes (or wants to rely on) drag gestures. */
@Composable
private fun ActionButtonRow(
    onDelete: () -> Unit,
    onSkip: () -> Unit,
    onKeep: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundActionButton(
            symbol = "✕",
            background = MaterialTheme.colorScheme.secondary,
            symbolColor = Color(0xFF1A0E0C),
            size = 64.dp,
            onClick = onDelete
        )
        RoundActionButton(
            symbol = "⏭",
            background = MaterialTheme.colorScheme.surfaceVariant,
            symbolColor = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 48.dp,
            onClick = onSkip
        )
        RoundActionButton(
            symbol = "✓",
            background = MaterialTheme.colorScheme.primary,
            symbolColor = Color(0xFF0F1113),
            size = 64.dp,
            onClick = onKeep
        )
    }
}

@Composable
private fun RoundActionButton(
    symbol: String,
    background: Color,
    symbolColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = symbolColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FinishedPanel(deletedCount: Int, reviewedCount: Int, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Mission accomplished!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                StatColumn("Items reviewed", "$reviewedCount")
                Spacer(Modifier.width(32.dp))
                StatColumn("Moved to Trash", "$deletedCount")
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text("Continue", modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwipeCard(
    item: MediaItem,
    externalDecision: SwipeDecision?,
    onExternalDecisionHandled: () -> Unit,
    onZoomRequest: () -> Unit,
    onDecision: (SwipeDecision) -> Unit
) {
    // Plain float state for the live drag — updated synchronously on every
    // pointer move with NO coroutine launch. Launching a coroutine per touch
    // event (an earlier approach) is what caused dragging lag; a simple
    // state write is essentially free and graphicsLayer reads it without
    // forcing a full recomposition.
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var thresholdCrossed by remember(item.id) { mutableStateOf(false) }

    suspend fun animateAndDecide(decision: SwipeDecision) {
        val target = if (decision is SwipeDecision.Keep) 1600f else -1600f
        animate(offsetX, target, animationSpec = tween(180)) { value, _ -> offsetX = value }
        onDecision(decision)
    }

    // Bottom buttons trigger this the same way a completed drag would.
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
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragEnd = {
                        val target = offsetX
                        // Only the release animation needs a coroutine — a single
                        // launch per gesture, not per pixel of movement.
                        when {
                            target > SWIPE_THRESHOLD_PX -> scope.launch { animateAndDecide(SwipeDecision.Keep) }
                            target < -SWIPE_THRESHOLD_PX -> scope.launch { animateAndDecide(SwipeDecision.Delete) }
                            else -> scope.launch {
                                animate(offsetX, 0f, animationSpec = tween(200)) { value, _ -> offsetX = value }
                            }
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    val crossed = abs(offsetX) > SWIPE_THRESHOLD_PX
                    if (crossed && !thresholdCrossed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    thresholdCrossed = crossed
                }
            },
        contentAlignment = Alignment.Center
    ) {
        MediaPreview(
            item = item,
            // Stretched to fill the square rather than cropped — the point of
            // the swipe review is seeing the whole photo, not a cropped
            // slice of it.
            contentScale = ContentScale.FillBounds,
            decodeSize = 1000,
            modifier = Modifier.fillMaxSize()
        )

        // Zoom affordance — a tap here opens the fullscreen viewer. Kept as
        // its own small clickable target (rather than making the whole card
        // tappable) so it never competes with the drag gesture.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onZoomRequest),
            contentAlignment = Alignment.Center
        ) {
            Text("⤢", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        // Color wash overlay — a simple repaint, no extra layout/measure cost.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = washColor)
        }

        if (abs(progress) > 0.12f) {
            val label = if (progress > 0) "KEEP" else "DELETE"
            val labelColor = if (progress > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            Text(
                label,
                color = labelColor,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(if (progress > 0) Alignment.TopStart else Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/** Full-screen pinch-to-zoom / pan viewer, opened by tapping the zoom affordance on a card. */
@Composable
private fun FullscreenViewer(item: MediaItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(item.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale <= 1f) Offset.Zero else offset + pan
                    }
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .size(1600)
                    .build(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Detailed file metadata — name, size, dimensions, location, dates. */
@Composable
private fun FileInfoDialog(item: MediaItem, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("File info") },
        text = {
            Column {
                InfoRow("Name", item.displayName)
                InfoRow("Size", formatBytes(item.sizeBytes))
                if (item.width > 0 && item.height > 0) {
                    InfoRow("Dimensions", "${item.width} × ${item.height}")
                }
                InfoRow("Album", item.bucketName)
                if (item.relativePath.isNotBlank()) {
                    InfoRow("Path", item.relativePath)
                }
                if (item.dateTakenMillis > 0) {
                    InfoRow("Taken", dateFormat.format(Date(item.dateTakenMillis)))
                }
                if (item.dateModifiedMillis > 0) {
                    InfoRow("Modified", dateFormat.format(Date(item.dateModifiedMillis)))
                }
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
