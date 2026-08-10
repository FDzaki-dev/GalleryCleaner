package com.example.gallerycleaner.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.example.gallerycleaner.AppTheme

/**
 * Batch27 — the "material language" a color style renders with, decoupled
 * from the color style itself. Before this batch every [AppTheme] rendered
 * through the exact same components (`GlassCard`/`GlassButton`/
 * `Modifier.glassPanel`) and only the M3 `ColorScheme` values differed —
 * so "Amber Reserve" was really just Signature's frosted-glass panels
 * repainted espresso/brass. That's a palette swap, not a different theme
 * architecture.
 *
 * [MaterialStyle] fixes that split: each [AppTheme] now maps to one style
 * via [materialStyleFor], and the shared components read [LocalMaterialStyle]
 * to decide HOW to render (translucent floating glass vs. matte raised
 * bevel), not just WHAT color. This is provided once in
 * `GalleryCleanerTheme` and consumed deep in `GlassCard`/`GlassButton`/
 * `GlassModifier.kt` — no call site in any screen file needs to know which
 * style is active or branch on it themselves.
 */
enum class MaterialStyle {
    /** Translucent frosted panels floating over an ambient gradient —
     *  the original Midnight Blue treatment (Signature), also used as-is
     *  by Indigo Noir (unchanged this batch — see PROJECT_STATE Batch27). */
    GLASS,

    /** Matte, opaque raised panels with a two-tone bevel edge (light
     *  catch top-left, dark shadow bottom-right) simulating a physically
     *  embossed surface — NOT the old, fully-deleted heavy
     *  `SkeuoMidnightTokens`/`MidnightSkeuoButton` system (metallic,
     *  debossed slots, multi-layer textures). "Lite" here means: solid
     *  color fill (no gradient sheen), a single directional shadow, and a
     *  2-stop border gradient — enough bevel to read as "physical object
     *  you could press" without the older system's visual weight. New for
     *  Batch27, used exclusively by Amber Reserve. */
    SKEUO_LITE
}

/** [AppTheme] → [MaterialStyle]. Signature and Indigo Noir keep the
 *  original glass material language exactly as before (0 regression —
 *  neither is touched by this mapping existing). Only Amber Reserve moves
 *  to [MaterialStyle.SKEUO_LITE], per explicit user request: a full
 *  material-language swap, not a recolor. */
fun materialStyleFor(appTheme: AppTheme): MaterialStyle = when (appTheme) {
    AppTheme.SIGNATURE -> MaterialStyle.GLASS
    AppTheme.AMBER_RESERVE -> MaterialStyle.SKEUO_LITE
    AppTheme.INDIGO_NOIR -> MaterialStyle.GLASS
}

/** Defaults to GLASS so any Composable that reads this without a provider
 *  above it (e.g. previews, tests) renders exactly like every call site
 *  did before Batch27 — this CompositionLocal being introduced can't
 *  silently change existing behavior anywhere it isn't explicitly wired. */
val LocalMaterialStyle = compositionLocalOf { MaterialStyle.GLASS }
