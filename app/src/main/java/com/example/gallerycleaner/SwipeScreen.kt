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
    var lastDecision by remember(group.key) { mutableStateOf<Pair<MediaItem, SwipeDecision>?>(null) }
    var buttonDecision by remember(group.key) { mutableStateOf<SwipeDecision?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(group.key) {
        index = progressStore.progressFlow(group.key).first().coerceIn(0, group.items.size)
        restored = true
    }

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
                    // FITUR PREVIEW BAYANGAN DI BELAKANG SUDAH DIHAPUS TOTAL DI SINI
                    // Hanya menyisakan satu kartu utama yang aktif dan responsif
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
            .clickable { onZoomRequest() }
            .pointerInput(item.id) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        MediaPreview(
            item = item,
            contentScale = ContentScale.Crop,
            decodeSize = 600,
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
                model = item.uri,
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


