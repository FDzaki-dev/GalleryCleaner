package com.example.gallerycleaner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// small local alias to avoid importing android.graphics.Color by mistake
private val Color0F = androidx.compose.ui.graphics.Color(0xFF0F1113)

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

@Composable
fun GalleryCleanerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GalleryDarkScheme,
        typography = GalleryTypography,
        content = content
    )
}
