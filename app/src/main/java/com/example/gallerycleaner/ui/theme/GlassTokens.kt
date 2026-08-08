package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens for "Premium AMOLED Hybrid Glassmorphism + Subtle Midnight
 * Blue + Micro-Skeuomorphism" (see /docs or the design spec this file
 * implements 1:1). Golden rule: AMOLED + glass must be perceived first,
 * Midnight Blue second, tactile effects third. If any single token here is
 * tuned up in isolation, re-check that rule still holds.
 */

// ---- §3 AMOLED FOUNDATION ----
// Near-black, not pure #000000, so glass layers on top stay perceptible.
val AmoledBlack = Color(0xFF030508)
val AmoledSurface = Color(0xFF070A0F)

// ---- §5 GLASS COLOR TOKENS ----
val GlassBase = Color(0xFF0A0F16)      // Level 1 — subtle translucent glass
val GlassElevated = Color(0xFF101722)  // Level 2 — elevated frosted glass
val GlassPressed = Color(0xFF070B11)   // recessed / pressed state

val GlassWhite = Color.White.copy(alpha = 0.045f)
val GlassHighlight = Color.White.copy(alpha = 0.065f)
val GlassBorder = Color.White.copy(alpha = 0.035f)
val GlassBorderStrong = Color.White.copy(alpha = 0.06f) // §8 "for stronger surfaces"

val GlassShadow = Color.Black.copy(alpha = 0.70f)

// ---- §6 MIDNIGHT BLUE — ATMOSPHERIC LAYER ONLY ----
// Never used as a solid background/base — gradient ingredient only.
val MidnightBlue = Color(0xFF191970)
val MidnightBlueAccent = Color(0xFF6670FF)
const val MidnightBlueAmbientAlpha = 0.06f

/**
 * §6 "Correct use" — ambient gradient for screen/level-2+ backgrounds.
 * Never call background(MidnightBlue) directly (§6 "Incorrect use").
 */
fun midnightAmbientGradient(
    from: Color = GlassBase,
    to: Color = GlassElevated
): Brush = Brush.linearGradient(
    colors = listOf(
        from,
        MidnightBlue.copy(alpha = MidnightBlueAmbientAlpha),
        to
    )
)

// ---- §9 LIGHTING MODEL — single simulated light, top-left → bottom-right ----
// All components must share this direction; don't flip per-component.
val LightSourceAlignment = Pair(/* x */ -1f, /* y */ -1f) // top-left origin

// ---- §16 TYPOGRAPHY COLOR ----
val GlassTextPrimary = Color(0xFFEAF0F8)
val GlassTextSecondary = Color(0xFFAAB5C4)
val GlassTextMuted = Color(0xFF737E8C)

// ---- §17 ACCENT SYSTEM ----
// Restrained cool-blue, reserved for selected/active/focused/progress/important-action.
val AccentBlue = Color(0xFF6670FF)

// ---- §18 GLOW ----
// Glow is an accent, not a material — always paired with AccentBlue and always localized.
val GlowAccent = AccentBlue.copy(alpha = 0.35f)
