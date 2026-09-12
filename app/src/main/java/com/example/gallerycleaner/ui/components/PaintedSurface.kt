package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.Painted
import com.example.gallerycleaner.ui.theme.PaintedShape

/**
 * Core building block for the Sage Wash "painted" material (Batch119 — 4th
 * color style, see PROJECT_STATE.md Batch119 for the full brief and
 * rationale).
 *
 * Recipe, deliberately different from all 3 existing materials:
 *
 * |                           | shadow(s)                | fill                          | edge                     | press mechanic          |
 * |---------------------------|---------------------------|---------------------------------|---------------------------|--------------------------|
 * | `glassPanel` (Signature)  | 1, ambient tinted glow    | translucent gradient            | visible gradient edge     | fill/edge color swap     |
 * | `NeumorphSurface` (Amber) | 2, independently offset   | flat solid                      | none (+fading border)     | shadow removal + fill swap |
 * | `CupertinoSurface` (Indigo)| 1, soft colorless        | flat solid opaque               | optional hairline         | whole-control alpha dim  |
 * | `PaintedSurface` (this)   | 1, plain M3 elevation     | flat base + layered wash blobs  | 3-hue brush-stroke gradient | state-layer scrim overlay |
 *
 * "Based on Material 3" (per the brief) is expressed in the SHADOW axis:
 * one ordinary `Modifier.shadow()` call, the plain M3 single-elevation
 * recipe every default `Card`/`Surface` in this app already uses elsewhere
 * — not glass's tinted glow, not neumorph's dual offset pair. What makes
 * this a distinct 4th material is everything layered ON TOP of that plain
 * M3 shadow: a textured, hand-painted-feeling FILL and a genuinely
 * different PRESS mechanic.
 *
 * **Watercolor wash fill**: 3 soft-edged radial-gradient "blobs" (moss,
 * ochre, terracotta — this theme's full accent palette, see
 * `PaintedTokens.kt`) layered over the flat base [fillColor], each at low
 * alpha (0.16-0.24) and centered at FIXED fractional offsets of this
 * panel's own measured `size` — a pure function of `size` recomputed each
 * `drawWithCache` pass, never randomized per-recomposition, so the texture
 * is stable across recompositions/re-layouts (same "no per-frame
 * randomness" discipline `NeumorphSurface.kt`'s shadow/border primitives
 * already follow). Centers are placed slightly OUTSIDE the panel's own
 * corners (fractions <0 or >1) so each wash reads as a soft bleed from
 * beyond the frame, the way a real watercolor pigment diffuses past where
 * the brush first touched paper, rather than a wash centered neatly inside
 * the card. Everything is drawn inside a `clipPath` built from this
 * panel's actual [shape] outline, so washes never spill past rounded/
 * asymmetric corners regardless of which [PaintedShape] role is passed.
 *
 * **Brush-stroke edge**: a hand-drawn stroke along the shape's own
 * outline, reusing the exact `clipPath`+`addOutline`+`Stroke`+
 * `Brush.linearGradient` technique `NeumorphSurface.kt`'s own fading
 * border already proved in this codebase (see that file's "Fading
 * edge-light border" doc) — drawn at DOUBLE the target width then clipped
 * to the shape outline so only the inner half survives (same
 * stays-inside-bounds guarantee that file documents), just with a 3-stop
 * moss→ochre→terracotta gradient instead of a single-hue fade, at partial
 * alpha so it reads as a translucent paint sweep rather than a solid ruled
 * border.
 *
 * **Press feedback — a genuine 4th mechanism**: Material 3's real press
 * convention is a semi-opaque "state layer" scrim drawn ON TOP of the
 * resting fill/content, not a fill-color swap (Glass/Neumorph) and not
 * whole-control alpha dimming (Cupertino). [pressed] draws
 * [Painted.PressedScrim] over the fill+wash layer (washes are skipped
 * entirely while pressed, since the scrim would otherwise sit on top of —
 * and visually fight — the same translucent blobs) while the brush-stroke
 * edge and shadow stay exactly as they are at rest, matching how a real M3
 * ripple/state layer darkens a control without touching its elevation or
 * border.
 */
