package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.Neumorph

/**
 * Core building block for the Amber Reserve Neumorphism (Soft UI) material
 * (Batch36 — full redesign, replaces Skeuomorphism-lite from Batch27/28).
 *
 * Neumorphism's one defining visual cue is a DUAL offset shadow — a light
 * highlight cast from an implied top-left light source, and a dark shadow
 * from the opposite bottom-right corner — on a FLAT, monochromatic surface
 * with NO border and NO gradient. That's the whole recipe, deliberately:
 *
 * |                                  | shadow(s)              | fill               | border/bevel |
 * |----------------------------------|-------------------------|---------------------|--------------|
 * | `glassPanel` (Signature/Indigo)  | 1, ambient               | translucent gradient | gradient edge |
 * | `skeuoPanel` (old Amber Reserve) | 1, ambient + specular    | gradient             | gradient bevel |
 * | `NeumorphSurface` (this)         | 2, independently offset  | flat solid           | none |
 *
 * **Why this is a `@Composable` and not a `Modifier.neumorphPanel()`
 * extension** like `glassPanel`/`skeuoPanel`: `Modifier.shadow()` (the
 * primitive both of those use) casts exactly ONE shadow driven by
 * Z-elevation — it has no X/Y offset parameter, so it structurally cannot
 * produce two independently-positioned shadows. Reaching for
 * `Modifier.blur`/`RenderEffect` (API 31+) would break this project's
 * `minSdk = 24`; raw `Paint.setShadowLayer` on a hardware-accelerated
 * canvas has inconsistent behavior across API levels pre-28. Instead, this
 * stacks TWO ordinary `Modifier.shadow()` calls on separate layered `Box`
 * children, each nudged with `Modifier.offset()` in opposite diagonal
 * directions — the exact same public, already-proven `Modifier.shadow()`
 * primitive `glassPanel`/`skeuoPanel` already use, just composed as two
 * layers instead of one linear chain. No new rendering technique is
 * introduced beyond "two of the thing already in use."
 *
 * **Known platform caveat, documented for visibility** (does not block
 * this implementation — flagged the same way this project already flags
 * other minSdk-driven tradeoffs): `Modifier.shadow()`'s `ambientColor`/
 * `spotColor` tinting only renders true-to-color on API 28+; on API 24-27
 * both shadow layers fall back to a default black shadow. For
 * `glassPanel`/`skeuoPanel` this fallback is invisible (their shadow tints
 * are already near-black). For [Neumorph.ShadowLight] (a white highlight)
 * the API24-27 fallback would render as a second dark shadow instead of a
 * light one — a real but minor visual degradation on a shrinking slice of
 * devices below this project's `minSdk`.
 *
 * **Pressed/inset state**: a true inner (recessed) shadow isn't
 * expressible with `Modifier.shadow()` either (outer/drop-shadow only).
 * Rather than build a bespoke clip+blur inner-shadow technique, this
 * follows the same pragmatic simplification `skeuoInset` already uses for
 * its own pressed state (no true emboss/deboss simulation there either):
 * drop BOTH shadow layers entirely and swap to a darker flat fill — reads
 * as "flush with the background", which is the correct visual language
 * for "pressed" even without a literal inner shadow.
 *
 * **Dense 3-layer stack (new)**: the single flat fill described above is
 * now 3 solid, fully-opaque rounded-rect layers fanned toward the
 * top-left — same direction as the light-source highlight above, so the
 * stack reads as "peeking toward the light" rather than an arbitrary
 * direction. Each layer is inset from the one behind it by [stackOffset]
 * (back → front: `pressedFillColor`, a 50/50 [lerp] of `fillColor`/
 * `pressedFillColor`, then `fillColor`) — mechanically derived from the
 * two colors already passed in, no new hue. Critically, the front (content)
 * layer is the one **normal** (non-`matchParentSize`) child, so it alone
 * determines this composable's own measured size — its `2×stackOffset`
 * start/top inset is baked directly into that measurement. That means the
 * fan-out margin is reserved inside this composable's OWN bounds rather
 * than by overflowing into whatever space a caller's layout happens to
 * leave around it, so it can't get clipped/truncated by a `LazyRow`/
 * `LazyColumn` item, a `Row`, or any other ancestor regardless of that
 * ancestor's own clipping — no call site needs to add spacing for this.
 * When `pressed`, all 3 layers collapse to the same `pressedFillColor` —
 * the steps become invisible (flush look preserved) without changing
 * which child drives sizing, so pressing never jitters the layout.
 */
