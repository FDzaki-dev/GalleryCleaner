package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.AccentBlue
import com.example.gallerycleaner.ui.theme.GlassElevated
import com.example.gallerycleaner.ui.theme.GlassTextMuted
import com.example.gallerycleaner.ui.theme.ShapeStandardControl

/**
 * §15 NAVIGATION — calm, immediately understandable. Selected item gets a
 * subtle glass elevation + restrained accent; unselected stays quiet glass
 * with muted content. Don't wrap every item in a glowing capsule — only
 * the selected one gets the glass "pill".
 */
@Composable
fun GlassNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val background: Color = if (selected) GlassElevated else Color.Transparent
    val contentColor: Color = if (selected) AccentBlue else GlassTextMuted

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .clip(ShapeStandardControl)
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Simple row container for [GlassNavigationItem]s — quiet glass bar, no ambient tint. */
@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    GlassSurface(modifier = modifier, level = 1) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
