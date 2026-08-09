package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Drop-in glass container — the Midnight Blue Glassmorphism equivalent of a
 * Material `Card`. Use this anywhere a floating panel/card look is wanted
 * (dashboard tiles, list rows, dialogs) to pick up the theme's dominant
 * frosted-glass visual instead of a flat Material surface.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassPanel(shape = shape, elevation = elevation)
            .padding(contentPadding),
        content = content
    )
}
