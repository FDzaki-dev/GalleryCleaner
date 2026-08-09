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
import java.util.Calendar

/** What the home screen shows in place of the normal folder list while
 *  search is active: folders whose name matches, and individual photos
 *  whose filename matches, each independently tappable. */
@Composable
internal fun SearchResultsContent(
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
internal fun SearchPhotoGrid(items: List<MediaItem>, onClick: (MediaItem) -> Unit) {
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

