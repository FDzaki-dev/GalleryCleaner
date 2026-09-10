package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Pure Neumorphism (Soft UI) shape — Amber Reserve exclusive.
 *
 * Through Batch83, every [com.example.gallerycleaner.ui.components.NeumorphSurface]
 * call site (`GlassCard.kt`'s default, `GlassButton.kt`'s CTA,
 * `SwipeScreenControls.kt`'s `InfoChip`) passed its own bare
 * `RoundedCornerShape(Ndp)` literal inline — unlike [NeumorphTokens]
 * (Batch36) and [NeumorphTypography] (Batch80), corner radius had no
 * single "murni" (theme-owned) source. Batch84 closed that by adding this
 * file (rounded corners, +4dp/role) and wiring [Card] into
 * `NeumorphSurface`'s default.
 *
 * **[GalleryShapes] (Shape.kt) is NOT the baseline being replaced here** —
 * investigated Batch84 (same "read before touching" discipline as every
 * prior audit item): it's entirely unwired project-wide — 0 call site
 * anywhere in the app reads it, and `Theme.kt`'s `GalleryCleanerTheme`
 * never passes `shapes = ...` to `MaterialTheme(...)` at all. So unlike
 * typography (which WAS actively reused from a shared, wired baseline
 * before Batch80), shape never had a shared baseline for Amber Reserve to
 * diverge from — every material (`glassPanel`, `skeuoPanel`,
 * `CupertinoSurface`, `NeumorphSurface`) already always supplied its own
 * standalone literal.
 *
 * **Batch85 — Blade Runner reskin**: per explicit user request ("theme
 * tetap Neumorphism, tapi...shape...pakai gaya visual ala Blade Runner").
 * Switches every role from [androidx.compose.foundation.shape.RoundedCornerShape]
 * to [CutCornerShape] — a diagonally-chamfered corner instead of a rounded
 * one, the angular "tech panel/HUD bracket" silhouette common to
 * Blade-Runner-adjacent sci-fi UI, as opposed to Batch84's deliberately
 * "pillowy" rounder-is-softer direction. This does NOT abandon
 * "Neumorphism" — the thing that makes a surface read as neumorphic is
 * [com.example.gallerycleaner.ui.components.NeumorphSurface]'s dual
 * offset shadow + flat monochrome fill (see that file's own doc comment),
 * and `Modifier.shadow()`/`.background()`/`.border()` all accept ANY
 * [Shape] — none of that recipe is rounded-corner-specific. Only the
 * corner GEOMETRY changes here; the soft-UI shadow/fill/border mechanism
 * that actually defines the material is completely untouched.
 * Values: Card 24dp round→16dp cut, Button 20dp round→12dp cut, Chip
 * 10dp round→6dp cut — sized down from Batch84's round values because a
 * cut corner of a given dp reads visually "bigger"/more aggressive than a
 * round corner of the same dp (removes a full right-triangle of area vs.
 * a rounded arc), so these are chosen to read as a comparable degree of
 * corner treatment, not a literal same-number carry-over. `CutCornerShape`
 * clamps gracefully if a cut would exceed a component's available space
 * (standard `CornerBasedShape` behavior), so [Chip] is safe even on the
 * smallest badge.
 *
 * **Wiring status (Batch91 — all 3 roles now wired)**: [Card] was already
 * `NeumorphSurface`'s own `shape` default since Batch84 (what `GlassCard.kt`
 * renders — by far the most common Neumorph panel in the app: every
 * dashboard tile, list row, dialog). As of Batch91, [Button] and [Chip] are
 * wired too — `GlassButton.kt`'s NEUMORPH branch now passes `NeumorphShape.Button`
 * (was bare `RoundedCornerShape(16.dp)`) and `SwipeScreenControls.kt`'s
 * `InfoChip` NEUMORPH branch now passes `NeumorphShape.Chip` (was bare
 * `RoundedCornerShape(6.dp)`) — both call sites read from this file instead
 * of carrying their own stale rounded literal. The angular cut-corner look
 * (Batch85's "Blade Runner" reskin) now reaches every Neumorph surface in
 * the app — card, CTA button, and info chip alike; the inconsistency flagged
 * in PROJECT_STATE.md's Pending Queue since Batch85 is closed. Only 2
 * call sites existed for [Button]/[Chip] project-wide (grepped, confirmed
 * before editing) — no other file references these tokens.
 */
object NeumorphShape {
    /** Wired — see [com.example.gallerycleaner.ui.components.NeumorphSurface]'s `shape` default. */
    val Card: Shape = CutCornerShape(16.dp)

    /** Wired (Batch91) — `GlassButton.kt`'s NEUMORPH branch passes this directly. */
    val Button: Shape = CutCornerShape(12.dp)

    /** Wired (Batch91) — `SwipeScreenControls.kt`'s `InfoChip` NEUMORPH branch passes this directly. */
    val Chip: Shape = CutCornerShape(6.dp)
}
