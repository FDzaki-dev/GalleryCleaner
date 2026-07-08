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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    items: List<MediaItem>,
    onBack: () -> Unit,
    onRestore: (List<Long>) -> Unit,
    onDeletePermanently: (List<Long>) -> Unit
) {
    val selected = remember { mutableStateListOf<Long>() }

    // Selection resets cleanly whenever the trash contents change (e.g. after
    // a permanent delete completes and items disappear from the list).
    LaunchedEffect(items.map { it.id }) {
        selected.retainAll(items.map { it.id }.toSet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selected.isEmpty()) "Trash (${items.size})" else "${selected.size} selected") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = {
                            if (selected.size == items.size) selected.clear()
                            else { selected.clear(); selected.addAll(items.map { it.id }) }
                        }) {
                            Text(if (selected.size == items.size) "Deselect all" else "Select all")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRestore(selected.toList()); selected.clear() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Restore") }
                        Button(
                            onClick = { onDeletePermanently(selected.toList()) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color(0xFF1A0E0C)
                            )
                        ) { Text("Delete permanently") }
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
                Text("Trash is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    val isSelected = item.id in selected
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
                            modifier = Modifier.fillMaxSize()
                        )
                        if (item.isVideo) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
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
}

/** Merges Scaffold's inner padding with an extra uniform inset for the grid. */
private fun mergePadding(base: PaddingValues, extra: Dp): PaddingValues =
    PaddingValues(
        start = extra,
        end = extra,
        top = base.calculateTopPadding() + extra,
        bottom = base.calculateBottomPadding() + extra
    )
