package com.example.gallerycleaner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Pure "painted" typography — Sage Wash exclusive (Batch119).
 *
 * [GalleryTypography] (Signature/Indigo Noir) and [NeumorphTypography]
 * (Amber Reserve) are both Sans-Serif throughout, differing only in weight/
 * tracking (Neumorph) or nothing at all (Signature/Indigo share Gallery's
 * exact values). Per the batch brief ("typography...keren underrated"),
 * this theme instead pairs [FontFamily.Serif] for prominent, large-scale
 * roles (display/headline/title) with [FontFamily.SansSerif] for body/
 * label roles — a genuinely distinct typographic identity, not just a
 * weight bump, and a deliberately uncommon choice for an Android utility
 * app (serif display type reads as editorial/hand-lettered, fitting a
 * "painted, personal, organic" brief) while keeping body text legible at
 * small sizes where serif glyphs are riskiest.
 *
 * [FontFamily.Serif] is a guaranteed system fallback bundled with every
 * Android version this app supports (same zero-asset-risk profile
 * [NeumorphTypography] already relies on for its own [FontFamily.Monospace]
 * roles) — no custom font file is added or assumed here.
 *
 * "Calm" direction on weight/tracking: every role here is EQUAL TO or
 * LIGHTER than [GalleryTypography]'s weight for the same role (never
 * bumped up, unlike [NeumorphTypography]'s uniform +1-step-heavier rule),
 * and letter-spacing is opened slightly rather than tightened — Gallery's
 * display/title tracking is negative (-0.02em/-0.01em, a "considered,
 * assertive" tightness); this theme's own tracking for the same roles is
 * loosened to 0em/positive values instead, reading as unhurried rather
 * than tightly kerned. Sizes are UNCHANGED from [GalleryTypography]/M3
 * defaults for every role — 0 layout risk, same "sizes never move" rule
 * every prior per-theme Typography in this project already follows.
 *
 * All 11 roles are defined explicitly (including [displayLarge]/
 * [headlineSmall]/[labelMedium]/[labelSmall] — the 4 roles [GalleryTypography]
 * itself still leaves undefined, a known gap flagged in PROJECT_STATE.md
 * since Batch84) so this new theme doesn't inherit that same silent
 * fall-through-to-M3-default gap on day one.
 */
val PaintedTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal, // M3 default
        fontSize = 57.sp,
        letterSpacing = 0.sp // M3 default: -0.25.sp — opened up for a calmer, less tightly-kerned hero size
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium, // GalleryTypography: SemiBold — one step lighter, calmer
        fontSize = 28.sp,
        letterSpacing = 0.em // GalleryTypography: -0.02.em — loosened, not tightened
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium, // GalleryTypography: SemiBold
        fontSize = 22.sp,
        letterSpacing = 0.em // GalleryTypography: -0.01.em
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal, // GalleryTypography: Medium
        fontSize = 17.sp,
        letterSpacing = 0.01.em // GalleryTypography: 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.015.em // GalleryTypography: 0.01.em
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.015.em // GalleryTypography: 0.01.em
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal, // GalleryTypography: Medium — lighter, calmer
        fontSize = 12.sp,
        letterSpacing = 0.09.em // GalleryTypography: 0.08.em
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // GalleryTypography: SemiBold — lighter
        fontSize = 12.sp,
        letterSpacing = 0.11.em // GalleryTypography: 0.1.em
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // M3 default — same value NeumorphTypography's Batch84 addition used
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // M3 default
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)
