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
 * **Wiring status (unchanged from Batch84)**: only [Card] is wired — it's
 * `NeumorphSurface`'s own `shape` default, which is what `GlassCard.kt`
 * renders (by far the most common Neumorph panel in the app: every
 * dashboard tile, list row, dialog) — so the cut-corner look already
 * reaches the app's main surfaces from this file alone. [Button] and
 * [Chip] are defined here with their new cut-corner values ready, but
 * `GlassButton.kt`'s NEUMORPH branch and `SwipeScreenControls.kt`'s
 * `InfoChip` still pass their OLD bare `RoundedCornerShape(16.dp)` /
 * `RoundedCornerShape(6.dp)` literals directly — meaning the CTA button
 * and small info chip will still render ROUNDED (not yet reskinned) until
 * those 2 call sites are wired to read from here. Deferred again this
 * batch to stay inside the 3-file-per-batch code cap (this file +
 * `NeumorphTokens.kt` + `NeumorphTypography.kt` already fill this batch's
 * 3 slots) — flagged with elevated priority in PROJECT_STATE.md's Pending
 * Queue this time, since it's now a visible style inconsistency (angular
 * cards next to rounded button/chip) rather than just an unfinished
 * "murni" cleanup.
 */
object NeumorphShape {
    /** Wired — see [com.example.gallerycleaner.ui.components.NeumorphSurface]'s `shape` default. */
    val Card: Shape = CutCornerShape(16.dp)

    /** Defined, NOT yet wired — `GlassButton.kt` still passes `RoundedCornerShape(16.dp)` inline (still rounded). */
    val Button: Shape = CutCornerShape(12.dp)

    /** Defined, NOT yet wired — `SwipeScreenControls.kt`'s `InfoChip` still passes `RoundedCornerShape(6.dp)` inline (still rounded). */
    val Chip: Shape = CutCornerShape(6.dp)
}
