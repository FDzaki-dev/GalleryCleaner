package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.clickable
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
 *
 * [onClick] (Batch22): optional, mirrors the old `Surface(...).clickable{}`
 * pattern this replaces across the app's dashboard/list panels. Deliberately
 * applied to the modifier chain AFTER [glassPanel] (not folded into the
 * caller-supplied [modifier], which is applied BEFORE glassPanel) — same
 * order [GlassButton] already uses, so the ripple/indication paints on top
 * of the glass fill instead of being drawn under it and hidden. [enabled]
 * covers callers that need to suppress the tap while a scan/action is
 * in flight (e.g. `ScanTriggerRow`) without hiding the card entirely.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassPanel(shape = shape, elevation = elevation)
            .let { if (onClick != null) it.clickable(enabled = enabled, onClick = onClick) else it }
            .padding(contentPadding),
        content = content
    )
}
