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
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groups: List<MediaGroup>,
    smartGroups: List<MediaGroup>,
    onThisDayItems: List<MediaItem> = emptyList(),
    blurryScanState: ScanState<List<MediaItem>> = ScanState.Idle,
    onScanBlurry: () -> Unit = {},
    nearDupScanState: ScanState<List<MediaGroup>> = ScanState.Idle,
    onScanNearDuplicates: () -> Unit = {},
    duplicateScanState: ScanState<List<MediaItem>> = ScanState.Idle,
    duplicateScanProgress: Float = 0f,
    onScanDuplicates: () -> Unit = {},
    onCancelDuplicateScan: () -> Unit = {},
    groupMode: GroupMode,
    sortOption: SortOption,
    progressStore: ProgressStore,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    trashCount: Int,
    totalLibraryBytes: Long,
    trashReclaimableBytes: Long,
    largestItems: List<MediaItem> = emptyList(),
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
    onCleanExpiredTrash: () -> Unit,
    randomModeEnabled: Boolean = false,
    onRandomModeToggle: (Boolean) -> Unit = {},
    cleanupGoalBytes: Long = DEFAULT_CLEANUP_GOAL_BYTES,
    onCleanupGoalChange: (Long) -> Unit = {}
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
        // Transparent (Batch22, was solid colorScheme.background): the
        // Signature/Midnight-Blue theme paints its ambient gradient onto the
        // MainActivity root Surface, one level up, specifically so glass
        // panels have visible depth to float over (see the doc comment at
        // that call site). A solid Scaffold background here was painting
        // flat over that gradient on every screen, which is why the app
        // looked like a plain dark theme instead of glassmorphism despite
        // Batch21's ColorScheme rewrite — this was the actual gap, not a
        // missing component. For Amber Reserve/Indigo Noir (no gradient
        // painted at the root, root Surface uses colorScheme.background
        // directly instead) this renders identically to before.
        // contentColor (Batch24 fix): a transparent containerColor makes
        // Scaffold's derived contentColorFor() return Unspecified, so any
        // Text() without its own explicit color= falls back to hard-default
        // black instead of the theme's text color — set explicitly.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
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
                                placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = { closeSearch() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.home_close_search_cd),
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
                                        contentDescription = stringResource(R.string.home_clear_search_cd),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.home_search_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.home_refresh_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Quick access to random clean mode without a trip to
                            // Settings — same persisted toggle either way (see
                            // SettingsScreen's "Swiping" section), tinted primary
                            // when on so its state is visible at a glance.
                            IconButton(onClick = { onRandomModeToggle(!randomModeEnabled) }) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = if (randomModeEnabled) {
                                        stringResource(R.string.home_random_mode_on_cd)
                                    } else {
                                        stringResource(R.string.home_random_mode_off_cd)
                                    },
                                    tint = if (randomModeEnabled) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.home_settings_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onTrashClick) {
                                Text(
                                    if (trashCount > 0) stringResource(R.string.home_trash_count, trashCount)
                                    else stringResource(R.string.home_trash_label),
                                    color = if (trashCount > 0) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        // Translucent (Batch22, was fully opaque) so the top
                        // bar reads as a frosted glass strip over the
                        // scrolling content behind it, matching the panels
                        // below — no Modifier.blur (minSdk 24, see
                        // MidnightGlassTokens doc comment), translucency
                        // alone carries the effect here same as it does for
                        // GlassCard/GlassButton.
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
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
                    stringResource(R.string.home_no_photos_found),
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
                        totalDeletedCount = totalDeletedCount,
                        cleanupGoalBytes = cleanupGoalBytes,
                        onCleanupGoalChange = onCleanupGoalChange
                    )
                }

                if (largestItems.isNotEmpty()) {
                    item {
                        LargestFilesCard(
                            items = largestItems,
                            onClick = {
                                onGroupClick(MediaGroup("Largest files", largestItems))
                            }
                        )
                    }
                }

                if (onThisDayItems.isNotEmpty()) {
                    item {
                        SectionLabel(stringResource(R.string.home_section_on_this_day))
                        OnThisDayRow(
                            photos = onThisDayItems,
                            onClick = {
                                onGroupClick(MediaGroup("On this day", onThisDayItems))
                            }
                        )
                    }
                }

                if (smartGroups.isNotEmpty()) {
                    item {
                        SectionLabel(stringResource(R.string.home_section_quick_clean))
                    }
                    items(smartGroups, key = { "smart-${it.key}" }) { group ->
                        SmartCategoryRow(group = group, onClick = { onGroupClick(group) })
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel(stringResource(R.string.home_section_smart_detection))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScanTriggerRow(
                            title = stringResource(R.string.home_scan_blurry_title),
                            subtitle = when (val s = blurryScanState) {
                                is ScanState.Done -> if (s.result.isEmpty()) stringResource(R.string.home_scan_none_found) else stringResource(R.string.home_scan_found_count, s.result.size)
                                else -> stringResource(R.string.home_scan_blurry_idle_subtitle)
                            },
                            scanning = blurryScanState is ScanState.Scanning,
                            onClick = {
                                val s = blurryScanState
                                if (s is ScanState.Done && s.result.isNotEmpty()) {
                                    onGroupClick(MediaGroup("Blurry photos", s.result))
                                } else if (s !is ScanState.Scanning) {
                                    onScanBlurry()
                                }
                            }
                        )
                        ScanTriggerRow(
                            title = stringResource(R.string.home_scan_similar_title),
                            subtitle = when (val s = nearDupScanState) {
                                is ScanState.Done -> if (s.result.isEmpty()) stringResource(R.string.home_scan_none_found) else stringResource(
                                    R.string.home_near_dup_found,
                                    s.result.sumOf { it.items.size },
                                    s.result.size,
                                    if (s.result.size != 1) "s" else ""
                                )
                                else -> stringResource(R.string.home_scan_similar_idle_subtitle)
                            },
                            scanning = nearDupScanState is ScanState.Scanning,
                            onClick = {
                                val s = nearDupScanState
                                if (s is ScanState.Done && s.result.isNotEmpty()) {
                                    onGroupClick(MediaGroup("Similar photos", s.result.flatMap { it.items }))
                                } else if (s !is ScanState.Scanning) {
                                    onScanNearDuplicates()
                                }
                            }
                        )
                        // Batch52 (Audit Gap P1 #6, stage 2b): was folded
                        // automatically into Quick Clean before this batch —
                        // now on-demand like the two rows above, but reports
                        // progress% and can be cancelled mid-scan (see
                        // CancellableScanTriggerRow in HomeScreenSections.kt).
                        CancellableScanTriggerRow(
                            title = stringResource(R.string.home_scan_duplicate_title),
                            subtitle = when (val s = duplicateScanState) {
                                is ScanState.Done -> if (s.result.isEmpty()) stringResource(R.string.home_scan_none_found) else stringResource(R.string.home_scan_found_count, s.result.size)
                                else -> stringResource(R.string.home_scan_duplicate_idle_subtitle)
                            },
                            scanning = duplicateScanState is ScanState.Scanning,
                            progress = duplicateScanProgress,
                            onClick = {
                                val s = duplicateScanState
                                if (s is ScanState.Done && s.result.isNotEmpty()) {
                                    onGroupClick(MediaGroup("Duplicate files", s.result))
                                } else if (s !is ScanState.Scanning) {
                                    onScanDuplicates()
                                }
                            },
                            onCancel = onCancelDuplicateScan
                        )
                    }
                }

                item {
                    SectionLabel(stringResource(R.string.home_section_all_photos))
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
