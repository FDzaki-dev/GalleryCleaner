package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

private val SignatureDark = darkColorScheme(
    primary = SageKeep,
    onPrimary = Color0F,
    secondary = CoralDelete,
    onSecondary = Color0F,
    background = GraphiteBg,
    onBackground = TextPrimary,
    surface = GraphiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteSurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = GraphiteOutline,
    error = CoralDelete,
    onError = Color0F
)

private val SignatureLight = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2F8552), // darker sage — keeps contrast on white
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFD44A32), // darker coral, same reason
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
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
    MaterialTheme(
        colorScheme = colorSchemeFor(appTheme, darkTheme),
        typography = GalleryTypography,
        content = content
    )
}
