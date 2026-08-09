package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Skeuomorphism-Dark palette — from "Panduan Lengkap: Desain Visual
 * 'Skeuomorphism-Dark' pada Native Kotlin", §2 Persiapan Palette Warna.
 * Values copied exactly as specified (not `#000000` pekat, so the dark
 * drop-shadow in [skeuomorphicDark] still reads against it — §1.1).
 */
val DarkSurface = Color(0xFF1E1F22)   // Warna dasar material
val DarkShadow = Color(0xFF0C0D0F)    // Bayangan bawah
val LightHighlight = Color(0xFF2E3136) // Pantulan cahaya atas
val AccentNeon = Color(0xFF00FFCC)    // Warna aksen (misal untuk lampu indikator)

/**
 * §4 Simulasi Material Logam/Kaca Gelap — procedural gradient in place of a
 * bitmap texture, kept as a single top-level `val` (not recomputed inside
 * composition) per the §6.3 `remember`/avoid-recompute performance tip:
 * a plain top-level `Brush` is already built once at classload, so no
 * `remember{}` wrapper is needed for this specific case.
 */
val metallicDarkBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2A2D32), // Terang di sudut kiri atas
        Color(0xFF1A1C1E), // Warna dasar di tengah
        Color(0xFF131416)  // Gelap di sudut kanan bawah
    )
)
