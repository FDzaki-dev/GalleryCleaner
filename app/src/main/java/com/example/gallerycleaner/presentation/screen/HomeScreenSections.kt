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

/** Surfaces items that have outlived the trash retention window and offers a
 *  one-tap permanent delete. Android has no silent background-delete API for
 *  scoped storage, so "auto-expiry" is this: a banner the user can dismiss
 *  by acting on, not a delete that happens without them noticing. */
@Composable
internal fun ExpiryBanner(count: Int, expiryDays: Int, onClean: () -> Unit) {
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
internal fun SectionLabel(text: String) {
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
internal fun LargestFilesCard(items: List<MediaItem>, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Biggest space hogs", fontWeight = FontWeight.SemiBold)
                    Text(
                        "The 5 files using the most storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Review",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            items.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaPreview(
                        item = item,
                        contentScale = ContentScale.Crop,
                        decodeSize = 96,
                        lowMemory = true,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.displayName,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            item.bucketName,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatBytes(item.sizeBytes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun StorageDashboard(
    totalLibraryBytes: Long,
    trashReclaimableBytes: Long,
    largestItems: List<MediaItem> = emptyList(),
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

/** A horizontally-scrolling "memories" strip — deliberately distinct from
 *  SmartCategoryRow below. That one reads as a chore ("here's junk to
 *  clear"); this one is a nudge to look back, so it's a filmstrip of actual
 *  thumbnails rather than another summary list row. Tapping any thumbnail
 *  opens the full set through the same MediaGroup-browsing flow every other
 *  entry point on this screen already uses (search results, Quick Clean,
 *  folders) — no new navigation path to reason about. */
@Composable
internal fun OnThisDayRow(photos: List<MediaItem>, onClick: () -> Unit) {
    val caption = remember(photos) {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val cal = Calendar.getInstance()
        val yearsAgo = photos.map { item ->
            cal.timeInMillis = item.dateTakenMillis
            thisYear - cal.get(Calendar.YEAR)
        }.distinct().sorted()
        when {
            yearsAgo.isEmpty() -> ""
            yearsAgo.size == 1 -> "${yearsAgo.first()} year${if (yearsAgo.first() != 1) "s" else ""} ago"
            else -> "${yearsAgo.first()}\u2013${yearsAgo.last()} years ago"
        }
    }
    Column(Modifier.fillMaxWidth()) {
        if (caption.isNotEmpty()) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Capped for the strip itself — this is a highlight reel, not an
            // exhaustive browser. The tap target isn't capped: onClick still
            // opens every matching photo via the full `photos` list above.
            items(photos.take(20), key = { it.id }) { item ->
                MediaPreview(
                    item = item,
                    contentScale = ContentScale.Crop,
                    decodeSize = 240,
                    lowMemory = true,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onClick)
                )
            }
        }
    }
}

/** A tap-to-scan row for the two on-demand detectors (blur, near-duplicate)
 *  — visually similar to SmartCategoryRow, but with an explicit idle /
 *  scanning / done lifecycle instead of just appearing once data is ready,
 *  since these require the person to actually ask for the (real, non-
 *  trivial) CPU work rather than happening silently in the background. */
@Composable
internal fun ScanTriggerRow(title: String, subtitle: String, scanning: Boolean, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !scanning, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Scan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** A Quick Clean shortcut row — visually distinct (accent-tinted) from the
 *  regular month/album rows below so it reads as a suggestion, not a folder. */
@Composable
internal fun SmartCategoryRow(group: MediaGroup, onClick: () -> Unit) {
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
internal fun FilterRow(
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
internal fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

