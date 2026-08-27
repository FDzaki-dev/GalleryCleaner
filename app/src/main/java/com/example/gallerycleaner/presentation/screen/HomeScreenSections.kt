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
import com.example.gallerycleaner.ui.components.DangerButton
import com.example.gallerycleaner.ui.components.GlassCard
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
            DangerButton(text = "Clean up", onClick = onClean)
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp,
        onClick = onClick
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
    totalDeletedCount: Int,
    cleanupGoalBytes: Long = DEFAULT_CLEANUP_GOAL_BYTES,
    onCleanupGoalChange: (Long) -> Unit = {}
) {
    var showGoalDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp
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
            // Cleanup goal (ROADMAP Fase A item 3) — always shown, not
            // gated behind totalDeletedCount > 0 like the "all time" line
            // below, since an empty progress bar toward a goal is itself
            // useful information (motivates a first session) whereas an
            // empty "all time" line would just be noise.
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cleanup goal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${formatBytes(totalFreedBytes.coerceAtMost(cleanupGoalBytes))} / ${formatBytes(cleanupGoalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            val goalProgress = if (cleanupGoalBytes > 0) {
                (totalFreedBytes.toFloat() / cleanupGoalBytes.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (goalProgress >= 1f) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (goalProgress >= 1f) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Goal reached! Tap to set a new one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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

    if (showGoalDialog) {
        CleanupGoalDialog(
            currentGoalBytes = cleanupGoalBytes,
            onConfirm = {
                onCleanupGoalChange(it)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }
}

/** Preset chips cover the common cases with one tap; the slider handles
 *  everything in between without needing a raw numeric-entry field (goal
 *  values don't need byte-level precision — nobody sets a goal of exactly
 *  "1.37 GB"). Range 100 MB .. 20 GB chosen to comfortably bracket
 *  DEFAULT_CLEANUP_GOAL_BYTES (2 GB) on both sides. */
@Composable
private fun CleanupGoalDialog(
    currentGoalBytes: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val presetsBytes = listOf(500_000_000L, 1_000_000_000L, 2_000_000_000L, 5_000_000_000L, 10_000_000_000L)
    var sliderBytes by remember { mutableStateOf(currentGoalBytes.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set cleanup goal") },
        text = {
            Column {
                Text(
                    formatBytes(sliderBytes.toLong()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = sliderBytes,
                    onValueChange = { sliderBytes = it },
                    valueRange = 100_000_000f..20_000_000_000f
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetsBytes.forEach { preset ->
                        FilterChip(
                            selected = sliderBytes.toLong() == preset,
                            onClick = { sliderBytes = preset.toFloat() },
                            label = { Text(formatBytes(preset)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderBytes.toLong()) }) { Text("Set goal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    val context = LocalContext.current
    val caption = remember(photos) {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val cal = Calendar.getInstance()
        val yearsAgo = photos.map { item ->
            cal.timeInMillis = item.dateTakenMillis
            thisYear - cal.get(Calendar.YEAR)
        }.distinct().sorted()
        when {
            yearsAgo.isEmpty() -> ""
            yearsAgo.size == 1 -> context.resources.getQuantityString(
                R.plurals.home_years_ago, yearsAgo.first(), yearsAgo.first()
            )
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp,
        onClick = onClick,
        enabled = !scanning
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

/** Batch52 — like [ScanTriggerRow] but for a scan that reports progress and
 *  can be cancelled mid-flight (currently just exact-duplicate detection,
 *  which streams onProgress(checked,total) — see
 *  MediaScanner.findExactDuplicates). Blur/near-dup stay on the plain
 *  ScanTriggerRow above; they weren't asked to gain cancel/progress in this
 *  batch, and this is kept as a separate composable rather than adding
 *  optional params to ScanTriggerRow to avoid touching its two existing
 *  call sites at all. */
@Composable
internal fun CancellableScanTriggerRow(
    title: String,
    subtitle: String,
    scanning: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp,
        onClick = onClick,
        enabled = !scanning
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (scanning) "Scanning… ${(progress * 100).toInt()}%" else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            if (scanning) {
                TextButton(onClick = onCancel) { Text("Cancel") }
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
        // Plain-language explanation of what the two modes actually mean —
        // added alongside GroupRow's per-row folder summary (same fix,
        // same root cause). "Month" pools photos from many folders into
        // one row with no single folder to name, which read as
        // unexplained/inconsistent next to "Album" (one row = one real
        // folder) and "Biggest space hogs" (individual files, each with
        // its own folder). This caption gives the concept up front; the
        // per-row summary confirms it in practice once a Month row shows.
        Text(
            if (groupMode == GroupMode.MONTH) {
                "One row per month, pooling photos from every folder"
            } else {
                "One row per folder, exactly as it exists on your device"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)) {
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