@Composable
fun NeumorphSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    pressed: Boolean = false,
    fillColor: Color = Neumorph.NavyCard,
    pressedFillColor: Color = Neumorph.DeepNavy,
    shadowElevation: Dp = 10.dp,
    shadowOffset: Dp = 6.dp,
    stackOffset: Dp = 4.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .let {
                when {
                    onClick == null -> it
                    interactionSource != null -> it.clickable(
                        interactionSource = interactionSource,
                        indication = null, // caller supplies its own press feedback (fill/shadow swap)
                        enabled = enabled,
                        onClick = onClick
                    )
                    else -> it.clickable(enabled = enabled, onClick = onClick)
                }
            }
    ) {
        // NOTE (fixed after CI run160 failure — see PROJECT_STATE.md
        // Batch37): `matchParentSize()` is declared INSIDE `interface
        // BoxScope` as `fun Modifier.matchParentSize(): Modifier` — it
        // needs BOTH an explicit `Modifier` extension receiver (so it must
        // be written `Modifier.matchParentSize()`, same as any other
        // modifier) AND an implicit `BoxScope` dispatch receiver (supplied
        // automatically here, since this whole block runs inside the outer
        // `Box { }` above). A bare `matchParentSize()` — no `Modifier.`
        // prefix — only supplies the second half and fails to resolve;
        // that was the actual compile error CI caught.
        if (!pressed) {
            // Dark shadow — bottom-right (depth)
            Box(
                Modifier.matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .shadow(
                        elevation = shadowElevation,
                        shape = shape,
                        clip = false,
                        ambientColor = Neumorph.ShadowDark,
                        spotColor = Neumorph.ShadowDark
                    )
            )
            // Light shadow — top-left (highlight)
            Box(
                Modifier.matchParentSize()
                    .offset(x = -shadowOffset, y = -shadowOffset)
                    .shadow(
                        elevation = shadowElevation,
                        shape = shape,
                        clip = false,
                        ambientColor = Neumorph.ShadowLight,
                        spotColor = Neumorph.ShadowLight
                    )
            )
        }
        // Dense 3-layer stack, fanned toward the top-left — see class doc
        // "Dense 3-layer stack" above for why the front layer (not
        // matchParentSize) is the one that drives this Box's own size, and
        // why pressed collapses all 3 to one color instead of branching
        // structure. No gradient, no border/bevel — each layer is still a
        // flat, monochromatic fill, same "pure" recipe as before, just 3
        // of them instead of 1.
        val backColor = pressedFillColor
        val midColor = lerp(fillColor, pressedFillColor, 0.5f)
        val frontColor = fillColor
        val stackInset = stackOffset * 2

        // Layer 3 — back-most, flush to this Box's own top-left corner.
        Box(
            Modifier.matchParentSize()
                .padding(end = stackInset, bottom = stackInset)
                .background(color = backColor, shape = shape)
        )
        // Layer 2 — middle, one step further toward the bottom-right.
        Box(
            Modifier.matchParentSize()
                .padding(stackOffset)
                .background(color = if (pressed) backColor else midColor, shape = shape)
        )
        // Layer 1 — front, holds the real content. Deliberately NOT
        // matchParentSize: its start/top inset is what this composable's
        // own measured size is based on (see doc comment), which is what
        // keeps the fan-out self-contained instead of overflowing into a
        // parent's layout space.
        Box(
            Modifier.padding(start = stackInset, top = stackInset)
                .background(color = if (pressed) backColor else frontColor, shape = shape)
        ) {
            Box(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