@Composable
fun PaintedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = PaintedShape.Card,
    pressed: Boolean = false,
    fillColor: Color = Painted.Surface,
    washColorA: Color = Painted.Moss,
    washColorB: Color = Painted.Ochre,
    washColorC: Color = Painted.Terracotta,
    shadowColor: Color = Painted.ShadowSoft,
    shadowElevation: Dp = 8.dp,
    showWash: Boolean = true,
    showBrushStroke: Boolean = true,
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
                        indication = null, // caller relies on this surface's own state-layer scrim for press feedback, not the default ripple
                        enabled = enabled,
                        onClick = onClick
                    )
                    else -> it.clickable(enabled = enabled, onClick = onClick)
                }
            }
    ) {
        // Single plain M3-style elevation shadow — see class doc table.
        Box(
            Modifier.matchParentSize()
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
        )
        // Fill + watercolor wash + brush-stroke edge — all clipped to this
        // panel's actual shape outline so nothing spills past rounded/
        // asymmetric corners. See class doc "Watercolor wash fill" and
        // "Brush-stroke edge" for the full technique breakdown.
        Box(
            Modifier.matchParentSize()
                .drawWithCache {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val clip = Path().apply { addOutline(outline) }
                    val maxDim = maxOf(size.width, size.height)

                    val centerA = Offset(size.width * 0.08f, size.height * -0.05f)
                    val centerB = Offset(size.width * 1.05f, size.height * 0.35f)
                    val centerC = Offset(size.width * 0.55f, size.height * 1.1f)
                    val radiusA = maxDim * 0.65f
                    val radiusB = maxDim * 0.55f
                    val radiusC = maxDim * 0.6f

                    // Batch120: alpha raised from v1 (0.24/0.16/0.18) — real-device
                    // screenshot feedback (see PaintedTokens.kt class doc) showed the
                    // wash reading as an almost-invisible flat fill rather than visible
                    // watercolor texture at the original values, especially against
                    // Batch120's now-darker/cooler [fillColor]. Raised until the wash
                    // is clearly perceptible without overpowering foreground content.
                    val washA = Brush.radialGradient(
                        colors = listOf(washColorA.copy(alpha = 0.34f), Color.Transparent),
                        center = centerA,
                        radius = radiusA
                    )
                    val washB = Brush.radialGradient(
                        colors = listOf(washColorB.copy(alpha = 0.24f), Color.Transparent),
                        center = centerB,
                        radius = radiusB
                    )
                    val washC = Brush.radialGradient(
                        colors = listOf(washColorC.copy(alpha = 0.26f), Color.Transparent),
                        center = centerC,
                        radius = radiusC
                    )

                    // Double-width, clipped to the outline so only the inner
                    // half survives — identical technique to NeumorphSurface's
                    // fading border, see that file's doc comment.
                    val strokePx = 3.dp.toPx()
                    val brushStroke = Brush.linearGradient(
                        colors = listOf(
                            washColorA.copy(alpha = 0.55f),
                            washColorB.copy(alpha = 0.45f),
                            washColorC.copy(alpha = 0.55f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )

                    onDrawBehind {
                        clipPath(clip) {
                            drawRect(color = fillColor)
                            if (showWash && !pressed) {
                                drawCircle(brush = washA, radius = radiusA, center = centerA)
                                drawCircle(brush = washB, radius = radiusB, center = centerB)
                                drawCircle(brush = washC, radius = radiusC, center = centerC)
                            }
                            if (pressed) {
                                drawRect(color = Painted.PressedScrim)
                            }
                            if (showBrushStroke) {
                                drawOutline(outline = outline, brush = brushStroke, style = Stroke(width = strokePx * 2))
                            }
                        }
                    }
                }
        )
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}
