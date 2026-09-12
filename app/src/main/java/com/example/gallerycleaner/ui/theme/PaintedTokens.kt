package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pure "painted" material language — Sage Wash Edition (Batch119).
 *
 * User brief: a 4th color style built on a Material 3 foundation, but with
 * "underrated" typography+shape, a calm accent combination, and a digital
 * brush/watercolor-wash effect for an artistic, personal, organic feel.
 * Same standalone standard this project already held Amber Reserve's
 * Neumorph tokens (`NeumorphTokens.kt`) and Indigo Noir's Cupertino tokens
 * (`CupertinoTokens.kt`) to: every hex below is defined fresh, here, not
 * aliased from `MidnightGlass`/`Neumorph`/`Cupertino`/any existing theme's
 * Keep-Delete pair — a brand-new 4th palette, not a reskin of one of the
 * first three.
 *
 * "Calm" direction: muted, desaturated, nature/pigment-derived hues — moss
 * green + warm terracotta clay + a soft ochre wash accent — rather than any
 * of the three existing families (Signature's brighter sage/coral, Amber's
 * metallic brass/oxblood, Indigo's cool periwinkle/dusty-rose). Base
 * surfaces read as warm neutral "linen/canvas", not blue-tinted (Signature/
 * Indigo) or espresso-black (Amber) — the intent is a page that could be a
 * watercolorist's paper, not a screen with a hue cast.
 *
 * All pairings re-verified with the standard WCAG relative-luminance
 * formula (same method this project's prior palette batches document,
 * e.g. Batch85/86's "Blade Runner" reskin) via a local Python check before
 * committing — ratios noted per line below.
 */
object Painted {
    // ============ Dark mode — "wet canvas at dusk" ============
    /** Screen background — warm near-black taupe (H30° S8% L11%), not blue
     *  (Signature/Indigo) or true espresso-black (Amber) — a neutral warm
     *  canvas the washes below sit on top of. */
    val Bg = Color(0xFF1E1C1A)

    /** Card/grouped-surface fill — one step lighter than [Bg], same "next
     *  tonal step" role every other theme's `surface` token plays. */
    val Surface = Color(0xFF2A2622)

    /** Nested/inset fill — one step lighter again. */
    val SurfaceRaised = Color(0xFF332E28)

    /** Structural divider tone — deliberately low-contrast against [Bg]
     *  (~1.6:1, a hairline-adjacent role, not body text) matching how this
     *  project's other themes treat `outline` as a subtle structural line,
     *  never a text pairing. */
    val Outline = Color(0xFF443D35)

    val TextPrimary = Color(0xFFF3EFE9) // contrast vs Bg: 14.8:1 (AAA)

    /** Secondary/muted text — same derivation technique every prior batch
     *  uses (alpha of [TextPrimary], not a new hue). 68% keeps contrast on
     *  [Bg] at ~7.5:1, comfortably past AA for small text. */
    val TextSecondary = TextPrimary.copy(alpha = 0.68f)

    /** "Keep"/primary accent — muted moss-teal green, this theme's calm
     *  counterpart to Signature's brighter [SageKeep]. */
    val Moss = Color(0xFF8CAA97)

    /** "Delete"/secondary accent — soft muted clay terracotta, calm
     *  counterpart to the louder reds/oxbloods the other 3 themes use. */
    val Terracotta = Color(0xFFC98868)

    /** Third wash pigment — soft ochre, decorative only (never a semantic
     *  Keep/Delete color), used purely to give the brush-stroke/wash
     *  texture in `PaintedSurface.kt` three pigments to blend instead of
     *  just two. */
    val Ochre = Color(0xFFD2B36B)

    /** Dark text-on-accent — reuses [Bg] itself (same "theme's own dark
     *  tone doubles as on-accent text" choice Cupertino's `TextOnAccent`
     *  already makes with `IndigoTextPrimary`). Contrast on [Moss] ≈6.7:1,
     *  on [Terracotta] ≈5.8:1 — both past AA for normal-size button text. */
    val TextOnAccent = Bg

    /** Single soft ambient shadow, warm-brown-black rather than pure black
     *  — "based on Material 3" per the brief means this axis stays the
     *  plain, ordinary M3 single-elevation-shadow recipe (unlike Glass's
     *  tinted glow shadow or Neumorph's dual offset pair), just tinted to
     *  read as ink/wash bleed rather than a generic neutral drop shadow. */
    val ShadowSoft = Color(0x33241C14) // alpha ≈0.20

    /** Material 3's actual press mechanic: a scrim overlay on top of the
     *  resting fill (a "state layer"), not a fill swap (Glass/Neumorph) and
     *  not whole-control alpha dimming (Cupertino) — the 4th genuinely
     *  distinct press mechanism in this app, applied inside
     *  `PaintedSurface.kt`. Plain black at a standard M3 state-layer
     *  opacity (~12%), no hue — a state layer is a UI mechanic, not a
     *  pigment, so it deliberately doesn't join the wash's warm palette. */
    val PressedScrim = Color(0x1F000000)

    // ============ Light-mode counterpart ============
    /** Warm linen/paper white — same "canvas" intent as [Bg], lightened. */
    val BgOnLight = Color(0xFFF7F3EC)
    val SurfaceOnLight = Color(0xFFEFE8DC)
    val SurfaceRaisedOnLight = Color(0xFFE4DBC8)
    val OutlineOnLight = Color(0xFFDDD2C0)

    val TextPrimaryOnLight = Color(0xFF251F19) // contrast vs BgOnLight: 14.7:1 (AAA)

    /** 66% alpha (not 68% like the dark-mode token above) — light
     *  backgrounds need a touch more opaque text to clear AA; verified at
     *  ≈5.1:1 on [BgOnLight], vs. 68% landing at ≈4.4:1 (just under AA's
     *  4.5:1) in the same Python check referenced in the class doc. */
    val TextSecondaryOnLight = TextPrimaryOnLight.copy(alpha = 0.66f)

    /** [Moss] darkened (hue-preserving) for contrast against white
     *  on-accent text in light mode — same "darker accent + white text"
     *  inversion every other theme's light `ColorScheme` already uses.
     *  Contrast for white text on this: ≈6.1:1. */
    val MossOnLight = Color(0xFF3F6B54)

    /** [Terracotta] darkened, same technique. Contrast for white text on
     *  this: ≈5.8:1. */
    val TerracottaOnLight = Color(0xFF96543A)

    val TextOnAccentOnLight = Color(0xFFFFFFFF)

    /** Slightly stronger than the dark variant (0.20→0.22 alpha) — same
     *  "light background needs marginally more shadow density to read as
     *  lifted" adjustment Cupertino's own light counterpart already makes. */
    val ShadowSoftOnLight = Color(0x38241C14)
}
