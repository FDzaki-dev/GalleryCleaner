package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Pure "painted" shape — Sage Wash exclusive (Batch119).
 *
 * Every other theme in this app uses a UNIFORM corner radius (Signature/
 * Indigo's plain `RoundedCornerShape(Ndp)`, Amber's `CutCornerShape(Ndp)`
 * per `NeumorphShape.kt`) — one radius/cut applied identically to all 4
 * corners. Per the batch brief ("shape keren underrated"), this theme
 * instead uses 4 DIFFERENT corner radii per shape: one large, two medium,
 * one tight — reading as a hand-trimmed or torn-paper edge rather than a
 * machined uniform curve, a genuinely underused shape choice in Android UI
 * (most design systems, this app's other 3 themes included, treat all 4
 * corners identically).
 *
 * Technically this is still exactly [RoundedCornerShape] — the same
 * well-tested, already-proven-in-this-project API every rounded panel in
 * the app already uses, just with 4 independent `topStart`/`topEnd`/
 * `bottomEnd`/`bottomStart` values instead of 1. `RoundedCornerShape`
 * clamps gracefully if any corner's radius would exceed the component's
 * available space (standard `CornerBasedShape` behavior, the same
 * clamping guarantee `NeumorphShape.kt`'s `CutCornerShape` already relies
 * on for its own smallest [Chip] role) — so this is safe at any component
 * size without a compiler/device in the loop to verify it.
 *
 * The same "big top-left, small top-right, medium bottom-right, small-
 * medium bottom-left" asymmetry is kept proportionally across all 3 roles
 * below (just scaled down for [Button]/[Chip]) so every Painted surface in
 * the app reads as ONE consistent hand-trimmed silhouette, not a different
 * random shape per component.
 */
object PaintedShape {
    val Card: Shape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 6.dp,
        bottomEnd = 22.dp,
        bottomStart = 10.dp
    )

    val Button: Shape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 6.dp,
        bottomEnd = 18.dp,
        bottomStart = 8.dp
    )

    val Chip: Shape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 4.dp,
        bottomEnd = 10.dp,
        bottomStart = 5.dp
    )
}
