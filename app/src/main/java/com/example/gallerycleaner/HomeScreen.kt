package com.example.gallerycleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groups: List<MediaGroup>,
    smartGroups: List<MediaGroup>,
    groupMode: GroupMode,
    sortOption: SortOption,
    progressStore: ProgressStore,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    trashCount: Int,
    totalLibraryBytes: Long,
    trashReclaimableBytes: Long,
    totalFreedBytes: Long,
    totalDeletedCount: Int,
    expiredTrashCount: Int,
    expiryDays: Int,
    folderLabels: Map<String, String>,
    onRenameFolder: (String, String) -> Unit,
    onGroupModeChange: (GroupMode) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onGroupClick: (MediaGroup) -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefresh: () -> Unit,
    onCleanExpiredTrash: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    // Reconstructed from `groups` rather than passed in separately — every
    // active photo is already in there once, grouped by month/album, so
    // flattening gives the full searchable set for free without MainActivity
    // needing to thread a second, parallel copy of the same data down.
    val allActiveItems = remember(groups) { groups.flatMap { it.items } }

    // debouncedQuery lags searchQuery by 150ms of no typing. The TextField
    // below is still bound directly to searchQuery, so every keystroke
    // appears instantly — only the expensive part (filtering allActiveItems,
    // which can be tens of thousands of items) waits for typing to pause,
    // instead of re-scanning the whole active list on every single
    // keystroke. Without this, fast typing on a large library reruns a full
    // O(n) filter per character — a classic, easy-to-miss cause of laggy-
    // feeling search input once a library is big enough for it to matter.
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        delay(150)
        debouncedQuery = searchQuery
    }

    val matchingFolders = remember(groups, debouncedQuery, folderLabels) {
        if (debouncedQuery.isBlank()) emptyList()
        else groups.filter { group ->
            (folderLabels[group.key] ?: group.key).contains(debouncedQuery, ignoreCase = true)
        }
    }
    val matchingPhotos = remember(allActiveItems, debouncedQuery) {
        if (debouncedQuery.isBlank()) emptyList()
        else allActiveItems
            .filter { it.displayName.contains(debouncedQuery, ignoreCase = true) }
            .take(60) // cap — this is a quick-jump aid, not a full results browser
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchFocusRequester.requestFocus()
    }

    fun closeSearch() {
        isSearchActive = false
        searchQuery = ""
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                                singleLine = true,
                                placeholder = { Text("Search folders or photos") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Text("Gallery Cleaner", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = { closeSearch() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear search text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Refresh library",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onTrashClick) {
                                Text(
                                    if (trashCount > 0) "Trash ($trashCount)" else "Trash",
                                    color = if (trashCount > 0) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // Thin, unobtrusive cue that the rest of a large gallery is still
                // streaming in behind the scenes — the groups already on screen
                // stay fully interactive while this shows.
                if (isLoadingMore) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { padding ->
        if (isSearchActive) {
            SearchResultsContent(
                padding = padding,
                query = searchQuery,
                matchingFolders = matchingFolders,
                matchingPhotos = matchingPhotos,
                folderLabels = folderLabels,
                onFolderClick = onGroupClick,
                onPhotoClick = { tapped ->
                    onGroupClick(
                        MediaGroup(
                            key = "Search results",
                            items = matchingPhotos.sortedBy { it.id != tapped.id }
                        )
                    )
                }
            )
        } else {
        when {
            isLoading -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            groups.isEmpty() && smartGroups.isEmpty() && !isLoadingMore -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No photos found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (expiredTrashCount > 0) {
                    item {
                        ExpiryBanner(
                            count = expiredTrashCount,
                            expiryDays = expiryDays,
                            onClean = onCleanExpiredTrash
                        )
                    }
                }

                item {
                    StorageDashboard(
                        totalLibraryBytes = totalLibraryBytes,
                        trashReclaimableBytes = trashReclaimableBytes,
                        totalFreedBytes = totalFreedBytes,
                        totalDeletedCount = totalDeletedCount
                    )
                }

                if (smartGroups.isNotEmpty()) {
                    item {
                        SectionLabel("QUICK CLEAN")
                    }
                    items(smartGroups, key = { "smart-${it.key}" }) { group ->
                        SmartCategoryRow(group = group, onClick = { onGroupClick(group) })
                    }
                }

                item {
                    SectionLabel("ALL PHOTOS")
                    FilterRow(
                        groupMode = groupMode,
                        sortOption = sortOption,
                        onGroupModeChange = onGroupModeChange,
                        onSortChange = onSortChange
                    )
                }

                items(groups, key = { it.key }) { group ->
                    GroupRow(
                        group = group,
                        progressStore = progressStore,
                        label = folderLabels[group.key],
                        onClick = { onGroupClick(group) },
                        onRename = { newLabel -> onRenameFolder(group.key, newLabel) }
                    )
                }
            }
        }
        }
    }
}

/** What the home screen shows in place of the normal folder list while
 *  search is active: folders whose name matches, and individual photos
 *  whose filename matches, each independently tappable. */
@Composable
private fun SearchResultsContent(
    padding: PaddingValues,
    query: String,
    matchingFolders: List<MediaGroup>,
    matchingPhotos: List<MediaItem>,
    folderLabels: Map<String, String>,
    onFolderClick: (MediaGroup) -> Unit,
    onPhotoClick: (MediaItem) -> Unit
) {
    if (query.isBlank()) {
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Search for a folder or photo by name",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    if (matchingFolders.isEmpty() && matchingPhotos.isEmpty()) {
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (matchingFolders.isNotEmpty()) {
            item { SectionLabel("FOLDERS") }
            items(matchingFolders, key = { "search-folder-${it.key}" }) { group ->
                GroupRow(
                    group = group,
                    progressStore = null,
                    label = folderLabels[group.key],
                    onClick = { onFolderClick(group) },
                    onRename = null
                )
            }
        }
        if (matchingPhotos.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)) }
            item { SectionLabel("PHOTOS") }
            item {
                SearchPhotoGrid(items = matchingPhotos, onClick = onPhotoClick)
            }
        }
    }
}

