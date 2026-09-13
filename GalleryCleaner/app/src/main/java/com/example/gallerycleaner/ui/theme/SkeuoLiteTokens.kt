package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Skeuomorphism-lite — Amber Reserve Edition (Batch27, retuned Batch28).
 *
 * A distinct material language from `MidnightGlass`, not a recolor of it.
 * Where glass panels are translucent and float over an ambient gradient,
 * these panels are opaque/matte and read as physically raised off a flat
 * espresso backdrop — a "leather-bound ledger" object language to match
 * Amber Reserve's brass/oxblood palette (see `Color.kt` comment on that
 * palette's intent).
 *
 * **Batch28 fix — user feedback: "efek timbul gak kerasa sama sekali,
 * mirip ganti pallet warna"**. Root cause diagnosed from the screenshots:
 * Batch27's `PanelFill` was a single FLAT color, and `ShadowColor` was a
 * near-black drop shadow drawn against an already near-black espresso
 * background — a dark shadow on a dark backdrop is essentially invisible.
 * With no gradient and an invisible shadow, all that was left visibly
 * different from a plain Material outlined card was the border color —
 * exactly the "pallet warna murahan" the user called out, even though the
 * underlying modifier chain WAS architecturally different (shadow+fill+
 * border vs `glassPanel`'s translucent version). Correct diagnosis, wrong
 * value tuning — this batch fixes the VALUES, the architecture from
 * Batch27 (`MaterialStyle`/`skeuoPanel`/theme-aware `GlassCard`) is
 * unchanged.
 *
 * Fix, three parts (still just "shadow + fill + border", no new
 * technique/`Modifier.blur` introduced):
 * 1. [PanelFillGradient] — fill is now a diagonal 2-stop gradient (light
 *    catching the top-left corner → darker in the bottom-right "shadow"
 *    corner of the object itself), not a flat color. This is the single
 *    biggest missing cue: a raised object's OWN face has a light
 *    gradient across it from the implied light source, independent of
 *    any shadow underneath it.
 * 2. [BevelGradient] — contrast roughly doubled (highlight alpha 0x8A→0xF0,
 *    shadow alpha 0x66→0xB3) and border width upped (`1.5.dp`→`2.dp` in
 *    `SkeuoModifier.kt`) so the bevel edge is legible at a glance instead
 *    of reading as a generic thin outline.
 * 3. [SpecularHighlight] — new: a small soft white/brass glow anchored at
 *    the panel's top-left corner (`SkeuoModifier.skeuoPanel` layers this
 *    as a second clipped background fill, after the base fill, before the
 *    border). This is the cue that was missing entirely before — a flat
 *    fill + shadow alone reads as "box with a border" no matter how dark
 *    the shadow is tuned; a corner specular catch is what reads as
 *    "curved/raised surface reflecting a light source" to the eye.
 *
 * Deliberately still "lite" — NOT the old, fully-deleted
 * `SkeuoMidnightTokens`/`SkeuoMidnightModifier`/`MidnightSkeuoButton`/
 * `MidnightSkeuoSlot` system (multi-layer metallic gradients, simulated
 * debossed slots, removed Batch21 for being visually heavy). This is
 * still exactly 3 ingredients — gradient fill, specular overlay, bevel
 * border — just tuned to actually be visible on a dark backdrop instead
 * of 3 ingredients that were individually too subtle to read.
 */
object SkeuoLite {
    // ---- Panel fill: diagonal gradient, NOT a flat color (Batch28 fix —
    // see class doc part 1). Brush.linearGradient's default start/end
    // (Offset.Zero → Offset.Infinite) already runs top-left → bottom-right,
    // matching the same implied light source the bevel border and specular
    // highlight both use, so all three cues agree with each other. ----
    val PanelFillTop = Color(0xFF423522)       // catches the light — lighter than old flat PanelFill
    val PanelFillBottom = Color(0xFF1A150D)    // recedes into shadow — darker than old flat PanelFill
    val PanelFillGradient = Brush.linearGradient(listOf(PanelFillTop, PanelFillBottom))
    // Pressed: fill gradient runs in reverse (dark top-left → lighter
    // bottom-right) — combined with the reversed bevel below, this is what
    // sells "pushed in" rather than just "recolored".
    val PanelFillGradientPressed = Brush.linearGradient(listOf(PanelFillBottom, EspressoSurface))

