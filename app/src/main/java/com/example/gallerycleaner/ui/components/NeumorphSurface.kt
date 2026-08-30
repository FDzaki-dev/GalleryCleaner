package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.Neumorph
import com.example.gallerycleaner.ui.theme.NeumorphShape

/**
 * Core building block for the Amber Reserve Neumorphism (Soft UI) material
 * (Batch36 — full redesign, replaces Skeuomorphism-lite from Batch27/28).
 *
 * Neumorphism's defining visual cue is a DUAL offset shadow — a light
 * highlight cast from an implied top-left light source, and a dark shadow
 * from the opposite bottom-right corner — on a FLAT, monochromatic surface.
 * Through Batch80 that surface deliberately had NO border and NO gradient.
 * Batch81 adds ONE exception, by explicit user request ("tambahkan garis
 * Border yang fade out ke arah kanan bawah pada semua panel"): a border
 * whose gradient FADES from a subtle top-left highlight to fully
 * transparent at the bottom-right — see "Fading edge-light border (Batch81)"
 * below. Batch83 thickened it 1dp→2dp ("pertebal garis border"); Batch86
 * thickens it again, decisively, 2dp→6dp — explicit user request, verbatim
 * "garis Border nya wajib 'ultra Bold'. gak mau tahu!!"; Batch87 fixes a bug
 * where the bottom-right corner wasn't actually fading (see "Fading
 * edge-light border" below for the fix) — width/hue/recipe unchanged, only
 * HOW the gradient's endpoints get resolved:
 *
 * |                                  | shadow(s)              | fill               | border/bevel |
 * |----------------------------------|-------------------------|---------------------|--------------|
 * | `glassPanel` (Signature/Indigo)  | 1, ambient               | translucent gradient | gradient edge |
 * | `skeuoPanel` (old Amber Reserve) | 1, ambient + specular    | gradient             | gradient bevel |
 * | `NeumorphSurface` (this)         | 2, independently offset  | flat solid           | 6dp fading gradient edge (Batch81→83→86→87) |
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
 * **Dense 3-layer stack**: the single flat fill described above is now 3
 * solid, fully-opaque rounded-rect layers fanned toward the top-left —
 * same direction as the light-source highlight above, so the stack reads
 * as "peeking toward the light" rather than an arbitrary direction. Each
 * layer is inset from the one behind it by [stackOffset]. Colors (back →
 * front) sit at 3 spread-out points on the straight line through
 * `fillColor`→`pressedFillColor` — back overshoots PAST `pressedFillColor`
 * (160% via [lerp]) and mid sits at 80%, rather than crammed into just the
 * `fillColor`↔`pressedFillColor` gap — mechanically derived from the two
 * colors already passed in (no new hue), but spread far enough apart to
 * actually read as 3 distinct layers instead of blending into one. The
 * front (content) layer is the one **normal** (non-`matchParentSize`)
 * child, so it alone determines this composable's own measured size — its
 * `2×stackOffset` start/top inset is baked directly into that
 * measurement. That means the fan-out margin is reserved inside this
 * composable's OWN bounds rather than by overflowing into whatever space
 * a caller's layout happens to leave around it, so it can't get
 * clipped/truncated by a `LazyRow`/`LazyColumn` item, a `Row`, or any
 * other ancestor regardless of that ancestor's own clipping — no call
 * site needs to add spacing for this. When `pressed`, all 3 layers
 * collapse to the plain `pressedFillColor` (not the extrapolated stack
 * tones) — the steps become invisible (flush look preserved) without
 * changing which child drives sizing, so pressing never jitters the
 * layout. **Batch87**: this composable's own `fillColor`/`pressedFillColor`
 * DEFAULTS moved from [Neumorph.NavyCard]/[Neumorph.DeepNavy] to the new
 * [Neumorph.StackFill]/[Neumorph.StackFillPressed] (muted magenta, same
 * S/L budget as before — see those tokens' doc comment in
 * `NeumorphTokens.kt` for the full rationale) per explicit user request,
 * "3-dense layer stacked ganti jadi warna magenta ala Blade Runner, tapi
 * jangan terlalu kontras/mencolok". Only the DEFAULTS changed — callers
 * that already pass their own `fillColor` (`GlassButton.kt`'s brass CTA)
 * are untouched, still deriving their stack from whatever color they pass.
 *
 * **Fading edge-light border (Batch81, fixed Batch87)**: introduced via
 * `Modifier.border()` with `Brush.linearGradient(listOf(Neumorph.BorderFade,
 * Color.Transparent))`, relying on Compose's default `Offset.Zero`→
 * `Offset.Infinite` resolving to the drawn bounds at paint time. Batch87
 * user report: "border tebal sudah benar, tapi ujung kanan bawah malah
 * tidak 'fade out'" — in practice that implicit resolution did not
 * reliably land on this panel's actual full measured size (border()'s
 * internal draw path picks its own resolution size per shape/width
 * combination, not guaranteed to be the outer bounds), so the gradient
 * could reach its transparent stop well before the true bottom-right
 * corner, reading as "not fading" there. Fix: `.border()` replaced with a
 * hand-drawn stroke via `Modifier.drawWithCache` — `size` there is this
 * panel's real measured `Size` every frame, so `start = Offset.Zero` /
 * `end = Offset(size.width, size.height)` are the panel's ACTUAL corners,
 * no implicit resolution anywhere: top-left is always
 * [Neumorph.BorderFade] at full strength, bottom-right is always exactly
 * 0 alpha. To keep the stroke fully INSIDE the panel bounds (the same
 * visual contract `.border()` had — no bleed onto Layer 2's stack step
 * behind it), the stroke is drawn at DOUBLE the target width and clipped
 * to the shape's own outline (`Path().apply { addOutline(outline) }`) so
 * only the inner half survives — lands on the identical visible `6.dp`
 * (Batch86) as before. Direction/placement otherwise unchanged from
 * Batch81: matches this file's light-source convention (top-left
 * highlight, bottom-right falloff), applied ONLY to Layer 1 (the front
 * content box, the surface users perceive as "the panel's edge") not the
 * back/mid stack layers, same for `pressed`/non-`pressed`. Width stays the
 * fixed `6.dp` from Batch86 ("ultra bold", was `2.dp` Batch83/`1.dp`
 * Batch81 introduction) — not exposed as a caller parameter, same
 * reasoning as before (every panel should read as one consistent
 * material). [Neumorph.BorderFade]'s hue/alpha (Batch86: 0.50) are
 * unchanged by this fix — this was a geometry/resolution bug, not a color
 * one.
 *
 * **Shape (Batch84)**: the `shape` default below now reads
 * [com.example.gallerycleaner.ui.theme.NeumorphShape.Card] instead of a
 * bare `RoundedCornerShape(20.dp)` literal — same 24dp-vs-20dp bump and
 * "murni, theme-owned source" rationale documented in `NeumorphShape.kt`,
 * which also covers why [com.example.gallerycleaner.ui.theme.GalleryShapes]
 * (`Shape.kt`) was investigated and found NOT to be this default's prior
 * baseline (it's unwired app-wide, not just for this composable).
 */
