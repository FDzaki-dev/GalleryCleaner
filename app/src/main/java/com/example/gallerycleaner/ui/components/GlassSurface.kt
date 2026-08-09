package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.GlassBase
import com.example.gallerycleaner.ui.theme.GlassBorder
import com.example.gallerycleaner.ui.theme.GlassElevated
import com.example.gallerycleaner.ui.theme.ShapeCard
import com.example.gallerycleaner.ui.theme.midnightAmbientGradient

/**
 * §4/§7 base glass material — a translucent layer floating above the AMOLED
 * canvas. Every elevated surface in the app should be built from this or
 * [GlassCard] rather than a raw `background(color)`, so the glass hierarchy
 * (§4 Level 0-4) stays consistent across screens.
 *
 * @param level 1 = subtle translucent glass, 2 = elevated frosted glass.
 *   Don't reach for level 2 everywhere — §4 "do not make every component
 *   Level 3 or Level 4."
 * @param ambient when true, blends in the §6 Midnight Blue atmospheric
 *   gradient instead of a flat glass fill. Use sparingly — large screen
 *   backgrounds or focused surfaces only, not every card.
 * @param borderWidth override for selection/emphasis states (e.g. a
 *   thicker accented border on a selected item). Defaults to the
 *   standard 1dp hairline (§8) — don't widen it for ordinary surfaces.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    level: Int = 1,
    shape: Shape = ShapeCard,
    ambient: Boolean = false,
    borderColor: Color = GlassBorder,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val fill: Brush = if (ambient) {
        midnightAmbientGradient(
            from = if (level >= 2) GlassElevated else GlassBase,
            to = if (level >= 2) GlassBase else GlassElevated
        )
    } else {
        val flat = if (level >= 2) GlassElevated else GlassBase
        Brush.linearGradient(listOf(flat, flat))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(width = borderWidth, color = borderColor, shape = shape)
    ) {
        content()
    }
}
