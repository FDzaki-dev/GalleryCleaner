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
 * bevel vs. soft dual-shadow neumorphism), not just WHAT color. This is
 * provided once in `GalleryCleanerTheme` and consumed deep in
 * `GlassCard`/`GlassButton`/`GlassModifier.kt`/`NeumorphSurface.kt` — no
 * call site in any screen file needs to know which style is active or
 * branch on it themselves.
 *
 * Batch36: Amber Reserve moved from [SKEUO_LITE] to the new [NEUMORPH].
 * [SKEUO_LITE] itself is NOT removed this batch — `SkeuoLiteTokens.kt`/
 * `SkeuoModifier.kt` still exist and still compile, just unreferenced by
 * any [AppTheme] now. Deleting them is a separate cleanup pending explicit
 * user approval (see PROJECT_STATE.md "Belum Dikerjakan") per this
 * project's delete-only-with-permission rule — kept here, unused, rather
 * than removed as a side effect of this redesign.
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
     *  you could press" without the older system's visual weight.
     *  Introduced Batch27 for Amber Reserve; unused as of Batch36 (see
     *  class doc above) — kept defined, not deleted. */
    SKEUO_LITE,

    /** Pure Neumorphism (Soft UI) — flat monochromatic surface, dual
     *  independently-offset shadow (light top-left + dark bottom-right),
     *  NO border, NO gradient, NO specular glow. A genuinely distinct
     *  recipe from both [GLASS] and [SKEUO_LITE] (see the comparison
     *  table on `NeumorphSurface.kt`'s doc comment) built on its own
     *  standalone token file (`NeumorphTokens.kt`) that doesn't alias any
     *  [SKEUO_LITE]/[GLASS] color — explicit user requirement for Batch36:
     *  "murni ... tanpa hybrid baseline bersama dari theme lain". New for
     *  Batch36, used exclusively by Amber Reserve. */
    NEUMORPH
}

/** [AppTheme] → [MaterialStyle]. Signature and Indigo Noir keep the
 *  original glass material language exactly as before (0 regression —
 *  neither is touched by this mapping existing, nor by Batch36). Amber
 *  Reserve moves to [MaterialStyle.NEUMORPH] as of Batch36 (was
 *  [MaterialStyle.SKEUO_LITE] since Batch27) — another full
 *  material-language swap, not a recolor, per explicit user request. */
fun materialStyleFor(appTheme: AppTheme): MaterialStyle = when (appTheme) {
    AppTheme.SIGNATURE -> MaterialStyle.GLASS
    AppTheme.AMBER_RESERVE -> MaterialStyle.NEUMORPH
    AppTheme.INDIGO_NOIR -> MaterialStyle.GLASS
}

/** Defaults to GLASS so any Composable that reads this without a provider
 *  above it (e.g. previews, tests) renders exactly like every call site
 *  did before Batch27 — this CompositionLocal being introduced can't
 *  silently change existing behavior anywhere it isn't explicitly wired. */
val LocalMaterialStyle = compositionLocalOf { MaterialStyle.GLASS }
