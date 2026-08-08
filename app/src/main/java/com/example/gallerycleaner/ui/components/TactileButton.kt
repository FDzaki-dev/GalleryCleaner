package com.example.gallerycleaner.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.AccentBlue
import com.example.gallerycleaner.ui.theme.ButtonPressedElevation
import com.example.gallerycleaner.ui.theme.ButtonPressedScale
import com.example.gallerycleaner.ui.theme.ButtonRestElevation
import com.example.gallerycleaner.ui.theme.ButtonRestScale
import com.example.gallerycleaner.ui.theme.GlassBase
import com.example.gallerycleaner.ui.theme.GlassBorderStrong
import com.example.gallerycleaner.ui.theme.GlassElevated
import com.example.gallerycleaner.ui.theme.GlassTextPrimary
import com.example.gallerycleaner.ui.theme.ShapeStandardControl
import com.example.gallerycleaner.ui.theme.TactileAnimationDurationMs

/**
 * §11 TACTILE BUTTON — glass surface + subtle elevation/highlight at rest;
 * on press: elevation down, slight scale down, highlight down, surface
 * reads a touch deeper. Short, immediate animation — no bounce (§20).
 *
 * @param accented use sparingly (§17/§18) for the one important/selected
 *   action on a screen — not the default for every button.
 */
@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accented: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) ButtonPressedScale else ButtonRestScale,
        animationSpec = tween(TactileAnimationDurationMs),
        label = "buttonScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) ButtonPressedElevation else ButtonRestElevation,
        animationSpec = tween(TactileAnimationDurationMs),
        label = "buttonElevation"
    )

    val baseColor = if (isPressed) GlassBase else GlassElevated
    val borderColor = if (accented) AccentBlue.copy(alpha = 0.4f) else GlassBorderStrong
    val contentColor: Color = if (accented) AccentBlue else GlassTextPrimary

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .scale(scale)
                .shadow(elevation, ShapeStandardControl, clip = false)
                .clip(ShapeStandardControl)
                .background(baseColor)
                .border(width = 1.dp, color = borderColor, shape = ShapeStandardControl)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
