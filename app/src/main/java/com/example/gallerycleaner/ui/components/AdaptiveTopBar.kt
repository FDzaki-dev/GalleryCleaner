package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * Batch143 — drop-in pengganti `TopAppBar` M3 yang TIDAK pernah memotong
 * judul. `TopAppBar` bawaan tingginya fixed 64dp dan judulnya cuma dapat
 * sisa lebar setelah ikon-ikon aksi (SwipeScreen: 1 nav + 5 aksi ≈ 290dp dari
 * ±360dp layar → sisa ~60dp), jadi judul panjang wrap-lalu-kepotong.
 *
 * Aturan layout (deterministik, tanpa maxLines/ellipsis):
 *  1. Kalau judul (1 baris, lebar alaminya) muat di antara nav dan aksi →
 *     1 baris, tinggi min 64dp, sama persis kayak TopAppBar.
 *  2. Kalau nggak muat → nav + aksi di baris atas, judul pindah ke baris
 *     sendiri di bawahnya selebar layar dan boleh turun baris berapa pun.
 *  3. Kalau nav + aksi sendiri sudah lebih lebar dari layar → aksi ikut
 *     turun ke baris sendiri (rata kanan).
 *
 * [allowTitleWrap] = false memaksa aturan 1 (dipakai HomeScreen saat mode
 * search: judulnya TextField `fillMaxWidth` yang harus tetap 1 baris).
 * Warna default meniru `TopAppBarDefaults.topAppBarColors()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    allowTitleWrap: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    navigationIconColor: Color = MaterialTheme.colorScheme.onSurface,
    actionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    Surface(color = containerColor, modifier = modifier) {
        Layout(
            content = {
                Box(Modifier.layoutId("nav").padding(start = 4.dp)) {
                    CompositionLocalProvider(LocalContentColor provides navigationIconColor) {
                        navigationIcon()
                    }
                }
                Box(Modifier.layoutId("title").padding(horizontal = 4.dp)) {
                    CompositionLocalProvider(LocalContentColor provides titleColor) {
                        ProvideTextStyle(titleStyle) { title() }
                    }
                }
                Row(
                    Modifier.layoutId("actions").padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalContentColor provides actionColor) {
                        actions()
                    }
                }
            },
            modifier = Modifier.windowInsetsPadding(windowInsets).fillMaxWidth()
        ) { measurables, constraints ->
            val maxW = constraints.maxWidth
            val loose = Constraints(maxWidth = maxW)
            val navM = measurables.first { it.layoutId == "nav" }
            val titleM = measurables.first { it.layoutId == "title" }
            val actM = measurables.first { it.layoutId == "actions" }

            val navP = navM.measure(loose)
            val actP = actM.measure(loose)

            val minRow = 64.dp.roundToPx()
            val compactRow = 56.dp.roundToPx()
            val titleInset = 12.dp.roundToPx()
            val titleStart = maxOf(titleInset, navP.width)
            val availSameRow = maxOf(maxW - titleStart - actP.width, 0)
            val naturalTitleW = if (allowTitleWrap) titleM.maxIntrinsicWidth(Constraints.Infinity) else 0

            if (!allowTitleWrap || naturalTitleW <= availSameRow) {
                // Aturan 1 — 1 baris (identik TopAppBar).
                val titleP = titleM.measure(Constraints(maxWidth = availSameRow))
                val h = maxOf(minRow, navP.height, actP.height, titleP.height)
                layout(maxW, h) {
                    navP.placeRelative(0, (h - navP.height) / 2)
                    titleP.placeRelative(titleStart, (h - titleP.height) / 2)
                    actP.placeRelative(maxW - actP.width, (h - actP.height) / 2)
                }
            } else {
                // Aturan 2/3 — judul di baris sendiri, wrap tanpa batas.
                val titleP = titleM.measure(Constraints(maxWidth = maxOf(maxW - 2 * titleInset, 0)))
                val bottomInset = 12.dp.roundToPx()
                val iconsFit = navP.width + actP.width <= maxW
                if (iconsFit) {
                    val row1 = maxOf(compactRow, navP.height, actP.height)
                    layout(maxW, row1 + titleP.height + bottomInset) {
                        navP.placeRelative(0, (row1 - navP.height) / 2)
                        actP.placeRelative(maxW - actP.width, (row1 - actP.height) / 2)
                        titleP.placeRelative(titleInset, row1)
                    }
                } else {
                    val row1 = maxOf(compactRow, navP.height)
                    val row2 = actP.height
                    layout(maxW, row1 + row2 + titleP.height + bottomInset) {
                        navP.placeRelative(0, (row1 - navP.height) / 2)
                        actP.placeRelative(maxW - actP.width, row1)
                        titleP.placeRelative(titleInset, row1 + row2)
                    }
                }
            }
        }
    }
}