    // ---- Specular highlight (Batch28 new — see class doc part 3): a soft
    // corner glow layered on top of the base fill, clipped to the same
    // panel shape. Anchored at a fixed local origin/radius rather than a
    // size-relative one — deliberately "lite": one static Brush reused by
    // every panel size (chip or full card) rather than a per-draw
    // size-aware gradient, same "no extra machinery" budget as the rest
    // of this object. ----
    val SpecularHighlight = Brush.radialGradient(
        colors = listOf(Color(0x4DFFF3D6), Color(0x00FFF3D6)),
        center = Offset(0f, 0f),
        radius = 260f
    )
    val SpecularHighlightPressed = Brush.radialGradient(
        colors = listOf(Color(0x00FFF3D6), Color(0x00FFF3D6)),
        center = Offset(0f, 0f),
        radius = 260f
    ) // pressed/inset panels get no specular catch — a recessed slot doesn't reflect light back at the viewer

    // ---- Bevel edge (simulates a physical edge catching light) — contrast
    // roughly doubled from Batch27 (Batch28 fix part 2) ----
    val BevelHighlight = Color(0xF0F5DFAE)         // warm brass-tinted light catch, top-left — was 0x8A, now near-solid
    val BevelShadow = Color(0xB3000000)            // true dark shadow, bottom-right — was 0x66, now much stronger
    val BevelGradient = Brush.linearGradient(listOf(BevelHighlight, EspressoOutline, BevelShadow))
    // Pressed state: the bevel direction visually inverts (top-left goes
    // dark, bottom-right catches a thin rim light) — this, not a color
    // change alone, is what sells "pushed in" for a raised-object material.
    val BevelGradientPressed = Brush.linearGradient(listOf(BevelShadow, EspressoOutline, BevelHighlight))

    // ---- Drop shadow tuning. Kept (still contributes a soft dark falloff
    // at the panel's bottom-right silhouette against the backdrop) but is
    // now a SECONDARY cue rather than the primary "raised" signal — the
    // gradient fill + specular + bevel above carry that now, since a dark
    // shadow alone was proven (Batch27 screenshots) to be invisible on
    // this dark a backdrop. ----
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
    // backdrop, mirroring how MidnightGlass pairs Void/Ice. Given the same
    // gradient-fill + specular + bevel treatment as the dark variant above
    // (Batch28) rather than the flat color it had in Batch27. ----
    val PanelFillTopLight = Color(0xFFFCF7EC)
    val PanelFillBottomLight = Color(0xFFE0D3AE)
    val PanelFillGradientLight = Brush.linearGradient(listOf(PanelFillTopLight, PanelFillBottomLight))
    val PanelFillGradientPressedLight = Brush.linearGradient(listOf(PanelFillBottomLight, CreamSurfaceRaised))
    val SpecularHighlightLight = Brush.radialGradient(
        colors = listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)),
        center = Offset(0f, 0f),
        radius = 260f
    )
    val BevelHighlightLight = Color(0xF2FFFFFF)
    val BevelShadowLight = Color(0x66695A34)
    val BevelGradientLight = Brush.linearGradient(listOf(BevelHighlightLight, CreamOutline, BevelShadowLight))
    val BevelGradientPressedLight = Brush.linearGradient(listOf(BevelShadowLight, CreamOutline, BevelHighlightLight))
    val TextBrightLight = EspressoTextPrimary
    val TextMutedLight = EspressoTextSecondary
    val AccentBrassLight = BrassKeepOnLight
}
