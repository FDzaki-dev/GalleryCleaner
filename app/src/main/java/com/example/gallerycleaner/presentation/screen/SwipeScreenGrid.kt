package com.example.gallerycleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.components.DangerButton
import com.example.gallerycleaner.ui.components.GlassButton
import com.example.gallerycleaner.ui.components.GlassCard

/** Multi-select alternative to swiping one photo at a time: tap thumbnails
 *  to select several, then bulk-delete them in one action. Selected items
 *  are handed off through [onDeleteSelected] into the same `pendingDeletes`
 *  list the swipe flow uses — nothing is permanently removed until the
 *  screen is exited (see finishAndExit in SwipeScreen), so this is exactly
 *  as safe/reversible as a normal swipe session, just faster for clearing
 *  out many photos at once. */
@Composable
internal fun GridSelectContent(
    items: List<MediaItem>,
    pendingDeleteIds: Set<Long>,
    selected: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onDeleteSelected: () -> Unit,
    onCompressSelected: () -> Unit,
    pendingOrganizedIds: Set<Long> = emptySet(),
    onOrganizeSelected: (() -> Unit)? = null
) {
    // Items already handled (via this grid or a prior swipe decision) drop
    // out of view immediately — visible, immediate confirmation that a
    // bulk-delete action actually took effect.
    val visibleItems = remember(items, pendingDeleteIds, pendingOrganizedIds) {
        items.filterNot { it.id in pendingDeleteIds || it.id in pendingOrganizedIds }
    }
    val allSelected = visibleItems.isNotEmpty() && selected.size == visibleItems.size
    var zoomedItem by remember { mutableStateOf<MediaItem?>(null) }

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
                    // Own clickable, nested inside the cell's clickable Box —
                    // Compose resolves the tap to whichever one is actually
                    // under the finger, so tapping this icon zooms without
                    // also toggling selection underneath it.
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "View full size",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { zoomedItem = item }
                            .padding(2.dp)
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
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GlassButton(text = "Compress ${selected.size}", onClick = onCompressSelected)
                    if (onOrganizeSelected != null) {
                        Spacer(Modifier.width(12.dp))
                        GlassButton(text = "Organize ${selected.size}", onClick = onOrganizeSelected)
                    }
                    Spacer(Modifier.width(12.dp))
                    DangerButton(text = "Delete ${selected.size} selected", onClick = onDeleteSelected)
                }
            }
        }
    }

    zoomedItem?.let { item ->
        FullscreenViewer(item = item, onDismiss = { zoomedItem = null })
    }
}

@Composable
internal fun Filmstrip(items: List<MediaItem>, currentIndex: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items.size, key = { i -> items[i].id }) { i ->
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

