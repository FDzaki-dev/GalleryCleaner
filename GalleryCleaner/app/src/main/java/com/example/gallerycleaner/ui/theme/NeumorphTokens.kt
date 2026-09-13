package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pure Neumorphism (Soft UI) — Amber Reserve Edition.
 * Batch36: original full redesign (see PROJECT_STATE.md Batch36).
 * Batch81: calmer palette refresh (softer champagne-gold, blue-violet navy).
 * Batch85: art-direction reskin per explicit user request — "theme tetap
 * Neumorphism, tapi komposisi warna...pakai gaya visual ala Blade Runner".
 * Reverses Batch81's "calm" direction on purpose — Blade Runner's identity
 * is high-contrast neo-noir, not gentle/considered, so this batch trades
 * Batch81's softened numbers for more dramatic ones. "Amber Reserve" was
 * already gold-accent-on-dark before this batch, which happens to line up
 * with Blade Runner's own iconic amber CRT/HUD readouts (Voight-Kampff,
 * ESPER "enhance" machine) — so the accent hue FAMILY stays brass/amber,
 * only pushed from soft champagne to a saturated neon-sign orange. What's
 * new is the near-black base (was navy) and a cyan rim-light on the
 * existing border token, for the classic BR contrast of warm key light
 * against cool ambient glow.
 *
 * This is again a hex-value refresh, NOT an identity change: every
 * property name below is unchanged from Batch36/81, so `Theme.kt`/
 * `GlassButton.kt`/`SettingsScreen.kt` need zero edits — they keep reading
 * `Neumorph.DeepNavy`/`Neumorph.ClassicBrass`/etc. exactly as before, just
 * with new values. The 60/30/10 (Base/Structural/Accent) ratio and every
 * derivation TECHNIQUE (hue-preserving lightness shift for pressed/
 * on-light variants, alpha-only derivation for secondary text) carry over
 * unchanged — only the underlying hue/saturation/lightness NUMBERS moved:
 * - Base/Card hue moved from blue-violet (H228°) to a cooler, near-black
 *   teal-blue (H210°, L7%/13%) — "night city, rain-slicked asphalt" rather
 *   than a lit interior navy.
 * - Accent pushed from a softened champagne-gold (H39° S58% L60%) to a
 *   saturated neon amber (H32° S92% L56%) — reads as glowing signage/CRT
 *   phosphor rather than a considered, muted gold.
 * - Shadow pair intensified back up (dark shadow alpha 0.45→0.60) for a
 *   harder, more dramatic depth cue — the opposite direction from Batch81,
 *   deliberately, since "calm" and "Blade Runner" pull opposite ways and
 *   this batch's explicit reference wins for this request.
 * - [BorderFade] re-hued from warm off-white to a saturated cyan
 *   (H185° S85% L58%) — the cool rim-light half of BR's signature
 *   warm-key/cool-rim contrast; [ShadowLight] gets the same cyan tint at a
 *   much lower alpha for the same reason.
 *
 * **Batch86 fix — stacked-card contrast + border boost**: user-reported
 * bug, "efek stacked card nya terlalu nyaru sama background belakang".
 * Root cause traced to `NeumorphSurface.kt`'s "Dense 3-layer stack" (3
 * flat panels fanned toward the light source, see that file's own doc
 * comment) — its back/mid layer colors are `lerp(fillColor=[NavyCard],
 * pressedFillColor=[DeepNavy], 1.6f/0.8f)`, extrapolated PAST [DeepNavy]
 * on the same line. [DeepNavy] is also the page/Scaffold background
 * itself, so that extrapolation's headroom is "how much darker than the
 * page background can we go" — Batch85 pushed both [DeepNavy] (L7%) and
 * [NavyCard] (L13%) so close together (both near the bottom of the
 * lightness range for the "near-black" Blade Runner mood) that the
 * extrapolated stack layers landed only ~6-10 RGB units from the actual
 * background, easily lost to a phone screen/JPEG's shadow crushing — the
 * literal cause of "nyaru". [ShadowDark] had the same problem for a
 * different reason: at H0/S0/L3%(≈8,8,8)/alpha 0.60 composited onto
 * [DeepNavy], the blended result landed only 1-10 RGB units from
 * [DeepNavy] itself. Fix (values only, same derivation techniques,
 * verified in Python before committing — see below): [NavyCard] lightness
 * bumped 13%→17% (widens the fillColor↔pressedFillColor line the stack
 * extrapolates along, without changing [DeepNavy]/the page bg at all —
 * the "near-black base" mood users see on first load is unchanged, only
 * the ELEVATED-panel tone lightened enough to give the stack somewhere to
 * separate to); [ShadowDark] pushed toward true black (H0 S0 L1%≈3,3,3)
 * at higher alpha (0.60→0.82) so it no longer half-vanishes into
 * [DeepNavy] when composited. Re-verified in Python: the back stack layer
 * now sits ~11-20 RGB units from the page background (was ~7-12), and
 * [ShadowDark] composited on [DeepNavy] now differs by ~6-18 RGB units
 * (was ~1-10) — real, computed improvement, though final judgment is a
 * real-device call this sandbox can't render-test.
 * Second, unrelated user request same message: "garis Border nya wajib
 * 'ultra Bold'" (border WIDTH lives in `NeumorphSurface.kt`, not this
 * file — see that file's own Batch86 note) — [BorderFade]'s alpha is
 * bumped 0.30→0.50 alongside the width increase there, same hue/direction
 * unchanged, purely so the now much-thicker stroke reads as a crisp bold
 * edge instead of a wide-but-diluted one.
 * Keep/Delete semantics (`OxbloodDelete`/`OxbloodDeleteOnLight` in
 * `Color.kt`, wired via `Theme.kt`'s `error`/`secondary`) are explicitly
 * OUT of scope here, same standing rule as every prior full-theme-rewrite
 * batch (Signature, Batch36, Cupertino Batch74, Batch81) — this reskin
 * only touches background/structural/accent/text/border, never Keep/Delete.
 * All values re-verified against WCAG below (contrast ratios computed the
 * same way every prior palette batch documented them — see per-line notes).
 *
 * **Batch87 — stacked-panel magenta rehue**: adds [StackFill]/
 * [StackFillPressed], dedicated magenta counterparts of [NavyCard]/
 * [DeepNavy] used only as `NeumorphSurface`'s new stack-fill DEFAULTS —
 * see those tokens' own doc comment below for the full rationale (why
 * dedicated tokens instead of re-huing [NavyCard]/[DeepNavy] directly).
 */
