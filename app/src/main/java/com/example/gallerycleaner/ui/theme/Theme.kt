package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.gallerycleaner.AppTheme

// small local alias to avoid importing android.graphics.Color by mistake
private val Color0F = androidx.compose.ui.graphics.Color(0xFF0F1113)

// Light-mode surfaces — warm off-white rather than stark white, so the same
// calm "sort, don't scroll" feeling carries over instead of the app just
// looking like a generic default Material light theme.
private val LightBg = androidx.compose.ui.graphics.Color(0xFFFAF9F7)
private val LightSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val LightSurfaceRaised = androidx.compose.ui.graphics.Color(0xFFF0EEEB)
private val LightOutline = androidx.compose.ui.graphics.Color(0xFFDDD9D4)
private val LightTextPrimary = androidx.compose.ui.graphics.Color(0xFF1C1E1F)
private val LightTextSecondary = androidx.compose.ui.graphics.Color(0xFF5C6167)

// FULL override (2026-08-10) — rewrite total dari "Skeuomorphism-Dark
// Midnight Blue Edition" (Batch13) ke **Glassmorphism — Midnight Blue
// Edition**. Sama seperti override sebelumnya, ini bukan tambahan di atas
// sistem lama — SELURUH token & komponen skeuomorphism (`SkeuoMidnightTokens.kt`,
// `SkeuoMidnightModifier.kt`, `MidnightSkeuoButton.kt`, `MidnightSkeuoSlot.kt`)
// DIHAPUS batch ini, digantikan 100% oleh `MidnightGlass` (lihat
// `MidnightGlassTokens.kt`) + komponen baru `GlassModifier.kt`/`GlassCard.kt`/
// `GlassButton.kt`. Permintaan eksplisit user: rewrite total (bukan ganti
// palet warna saja) jadi hint & gradasi warna Midnight-Blue dengan gaya
// visual Glassmorphism yang paling dominan.
// Hue family (deep navy/indigo blue) DIPERTAHANKAN dari override sebelumnya
// — yang berubah adalah material language: raised/debossed metallic →
// translucent frosted glass. Primary/Secondary (SageKeep/CoralDelete) tetap
// TIDAK diubah — aturan project konsisten sejak override pertama: Keep/Delete
// semantic colors app-critical UX, di luar cakupan spec visual manapun.
// `surface`/`surfaceVariant` dipakai sebagai fallback SOLID (bukan translucent)
// untuk context M3 yang butuh `Color` biasa (mis. `CardDefaults.cardColors`)
// — translucency & edge-light "kaca" yang sesungguhnya datang dari
// `Modifier.glassPanel()`/`GlassCard` yang dipasang eksplisit per-komponen,
// bukan lewat ColorScheme (Color tidak bisa membawa Brush gradient).
private val SignatureDark = darkColorScheme(
    primary = SageKeep,
    onPrimary = Color0F,
    secondary = CoralDelete,
    onSecondary = Color0F,
    tertiary = MidnightGlass.GlowBlue,           // signature glass glow — selection/focus/progress/active accent
    onTertiary = Color0F,
    background = MidnightGlass.VoidDeep,         // deepest tone — canvas the ambient gradient + glass panels float over
    onBackground = MidnightGlass.TextBright,
    surface = MidnightGlass.GlassSurfaceFlat,    // solid fallback for plain M3 surfaces (Card/Sheet default colors)
    onSurface = MidnightGlass.TextBright,
    surfaceVariant = MidnightGlass.NavyCore,
    onSurfaceVariant = MidnightGlass.TextMuted,
    outline = MidnightGlass.EdgeHighlight,       // hairline reads as a faint glass edge, not a hard M3 divider
    error = CoralDelete,
    onError = Color0F
)

private val SignatureLight = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2F8552), // darker sage — keeps contrast on white
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFD44A32), // darker coral, same reason
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiary = MidnightGlass.GlowBlueOnLight,    // darker Midnight-Blue glow accent, for light-bg contrast
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = MidnightGlass.IceBackground,    // "frosted ice" counterpart to the dark glass ambient
    onBackground = MidnightGlass.IceTextPrimary,
    surface = MidnightGlass.IceGlassSurfaceFlat,
    onSurface = MidnightGlass.IceTextPrimary,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE4EAFB),
    onSurfaceVariant = MidnightGlass.IceTextSecondary,
    outline = androidx.compose.ui.graphics.Color(0xFFCBD5F0),
    error = androidx.compose.ui.graphics.Color(0xFFD44A32),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
)

