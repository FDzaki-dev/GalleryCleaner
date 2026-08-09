package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Skeuomorphism-Dark (Midnight Blue Edition) — from uploaded spec
 * "Panduan Lengkap: Desain Visual Skeuomorphism-Dark (Midnight Blue
 * Edition)", §2 Palet Warna & Brush Prosedural. Nilai 100% copy dari
 * spec; satu-satunya perubahan adalah nama package (spec asli
 * `com.example.skeuomorphism.ui.theme` → project ini
 * `com.example.gallerycleaner.ui.theme`), sama seperti adaptasi paket
 * pada override tema sebelumnya.
 *
 * Full override (bukan partial): file ini MENGGANTIKAN seluruh sistem
 * token tema lama (`GlassTokens.kt`, `TactileTokens.kt`, dan
 * `SkeuoTokens.kt` versi Cyan Batch12) — ketiganya dihapus di batch ini,
 * bukan dipertahankan berdampingan.
 */
object SkeuoMidnightTheme {
    // Surface & Shadow Tokens
    val BaseSurface = Color(0xFF0F172A)       // Deep Midnight Slate
    val DarkShadow = Color(0xFF050B14)        // Ambient Drop Shadow (Bottom-Right)
    val LightHighlight = Color(0xFF23324D)    // Specular Light (Top-Left)

    // Inset Tokens (Debossed/Cekung)
    val InnerShadowDark = Color(0xFF070D18)   // Cekungan Atas-Kiri
    val InnerShadowLight = Color(0xFF1E293B)  // Refleksi Bawah-Kanan

    // Accent Tokens
    val ElectricCyan = Color(0xFF00E5FF)      // Active Glow Accent
    val TextMuted = Color(0xFF94A3B8)         // Secondary Text
    val TextBright = Color(0xFFF8FAFC)        // Primary Text

    // Gradient Material Timbul (Metallic/Glass Midnight)
    val RaisedGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E293B), // Pantulan sudut atas-kiri
            Color(0xFF0F172A), // Inti material tengah
            Color(0xFF070D18)  // Bayangan sudut bawah-kanan
        )
    )

    // Gradient Material Cekung (Slot/Input Inset)
    val InsetGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF070D18), // Gelap di atas
            Color(0xFF0F172A), // Warna dasar di tengah
            Color(0xFF1E293B)  // Refleksi cahaya lembut di bawah
        )
    )
}
