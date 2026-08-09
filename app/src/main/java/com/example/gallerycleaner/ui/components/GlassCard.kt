package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.GlassBorder
import com.example.gallerycleaner.ui.theme.ShapeCard

/**
 * §14 CARDS — "glass surfaces first, not physical objects." No strong
 * bevel, heavy shadow, thick border, glow, or extrusion: just [GlassSurface]
 * at level 1 with generous internal padding. Structural elements stay
 * restrained (§10) — cards are not a place for tactile/skeuomorphic effects.
 *
 * @param shape override only when a screen's existing card geometry must
 *   be preserved during migration (§19 default is [ShapeCard], 16-20dp).
 * @param borderColor/[borderWidth] override for a selected/emphasized card
 *   (e.g. an accented 2dp border on the chosen item in a picker list).
 *   Defaults match the standard hairline glass border (§8).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    shape: Shape = ShapeCard,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        level = 1,
        shape = shape,
        ambient = false,
        borderColor = borderColor,
        borderWidth = borderWidth
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
