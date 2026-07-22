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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewCarousel
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

// Shared between the prefetch pass above and SwipeCard's own MediaPreview
// call below — both MUST request the same decode size, since Coil's cache
// key includes it. A mismatch here means prefetching does nothing useful.
private const val SWIPE_CARD_DECODE_SIZE = 600

/** Two ways to review the same folder: one photo at a time (Swipe), or
 *  several at once via checkboxes in a grid (Grid) — see the view-mode
 *  toggle in SwipeScreen's top bar. */
private enum class SwipeViewMode { Swipe, Grid }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    group: MediaGroup,
    displayName: String,
    progressStore: ProgressStore,
    onBack: () -> Unit,
    onFinishWithDeletions: (List<MediaItem>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var index by remember(group.key) { mutableIntStateOf(0) }
    val pendingDeletes = remember(group.key) { mutableStateListOf<MediaItem>() }
    var restored by remember(group.key) { mutableStateOf(false) }
    var lastDecision by remember(group.key) { mutableStateOf<Pair<MediaItem, SwipeDecision>?>(null) }
    var buttonDecision by remember(group.key) { mutableStateOf<SwipeDecision?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    // Blocks a NEW decision from starting until the current one has fully
    // resolved (animation finished, index advanced, progress saved). Without
    // this, spamming Delete/Keep rapidly could change `buttonDecision`
    // again while SwipeCard's animate-then-decide coroutine for the
    // PREVIOUS tap was still mid-flight — Compose cancels that coroutine
    // when its key changes, so `onDecision` (the index++ / pendingDeletes
    // update) could be skipped entirely for the cancelled tap, leaving
    // `index` and `pendingDeletes` out of sync with what actually got
    // reviewed. Enough of that compounding under sustained spam is what was
    // showing up as the app going unresponsive.
    var isTransitioning by remember(group.key) { mutableStateOf(false) }

    var viewMode by remember(group.key) { mutableStateOf(SwipeViewMode.Swipe) }
    val gridSelected = remember(group.key) { mutableStateListOf<Long>() }

    LaunchedEffect(group.key) {
        index = progressStore.progressFlow(group.key).first().coerceIn(0, group.items.size)
        restored = true
    }

    // Quietly warm the image cache for the next couple of photos so the swipe
    // never has to wait on a fresh decode mid-gesture. This size MUST match
    // SwipeCard's own request size (see MediaPreview call below) — Coil's
    // cache key includes the requested size, so a mismatched prefetch size
    // creates a second, never-reused cache entry for the same photo instead
    // of warming the one the card will actually ask for. That used to be
    // 900 here vs 600 on the card: every prefetch was pure waste, decoding
    // and caching a bitmap nothing ever displayed.
    LaunchedEffect(index, group.key) {
        val loader = context.imageLoader
        (index + 1..index + 2).forEach { i ->
            group.items.getOrNull(i)?.let { item ->
                loader.enqueue(
                    ImageRequest.Builder(context)
                        .data(item.uri)
                        .size(SWIPE_CARD_DECODE_SIZE)
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

    BackHandler { finishAndExit() }

    // Grid multi-select (below) can mark items for deletion out of sequence,
    // ahead of wherever `index` currently points. Without skipping past
    // those here, continuing to swipe afterward could show the user a photo
    // they already decided to delete via grid — plain `group.items.getOrNull(index)`
    // has no way to know that happened. Recomputed fresh every recomposition
    // rather than memoized, since pendingDeletes is small (one folder's worth)
    // and this keeps it trivially correct with no stale-cache risk.
    val pendingDeleteIds = pendingDeletes.map { it.id }.toSet()
    val currentItem = run {
        var i = index
        while (i < group.items.size && group.items[i].id in pendingDeleteIds) i++
        group.items.getOrNull(i)
    }
    // The raw `index` can undercount once items ahead of it have been
    // grid-deleted — find currentItem's real position for an accurate
    // "N of Total" label instead of just showing the stale pointer.
    val currentPosition = currentItem?.let { item ->
        group.items.indexOfFirst { it.id == item.id } + 1
    } ?: (index + 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName) },
                navigationIcon = {
                    IconButton(onClick = { finishAndExit() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewMode == SwipeViewMode.Swipe) {
                        if (currentItem != null) {
                            IconButton(onClick = { showInfo = true }) {
                                Text("ⓘ", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (lastDecision != null) {
                            IconButton(
                                enabled = !isTransitioning,
                                onClick = {
                                    val (item, decision) = lastDecision!!
                                    if (decision is SwipeDecision.Delete) pendingDeletes.remove(item)
                                    index = (index - 1).coerceAtLeast(0)
                                    lastDecision = null
                                    scope.launch { progressStore.saveProgress(group.key, index) }
                                }
                            ) {
                                Icon(Icons.Filled.Undo, contentDescription = "Undo last swipe")
                            }
                        }
                    }
                    // Grid mode is an alternative to swiping one at a time —
                    // multi-select several photos and bulk-delete them.
                    // Deliberately independent of isTransitioning: grid
                    // actions are synchronous list mutations, not animated,
                    // so there's no analogous in-flight-animation race to
                    // guard against here.
                    IconButton(onClick = {
                        viewMode = if (viewMode == SwipeViewMode.Swipe) SwipeViewMode.Grid else SwipeViewMode.Swipe
                        gridSelected.clear()
                    }) {
                        Icon(
                            if (viewMode == SwipeViewMode.Swipe) Icons.Filled.GridView else Icons.Filled.ViewCarousel,
                            contentDescription = if (viewMode == SwipeViewMode.Swipe) "Switch to grid view" else "Switch to swipe view"
                        )
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

            if (viewMode == SwipeViewMode.Grid) {
                GridSelectContent(
                    items = group.items,
                    pendingDeleteIds = pendingDeleteIds,
                    selected = gridSelected,
                    onToggleSelect = { id ->
                        if (id in gridSelected) gridSelected.remove(id) else gridSelected.add(id)
                    },
                    onDeleteSelected = {
                        group.items
                            .filter { it.id in gridSelected && it.id !in pendingDeleteIds }
                            .forEach { pendingDeletes.add(it) }
                        gridSelected.clear()
                    }
                )
            } else {
                Filmstrip(
                    items = group.items,
                    currentIndex = index,
                    onSelect = { tapped ->
                        index = tapped
                        scope.launch { progressStore.saveProgress(group.key, index) }
                    }
                )

                if (currentItem != null) {
                    InfoBar(item = currentItem, position = currentPosition, total = group.items.size)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (currentItem == null) {
                        FinishedPanel(
                            deletedCount = pendingDeletes.size,
                            reviewedCount = group.items.size,
                            onDone = { finishAndExit() }
                        )
                    } else {
                        // FITUR PREVIEW BAYANGAN DI BELAKANG SUDAH DIHAPUS TOTAL DI SINI
                        // Hanya menyisakan satu kartu utama yang aktif dan responsif
                        SwipeCard(
                            item = currentItem,
                            enabled = !isTransitioning,
                            externalDecision = buttonDecision,
                            onExternalDecisionHandled = { buttonDecision = null },
                            onZoomRequest = { showFullscreen = true },
                            onDecision = { decision ->
                                if (decision is SwipeDecision.Delete) pendingDeletes.add(currentItem)
                                lastDecision = currentItem to decision
                                index += 1
                                scope.launch { progressStore.saveProgress(group.key, index) }
                                isTransitioning = false
                            }
                        )
                    }
                }

                if (currentItem != null) {
                    ActionButtonRow(
                        enabled = !isTransitioning,
                        onDelete = {
                            if (!isTransitioning) {
                                isTransitioning = true
                                buttonDecision = SwipeDecision.Delete
                            }
                        },
                        onSkip = {
                            if (!isTransitioning) {
                                isTransitioning = true
                                lastDecision = currentItem to SwipeDecision.Keep
                                index += 1
                                scope.launch { progressStore.saveProgress(group.key, index) }
                                // Skip has no animation to wait on, but still goes
                                // through the same gate so a burst of rapid taps
                                // advances one item per tap instead of racing
                                // ahead of Compose's own recomposition.
                                isTransitioning = false
                            }
                        },
                        onKeep = {
                            if (!isTransitioning) {
                                isTransitioning = true
                                buttonDecision = SwipeDecision.Keep
                            }
                        }
                    )
                }
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

/** Multi-select alternative to swiping one photo at a time: tap thumbnails
 *  to select several, then bulk-delete them in one action. Selected items
 *  are handed off through [onDeleteSelected] into the same `pendingDeletes`
 *  list the swipe flow uses — nothing is permanently removed until the
 *  screen is exited (see finishAndExit in SwipeScreen), so this is exactly
 *  as safe/reversible as a normal swipe session, just faster for clearing
 *  out many photos at once. */
@Composable
private fun GridSelectContent(
    items: List<MediaItem>,
    pendingDeleteIds: Set<Long>,
    selected: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onDeleteSelected: () -> Unit
) {
    // Items already handled (via this grid or a prior swipe decision) drop
    // out of view immediately — visible, immediate confirmation that a
    // bulk-delete action actually took effect.
    val visibleItems = remember(items, pendingDeleteIds) {
        items.filterNot { it.id in pendingDeleteIds }
    }
    val allSelected = visibleItems.isNotEmpty() && selected.size == visibleItems.size

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (selected.isEmpty()) "${visibleItems.size} photos" else "${selected.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                if (allSelected) {
                    // `selected` is owned by the caller (SwipeScreen) as a
                    // SnapshotStateList; toggle each one off individually
                    // through the same callback rather than assuming direct
                    // mutation access to it here.
                    selected.toList().forEach(onToggleSelect)
                } else {
                    visibleItems.map { it.id }.filterNot { it in selected }.forEach(onToggleSelect)
                }
            }) {
                Text(if (allSelected) "Deselect all" else "Select all")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(visibleItems, key = { it.id }) { item ->
                val isSelected = item.id in selected
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleSelect(item.id) }
                ) {
                    MediaPreview(
                        item = item,
                        contentScale = ContentScale.Crop,
                        decodeSize = 200,
                        lowMemory = true,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        if (selected.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDeleteSelected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF1A0E0C)
                        )
                    ) {
                        Text("Delete ${selected.size} selected")
                    }
                }
            }
        }
    }
}

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
                        contentScale = ContentScale.Fit,
                        decodeSize = 100,
                        lowMemory = true, // the whole strip can be scrolled through rapidly
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

@Composable
private fun ActionButtonRow(
    enabled: Boolean,
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
            enabled = enabled,
            onClick = onDelete
        )
        RoundActionButton(
            symbol = "⏭",
            background = MaterialTheme.colorScheme.surfaceVariant,
            symbolColor = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 48.dp,
            enabled = enabled,
            onClick = onSkip
        )
        RoundActionButton(
            symbol = "✓",
            background = MaterialTheme.colorScheme.primary,
            symbolColor = Color(0xFF0F1113),
            size = 64.dp,
            enabled = enabled,
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            // Dimmed while disabled — a clear, immediate signal that the tap
            // during a spam burst was seen but intentionally ignored, rather
            // than the button just silently doing nothing.
            .background(if (enabled) background else background.copy(alpha = 0.4f), CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
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
    enabled: Boolean,
    externalDecision: SwipeDecision?,
    onExternalDecisionHandled: () -> Unit,
    onZoomRequest: () -> Unit,
    onDecision: (SwipeDecision) -> Unit
) {
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    suspend fun animateAndDecide(decision: SwipeDecision) {
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
                                        animate(offsetX, 0f, animationSpec = tween(200)) { value, _ -> offsetX = value }
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
private fun FullscreenViewer(item: MediaItem, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }) {
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
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FileInfoDialog(item: MediaItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Info") },
        text = {
            Column {
                Text("Name: ${item.displayName}")
                Text("Size: ${formatBytes(item.sizeBytes)}")
                Text("ID: ${item.id}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}


