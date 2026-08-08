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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.AccentBlue
import com.example.gallerycleaner.ui.theme.GlassBorder
import com.example.gallerycleaner.ui.theme.GlassElevated
import com.example.gallerycleaner.ui.theme.GlassPressed
import com.example.gallerycleaner.ui.theme.SwitchOffElevation
import com.example.gallerycleaner.ui.theme.SwitchOnElevation
import com.example.gallerycleaner.ui.theme.SwitchPressedScale
import com.example.gallerycleaner.ui.theme.TactileAnimationDurationMs

/**
 * §12 TACTILE SWITCH — recessed into the glass surface when OFF, subtly
 * elevated with the cool accent when ON. Per §21 (Accessibility), the
 * on/off distinction never relies on depth alone — track color + knob
 * position + elevation all change together.
 */
@Composable
fun TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val knobScale by animateFloatAsState(
        targetValue = if (isPressed) SwitchPressedScale else 1f,
        animationSpec = tween(TactileAnimationDurationMs),
        label = "switchKnobScale"
    )
    val trackElevation by animateDpAsState(
        targetValue = if (checked) SwitchOnElevation else SwitchOffElevation,
        animationSpec = tween(TactileAnimationDurationMs),
        label = "switchTrackElevation"
    )
    val knobOffsetTarget = if (checked) 20.dp else 0.dp
    val knobOffset by animateDpAsState(
        targetValue = knobOffsetTarget,
        animationSpec = tween(TactileAnimationDurationMs),
        label = "switchKnobOffset"
    )

    val trackColor = if (checked) AccentBlue.copy(alpha = 0.28f) else GlassPressed
    val trackBorder = if (checked) AccentBlue.copy(alpha = 0.5f) else GlassBorder
    val knobColor = if (checked) AccentBlue else GlassElevated

    Box(
        modifier = modifier
            .width(44.dp)
            .height(24.dp)
            .shadow(trackElevation, CircleShape, clip = false)
            .clip(CircleShape)
            .background(trackColor)
            .border(width = 1.dp, color = trackBorder, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = knobOffset)
                .size(20.dp)
                .scale(knobScale)
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}
