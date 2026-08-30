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
 *
 * **Batch84 addition — 4 previously-missing roles**: investigated first
 * (grep every `MaterialTheme.typography.*` call site project-wide, same
 * "read before touching" discipline as every prior audit item) and found
 * `displayLarge`/`headlineSmall`/`labelMedium`/`labelSmall` ARE actually
 * used (`OnboardingScreen.kt`'s emoji + page title, `MainActivity.kt`'s
 * lock-screen title, `HomeScreenSections.kt`/`SettingsScreen.kt`/
 * `TrashScreen.kt`/`HomeScreenFolderRow.kt`'s small labels/badges) but
 * were undefined in BOTH this file and [GalleryTypography] — meaning those
 * specific `Text()` calls silently fell through to Compose Material3's own
 * hardcoded, un-themed default `Typography()` (not Signature's look, not
 * Amber Reserve's — literally neither custom type system this project
 * has), the one remaining "not murni" gap Batch80 didn't know to close.
 * Sizes/letter-spacing below are Material3's own official default values
 * for these 4 roles (the true pre-existing baseline these calls were
 * already rendering at — 0 layout risk, same as the "sizes unchanged"
 * rule above), fontFamily set to match the rest of this file, and weight
 * bumped the same mechanical +1 step. [GalleryTypography] (Signature/
 * Indigo Noir) has the identical gap for these same 4 roles — untouched
 * here, out of scope for an Amber-Reserve-only "Neumorphism murni" batch,
 * flagged in PROJECT_STATE.md's Pending Queue.
 * (Note: this paragraph originally shipped mislabeled "Batch83" in-code —
 * corrected to Batch84 here to match PROJECT_STATE.md/CHANGELOG.md.)
 *
 * **Batch85 — Blade Runner reskin, fontFamily lever**: per explicit user
 * request ("theme tetap Neumorphism, tapi...typography...pakai gaya
 * visual ala Blade Runner"). `fontFamily` switches to [FontFamily.Monospace]
 * for the 4 roles used as prominent, single-line, generously-spaced text —
 * [displayLarge] (onboarding emoji — moot, emoji ignore fontFamily, kept
 * only for definition consistency), [headlineSmall]/[headlineMedium]/
 * [titleLarge] (page titles, dialog/lock-screen titles, section headers).
 * Monospace is a standard, widely-recognized "terminal/HUD readout" cue
 * (Voight-Kampff/ESPER-style on-screen text) — a real typographic identity
 * shift, not just weight. Deliberately NOT applied to [titleMedium]/
 * [bodyLarge]/[bodyMedium]/[bodySmall]/[labelLarge]/[labelMedium]/
 * [labelSmall] (left at [FontFamily.SansSerif], unchanged): monospace
 * glyphs are wider per-character than proportional sans at the same size,
 * and this project has no compiler/device in the loop to verify wrapping —
 * risking truncation in the app's tightest-fit elements (`InfoChip`
 * badges, list-row labels) for a purely aesthetic gain on text that's
 * rarely a single hero line isn't a trade this batch makes. fontSize/
 * letterSpacing/fontWeight are UNCHANGED on every role (this batch's only
 * lever is fontFamily on 4 roles) — 0 further layout-risk beyond the
 * font-width change on those 4 already-generously-spaced roles.
 */
val NeumorphTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace, // Batch85: was SansSerif
        fontWeight = FontWeight.Medium, // M3 default: Normal
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace, // Batch85: was SansSerif
        fontWeight = FontWeight.Medium, // M3 default: Normal
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace, // Batch85: was SansSerif
        fontWeight = FontWeight.Bold, // GalleryTypography: SemiBold
        fontSize = 28.sp,
        letterSpacing = (-0.02).em
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace, // Batch85: was SansSerif
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
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // M3 default: Medium
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // M3 default: Medium
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)
