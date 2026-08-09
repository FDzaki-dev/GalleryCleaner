package com.example.gallerycleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun InfoBar(item: MediaItem, position: Int, total: Int) {
    val format = item.displayName.substringAfterLast('.', "").uppercase().ifEmpty { "?" }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfoChip(formatBytes(item.sizeBytes))
        InfoChip(format)
        InfoChip("$position/$total")
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
internal fun ActionButtonRow(
    enabled: Boolean,
    onDelete: () -> Unit,
    onSkip: () -> Unit,
    onKeep: () -> Unit,
    onOrganize: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundActionButton(
            symbol = "✕",
            background = MaterialTheme.colorScheme.secondary,
            symbolColor = Color(0xFF1A0E0C),
            size = 64.dp,
            enabled = enabled,
            onClick = onDelete
        )
        RoundActionButton(
            symbol = "⏭",
            background = MaterialTheme.colorScheme.surfaceVariant,
            symbolColor = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 48.dp,
            enabled = enabled,
            onClick = onSkip
        )
        // Optional 3rd action ("Organize" — ROADMAP Fase A item 2): moves the
        // current photo to a folder of the user's choosing instead of
        // keep/delete. Nullable + separate small button rather than a new
        // swipe-gesture direction — SwipeCard's gesture detection already
        // owns left/right for Delete/Keep, and a 3rd gesture direction risks
        // colliding with the existing tap-to-zoom / drag interactions there.
        // A button is explicitly an accepted alternative per the roadmap.
        if (onOrganize != null) {
            RoundActionButton(
                symbol = "🗂",
                background = MaterialTheme.colorScheme.surfaceVariant,
                symbolColor = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 48.dp,
                enabled = enabled,
                onClick = onOrganize
            )
        }
        RoundActionButton(
            symbol = "✓",
            background = MaterialTheme.colorScheme.primary,
            symbolColor = Color(0xFF0F1113),
            size = 64.dp,
            enabled = enabled,
            onClick = onKeep
        )
    }
}

@Composable
private fun RoundActionButton(
    symbol: String,
    background: Color,
    symbolColor: Color,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            // Dimmed while disabled — a clear, immediate signal that the tap
            // during a spam burst was seen but intentionally ignored, rather
            // than the button just silently doing nothing.
            .background(if (enabled) background else background.copy(alpha = 0.4f), CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = symbolColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun FinishedPanel(deletedCount: Int, reviewedCount: Int, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Mission accomplished!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                StatColumn("Items reviewed", "$reviewedCount")
                Spacer(Modifier.width(32.dp))
                StatColumn("Moved to Trash", "$deletedCount")
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text("Continue", modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Folder picker for the "Organize" action: pick from folders that already
 *  exist in the library (so a stray typo can't scatter photos into a new
 *  near-duplicate folder), or type a new one to create. [suggestedFolders]
 *  should be distinct `MediaItem.relativePath` values, trailing slash
 *  included (same shape the rest of the app already uses). */
@Composable
internal fun OrganizeFolderDialog(
    itemCount: Int,
    suggestedFolders: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customFolder by remember { mutableStateOf("") }
    var selectedExisting by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (itemCount == 1) "Move to folder" else "Move $itemCount items to folder") },
        text = {
            Column {
                if (suggestedFolders.isNotEmpty()) {
                    Text(
                        "Existing folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        suggestedFolders.take(6).forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExisting = folder
                                        customFolder = ""
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedExisting == folder,
                                    onClick = {
                                        selectedExisting = folder
                                        customFolder = ""
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(folder, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "Or create a new one",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = customFolder,
                    onValueChange = {
                        customFolder = it
                        if (it.isNotBlank()) selectedExisting = null
                    },
                    singleLine = true,
                    placeholder = { Text("Pictures/GalleryCleaner/Organized") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val target = selectedExisting ?: customFolder.trim().let { raw ->
                if (raw.isEmpty()) null else if (raw.endsWith("/")) raw else "$raw/"
            }
            TextButton(
                enabled = target != null,
                onClick = { target?.let(onConfirm) }
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