/** Simple wrapping grid of thumbnails for photo-name search matches — not a
 *  LazyVerticalGrid since this sits inside an outer LazyColumn already
 *  (nesting two lazy-scrolling containers vertically is the usual Compose
 *  footgun); the result count is capped at the call site specifically so a
 *  plain non-lazy grid here stays cheap. */
@Composable
private fun SearchPhotoGrid(items: List<MediaItem>, onClick: (MediaItem) -> Unit) {
    val columns = 4
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onClick(item) }
                    ) {
                        MediaPreview(
                            item = item,
                            contentScale = ContentScale.Crop,
                            decodeSize = 200,
                            lowMemory = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                // Pad the last row so it doesn't stretch to fill the row
                // width when it has fewer than `columns` items.
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Surfaces items that have outlived the trash retention window and offers a
 *  one-tap permanent delete. Android has no silent background-delete API for
 *  scoped storage, so "auto-expiry" is this: a banner the user can dismiss
 *  by acting on, not a delete that happens without them noticing. */
@Composable
private fun ExpiryBanner(count: Int, expiryDays: Int, onClean: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$count item(s) have been in Trash over $expiryDays days",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Delete them permanently to free up space",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClean,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color(0xFF1A0E0C)
                )
            ) {
                Text("Clean up")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/** Top-of-screen summary: how much space photos take up, how much
 *  sits in trash waiting to be freed, and all-time cleanup totals. */
@Composable
private fun StorageDashboard(
    totalLibraryBytes: Long,
    trashReclaimableBytes: Long,
    totalFreedBytes: Long,
    totalDeletedCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Library size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatBytes(totalLibraryBytes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (trashReclaimableBytes > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatBytes(trashReclaimableBytes)} waiting in Trash — empty it to reclaim space",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (totalDeletedCount > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))
                Text(
                    "All time: ${formatBytes(totalFreedBytes)} freed · $totalDeletedCount item(s) cleaned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** A Quick Clean shortcut row — visually distinct (accent-tinted) from the
 *  regular month/album rows below so it reads as a suggestion, not a folder. */
@Composable
private fun SmartCategoryRow(group: MediaGroup, onClick: () -> Unit) {
    val totalBytes = remember(group.key) { group.items.sumOf { it.sizeBytes } }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverThumbnail(items = group.items)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.key, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${group.items.size} items · ${formatBytes(totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    groupMode: GroupMode,
    sortOption: SortOption,
    onGroupModeChange: (GroupMode) -> Unit,
    onSortChange: (SortOption) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "GROUP BY",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)) {
            GroupMode.values().forEach { mode ->
                PillChip(
                    label = mode.label,
                    selected = groupMode == mode,
                    onClick = { onGroupModeChange(mode) }
                )
            }
        }
        Text(
            "SORT BY",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            SortOption.values().forEach { option ->
                PillChip(
                    label = option.label,
                    selected = sortOption == option,
                    onClick = { onSortChange(option) }
                )
            }
        }
    }
}

@Composable
private fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color(0xFF0F1113) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun GroupRow(
    group: MediaGroup,
    progressStore: ProgressStore?,
    label: String?,
    onClick: () -> Unit,
    onRename: ((String) -> Unit)?
) {
    var reviewed by remember(group.key) { mutableStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(group.key, progressStore) {
        if (progressStore != null) {
            reviewed = progressStore.progressFlow(group.key).first()
        }
    }

    // Clamped here, on every recomposition, rather than only once inside the
    // LaunchedEffect above. `group.items.size` can shrink after the effect
    // already cached `reviewed` — e.g. the user swipes 4 of 10 photos
    // (progress saved as 4), one of those 4 lands in Trash, and Home's
    // LazyColumn keeps this same GroupRow alive (same `group.key`) instead
    // of remounting it. The effect never reruns since its keys didn't
    // change, so a stale, un-clamped `reviewed` against the now-smaller item
    // count could push the fraction past 1.0 — showing a still-partial
    // folder as "done" (full ring + checkmark) even though photos in it
    // haven't actually all been swiped.
    val clampedReviewed = reviewed.coerceIn(0, group.items.size)
    val fraction = if (group.items.isEmpty()) 0f else clampedReviewed / group.items.size.toFloat()
    val done = fraction >= 1f && group.items.isNotEmpty()
    val displayName = label ?: group.key

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverThumbnail(items = group.items)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${group.items.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Custom in-app name, independent of whatever the device's own
            // Gallery app calls this folder — see FolderLabelStore for why
            // that's necessary rather than just reading the OS name. Not
            // offered for search-result rows (onRename == null) — renaming
            // from a filtered, possibly-partial view of a folder's contents
            // would be a confusing place to do it.
            if (onRename != null) {
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Rename folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (progressStore != null) {
                ProgressRing(fraction = fraction, done = done)
            }
        }
    }

    if (showRenameDialog && onRename != null) {
        RenameFolderDialog(
            currentName = displayName,
            hasCustomLabel = label != null,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            },
            onResetToOriginal = {
                onRename("")
                showRenameDialog = false
            }
        )
    }
}

@Composable
private fun RenameFolderDialog(
    currentName: String,
    hasCustomLabel: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onResetToOriginal: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            Column {
                Text(
                    "This only changes the name shown in this app — it won't " +
                        "rename the actual folder or affect your device's Gallery app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (hasCustomLabel) {
                    TextButton(onClick = onResetToOriginal) { Text("Reset") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/** A single, clear cover thumbnail — replaces the earlier 3-photo overlapping
 *  stack, which read as visually messy and tripled the image-decode work per
 *  row for little benefit. One crisp image loads faster and looks cleaner. */
@Composable
private fun CoverThumbnail(items: List<MediaItem>) {
    val cover = items.firstOrNull()
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (cover != null) {
            MediaPreview(
                item = cover,
                contentScale = ContentScale.Crop,
                decodeSize = 160, // small, exact decode target — keeps list scrolling smooth
                lowMemory = true, // dozens of these can be alive on screen at once
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Small circular progress indicator drawn by hand for a tighter, calmer look than the default. */
@Composable
private fun ProgressRing(fraction: Float, done: Boolean) {
    val ringColor = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(40.dp)) {
            val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = stroke
            )
        }
        if (done) {
            Text("✓", color = ringColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
