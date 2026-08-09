package com.example.gallerycleaner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Shared between the prefetch pass above and SwipeCard's own MediaPreview
// call below — both MUST request the same decode size, since Coil's cache
// key includes it. A mismatch here means prefetching does nothing useful.
internal const val SWIPE_CARD_DECODE_SIZE = 600

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
    hapticsEnabled: Boolean = true,
    onBack: () -> Unit,
    onFinishWithDeletions: (List<MediaItem>) -> Unit,
    onCompressRequest: (List<MediaItem>) -> Unit = {},
    existingFolders: List<String> = emptyList(),
    onOrganizeRequest: (List<MediaItem>, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var index by remember(group.key) { mutableIntStateOf(0) }
    val pendingDeletes = remember(group.key) { mutableStateListOf<MediaItem>() }
    // Items sent off to a different folder via "Organize" — kept separate
    // from pendingDeletes (they're not deleted, and onFinishWithDeletions'
    // contract is specifically about trash/deletion) but tracked the same
    // way so the swipe/grid flow skips past them just like a delete would.
    val pendingOrganized = remember(group.key) { mutableStateListOf<MediaItem>() }
    var organizeTarget by remember(group.key) { mutableStateOf<List<MediaItem>?>(null) }
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
    val pendingOrganizedIds = pendingOrganized.map { it.id }.toSet()
    val skipIds = pendingDeleteIds + pendingOrganizedIds
    val currentItem = run {
        var i = index
        while (i < group.items.size && group.items[i].id in skipIds) i++
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
                    },
                    onCompressSelected = {
                        val toCompress = group.items.filter { it.id in gridSelected }
                        onCompressRequest(toCompress)
                        gridSelected.clear()
                    },
                    pendingOrganizedIds = pendingOrganizedIds,
                    onOrganizeSelected = {
                        organizeTarget = group.items.filter { it.id in gridSelected }
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
                            hapticsEnabled = hapticsEnabled,
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
                        },
                        onOrganize = {
                            if (!isTransitioning) {
                                organizeTarget = listOf(currentItem)
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
    organizeTarget?.let { itemsToOrganize ->
        OrganizeFolderDialog(
            itemCount = itemsToOrganize.size,
            suggestedFolders = existingFolders,
            onConfirm = { targetFolder ->
                pendingOrganized.addAll(itemsToOrganize.filterNot { it.id in pendingOrganizedIds })
                onOrganizeRequest(itemsToOrganize, targetFolder)
                gridSelected.removeAll(itemsToOrganize.map { it.id }.toSet())
                // Single-photo organize from the swipe button behaves like a
                // decision that isn't Keep/Delete: advance past it the same
                // way Skip does, so the flow doesn't get stuck re-showing a
                // photo that already left this folder.
                if (itemsToOrganize.size == 1 && itemsToOrganize.first().id == currentItem?.id) {
                    index += 1
                    scope.launch { progressStore.saveProgress(group.key, index) }
                }
                organizeTarget = null
            },
            onDismiss = { organizeTarget = null }
        )
    }
}

