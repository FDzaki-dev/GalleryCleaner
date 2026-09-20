package com.example.gallerycleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gallerycleaner.ui.components.DangerButton
import com.example.gallerycleaner.ui.components.GlassButton
import com.example.gallerycleaner.ui.components.GlassCard
import com.example.gallerycleaner.ui.components.AdaptiveTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    items: List<MediaItem>,
    trashedAtMillis: Map<Long, Long>,
    expiryDays: Int,
    onBack: () -> Unit,
    onRestore: (List<Long>) -> Unit,
    onDeletePermanently: (List<Long>) -> Unit
) {
    // Batch115 (UI State Rotation-Survival Sweep, kelas custom-Saver — lanjutan
    // Stage 1-5 Batch97/98/99/103/104): `selected` (SnapshotStateList<Long>)
    // sekarang rotation-survival lewat listSaver. Elemen Long primitif, langsung
    // didukung Bundle — 0 custom encode/decode dibutuhkan, beda kelas masalah dari
    // organizeTarget/zoomedItem (bawa MediaItem) atau updateState (sealed class),
    // yang MASIH pending (lihat tracker "Belum Dikerjakan").
    val selected = rememberSaveable(
        saver = listSaver<SnapshotStateList<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<Long>() }
    var showEmptyTrashConfirm by rememberSaveable { mutableStateOf(false) }

    // [Batch118] Regression found after Batch117 unblocked Trash from
    // surviving rotation: this effect used to run unconditionally on every
    // `items` change, INCLUDING the very first composition right after an
    // Activity recreation — at that instant `items` (MainActivity's
    // trashItems, sourced from allMedia -> derivedMedia) is still its
    // default empty list, because reloading from MediaStore after rotation
    // hasn't finished yet. `retainAll(emptySet())` against that transient
    // empty snapshot wiped the selection `selected` had JUST restored via
    // its own listSaver (Batch115/116) — by the time the real trash list
    // arrived a moment later, there was nothing left to retain. This bug
    // was already latent in Batch115/116, just unreachable: before Batch117,
    // rotating always bounced back to Home before TrashScreen ever got to
    // compose far enough to hit this effect at all.
    // Fix: skip exactly the first firing in this composable instance's
    // lifetime (hasSyncedOnce) — a transiently-empty items snapshot can no
    // longer clobber a just-restored selection. Every firing AFTER that
    // first one still prunes normally against real data, so the original
    // intent (drop selected ids that fall out of trash — permanent delete
    // completing, trash genuinely going empty) is unchanged.
    var hasSyncedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(items.map { it.id }) {
        if (hasSyncedOnce) {
            selected.retainAll(items.map { it.id }.toSet())
        }
        hasSyncedOnce = true
    }

    Scaffold(
        topBar = {
            AdaptiveTopBar(
                title = {
                    Text(
                        if (selected.isEmpty()) stringResource(R.string.trash_title_count, items.size)
                        else stringResource(R.string.trash_title_selected, selected.size)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.trash_back_cd))
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        if (selected.isEmpty()) {
                            // One tap to clear everything at once — no need to
                            // Select all -> Delete permanently for the common
                            // "just empty the whole trash" case.
                            TextButton(
                                onClick = { showEmptyTrashConfirm = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(stringResource(R.string.trash_empty_button))
                            }
                        } else {
                            TextButton(onClick = {
                                if (selected.size == items.size) selected.clear()
                                else { selected.clear(); selected.addAll(items.map { it.id }) }
                            }) {
                                Text(
                                if (selected.size == items.size) stringResource(R.string.trash_deselect_all)
                                else stringResource(R.string.trash_select_all)
                            )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
            )
        },
        // Transparent (Batch22) — see matching comment in HomeScreen.kt.
        // contentColor (Batch24 fix) — see matching comment in HomeScreen.kt.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (selected.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassButton(
                            text = stringResource(R.string.trash_restore_button),
                            modifier = Modifier.weight(1f),
                            onClick = { onRestore(selected.toList()); selected.clear() }
                        )
                        DangerButton(
                            text = stringResource(R.string.trash_delete_permanently_button),
                            modifier = Modifier.weight(1f),
                            onClick = { onDeletePermanently(selected.toList()) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.trash_empty_state), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val gridPadding = mergePadding(padding, 12.dp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = gridPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    // Reading `selected.contains()` directly here ties EVERY visible
                    // cell's recomposition scope to the same SnapshotStateList
                    // instance — toggling any one item would recompose the whole
                    // visible grid, not just that cell (structural reads on a
                    // SnapshotStateList invalidate at the object level, not per
                    // element). derivedStateOf still re-evaluates the `contains`
                    // check on every `selected` write, but its `.value` — and thus
                    // this cell's recomposition — only changes when THIS item's own
                    // membership actually flips.
                    val isSelected by remember(item.id) { derivedStateOf { item.id in selected } }
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isSelected) selected.remove(item.id) else selected.add(item.id)
                            }
                    ) {
                        MediaPreview(
                            item = item,
                            contentScale = ContentScale.Crop,
                            decodeSize = 300,
                            lowMemory = true, // a full grid of these can be alive at once
                            modifier = Modifier.fillMaxSize()
                        )
                        val daysLeft = trashedAtMillis[item.id]?.let { trashedAt ->
                            val ageDays = (System.currentTimeMillis() - trashedAt) / (24 * 60 * 60 * 1000L)
                            (expiryDays - ageDays).coerceAtLeast(0)
                        }
                        // Only bother the user once it's actually close —
                        // no need to label something that just got trashed.
                        if (daysLeft != null && daysLeft <= 7) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                            ) {
                                Text(
                                    if (daysLeft <= 0) stringResource(R.string.trash_expires_today)
                                    else stringResource(R.string.trash_days_left, daysLeft),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0F1113),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text(stringResource(R.string.trash_empty_dialog_title)) },
            text = {
                Text(stringResource(R.string.trash_empty_dialog_body, items.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyTrashConfirm = false
                    onDeletePermanently(items.map { it.id })
                }) {
                    Text(stringResource(R.string.trash_empty_dialog_confirm), color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) { Text(stringResource(R.string.trash_empty_dialog_cancel)) }
            }
        )
    }
}

/** Merges Scaffold's inner padding with an extra uniform inset for the grid. */
private fun mergePadding(base: PaddingValues, extra: Dp): PaddingValues =
    PaddingValues(
        start = extra,
        end = extra,
        top = base.calculateTopPadding() + extra,
        bottom = base.calculateBottomPadding() + extra
    )