private val AmberReserveDark = darkColorScheme(
    primary = BrassKeep,
    onPrimary = Color0F,
    secondary = OxbloodDelete,
    onSecondary = Color0F,
    background = EspressoBg,
    onBackground = IvoryText,
    surface = EspressoSurface,
    onSurface = IvoryText,
    surfaceVariant = EspressoSurfaceRaised,
    onSurfaceVariant = IvoryTextSecondary,
    outline = EspressoOutline,
    error = OxbloodDelete,
    onError = Color0F
)

private val AmberReserveLight = lightColorScheme(
    primary = BrassKeepOnLight,
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondary = OxbloodDeleteOnLight,
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = CreamBg,
    onBackground = EspressoTextPrimary,
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = EspressoTextPrimary,
    surfaceVariant = CreamSurfaceRaised,
    onSurfaceVariant = EspressoTextSecondary,
    outline = CreamOutline,
    error = OxbloodDeleteOnLight,
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
)

private val IndigoNoirDark = darkColorScheme(
    primary = PeriwinkleKeep,
    onPrimary = Color0F,
    secondary = DustyRoseDelete,
    onSecondary = Color0F,
    background = IndigoBg,
    onBackground = PlatinumText,
    surface = IndigoSurface,
    onSurface = PlatinumText,
    surfaceVariant = IndigoSurfaceRaised,
    onSurfaceVariant = PlatinumTextSecondary,
    outline = IndigoOutline,
    error = DustyRoseDelete,
    onError = Color0F
)

private val IndigoNoirLight = lightColorScheme(
    primary = PeriwinkleKeepOnLight,
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondary = DustyRoseDeleteOnLight,
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = LilacBg,
    onBackground = IndigoTextPrimary,
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = IndigoTextPrimary,
    surfaceVariant = LilacSurfaceRaised,
    onSurfaceVariant = IndigoTextSecondary,
    outline = LilacOutline,
    error = DustyRoseDeleteOnLight,
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
)

private fun colorSchemeFor(appTheme: AppTheme, darkTheme: Boolean): ColorScheme = when (appTheme) {
    AppTheme.SIGNATURE -> if (darkTheme) SignatureDark else SignatureLight
    AppTheme.AMBER_RESERVE -> if (darkTheme) AmberReserveDark else AmberReserveLight
    AppTheme.INDIGO_NOIR -> if (darkTheme) IndigoNoirDark else IndigoNoirLight
}

/** [darkTheme] mirrors com.example.gallerycleaner.ThemeMode (brightness);
 *  [appTheme] selects the color style (character) — see AppTheme. Both are
 *  resolved by the caller (MainActivity) from Settings and passed in here
 *  rather than read from DataStore directly, so this file stays pure
 *  Compose theming with no DataStore/Context dependency of its own. It does
 *  import the AppTheme enum from the app package — a one-directional
 *  dependency (ui.theme → app), which is fine within a single Gradle
 *  module: everything here compiles as one unit regardless of package
 *  layout, so this isn't a circular *module* dependency, just two packages
 *  in the same compilation. The alternative (a duplicate enum local to
 *  ui.theme) would only trade this for two enums that could silently drift
 *  out of sync — worse.
 *
 *  [appTheme] defaults to SIGNATURE (the original look) so any existing
 *  call site that doesn't pass it explicitly renders exactly as before —
 *  adding the color-style picker in Settings couldn't change anyone's
 *  existing app appearance by accident. */
@Composable
fun GalleryCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.SIGNATURE,
    content: @Composable () -> Unit
) {
    // Batch27: provide the resolved MaterialStyle once, here, alongside the
    // ColorScheme — see MaterialStyle.kt for why this is a separate axis
    // from color. GlassCard/GlassButton (and anything reading
    // LocalMaterialStyle in future) pick this up automatically; no other
    // call site in the app needed to change for Amber Reserve to switch
    // material language.
    CompositionLocalProvider(LocalMaterialStyle provides materialStyleFor(appTheme)) {
        MaterialTheme(
            colorScheme = colorSchemeFor(appTheme, darkTheme),
            typography = GalleryTypography,
            content = content
        )
    }
}