@Composable
fun NeumorphSurface(
    modifier: Modifier = Modifier,
    shape: Shape = NeumorphShape.Card, // Batch84: was bare RoundedCornerShape(20.dp) — see NeumorphShape.kt
    pressed: Boolean = false,
    fillColor: Color = Neumorph.StackFill, // Batch87: was Neumorph.NavyCard — see NeumorphTokens.kt "Batch87" note
    pressedFillColor: Color = Neumorph.StackFillPressed, // Batch87: was Neumorph.DeepNavy
    shadowElevation: Dp = 10.dp,
    shadowOffset: Dp = 6.dp,
    stackOffset: Dp = 8.dp,
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
        // structure. No gradient FILL, no bevel on any of the 3 layers —
        // each is still a flat, monochromatic fill, same "pure" recipe as
        // before. The one exception is the Batch81 fading BORDER on Layer 1
        // only (see class doc "Fading edge-light border" above) — a border,
        // not a gradient fill, and deliberately not applied to Layer 2/3.
        //
        // Color spread (Batch79 fix — Batch78's plain 50/50 blend crammed
        // all 3 stops into the narrow fillColor↔pressedFillColor gap,
        // which read as "1 layer" since mid/back were barely distinguishable
        // from each other): stops are placed on the SAME straight line
        // through fillColor→pressedFillColor (same "derived, not invented"
        // technique this file already uses elsewhere, e.g.
        // `Neumorph.ClassicBrassPressed`'s HLS shift) but spaced further
        // apart — mid overshoots to 80% of that line and back overshoots
        // PAST pressedFillColor entirely (160%) instead of stopping at it,
        // so all 3 layers land on evenly-spaced, clearly separate tones.
        val stackBackColor = lerp(fillColor, pressedFillColor, 1.6f)
        val stackMidColor = lerp(fillColor, pressedFillColor, 0.8f)
        val stackFrontColor = fillColor
        val stackInset = stackOffset * 2

        // Layer 3 — back-most, flush to this Box's own top-left corner.
        Box(
            Modifier.matchParentSize()
                .padding(end = stackInset, bottom = stackInset)
                .background(color = if (pressed) pressedFillColor else stackBackColor, shape = shape)
        )
        // Layer 2 — middle, one step further toward the bottom-right.
        Box(
            Modifier.matchParentSize()
                .padding(stackOffset)
                .background(color = if (pressed) pressedFillColor else stackMidColor, shape = shape)
        )
        // Layer 1 — front, holds the real content. Deliberately NOT
        // matchParentSize: its start/top inset is what this composable's
        // own measured size is based on (see doc comment), which is what
        // keeps the fan-out self-contained instead of overflowing into a
        // parent's layout space.
        Box(
            Modifier.padding(start = stackInset, top = stackInset)
                .background(color = if (pressed) pressedFillColor else stackFrontColor, shape = shape)
                // Batch87 fix — user-reported bug: "border tebal sudah
                // benar, tapi ujung kanan bawah malah tidak 'fade out'".
                // Root cause: plain `.border(width, brush, shape)` resolves
                // the brush's Offset.Infinite end-point using whatever
                // internal size Compose's border draw path happens to use
                // for THAT shape/width combo (varies by shape fast-path) —
                // not reliably the panel's full measured bounds, so the
                // gradient could evaluate against a much smaller area and
                // never actually reach its "fully transparent" stop by the
                // true bottom-right corner. Fix: draw the stroke by hand
                // via `drawWithCache`, which hands us the panel's real
                // `size` every frame — the gradient's `start`/`end` are set
                // explicitly to `Offset.Zero`/`Offset(size.width,
                // size.height)` (the ACTUAL corners), so top-left is
                // guaranteed [Neumorph.BorderFade] and bottom-right is
                // guaranteed fully transparent, no implicit resolution
                // involved anywhere. To keep the border fully INSIDE the
                // panel bounds (same visual contract `.border()` had, no
                // bleed into Layer 2's stack step behind it) the stroke is
                // drawn at 2× width then clipped to the shape's own
                // outline — only the inner half survives, landing on
                // exactly the same 6dp (Batch86) visible width as before.
                .drawWithCache {
                    val strokePx = 6.dp.toPx()
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val clip = Path().apply { addOutline(outline) }
                    val fadeBrush = Brush.linearGradient(
                        colors = listOf(Neumorph.BorderFade, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    onDrawWithContent {
                        drawContent()
                        clipPath(clip) {
                            drawOutline(
                                outline = outline,
                                brush = fadeBrush,
                                style = Stroke(width = strokePx * 2)
                            )
                        }
                    }
                }
        ) {
            Box(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