object Neumorph {
    // ============ Batch85 "Blade Runner" palette — same roles as Batch36/81 ============
    val DeepNavy = Color(0xFF0A1219)     // H210° S42% L7% — near-black cool teal, same "base" role (name kept for stability, no longer literally "navy")
    val NavyCard = Color(0xFF1C2B3B)     // H210° S36% L17% (Batch86: was L13%) — widens the fillColor↔pressedFillColor gap the 3-layer stack extrapolates along, see class doc "Batch86 fix"
    val ClassicBrass = Color(0xFFF69628) // H32° S92% L56% — saturated neon amber, same "accent" role (name kept, no longer a soft champagne)

    val TextPrimary = Color(0xFFF2F6F7)  // near-white, faint cyan tint (contrast 17.3:1 on DeepNavy — well past AAA's 7:1)
    val TextOnBrass = Color(0xFF0A1219)  // = DeepNavy reused as dark-on-brass text (contrast 8.4:1 on ClassicBrass — past AAA's 4.5:1)

    /** Secondary/muted text — same derivation technique as Batch36/81 (alpha
     *  of [TextPrimary], not a new hue). 70% alpha keeps contrast on
     *  [DeepNavy] at ~8.8:1, comfortably above AA for small text. */
    val TextSecondary = TextPrimary.copy(alpha = 0.70f)

    /** CTA pressed-state fill — [ClassicBrass] darkened (same hue/saturation,
     *  lightness 56%→36%), identical hue-preserving technique every prior
     *  batch used for pressed/on-light variants. */
    val ClassicBrassPressed = Color(0xFFB06107)

    // ============ Shadow pair — the neumorphism dual-shadow recipe ============
    // Same technique as Batch36/81 (pure black/white blended into the
    // surface at low alpha, light source top-left / falloff bottom-right)
    // — alpha intensified this batch (opposite of Batch81's softening) for
    // a harder, more film-noir depth cue; light half re-tinted cyan (was
    // pure white) to match [BorderFade]'s new cool rim-light.
    val ShadowDark = Color(0xD1030303)   // bottom-right, alpha ≈0.82, near-true-black (Batch86: was 0.60/RGB 8,8,8 — see class doc "Batch86 fix")
    val ShadowLight = Color(0x1A85D9E0)  // top-left, alpha ≈0.10, cyan-tinted (was warm white 0.06)

    /** Batch81 introduced this token (fading edge-light border, see
     *  NeumorphSurface.kt); Batch85 re-hues it from warm off-white to a
     *  saturated cyan (H185° S85% L58%) — the cool half of Blade Runner's
     *  warm-key/cool-rim lighting contrast against [ClassicBrass]'s neon
     *  amber. Paired with `Color.Transparent` at the panel's bottom-right
     *  in a `Brush.linearGradient` — this token is only ever the
     *  gradient's START color, never a flat fill. */
    val BorderFade = Color(0x8039E0EF)   // alpha ≈0.50, neon cyan (Batch86: was 0.30 — pairs with the width bump in NeumorphSurface.kt, see that file)

