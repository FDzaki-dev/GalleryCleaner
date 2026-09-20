package com.example.gallerycleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.gallerycleaner.ui.components.CupertinoSurface
import com.example.gallerycleaner.ui.components.GlassButton
import com.example.gallerycleaner.ui.components.GlassCard
import com.example.gallerycleaner.ui.components.NeumorphSurface
import com.example.gallerycleaner.ui.components.PaintedSurface
import com.example.gallerycleaner.ui.components.glassPanel
import com.example.gallerycleaner.ui.components.skeuoPanel
import com.example.gallerycleaner.ui.theme.LocalMaterialStyle
import com.example.gallerycleaner.ui.theme.MaterialStyle
import com.example.gallerycleaner.ui.theme.NeumorphShape
import com.example.gallerycleaner.ui.theme.PaintedShape

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
    // Small pill overlay on the photo preview — thinner border/lower
    // elevation than GlassCard/skeuoPanel defaults since this sits
    // directly over the image, not as a standalone panel (Batch22, was
    // Surface(surfaceVariant)). Batch27: made material-style-aware like
    // GlassCard/GlassButton, since this is the one raw glassPanel() call
    // site outside those two components (see PROJECT_STATE Batch27) — for
    // Amber Reserve to be a genuine full material swap this chip needed
    // the same treatment, not just the cards/buttons.
    // Batch36: NEUMORPH branches to NeumorphSurface (a composable, not a
    // Modifier — see its doc comment) with an early return, same pattern
    // GlassCard.kt uses, since it can't join the `when` below. Batch77:
    // CUPERTINO (Indigo Noir, was GLASS through Batch76 — see
    // MaterialStyle.kt) follows the identical early-return shape, calling
    // CupertinoSurface instead.
    val style = LocalMaterialStyle.current
    if (style == MaterialStyle.NEUMORPH) {
        NeumorphSurface(
            shape = NeumorphShape.Chip,
            shadowElevation = 3.dp,
            shadowOffset = 2.dp,
            contentPadding = 0.dp
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        return
    }
    // Batch77: CUPERTINO branches the same way NEUMORPH does above —
    // CupertinoSurface is a Composable, not a Modifier (see its doc
    // comment) — smaller shadow/no offset to match this chip's existing
    // "thinner than GlassCard defaults" sizing intent.
    if (style == MaterialStyle.CUPERTINO) {
        CupertinoSurface(
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 3.dp,
            contentPadding = 0.dp
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        return
    }
    // Batch119: PAINTED follows the identical early-return shape as
    // NEUMORPH/CUPERTINO above. showWash = false at this chip's small
    // size (same reasoning as its smaller shadow/no-offset vs. GlassCard
    // defaults) — a multi-blob wash wouldn't read at this scale, so only
    // the brush-stroke edge (this theme's other signature) shows.
    if (style == MaterialStyle.PAINTED) {
        PaintedSurface(
            shape = PaintedShape.Chip,
            shadowElevation = 3.dp,
            showWash = false,
            contentPadding = 0.dp
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        return
    }
    Box(
        modifier = when (style) {
            MaterialStyle.GLASS -> Modifier.glassPanel(
                shape = RoundedCornerShape(8.dp),
                elevation = 3.dp,
                borderWidth = 0.5.dp
            )
            MaterialStyle.SKEUO_LITE -> Modifier.skeuoPanel(
                shape = RoundedCornerShape(6.dp),
                elevation = 2.dp,
                borderWidth = 1.dp
            )
            MaterialStyle.NEUMORPH -> Modifier // unreachable — handled by the early return above
            MaterialStyle.CUPERTINO -> Modifier // unreachable — handled by the early return above
            MaterialStyle.PAINTED -> Modifier // unreachable — handled by the early return above
        }
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
        Text("Misi kelar!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        GlassCard(contentPadding = 0.dp) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                StatColumn("Item dicek", "$reviewedCount")
                Spacer(Modifier.width(32.dp))
                StatColumn("Masuk Sampah", "$deletedCount")
            }
        }
        Spacer(Modifier.height(28.dp))
        GlassButton(text = "Lanjut", onClick = onDone)
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
 *  included (same shape the rest of the app already uses).
 *
 *  Batch144 ("Manage move-to albums"): tombol "Kelola" buka mode kelola —
 *  PIN folder biar naik ke atas, atau SEMBUNYIIN folder dari daftar ini
 *  (cuma daftar tujuan; foldernya sendiri 0 tersentuh). Sebelumnya daftar
 *  ini cuma `take(6)` urutan abjad tanpa cara nyampe folder ke-7 dst, jadi
 *  sekarang ada "Tampilin semua". State pin/sembunyi dipegang pemanggil
 *  (SettingsStore) — default param bikin dialog tetap valid tanpa itu. */
@Composable
internal fun OrganizeFolderDialog(
    itemCount: Int,
    suggestedFolders: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    pinnedFolders: Set<String> = emptySet(),
    hiddenFolders: Set<String> = emptySet(),
    onTogglePinned: (String) -> Unit = {},
    onToggleHidden: (String) -> Unit = {}
) {
    var customFolder by remember { mutableStateOf("") }
    var selectedExisting by remember { mutableStateOf<String?>(null) }
    var manageMode by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    // sortedBy stabil: pinned (false) naik ke atas, urutan abjad asli tetap
    // dijaga di dalam tiap kelompok.
    val orderedFolders = remember(suggestedFolders, pinnedFolders) {
        suggestedFolders.sortedBy { it !in pinnedFolders }
    }
    val visibleFolders = orderedFolders.filter { it !in hiddenFolders }
    val shownFolders = when {
        manageMode -> orderedFolders
        showAll -> visibleFolders
        else -> visibleFolders.take(6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (itemCount == 1) "Pindahin ke folder" else "Pindahin $itemCount item ke folder") },
        text = {
            // Batch144: seluruh isi dialog bisa di-scroll — daftar folder
            // sekarang bisa panjang ("Tampilin semua"/mode Kelola) dan
            // AlertDialog M3 sendiri TIDAK scroll (isi yang kelewat tinggi
            // layar, mis. landscape, bakal kepotong).
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Audit Gap P1 #10: on API 30+ MoveHelper.supportsBatchWriteRequest()
                // already gets ONE MediaStore.createWriteRequest() consent dialog
                // for the whole selection (see MainActivity.kt's performOrganize) —
                // that's the platform's only single-dialog batch-grant primitive,
                // and it doesn't exist pre-API 30. Below that, MoveHelper falls
                // back to per-item RecoverableSecurityException, so moving several
                // items that each need a fresh grant can surface as multiple
                // prompts in a row. Nothing here makes that faster — there's no
                // OS API to batch it below API 30 — this just sets the
                // expectation up front instead of the prompts appearing to be a
                // bug. Single-item moves never hit this (only one prompt either
                // way), so the note only shows for itemCount > 1.
                if (itemCount > 1 && !MoveHelper.supportsBatchWriteRequest()) {
                    Text(
                        "Versi Android kamu mungkin munculin izin terpisah buat tiap foto yang butuh izin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (suggestedFolders.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Folder yang udah ada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        TextButton(onClick = { manageMode = !manageMode }) {
                            Text(if (manageMode) "Beres" else "Kelola")
                        }
                    }
                    if (manageMode) {
                        Text(
                            "Pin biar folder muncul paling atas. Sembunyiin buat nyingkirin folder dari daftar ini — nggak ngehapus apa pun.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        shownFolders.forEach { folder ->
                            val isPinned = folder in pinnedFolders
                            val isHidden = folder in hiddenFolders
                            if (manageMode) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        (if (isPinned) "★ " else "") + folder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isHidden) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { onTogglePinned(folder) }) {
                                            Text(if (isPinned) "Lepas pin" else "Pin ke atas")
                                        }
                                        TextButton(
                                            onClick = {
                                                if (folder == selectedExisting) selectedExisting = null
                                                onToggleHidden(folder)
                                            }
                                        ) {
                                            Text(if (isHidden) "Tampilin lagi" else "Sembunyiin")
                                        }
                                    }
                                }
                            } else {
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
                                    Text(
                                        (if (isPinned) "★ " else "") + folder,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    if (!manageMode && visibleFolders.isEmpty()) {
                        Text(
                            "Semua folder lagi disembunyiin — tap Kelola buat nampilin lagi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!manageMode && !showAll && visibleFolders.size > 6) {
                        TextButton(onClick = { showAll = true }) {
                            Text("Tampilin semua (${visibleFolders.size})")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "Atau bikin yang baru",
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
                Text("Pindah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

