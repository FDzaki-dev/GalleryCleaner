package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.ShapeCard

/**
 * §14 CARDS — "glass surfaces first, not physical objects." No strong
 * bevel, heavy shadow, thick border, glow, or extrusion: just [GlassSurface]
 * at level 1 with generous internal padding. Structural elements stay
 * restrained (§10) — cards are not a place for tactile/skeuomorphic effects.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        level = 1,
        shape = ShapeCard,
        ambient = false
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
