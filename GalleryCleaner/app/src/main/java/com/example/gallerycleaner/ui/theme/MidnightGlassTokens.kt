package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism — Midnight Blue Edition.
 *
 * Full override of the previous "Skeuomorphism-Dark Midnight Blue"
 * system (`SkeuoMidnightTokens.kt` + `SkeuoMidnightModifier.kt` +
 * `MidnightSkeuoButton.kt` + `MidnightSkeuoSlot.kt`, all deleted this
 * batch). Same hue family (deep navy/indigo blue) carried forward —
 * only the *material language* changes: raised/debossed metallic
 * skeuomorphism → translucent, soft-edged frosted glass panels
 * floating over a layered blue-black ambient gradient.
 *
 * Real backdrop blur (`Modifier.blur` / RenderEffect) needs API 31+ and
 * silently no-ops below that — with `minSdk = 24` that would mean a
 * visibly different (non-glass) look on a meaningful slice of devices.
 * Instead the frosted look here is built entirely from translucency +
 * gradient + a soft two-tone edge-light border, which renders
 * identically on every supported API level.
 */
object MidnightGlass {
    // ---- Ambient background (ombre wash the glass panels float over) ----
    val VoidDeep = Color(0xFF040711)        // Near-black base, screen corners
    val NavyCore = Color(0xFF0B1226)        // Mid wash
    val IndigoGlow = Color(0xFF16204A)      // Warmer bloom, screen center-top
    val AzureBloom = Color(0xFF1B2C63)      // Brightest ambient accent, used sparingly

    val AmbientGradient = Brush.radialGradient(
        colors = listOf(AzureBloom.copy(alpha = 0.55f), IndigoGlow.copy(alpha = 0.35f), NavyCore, VoidDeep),
        radius = 1400f
    )

    // ---- Glass panel fill (translucent, layered light-to-dark) ----
    val GlassTintTop = Color(0x2E7C9CFF)    // Cool blue-white sheen, top edge of a panel
    val GlassTintBottom = Color(0x14304A8C) // Deeper, cooler tint toward the bottom
    val GlassFillGradient = Brush.verticalGradient(listOf(GlassTintTop, GlassTintBottom))

    // Flat fallback (non-Brush contexts, e.g. CardColors which need a solid Color)
    val GlassSurfaceFlat = Color(0xE6101A33) // ~90% opaque navy, reads as "surface" for M3 defaults

    // ---- Edge light (the border stroke that sells the "glass" read) ----
    val EdgeHighlight = Color(0x99FFFFFF)   // Bright specular catch, top-left
    val EdgeFade = Color(0x00FFFFFF)        // Fully transparent midpoint
    val EdgeShadow = Color(0x33000A1F)      // Faint cool shadow, bottom-right
    val EdgeGradient = Brush.linearGradient(listOf(EdgeHighlight, EdgeFade, EdgeShadow))

    // ---- Inset / pressed variant (slightly recessed glass, for input-like slots) ----
    val InsetTintTop = Color(0x1A0A1430)
    val InsetTintBottom = Color(0x267C9CFF)
    val InsetFillGradient = Brush.verticalGradient(listOf(InsetTintTop, InsetTintBottom))

    // ---- Accent glow (selection / focus / progress / active state) ----
    val GlowBlue = Color(0xFF6C8CFF)        // Primary glass accent — Midnight Blue's signature hue
    val GlowBlueDim = Color(0xFF2A3B77)     // Same hue, low-emphasis container

    // ---- Text ----
    val TextBright = Color(0xFFF3F6FF)
    val TextMuted = Color(0xFFA9B4D6)

    // ---- Light-mode counterpart: "frosted ice" rather than "midnight" ----
    // Kept in the same file (not a separate object) since it's the same
    // material language, just re-lit for a bright backdrop.
    val IceBackground = Color(0xFFEFF3FC)
    val IceAmbientGradient = Brush.radialGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFE4EAFB), IceBackground)
    )
    val IceGlassFillGradient = Brush.verticalGradient(
        listOf(Color(0xB3FFFFFF), Color(0x99D6E1FA))
    )
    val IceGlassSurfaceFlat = Color(0xF2FFFFFF)
    val IceEdgeGradient = Brush.linearGradient(
        listOf(Color(0xCCFFFFFF), Color(0x00FFFFFF), Color(0x33AAB8E0))
    )
    val IceTextPrimary = Color(0xFF141A33)
    val IceTextSecondary = Color(0xFF5B6688)
    val GlowBlueOnLight = Color(0xFF3E5CD6)
}