    // ============ Light-mode counterpart ============
    // Same hue-preserving derivation technique as every prior batch (same
    // hue as the dark-mode token, lightness pushed up in HLS space) — only
    // the resulting numbers moved along with the dark-mode reskin above.
    // NOTE: Blade Runner is a night-native aesthetic; this light variant
    // exists only to keep the app's existing dark/light toggle working
    // (same reason Batch81 kept one too), not as a considered "daytime BR"
    // art direction.
    val LightBg = Color(0xFFF1F5F9)          // DeepNavy hue, HLS lightness → 0.96
    val LightCard = Color(0xFFDCE6EF)        // NavyCard hue, HLS lightness → 0.90
    val BrassOnLight = Color(0xFFCE7209)     // ClassicBrass hue, HLS lightness → 0.42 (contrast 5.4:1 vs TextOnBrass, passes AA)
    val TextPrimaryOnLight = DeepNavy         // reuses the dark-navy hex as dark-on-light text (contrast 17.2:1)
    val TextSecondaryOnLight = DeepNavy.copy(alpha = 0.64f) // contrast ≈5.5:1, passes AA

    // Light-mode shadow pair — same standard soft-UI-on-light-bg formula as
    // every prior batch, alpha nudged up slightly to echo the dark-mode
    // pair's harder falloff; light half cyan-tinted for the same rim-light
    // consistency as ShadowLight/BorderFade above. NOTE (pre-existing, not
    // introduced this batch): NeumorphSurface's own fillColor/
    // pressedFillColor params still default to the dark-mode tokens only —
    // these OnLight tokens are wired into AmberReserveLight's ColorScheme
    // (Theme.kt) but not yet read by NeumorphSurface itself, so light-mode
    // Neumorph panels are a known, untouched gap (out of scope for this
    // batch — ZERO-REFACTOR, same gap Batch81 already flagged).
    val ShadowDarkOnLight = Color(0x26241924)   // alpha ≈0.15 (was 0.13 Batch81)
    val ShadowLightOnLight = Color(0xE6F1F8F9)  // alpha ≈0.90, faint cyan tint (was pure white)

    /** Batch81 introduced this token (light-mode counterpart of
     *  [BorderFade]); Batch85 re-hues it to a dark cyan edge, matching the
     *  dark-mode token's new hue family. Currently UNUSED (same
     *  pre-existing gap noted above: NeumorphSurface doesn't yet branch on
     *  light/dark), included only so the token exists ready for whenever
     *  that gap is closed — not wired to anything this batch. */
    val BorderFadeOnLight = Color(0x38144D52)   // alpha ≈0.22, dark cyan edge (was neutral dark 0.18)

    // ============ Batch87 — stacked-panel magenta rehue ============
    /** User: "3-dense layer stacked ganti jadi warna magenta ala Blade
     *  Runner, tapi jangan terlalu kontras/mencolok". These are dedicated
     *  tokens for `NeumorphSurface`'s stack-fill default — deliberately
     *  NOT a hue edit to [NavyCard]/[DeepNavy] themselves, since those two
     *  are also read directly by `Theme.kt` (page `background`/`surface`/
     *  `outline`) and by `NeumorphSurface`'s stack-derivation math for
     *  ANY caller that passes a different `fillColor` (e.g.
     *  `GlassButton.kt`'s CTA passes [ClassicBrass] explicitly) — editing
     *  the shared tokens would've re-hued the page background and bled
     *  into the brass CTA button's own stack shadow, neither of which the
     *  user asked for. Wiring only touches `NeumorphSurface.kt`'s two
     *  DEFAULT parameter values, so callers that already override
     *  fillColor/pressedFillColor (`GlassButton.kt`) are unaffected, and
     *  `Theme.kt`'s direct [NavyCard]/[DeepNavy] reads are unaffected too.
     *  Derivation: same hue-preserving-lightness technique as every prior
     *  reskin batch (verified in Python) — [NavyCard]'s exact S/L
     *  (H211° S36% L17%) and [DeepNavy]'s exact S/L (H208° S43% L7%)
     *  carried over unchanged, only hue rotated to H320° (a muted
     *  plum-magenta, not a saturated neon pink) — same saturation/
     *  lightness budget as before is exactly what keeps this "tidak
     *  terlalu kontras/mencolok" per the request, since nothing about how
     *  LOUD the color reads (S/L) changed, only its hue family. */
    val StackFill = Color(0xFF3B1C31)         // H320° S36% L17% — magenta counterpart of NavyCard
    val StackFillPressed = Color(0xFF190A14)  // H320° S43% L7% — magenta counterpart of DeepNavy
}
