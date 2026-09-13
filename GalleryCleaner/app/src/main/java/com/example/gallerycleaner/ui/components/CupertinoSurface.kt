package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.Cupertino

/**
 * Core building block for the Indigo Noir Cupertino (iOS) material (Batch74
 * stage 1/2 — see `CupertinoTokens.kt` for the full rationale and the
 * technique-comparison table against `glassPanel`/`NeumorphSurface`).
 *
 * The recipe, deliberately the inverse of both existing materials: ONE
 * soft, low-opacity, colorless shadow (not glass's crisper single shadow,
 * not neumorphism's independently-offset PAIR), an OPAQUE flat fill (no
 * translucency, no gradient — unlike glass's translucent gradient AND
 * unlike neumorph which is also flat-solid but with zero border at all),
 * and an optional near-invisible hairline instead of glass's visible
 * gradient edge.
 *
 * **Why a `@Composable` and not a `Modifier.cupertinoPanel()` extension**
 * like `glassPanel`/`skeuoPanel`: architecturally this recipe (one shadow +
 * one fill + optional border) COULD be expressed as a linear `Modifier`
 * chain same as those two. It's built as a `@Composable` instead purely
 * for INTEGRATION consistency with `GlassCard`'s existing branch shape —
 * that call site already does an early-`return` to `NeumorphSurface { }`
 * for one style; adding a second, differently-shaped integration (Modifier
 * chain vs. Composable slot) for a third style would mean two different
 * "how do I add a material" patterns in the same file. One early-return
 * pattern for every non-linear-chain style keeps `GlassCard`/`GlassButton`
 * predictable to extend later.
 *
 * **Press feedback**: unlike Glass (fill/edge color swap) or Neumorph
 * (shadow removal + fill swap), Cupertino's real press mechanic is
 * `Modifier.alpha()` dimming the WHOLE control — shadow, fill, and content
 * together as one compositing layer — to [Cupertino.PRESSED_ALPHA]. This
 * is applied at the outermost `Box`, not the fill layer alone, so the
 * shadow visibly recedes too, matching how a real iOS control looks
 * pressed (dimmer AND flatter), not just a recolored fill.
 */
@Composable
fun CupertinoSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    pressed: Boolean = false,
    fillColor: Color = Cupertino.CardFill,
    shadowColor: Color = Cupertino.ShadowSoft,
    shadowElevation: Dp = 14.dp,
    showHairline: Boolean = true,
    hairlineColor: Color = Cupertino.Hairline,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .let {
                when {
                    onClick == null -> it
                    interactionSource != null -> it.clickable(
                        interactionSource = interactionSource,
                        indication = null, // caller supplies its own press feedback (alpha dim)
                        enabled = enabled,
                        onClick = onClick
                    )
                    else -> it.clickable(enabled = enabled, onClick = onClick)
                }
            }
            .alpha(if (pressed) Cupertino.PRESSED_ALPHA else 1f)
    ) {
        // Single soft ambient shadow — no offset, no second layer (see
        // class doc comparison table). Dims together with the fill below
        // via the outer Box's `.alpha()` when pressed, rather than being
        // removed outright the way NeumorphSurface's pair is.
        Box(
            Modifier.matchParentSize()
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
        )
        // Flat, fully opaque fill — no gradient, no translucency — with an
        // optional hairline border standing in for glass's visible edge
        // gradient (barely-there by design, see CupertinoTokens.Hairline).
        Box(
            Modifier.matchParentSize()
                .background(color = fillColor, shape = shape)
                .let { if (showHairline) it.border(width = 0.5.dp, color = hairlineColor, shape = shape) else it }
        )
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}
