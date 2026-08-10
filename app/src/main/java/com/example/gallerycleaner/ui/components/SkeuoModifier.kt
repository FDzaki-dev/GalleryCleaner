package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.SkeuoLite

/**
 * Core "raised bevel" building block for the Amber Reserve
 * Skeuomorphism-lite material (Batch27) — the direct sibling of
 * `Modifier.glassPanel()`, deliberately built the same "shadow + fill +
 * border" way so the two material languages stay equally cheap to render
 * (no `Modifier.blur`, works identically down to `minSdk = 24`).
 *
 * The material difference from `glassPanel` is in the VALUES, not the
 * technique: [fill] is one opaque [Color] (a skeuomorphic object is
 * solid, not translucent — there is deliberately no `Brush` fill
 * parameter here, unlike glassPanel's gradient fill), the shadow is a
 * single warm near-black rather than glass's cool ambient glow, and
 * [shape] defaults to a tighter corner radius (12.dp vs glass's 18.dp) —
 * a ledger/card object reads as less "soft" than a floating glass pane.
 */
fun Modifier.skeuoPanel(
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 8.dp,
    borderWidth: Dp = 1.5.dp,
    fill: Color = SkeuoLite.PanelFill,
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
        .background(color = fill, shape = shape)
        .border(width = borderWidth, brush = edge, shape = shape)
)

/** Pressed/inset variant — no shadow (a debossed slot doesn't cast one,
 *  same physical logic as `glassInset`), darker recessed fill, and the
 *  bevel gradient run in reverse ([SkeuoLite.BevelGradientPressed]) so the
 *  light catch flips to bottom-right — this reversal, not just a darker
 *  fill, is what reads as "pushed in" rather than merely "dimmed". */
fun Modifier.skeuoInset(
    shape: Shape = RoundedCornerShape(10.dp),
    borderWidth: Dp = 1.5.dp
): Modifier = this.then(
    Modifier
        .background(color = SkeuoLite.PanelFillPressed, shape = shape)
        .border(width = borderWidth, brush = SkeuoLite.BevelGradientPressed, shape = shape)
)
