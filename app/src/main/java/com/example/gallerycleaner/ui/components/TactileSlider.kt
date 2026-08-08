package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.AccentBlue
import com.example.gallerycleaner.ui.theme.GlassElevated
import com.example.gallerycleaner.ui.theme.GlassHighlight
import com.example.gallerycleaner.ui.theme.GlassPressed
import com.example.gallerycleaner.ui.theme.SliderKnobElevation

/**
 * §13 TACTILE SLIDER — dark glass track, clear active track, tactile knob
 * with a restrained radial highlight. Built on Material3's [Slider] (drag
 * gesture handling, accessibility semantics already correct) with track
 * and thumb re-skinned to the glass/tactile tokens — deliberately not a
 * from-scratch gesture implementation, per §22 "prefer native Compose
 * primitives."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TactileSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            activeTrackColor = AccentBlue,
            inactiveTrackColor = GlassPressed,
            thumbColor = AccentBlue
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .shadow(SliderKnobElevation, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GlassHighlight, GlassElevated)
                        )
                    )
            )
        }
    )
}
