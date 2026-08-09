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
import com.example.gallerycleaner.ui.theme.MidnightGlass

/**
 * Core "frosted glass" building block for the Midnight Blue Glassmorphism
 * theme. Deliberately built from shadow + gradient fill + gradient border
 * rather than `Modifier.blur` — see the doc comment on `MidnightGlassTokens`
 * for why (API 31+ only, would silently break the look on older devices
 * given this project's `minSdk = 24`).
 *
 * [fill] / [edge] let callers opt into the light-mode ("frosted ice")
 * gradients from [MidnightGlass] instead of the dark defaults — Theme.kt
 * doesn't currently thread a light/dark flag into components, so this is a
 * parameter rather than an internal `isSystemInDarkTheme()` check, keeping
 * this modifier pure and independent of composition context.
 */
fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 12.dp,
    borderWidth: Dp = 1.dp,
    fill: Brush = MidnightGlass.GlassFillGradient,
    edge: Brush = MidnightGlass.EdgeGradient
): Modifier = this.then(
    Modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = MidnightGlass.VoidDeep,
            spotColor = MidnightGlass.VoidDeep
        )
        .background(brush = fill, shape = shape)
        .border(width = borderWidth, brush = edge, shape = shape)
)

/** Recessed variant — for value slots / input-like containers that should
 *  read as "set into" the glass panel above them rather than floating on
 *  their own. No shadow (a debossed element doesn't cast one), inset fill
 *  instead of the raised gradient. */
fun Modifier.glassInset(
    shape: Shape = RoundedCornerShape(14.dp),
    borderWidth: Dp = 1.dp
): Modifier = this.then(
    Modifier
        .background(brush = MidnightGlass.InsetFillGradient, shape = shape)
        .border(width = borderWidth, brush = MidnightGlass.EdgeGradient, shape = shape)
)
