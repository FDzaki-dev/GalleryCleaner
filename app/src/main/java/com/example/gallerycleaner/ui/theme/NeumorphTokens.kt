package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pure Neumorphism (Soft UI) — Amber Reserve Edition.
 * Batch36: original full redesign (see PROJECT_STATE.md Batch36).
 * Batch81: palette REFRESH per explicit user request — "ganti komposisi
 * warna jadi lebih menarik, calm, dan tetap sesuai identitas theme
 * Neumorphism" — plus a new border token for the fading edge-light
 * border (see NeumorphSurface.kt doc comment for the border itself).
 *
 * This is a hex-value refresh, NOT an identity change: every property
 * name below is unchanged from Batch36, so `Theme.kt`/`GlassButton.kt`/
 * `SettingsScreen.kt` need zero edits — they keep reading
 * `Neumorph.DeepNavy`/`Neumorph.ClassicBrass`/etc. exactly as before, just
 * with new values. The 60/30/10 (Base/Structural/Accent) ratio, the
 * "Amber Reserve" gold-on-navy identity, and every derivation TECHNIQUE
 * (hue-preserving lightness shift for pressed/on-light variants, alpha-only
 * derivation for secondary text) all carry over unchanged from Batch36 —
 * only the underlying hue/saturation/lightness NUMBERS moved, chosen to
 * feel calmer and less generic than the old slate-gray navy + raw-yellow
 * gold pairing:
 * - Base/Card hue nudged from a flat slate-gray toward a richer
 *   blue-violet (H≈228°) — same "dark navy" identity, less flat/cold.
 * - Accent desaturated + lightened from a raw gold (H46° S65% L52%) to a
 *   softer champagne/honey gold (H39° S58% L60%) — reads as considered
 *   rather than loud, while staying unmistakably "brass".
 * - Shadow pair softened slightly (dark shadow alpha 0.55→0.45, light
 *   highlight alpha 0.045→0.06) so the dual-shadow depth cue reads as
 *   gentle rather than harsh against the richer base hue.
 * Keep/Delete semantics (`OxbloodDelete`/`OxbloodDeleteOnLight` in
 * `Color.kt`, wired via `Theme.kt`'s `error`/`secondary`) are explicitly
 * OUT of scope here, same standing rule as every prior full-theme-rewrite
 * batch (Signature, Batch36 itself, Cupertino Batch74) — this refresh only
 * touches background/structural/accent/text, never the Keep/Delete pair.
 * All values re-verified against WCAG per-value below (contrast ratios
 * computed the same way Batch36 originally documented them).
 */
object Neumorph {
    // ============ Calmer Batch81 palette — same roles as Batch36 ============
    val DeepNavy = Color(0xFF13182A)     // H228° S38% L12% — richer blue-violet navy, same "base" role
    val NavyCard = Color(0xFF20263C)     // H228° S30% L18% — same hue family as DeepNavy, one step lighter
    val ClassicBrass = Color(0xFFD4AB5E) // H39° S58% L60% — softer champagne-gold, same "accent" role

    val TextPrimary = Color(0xFFF3F3F7)  // near-white with a faint cool tint (contrast 15.9:1 on DeepNavy — past AAA's 7:1)
    val TextOnBrass = Color(0xFF13182A)  // = DeepNavy reused as dark-on-brass text (contrast 8.2:1 on ClassicBrass — past AAA's 4.5:1)

    /** Secondary/muted text — same derivation technique as Batch36 (alpha
     *  of [TextPrimary], not a new hue). 68% alpha keeps contrast on
     *  [DeepNavy] at ~7.9:1, still comfortably above AA for small text. */
    val TextSecondary = TextPrimary.copy(alpha = 0.68f)

    /** CTA pressed-state fill — [ClassicBrass] darkened (same hue/saturation,
     *  lightness 60%→40%), identical hue-preserving technique Batch36 used
     *  and every other theme's "OnLight" variant already uses. */
    val ClassicBrassPressed = Color(0xFFA1782B)

    // ============ Shadow pair — the neumorphism dual-shadow recipe ============
    // Same technique as Batch36 (pure black/white blended into the surface
    // at low alpha, light source top-left / falloff bottom-right) — only
    // the alpha softened slightly for a calmer, less harsh depth cue now
    // that the base hue itself carries a bit more richness than before.
    val ShadowDark = Color(0x73000000)   // bottom-right, alpha ≈0.45 (was 0.55)
    val ShadowLight = Color(0x0FFFFFFF)  // top-left, alpha ≈0.06 (was 0.045)

    /** NEW (Batch81) — edge-light for the fading border added to every
     *  panel this batch (see NeumorphSurface.kt). Warm off-white rather
     *  than pure white, so the "light catching the top-left edge" reads
     *  as coming from the same warm light implied by [ClassicBrass],
     *  instead of a cold, unrelated rim-light hue. Paired with
     *  `Color.Transparent` at the panel's bottom-right in a
     *  `Brush.linearGradient` — this token is only ever the gradient's
     *  START color, never used as a flat fill. */
    val BorderFade = Color(0x33FDF6E8)   // alpha ≈0.20, warm off-white

    // ============ Light-mode counterpart ============
    // Same hue-preserving derivation technique as Batch36 (same hue as the
    // dark-mode token, lightness pushed up in HLS space) — only the
    // resulting numbers moved along with the dark-mode refresh above.
    val LightBg = Color(0xFFF2F4FA)          // DeepNavy hue, HLS lightness → 0.965
    val LightCard = Color(0xFFDDE1EE)        // NavyCard hue, HLS lightness → 0.90
    val BrassOnLight = Color(0xFFB58730)     // ClassicBrass hue, HLS lightness → 0.45 (contrast 5.4:1 vs TextOnBrass, passes AA)
    val TextPrimaryOnLight = DeepNavy         // reuses the dark-navy hex as dark-on-light text (contrast 16.0:1)
    val TextSecondaryOnLight = DeepNavy.copy(alpha = 0.64f)

    // Light-mode shadow pair — same standard soft-UI-on-light-bg formula
    // as Batch36, alpha nudged slightly softer to match the calmer dark-mode
    // shadow pair above. NOTE (pre-existing, not introduced this batch):
    // NeumorphSurface's own fillColor/pressedFillColor params still default
    // to the dark-mode tokens only — these OnLight tokens are wired into
    // AmberReserveLight's ColorScheme (Theme.kt) but not yet read by
    // NeumorphSurface itself, so light-mode Neumorph panels are a known,
    // untouched gap (out of scope for this batch — ZERO-REFACTOR).
    val ShadowDarkOnLight = Color(0x21000000)   // alpha ≈0.13 (was 0.15)
    val ShadowLightOnLight = Color(0xE6FFFFFF)  // alpha ≈0.90 (unchanged)

    /** NEW (Batch81) — light-mode counterpart of [BorderFade], same
     *  soft/dark-edge convention as [ShadowDarkOnLight]. Currently UNUSED
     *  (same pre-existing gap noted above: NeumorphSurface doesn't yet
     *  branch on light/dark), included only so the token exists ready for
     *  whenever that gap is closed — not wired to anything this batch. */
    val BorderFadeOnLight = Color(0x2E000000)   // alpha ≈0.18, soft dark edge
}
