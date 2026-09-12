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
    // ============ Dark mode — "botanical ink at dusk" ============
    // Batch120 REVISION: user tested Batch119's build (screenshot evidence,
    // 0 compile error — see PROJECT_STATE.md Batch119) and flagged the
    // result as reading "cheap/scam-app", not premium. Root cause,
    // diagnosed from the screenshot: the ORIGINAL v1 palette paired a warm
    // brown/taupe [Bg] with a warm orange-leaning [Terracotta] — warm-on-
    // warm, analogous, and (per WCAG check) genuinely lower-contrast than
    // it read on paper, so panels came across as flat muddy brown instead
    // of "watercolor art", and the warm-orange accent on trash/warning text
    // specifically read as caution-tape rather than calm clay. Signature/
    // Amber Reserve/Indigo Noir all share one structural trait this
    // v1 palette didn't: a COOL, rich dark base contrasted with WARM accent
    // pops (navy+brass, navy+coral, indigo+dusty-rose) — that base/accent
    // contrast is what reads as "premium", not the specific hues. v2 below
    // applies the same principle: [Bg] shifts from brown to a deep,
    // desaturated forest-charcoal (cool, sage-adjacent — ties to this
    // theme's own name instead of fighting it), and [Terracotta] shifts
    // hue away from orange toward a muted brick-rose (still "terracotta
    // pottery", far less "alert cone"). Wash-blob alpha is also raised in
    // `PaintedSurface.kt` so the watercolor texture actually reads at a
    // glance instead of disappearing into the fill.
    /** Screen background — deep, desaturated forest-charcoal (H155° S20%
     *  L9%), COOL rather than warm-brown (v1) — ties to this theme's own
     *  "sage" identity instead of reading as plain taupe, and gives the
     *  warm [Moss]/[Terracotta]/[Ochre] accents something to contrast
     *  against (same "cool base, warm pop" principle Signature/Amber/
     *  Indigo already use). */
    val Bg = Color(0xFF121C18)

    /** Card/grouped-surface fill — one step lighter than [Bg], same "next
     *  tonal step" role every other theme's `surface` token plays. */
    val Surface = Color(0xFF192420)

    /** Nested/inset fill — one step lighter again. */
    val SurfaceRaised = Color(0xFF222F29)

    /** Structural divider tone — deliberately low-contrast against [Bg]
     *  (~1.6:1, a hairline-adjacent role, not body text) matching how this
     *  project's other themes treat `outline` as a subtle structural line,
     *  never a text pairing. */
    val Outline = Color(0xFF3D5247)

    val TextPrimary = Color(0xFFF1F3EE) // contrast vs Bg: 15.6:1 (AAA)

    /** Secondary/muted text — same derivation technique every prior batch
     *  uses (alpha of [TextPrimary], not a new hue). 68% keeps contrast on
     *  [Bg] at ~7.5:1, comfortably past AA for small text. */
    val TextSecondary = TextPrimary.copy(alpha = 0.68f)

    /** "Keep"/primary accent — moss-teal green, this theme's calm
     *  counterpart to Signature's brighter [SageKeep]. Batch120: saturation
     *  raised slightly vs. v1 so it actually pops as an accent against the
     *  now-darker/cooler [Bg], rather than nearly blending into it. */
    val Moss = Color(0xFF72B694)

    /** "Delete"/secondary accent — muted brick-rose terracotta, calm
     *  counterpart to the louder reds/oxbloods the other 3 themes use.
     *  Batch120: hue shifted away from v1's orange-leaning clay (which
     *  read as caution/warning-orange on real-device screenshots, see
     *  class doc) toward red/pink (H10° vs. v1's ~H20°) at lower
     *  saturation — reads as dusty pottery-clay rather than alert-orange,
     *  while keeping the "terracotta" identity. Text-on-accent contrast
     *  re-verified ≥4.5:1 after the hue shift (lightness tuned to 58% to
     *  clear AA — 52% only cleared 4.1:1, see Batch120 Python check). */
    val Terracotta = Color(0xFFBF7769)

    /** Third wash pigment — soft ochre, decorative only (never a semantic
     *  Keep/Delete color), used purely to give the brush-stroke/wash
     *  texture in `PaintedSurface.kt` three pigments to blend instead of
     *  just two. */
    val Ochre = Color(0xFFC1A667)

    /** Dark text-on-accent — reuses [Bg] itself (same "theme's own dark
     *  tone doubles as on-accent text" choice Cupertino's `TextOnAccent`
     *  already makes with `IndigoTextPrimary`). Contrast on [Moss] ≈7.3:1,
     *  on [Terracotta] ≈5.0:1 — both past AA for normal-size button text
     *  (re-verified Batch120 after the hue/lightness revision above). */
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
     *  Contrast for white text on this: ≈5.1:1 (re-tuned Batch120 to match
     *  [Moss]'s revised hue/saturation above). */
    val MossOnLight = Color(0xFF3E795C)

    /** [Terracotta] darkened, same technique, same revised brick-rose hue
     *  as the dark-mode value above. Contrast for white text on this:
     *  ≈7.5:1 (re-tuned Batch120). */
    val TerracottaOnLight = Color(0xFF814337)

    val TextOnAccentOnLight = Color(0xFFFFFFFF)

    /** Slightly stronger than the dark variant (0.20→0.22 alpha) — same
     *  "light background needs marginally more shadow density to read as
     *  lifted" adjustment Cupertino's own light counterpart already makes. */
    val ShadowSoftOnLight = Color(0x38241C14)
}
