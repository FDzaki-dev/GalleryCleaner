package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pure Neumorphism (Soft UI) — Amber Reserve Edition (Batch36 full
 * redesign, replaces Skeuomorphism-lite from Batch27/28 — see
 * PROJECT_STATE.md Batch36 for the full rationale).
 *
 * Explicit user requirement for this batch: "eksplisit Neumorphism murni
 * tanpa rekayasa ngide sendiri & tanpa hybrid baseline bersama dari theme
 * lain" — so unlike [SkeuoLite] (which reused several [Color.kt] tokens
 * from the old espresso/brass palette as aliases, e.g. `AccentBrass =
 * BrassKeep`), this object is 100% self-contained: every value below is
 * either the EXACT hex the user specified, or a value mechanically
 * DERIVED from those exact hexes (documented per-value below) — nothing
 * is picked freehand, and nothing is aliased from [SkeuoLite]/[Color.kt]'s
 * old Amber Reserve tokens. Zero shared baseline with `SkeuoLite` or
 * `MidnightGlass` at the token level, matching zero shared baseline at
 * the rendering-technique level (see `NeumorphSurface.kt`).
 *
 * ---- Given palette (verbatim from user spec, WCAG-compliant) ----
 * 60% Dominant (Base):     DeepNavy `#0F172A` — latar belakang utama.
 * 30% Structural:          NavyCard `#1E293B` — kontainer/topbar/komponen.
 * 10% Accent:              ClassicBrass `#D4AF37` — CTA utama & status aktif.
 * Teks utama:              TextPrimary `#F8FAFC` on Navy (contrast 17.06:1,
 *                          verified — well past AAA's 7:1).
 * Teks tombol Brass:       TextOnBrass `#0F172A` (contrast 8.49:1 on
 *                          ClassicBrass, verified — past AAA's 4.5:1 for
 *                          large/bold CTA text).
 */
object Neumorph {
    // ============ EXACT palette given by user — unchanged ============
    val DeepNavy = Color(0xFF0F172A)
    val NavyCard = Color(0xFF1E293B)
    val ClassicBrass = Color(0xFFD4AF37)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextOnBrass = Color(0xFF0F172A)

    /** Secondary/muted text (captions, subtitles) — NOT given by the spec
     *  (only "Teks Utama" and "Tombol Brass" text rules were specified).
     *  Rather than invent a new hue, this is [TextPrimary] at reduced
     *  alpha — a derived variant of the one text color that WAS given,
     *  not a new color identity. 68% alpha keeps contrast on [DeepNavy]
     *  comfortably above AA (~11.6:1) for small/secondary text. */
    val TextSecondary = TextPrimary.copy(alpha = 0.68f)

    /** CTA pressed-state fill — [ClassicBrass] darkened (HLS lightness
     *  0.62→0.42, same hue/saturation), same hue-preserving technique
     *  this project already uses for every OTHER theme's "OnLight" accent
     *  variant ([BrassKeepOnLight], [PeriwinkleKeepOnLight], etc.) — a
     *  mechanical derivation of the given hex, not a new one. */
    val ClassicBrassPressed = Color(0xFFB09026)

    // ============ Shadow pair — the neumorphism dual-shadow recipe ============
    // NOT a new invented hue: pure black/white blended into the surface at
    // low alpha is the textbook neumorphism shadow technique (light source
    // top-left = lighter-than-surface shadow, falloff bottom-right =
    // darker-than-surface shadow) — see NeumorphSurface.kt doc comment for
    // why this needs two independently-offset `Modifier.shadow()` layers
    // instead of `skeuoPanel`/`glassPanel`'s single ambient shadow.
    val ShadowDark = Color(0x8C000000)   // bottom-right, alpha ≈0.55
    val ShadowLight = Color(0x0CFFFFFF)  // top-left, alpha ≈0.045 (subtle on a near-black base)

    // ============ Light-mode counterpart ============
    // The user's spec only gave a dark palette. Rather than invent an
    // unrelated light scheme (e.g. reusing the old cream/oxblood "ledger"
    // look, which IS a different, uncoordinated hue family), this is
    // computed mechanically FROM the exact 3 given hexes — same hue,
    // lightness pushed up in HLS space — the identical hue-preserving
    // technique this codebase already used 3 times (EspressoBg→CreamBg,
    // IndigoBg→LilacBg, MidnightGlass Void→Ice). Flagged in
    // PROJECT_STATE.md Batch36 as a default open to correction if a light
    // spec is given later.
    val LightBg = Color(0xFFF1F4FC)          // DeepNavy hue, HLS lightness → 0.965
    val LightCard = Color(0xFFDCE3EF)        // NavyCard hue, HLS lightness → 0.90
    val BrassOnLight = Color(0xFFA3841F)     // ClassicBrass hue, HLS lightness → 0.38 (contrast 5.0:1 vs TextOnBrass, passes AA)
    val TextPrimaryOnLight = DeepNavy         // reuses the exact given dark-navy hex as dark-on-light text (0 new hue, contrast 16.23:1)
    val TextSecondaryOnLight = DeepNavy.copy(alpha = 0.64f)

    // Light-mode shadow pair — standard soft-UI-on-light-bg formula
    // (stronger white highlight since the base is already near-white,
    // softer black shadow so it doesn't read as a hard dark card).
    val ShadowDarkOnLight = Color(0x26000000)   // alpha ≈0.15
    val ShadowLightOnLight = Color(0xE6FFFFFF)  // alpha ≈0.90
}
