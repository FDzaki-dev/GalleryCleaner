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
        // NOTE: `matchParentSize()` is a `BoxScope` extension (not a
        // `Modifier` one) — called bare here relying on this lambda's
        // implicit `BoxScope` receiver (this whole block runs inside the
        // outer `Box{ }` above), then `.offset()`/`.shadow()` chain onto
        // the `Modifier` it returns.
        if (!pressed) {
            // Dark shadow — bottom-right (depth)
            Box(
                matchParentSize()
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
                matchParentSize()
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
        // Flat fill, no gradient, no border/bevel — the "pure" surface itself.
        Box(
            matchParentSize()
                .background(color = if (pressed) pressedFillColor else fillColor, shape = shape)
        )
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}
