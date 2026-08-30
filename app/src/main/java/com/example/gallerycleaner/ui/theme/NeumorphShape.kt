package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Pure Neumorphism (Soft UI) shape — Amber Reserve exclusive.
 *
 * Through Batch82, every [com.example.gallerycleaner.ui.components.NeumorphSurface]
 * call site (`GlassCard.kt`'s default, `GlassButton.kt`'s CTA,
 * `SwipeScreenControls.kt`'s `InfoChip`) passed its own bare
 * `RoundedCornerShape(Ndp)` literal inline — unlike [NeumorphTokens]
 * (Batch36) and [NeumorphTypography] (Batch80), corner radius had no
 * single "murni" (theme-owned) source. This file closes that gap.
 *
 * **[GalleryShapes] (Shape.kt) is NOT the baseline being replaced here** —
 * investigated first (same "read before touching" discipline as every
 * prior audit item): it's entirely unwired project-wide — 0 call site
 * anywhere in the app reads it, and `Theme.kt`'s `GalleryCleanerTheme`
 * never passes `shapes = ...` to `MaterialTheme(...)` at all. So unlike
 * typography (which WAS actively reused from a shared, wired baseline
 * before Batch80), shape never had a shared baseline for Amber Reserve to
 * diverge from — every material (`glassPanel`, `skeuoPanel`,
 * `CupertinoSurface`, `NeumorphSurface`) already always supplied its own
 * standalone literal. This file doesn't change that app-wide pattern; it
 * only gives Amber Reserve's own literals one owned, named home instead of
 * bare numbers repeated inline.
 *
 * Values are mechanically bumped +4dp per role from the pre-existing
 * literals (Card 20→24dp, Button 16→20dp, Chip 6→10dp) — one uniform step
 * applied to every role, not picked per-value, the same discipline
 * [NeumorphTypography]'s +1-weight-step rule already established.
 * Direction (rounder, not sharper): a more generous corner radius reads as
 * softer/more pillow-like, reinforcing the same soft-UI identity the
 * dual-offset shadow already carries — a standard soft-UI shape
 * convention, not a freehand pick.
 *
 * **Wiring status (this batch)**: only [Card] is wired so far — it's now
 * `NeumorphSurface`'s own `shape` default, which is what `GlassCard.kt`
 * renders (by far the most common Neumorph panel in the app: every
 * dashboard tile, list row, dialog). [Button] and [Chip] are defined here
 * with their bumped values ready, but `GlassButton.kt`'s NEUMORPH branch
 * and `SwipeScreenControls.kt`'s `InfoChip` still pass their OLD bare
 * literals (`RoundedCornerShape(16.dp)` / `RoundedCornerShape(6.dp)`) —
 * wiring those 2 remaining call sites to read from here is deferred to a
 * future batch to stay inside this project's 3-file-per-batch code cap
 * (this file + `NeumorphSurface.kt` + `NeumorphTypography.kt` already fill
 * this batch's 3 slots). 0 visual change to the button/chip yet from this
 * file alone — tracked in PROJECT_STATE.md's Pending Queue.
 */
object NeumorphShape {
    /** Wired this batch — see [com.example.gallerycleaner.ui.components.NeumorphSurface]'s `shape` default. */
    val Card: Shape = RoundedCornerShape(24.dp)

    /** Defined, NOT yet wired — `GlassButton.kt` still passes `RoundedCornerShape(16.dp)` inline. */
    val Button: Shape = RoundedCornerShape(20.dp)

    /** Defined, NOT yet wired — `SwipeScreenControls.kt`'s `InfoChip` still passes `RoundedCornerShape(6.dp)` inline. */
    val Chip: Shape = RoundedCornerShape(10.dp)
}
