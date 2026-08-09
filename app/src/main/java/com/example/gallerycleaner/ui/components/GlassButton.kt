package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallerycleaner.ui.theme.MidnightGlass

/**
 * Glass equivalent of the previous `MidnightSkeuoButton` (raised/pressed
 * metallic button). Press feedback here is a brighter glow-tinted label +
 * a slightly warmer border gradient, rather than a simulated elevation
 * change — glass panels read as floating, not physically depressible, so
 * "pressed" is communicated through light/glow instead of shadow depth.
 */
@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val edgeBrush = if (isPressed) {
        Brush.linearGradient(
            listOf(MidnightGlass.GlowBlue.copy(alpha = 0.9f), MidnightGlass.EdgeFade, MidnightGlass.EdgeShadow)
        )
    } else {
        MidnightGlass.EdgeGradient
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .glassPanel(shape = RoundedCornerShape(16.dp), elevation = 6.dp, edge = edgeBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // custom glow feedback below stands in for the ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPressed) MidnightGlass.GlowBlue else MidnightGlass.TextBright,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}
