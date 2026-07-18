package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

private val GalleryDarkScheme = darkColorScheme(
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

private val GalleryLightScheme = lightColorScheme(
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

/** Mirrors com.example.gallerycleaner.ThemeMode without this module needing
 *  to depend on the app package — kept as plain strings/enum-free so
 *  ui.theme stays a leaf module. Callers pass the resolved boolean instead. */
@Composable
fun GalleryCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) GalleryDarkScheme else GalleryLightScheme,
        typography = GalleryTypography,
        content = content
    )
}
