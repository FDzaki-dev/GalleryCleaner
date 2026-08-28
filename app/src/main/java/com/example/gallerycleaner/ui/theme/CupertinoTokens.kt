package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pure Cupertino (iOS) material language — Indigo Noir Edition (Batch74,
 * stage 1/2 — see PROJECT_STATE.md Batch74 for full rationale).
 *
 * Explicit user requirement for this batch: "restyling total theme Indigo
 * Noir -> 'Cupertino Style' murni 100%!!" — same "murni, no hybrid
 * baseline" standard this project already held Amber Reserve's Neumorph
 * redesign to (Batch36). Two things follow from that:
 *
 * 1. This is a MATERIAL-LANGUAGE swap (how the surface renders), not a
 *    re-hue — Indigo Noir's given palette (deep indigo bg, platinum text,
 *    periwinkle "keep" / dusty-rose "delete" accents, see [Color.kt]'s
 *    "---- Indigo Noir ----" section) is UNCHANGED and REUSED here by
 *    direct alias, not invented fresh. What changes is the *technique*:
 *    Indigo Noir currently renders through [MaterialStyle.GLASS], which
 *    means its actual panel fill/edge today is [MidnightGlass]'s blue
 *    frosted-glass gradient (Signature's look) — `glassPanel()`'s
 *    fill/edge params are never overridden per-theme, only the M3
 *    `ColorScheme` (text/icon colors) is Indigo-specific. Moving Indigo
 *    Noir to its own [MaterialStyle.CUPERTINO] (stage 2) finally gives it
 *    a panel surface that's ACTUALLY indigo/platinum/dusty-rose, not a
 *    reskinned Signature panel with Indigo-colored text on top.
 * 2. Every value below is either an exact alias of an existing Indigo Noir
 *    hex from [Color.kt] (documented per-value) or mechanically DERIVED
 *    from one (same hue, alpha/lightness changed) — zero new invented hue,
 *    zero color borrowed from [MidnightGlass]/[SkeuoLite]/[Neumorph].
 *
 * ---- What "pure Cupertino" means as a rendering technique here ----
 * iOS's grouped-card style is deliberately the opposite recipe from both
 * existing materials in this app:
 *
 * |                          | shadow(s)         | fill                | border/edge          |
 * |--------------------------|--------------------|-----------------------|------------------------|
 * | `glassPanel` (Signature) | 1, ambient          | translucent gradient  | visible gradient edge  |
 * | `NeumorphSurface` (Amber)| 2, independently offset | flat solid       | none                    |
 * | `CupertinoSurface` (this)| 1, soft/low-opacity | flat solid (opaque)  | hairline only, optional |
 *
 * No translucency (iOS grouped cells are opaque, not glass), no dual
 * shadow (that's neumorphism's signature, not iOS's), a single soft
 * far-spread low-opacity shadow instead of glass's crisper elevation
 * shadow, and an optional hairline separator instead of a visible
 * gradient-lit edge. See `CupertinoSurface.kt` (stage 2) for the actual
 * rendering — this file is tokens only.
 *
 * ---- Dark palette (aliased from existing Indigo Noir, Color.kt) ----
 */
object Cupertino {
    // ============ Direct aliases — Indigo Noir's own existing palette ============
    /** Screen background — same as `IndigoBg`. */
    val Bg = IndigoBg
    /** Grouped-card fill — iOS's "one step lighter than the screen" rule;
     *  `IndigoSurface` already IS exactly that step for this theme. */
    val CardFill = IndigoSurface
    /** Nested/inset row fill (e.g. a value slot inside a card) — the next
     *  step up again, same as glass's "raised" tier. */
    val CardFillRaised = IndigoSurfaceRaised
    val TextPrimary = PlatinumText
    val TextSecondary = PlatinumTextSecondary
    /** Primary CTA fill — this theme's existing 3rd accent tier
     *  (`PeriwinkleKeep`, already used for the swipe-keep action) reused as
     *  the solid, fully-opaque iOS button fill — same "theme's own accent
     *  becomes the CTA fill" choice this project already made for Amber
     *  Reserve's `ClassicBrass`. */
    val AccentFill = PeriwinkleKeep
    /** Dark text on the light-periwinkle CTA fill — reuses this theme's
     *  OWN light-mode primary-text hex (`IndigoTextPrimary`) rather than a
     *  generic near-black, so the CTA text is still drawn from Indigo
     *  Noir's own palette, not a cross-theme value. Contrast on
     *  `PeriwinkleKeep` ≈ 8.9:1 — comfortably past AAA's 7:1 for normal
     *  text. */
    val TextOnAccent = IndigoTextPrimary

    // ============ Derived — the iOS-specific rendering primitives ============
    /** Single soft ambient shadow. iOS grouped-cell shadows are subtle: low
     *  opacity, no color tint (pure black, not a hue-tinted glow like
     *  glass's `VoidDeep`/`GlowBlue` shadow) — 20% alpha is the same
     *  "considered, not decorative" restraint this app already applies to
     *  its accent hues (see the "Indigo Noir" comment block in Color.kt). */
    val ShadowSoft = Color(0x33000000) // black, alpha ≈0.20

    /** Hairline separator — iOS's `separatorColor` equivalent: text color
     *  at very low alpha, NOT a distinct outline hue like `IndigoOutline`
     *  (that's the glass edge-gradient's base, a different visual role).
     *  12% keeps it barely-there, present only on close inspection, same
     *  restraint intent as [ShadowSoft]. */
    val Hairline = PlatinumText.copy(alpha = 0.12f)

    /** Press-state dim — iOS's actual press mechanic is opacity reduction
     *  on the whole control (not a fill-color swap like Glass's glow-tint
     *  or a shadow-removal like Neumorph's pressed state) — applied via
     *  `Modifier.alpha()` at the call site, this constant is the target
     *  alpha, not a color. Documented here (not in `CupertinoSurface.kt`)
     *  so every Cupertino call site derives the same "how much dimmer"
     *  answer instead of picking its own value. */
    const val PRESSED_ALPHA = 0.72f

    // ============ Light-mode counterpart — same aliasing rule ============
    val BgOnLight = LilacBg
    val CardFillOnLight = LilacSurfaceRaised
    val CardFillRaisedOnLight = LilacOutline
    val TextPrimaryOnLight = IndigoTextPrimary
    val TextSecondaryOnLight = IndigoTextSecondary
    val AccentFillOnLight = PeriwinkleKeepOnLight
    /** Light text on the darker light-mode accent fill — reuses this
     *  theme's own `PlatinumText`, contrast ≈8.1:1 on `PeriwinkleKeepOnLight`,
     *  past AAA. */
    val TextOnAccentOnLight = PlatinumText
    /** Slightly stronger than the dark variant (12%→15%) — a light
     *  background needs marginally more shadow density to read as lifted
     *  at all, same adjustment direction Neumorph's light counterpart
     *  already made for its own shadow pair. */
    val ShadowSoftOnLight = Color(0x26000000) // black, alpha ≈0.15
    val HairlineOnLight = IndigoTextPrimary.copy(alpha = 0.10f)
}
