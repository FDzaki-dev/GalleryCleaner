package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.SkeuoLite

/**
 * Core "raised bevel" building block for the Amber Reserve
 * Skeuomorphism-lite material (Batch27, retuned Batch28 — see the class
 * doc on `SkeuoLite` for the full root-cause writeup of why Batch27's
 * version didn't read as raised).
 *
 * Layer order (4 ingredients now, up from 3 in Batch27): shadow → base
 * fill gradient → specular corner highlight → bevel border. Still no
 * `Modifier.blur`/RenderEffect (stays `minSdk = 24`-safe, same constraint
 * `glassPanel` follows) — every layer here is shadow/background(brush)/
 * border, the same primitives Compose Foundation already provides.
 */
fun Modifier.skeuoPanel(
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 8.dp,
    borderWidth: Dp = 2.dp,
    fill: Brush = SkeuoLite.PanelFillGradient,
    specular: Brush = SkeuoLite.SpecularHighlight,
    edge: Brush = SkeuoLite.BevelGradient
): Modifier = this.then(
    Modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = SkeuoLite.ShadowColor,
            spotColor = SkeuoLite.ShadowColor
        )
        .background(brush = fill, shape = shape)
        // Specular corner glow, layered as its own clipped background pass
        // on top of the base fill — this is the cue Batch27 was missing
        // entirely; a flat/gradient fill plus a border reads as "a box",
        // a corner light catch is what reads as "a curved raised object".
        .background(brush = specular, shape = shape)
        .border(width = borderWidth, brush = edge, shape = shape)
)

/** Pressed/inset variant — no shadow (a debossed slot doesn't cast one,
 *  same physical logic as `glassInset`), the fill gradient reversed (dark
 *  top-left instead of light), no specular catch (a recessed slot doesn't
 *  reflect light back at the viewer), and the bevel gradient reversed
 *  ([SkeuoLite.BevelGradientPressed]) so the light catch flips to
 *  bottom-right. It's the combination of all three reversals, not any one
 *  alone, that reads as "pushed in" rather than merely "recolored". */
fun Modifier.skeuoInset(
    shape: Shape = RoundedCornerShape(10.dp),
    borderWidth: Dp = 2.dp
): Modifier = this.then(
    Modifier
        .background(brush = SkeuoLite.PanelFillGradientPressed, shape = shape)
        .border(width = borderWidth, brush = SkeuoLite.BevelGradientPressed, shape = shape)
)
