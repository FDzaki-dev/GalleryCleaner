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
import androidx.compose.material.icons.filled.Folder
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
import com.example.gallerycleaner.ui.components.GlassCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Calendar

@Composable
internal fun GroupRow(
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

    // Folder-summary line — only shown when the group actually spans more
    // than one folder. In Album mode this is always exactly 1 (the row's
    // own title IS the folder), so it's naturally skipped there — nothing
    // extra to render. In Month mode a row routinely pools photos from
    // several folders (Camera, WhatsApp, Screenshots, ...) with no single
    // folder to name, which is exactly what made a Month row look
    // unexplained next to "Biggest space hogs" (that card lists individual
    // files, each with its own one true folder) — a person unfamiliar with
    // the app had no way to tell why one screen shows folder names and the
    // other doesn't. Surfacing which folders actually contributed makes
    // that difference legible without needing to already understand
    // Month-vs-Album as a concept.
    val distinctFolders = remember(group.items) { group.items.map { it.bucketName }.distinct() }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp,
        onClick = onClick
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
                if (distinctFolders.size > 1) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            folderSummaryText(distinctFolders),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
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

/** "Camera, WhatsApp Images" for 2, "Camera, WhatsApp Images +3 more" for
 *  more than 2 — short enough to fit a single line next to the folder icon
 *  without needing the row to grow taller for a long list of names. */
private fun folderSummaryText(folders: List<String>): String =
    if (folders.size <= 2) {
        folders.joinToString(", ")
    } else {
        "${folders.take(2).joinToString(", ")} +${folders.size - 2} more"
    }

@Composable
internal fun RenameFolderDialog(
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
internal fun CoverThumbnail(items: List<MediaItem>) {
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
internal fun ProgressRing(fraction: Float, done: Boolean) {
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

