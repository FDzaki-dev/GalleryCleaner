package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.gallerycleaner.ui.theme.Neumorph
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
 * - **Skeuo-lite** (unused as of Batch36 — see `MaterialStyle.kt`): press
 *   = `skeuoPanel` → `skeuoInset` swap, shadow disappears and fill/bevel
 *   reverse to the recessed variant.
 * - **Neumorph** (Batch36, Amber Reserve): press = both offset shadow
 *   layers disappear and the flat fill swaps from [Neumorph.ClassicBrass]
 *   to [Neumorph.ClassicBrassPressed] (a darkened derivative of the same
 *   hue, not a new color) — same "shadow gone + fill swap" mechanism as
 *   skeuo-lite's press feedback, via `NeumorphSurface`'s `pressed` param
 *   instead of a manual `skeuoPanel`/`skeuoInset` modifier swap.
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
        MaterialStyle.NEUMORPH -> {
            // Batch36: Brass fill (the spec's "10% Accent — khusus tombol
            // CTA utama") rather than NeumorphSurface's NavyCard default —
            // this IS the CTA. Text is Neumorph.TextOnBrass unconditionally
            // (pressed or not), per the spec's explicit WCAG button-text
            // rule — unlike Glass/Skeuo-lite above, there's no "pressed
            // color" swap for the TEXT here, only the surface swaps.
            NeumorphSurface(
                modifier = modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                pressed = isPressed,
                fillColor = Neumorph.ClassicBrass,
                pressedFillColor = Neumorph.ClassicBrassPressed,
                shadowElevation = 6.dp,
                shadowOffset = 4.dp,
                contentPadding = 0.dp,
                onClick = onClick,
                interactionSource = interactionSource // indication=null handled inside NeumorphSurface
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        color = Neumorph.TextOnBrass,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
