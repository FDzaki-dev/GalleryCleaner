package com.example.gallerycleaner.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Shared destructive-action CTA — solid, opaque `colorScheme.secondary`
 * (CoralDelete) button, deliberately NOT `GlassButton`/skeuo-lite. Same
 * principle as `PillChip`'s "selected" state (Batch22): a destructive
 * action needs an unambiguous, high-contrast signal, not a translucent
 * floating panel — glass/skeuo materials are for a surface's default
 * state, not "this deletes something permanently". `GlassButton` has no
 * color parameter for this precisely because it was never meant to cover
 * it (see `GlassButton.kt` doc comment — its whole point is glow/inset
 * feedback, which reads as "safe to tap", the opposite of the deliberately
 * hard, un-glowy Delete button this app has always shown).
 *
 * Extracted (Batch30) from 3 IDENTICAL `Button(colors =
 * ButtonDefaults.buttonColors(containerColor = colorScheme.secondary,
 * contentColor = Color(0xFF1A0E0C)))` blocks duplicated verbatim across
 * `HomeScreenSections.kt` ("Clean up"), `SwipeScreenGrid.kt` ("Delete N
 * selected"), `TrashScreen.kt` ("Delete permanently") — same DRY risk the
 * `GlassSurface` extraction already fixed for panels: 3 copies that could
 * silently drift apart if the destructive-button style ever needed to
 * change. `MainActivity.kt`'s crash-dialog "Saya Mengerti" button is NOT
 * this pattern (plain default `Button`, no custom colors) and is
 * deliberately left untouched — a crash dialog should stay as simple and
 * dependency-free as possible.
 */
@Composable
fun DangerButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color(0xFF1A0E0C)
        )
    ) {
        Text(text)
    }
}
