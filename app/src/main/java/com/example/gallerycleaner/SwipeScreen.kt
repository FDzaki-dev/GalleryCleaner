package com.example.gallerycleaner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    LaunchedEffect(group.key) {
        index = progressStore.progressFlow(group.key).first().coerceIn(0, group.items.size)
        restored = true
    }

    // Quietly warm the image cache for the next couple of photos so the swipe
    // never has to wait on a fresh decode mid-gesture. Videos are decoded via
    // MediaMetadataRetriever (see VideoThumbnail.kt) rather than Coil, so
    // they're not part of this prefetch.
    LaunchedEffect(index, group.key) {
        val loader = context.imageLoader
        (index + 1..index + 2).forEach { i ->
            group.items.getOrNull(i)?.let { item ->
                if (!item.isVideo) {
                    loader.enqueue(
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(900)
                            .build()
                    )
                }
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

            LinearProgressIndicator(
                progress = { if (group.items.isEmpty()) 0f else index / group.items.size.toFloat() },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (index >= group.items.size) {
                    FinishedPanel(deletedCount = pendingDeletes.size, onDone = { finishAndExit() })
                } else {
                    val item = group.items[index]
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
                        item = item,
                        onDecision = { decision ->
                            if (decision is SwipeDecision.Delete) pendingDeletes.add(item)
                            lastDecision = item to decision
                            index += 1
                            scope.launch { progressStore.saveProgress(group.key, index) }
                        }
                    )
                }
            }

            if (index < group.items.size) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "← delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "keep →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishedPanel(deletedCount: Int, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("All done for this group", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "$deletedCount item(s) will move to Trash.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}

@Composable
private fun SwipeCard(item: MediaItem, onDecision: (SwipeDecision) -> Unit) {
    // Plain float state for the live drag — updated synchronously on every
    // pointer move with NO coroutine launch. Launching a coroutine per touch
    // event (the previous approach) is what caused the dragging lag; a simple
    // state write is essentially free and graphicsLayer reads it without
    // forcing a full recomposition.
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var thresholdCrossed by remember(item.id) { mutableStateOf(false) }

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
                            target > SWIPE_THRESHOLD_PX -> scope.launch {
                                animate(offsetX, 1600f, animationSpec = tween(180)) { value, _ -> offsetX = value }
                                onDecision(SwipeDecision.Keep)
                            }
                            target < -SWIPE_THRESHOLD_PX -> scope.launch {
                                animate(offsetX, -1600f, animationSpec = tween(180)) { value, _ -> offsetX = value }
                                onDecision(SwipeDecision.Delete)
                            }
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
            // the swipe review is seeing the whole photo/video, not a cropped
            // slice of it.
            contentScale = ContentScale.FillBounds,
            decodeSize = 1000,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(
                    "VIDEO",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
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
