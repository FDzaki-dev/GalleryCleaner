package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Skeuomorphism-lite — Amber Reserve Edition (Batch27).
 *
 * A distinct material language from `MidnightGlass`, not a recolor of it.
 * Where glass panels are translucent and float over an ambient gradient,
 * these panels are opaque/matte and read as physically raised off a flat
 * espresso backdrop — a "leather-bound ledger" object language to match
 * Amber Reserve's brass/oxblood palette (see `Color.kt` comment on that
 * palette's intent).
 *
 * Deliberately "lite" — this does NOT resurrect the old, fully-deleted
 * `SkeuoMidnightTokens`/`SkeuoMidnightModifier`/`MidnightSkeuoButton`/
 * `MidnightSkeuoSlot` system (multi-layer metallic gradients, simulated
 * debossed slots). That system was removed batch 21 because it was
 * visually heavy and hard to keep consistent across every component. This
 * version is intentionally simpler: ONE solid fill color, ONE directional
 * drop shadow (bottom-right, like a raised card catching light from
 * top-left), and a 2-stop border gradient for the bevel edge. Same
 * "3 ingredients" budget `MidnightGlass` panels use (shadow + fill +
 * border) — different values, not more machinery.
 */
object SkeuoLite {
    // ---- Panel fill (opaque, NOT translucent — this is the core material
    // difference from MidnightGlass: a skeuomorphic object is solid) ----
    val PanelFill = EspressoSurfaceRaised          // matte raised card face
    val PanelFillPressed = EspressoSurface         // darker/recessed on press — deboss, not glow

    // ---- Bevel edge (simulates a physical edge catching light) ----
    val BevelHighlight = Color(0x8AF0DDB0)         // warm brass-tinted light catch, top-left
    val BevelShadow = Color(0x66000000)            // true dark shadow, bottom-right — NOT a cool blue like glass's EdgeShadow
    val BevelGradient = Brush.linearGradient(listOf(BevelHighlight, EspressoOutline, BevelShadow))
    // Pressed state: the bevel direction visually inverts (top-left goes
    // dark, bottom-right catches a thin rim light) — this, not a color
    // change alone, is what sells "pushed in" for a raised-object material.
    val BevelGradientPressed = Brush.linearGradient(listOf(BevelShadow, EspressoOutline, BevelHighlight))

    // ---- Drop shadow tuning (single directional shadow, not glass's soft
    // ambient — a raised object needs a shadow color that reads as "cast
    // onto the espresso backdrop below it", i.e. warm-tinted near-black
    // rather than glass's cool VoidDeep) ----
    val ShadowColor = Color(0xFF0B0906)

    // ---- Accent glow analogue — used for focus/active states, brass hue
    // instead of glass's blue GlowBlue ----
    val AccentBrass = BrassKeep
    val AccentBrassDim = BrassKeepDim

    // ---- Text (reuses Amber Reserve's existing Ivory tokens — text color
    // is a palette concern, not a material-language one) ----
    val TextBright = IvoryText
    val TextMuted = IvoryTextSecondary

    // ---- Light-mode counterpart ("cream ledger" rather than "espresso
    // ledger") — same bevel-object material language, re-lit for a bright
    // backdrop, mirroring how MidnightGlass pairs Void/Ice ----
    val PanelFillLight = CreamSurfaceRaised
    val PanelFillPressedLight = Color(0xFFE6DBC0)
    val BevelHighlightLight = Color(0xCCFFFFFF)
    val BevelShadowLight = Color(0x40695A34)
    val BevelGradientLight = Brush.linearGradient(listOf(BevelHighlightLight, CreamOutline, BevelShadowLight))
    val TextBrightLight = EspressoTextPrimary
    val TextMutedLight = EspressoTextSecondary
    val AccentBrassLight = BrassKeepOnLight
}
