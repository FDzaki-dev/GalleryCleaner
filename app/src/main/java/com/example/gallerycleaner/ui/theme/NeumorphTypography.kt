package com.example.gallerycleaner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Pure Neumorphism (Soft UI) typography — Amber Reserve exclusive.
 *
 * [GalleryTypography] (Type.kt) is shared, unmodified, across every OTHER
 * [MaterialStyle] ([MaterialStyle.GLASS]/[MaterialStyle.CUPERTINO]/
 * [MaterialStyle.SKEUO_LITE]) — until this batch it was ALSO reused as-is
 * for [MaterialStyle.NEUMORPH], the one gap left in this project's "murni,
 * no hybrid baseline from other themes" standard already enforced at the
 * color layer ([NeumorphTokens.kt], explicit Batch36 requirement: "tanpa
 * hybrid baseline bersama dari theme lain") and the surface layer
 * ([NeumorphSurface.kt]'s dual-shadow recipe). Type was the only axis
 * Amber Reserve still 100% inherited from Signature/Indigo Noir's shared
 * baseline — this file closes that gap.
 *
 * Sizes and letter-spacing are UNCHANGED from [GalleryTypography] — 0
 * layout risk, every screen composable that assumes those metrics keeps
 * rendering at identical dimensions regardless of which [AppTheme] is
 * active. The one deliberate, mechanical shift is FontWeight: one step up
 * per role (Normal→Medium, Medium→SemiBold, SemiBold→Bold). Rationale: a
 * flat monochromatic dual-shadow surface has near-zero color/border
 * contrast to lean on for hierarchy — unlike [MaterialStyle.GLASS]'s
 * translucent panel edges or [MaterialStyle.CUPERTINO]'s hairline
 * borders — so weight has to carry the hierarchy color would elsewhere.
 * That is a textbook soft-UI typography rule, not a freehand aesthetic
 * pick, and it is applied uniformly (same +1 step, every role) rather
 * than picked per-value.
 */
val NeumorphTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold, // GalleryTypography: SemiBold
        fontSize = 28.sp,
        letterSpacing = (-0.02).em
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold, // GalleryTypography: SemiBold
        fontSize = 22.sp,
        letterSpacing = (-0.01).em
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // GalleryTypography: Medium
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // GalleryTypography: Normal
        fontSize = 16.sp,
        letterSpacing = 0.01.em
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // GalleryTypography: Normal
        fontSize = 14.sp,
        letterSpacing = 0.01.em
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // GalleryTypography: Medium
        fontSize = 12.sp,
        letterSpacing = 0.08.em
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold, // GalleryTypography: SemiBold
        fontSize = 12.sp,
        letterSpacing = 0.1.em
    )
)
