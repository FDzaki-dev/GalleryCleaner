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
import com.example.gallerycleaner.ui.theme.LocalMaterialStyle
import com.example.gallerycleaner.ui.theme.MaterialStyle
import com.example.gallerycleaner.ui.theme.MidnightGlass
import com.example.gallerycleaner.ui.theme.SkeuoLite

/**
 * Shared full-width CTA button for every color style — the `Button`
 * equivalent of `GlassCard`. Same [LocalMaterialStyle] branch point
 * (Batch27): Signature/Indigo Noir render the original glass glow button
 * unchanged; Amber Reserve now renders a distinct skeuomorphism-lite
 * button instead of a recolored glass one.
 *
 * The two press behaviors are deliberately different MECHANISMS, not just
 * different colors — this is the material-language distinction, applied
 * to interaction feedback rather than just idle appearance:
 * - **Glass** (unchanged): press = brighter glow-tinted label + warmer
 *   border gradient. Floating glass doesn't get physically "pushed", so
 *   feedback reads as a light/glow change.
 * - **Skeuo-lite** (new): press = `skeuoPanel` → `skeuoInset` swap, i.e.
 *   the shadow disappears and the fill/bevel reverse to the recessed
 *   variant — the button visually depresses into the surface, like a
 *   real embossed key, then springs back on release.
 */
@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val style = LocalMaterialStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    when (style) {
        MaterialStyle.GLASS -> {
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
        MaterialStyle.SKEUO_LITE -> {
            Box(
                modifier = modifier
                    .height(52.dp)
                    .let {
                        if (isPressed) it.skeuoInset(shape = RoundedCornerShape(14.dp))
                        else it.skeuoPanel(shape = RoundedCornerShape(14.dp), elevation = 5.dp)
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null, // deboss swap above stands in for the ripple
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (isPressed) SkeuoLite.AccentBrass else SkeuoLite.TextBright,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}
