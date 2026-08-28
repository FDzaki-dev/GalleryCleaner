package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.LocalMaterialStyle
import com.example.gallerycleaner.ui.theme.MaterialStyle

/**
 * Drop-in floating-panel container — the shared `Card` equivalent for
 * every color style. Use this anywhere a floating panel/card look is
 * wanted (dashboard tiles, list rows, dialogs).
 *
 * Batch27: this composable is the single branch point between the
 * material languages the app supports. It reads [LocalMaterialStyle] (set
 * once per color style in `GalleryCleanerTheme`, see `MaterialStyle.kt`)
 * and renders `Modifier.glassPanel()` (Signature — translucent frosted
 * glass), `Modifier.skeuoPanel()` (unused as of Batch36, see
 * `MaterialStyle.kt`), `NeumorphSurface` (Batch36, Amber Reserve — pure
 * dual-shadow soft-UI), or `CupertinoSurface` (Batch77, Indigo Noir — flat
 * opaque grouped-card). No call site anywhere in the app
 * (`HomeScreenSections.kt`, `TrashScreen.kt`, etc.) changed to make this
 * happen or the Batch36 redesign happen — they all still just call
 * `GlassCard { ... }` and get whichever material the active theme maps to.
 *
 * Batch36: [MaterialStyle.NEUMORPH] branches BEFORE the `Box`/modifier-
 * chain construction below (early `return`), not inside the `.let { when
 * ... } ` — `NeumorphSurface` needs two independently-offset shadow layers
 * (see its doc comment), which can't be expressed as one linear `Modifier`
 * chain the way `glassPanel`/`skeuoPanel` can. Batch77: [MaterialStyle.CUPERTINO]
 * (Indigo Noir) follows the exact same early-return shape, calling
 * `CupertinoSurface` instead — see its doc comment for why it's also a
 * Composable, not a Modifier extension. Everything else about this
 * function (params, [onClick]/[enabled] semantics, the `LocalContentColor`
 * fix from Batch23) is identical either way.
 *
 * [shape]/[elevation] defaults intentionally stay glass's values
 * (`RoundedCornerShape(18.dp)`/`12.dp`) for source compatibility with
 * every existing call site — when [MaterialStyle.SKEUO_LITE] is active,
 * `skeuoPanel` is called with ITS OWN defaults (`12.dp` corner/`8.dp`
 * elevation) rather than these, so Amber Reserve still gets its intended
 * tighter/flatter bevel look without any call site needing to pass
 * different values in for that theme.
 *
 * [onClick] (Batch22): optional, mirrors the old `Surface(...).clickable{}`
 * pattern this replaces across the app's dashboard/list panels. Deliberately
 * applied to the modifier chain AFTER the panel modifier (not folded into
 * the caller-supplied [modifier], which is applied BEFORE it) — same order
 * [GlassButton] already uses, so the ripple/indication paints on top of the
 * panel fill instead of being drawn under it and hidden. [enabled] covers
 * callers that need to suppress the tap while a scan/action is in flight
 * (e.g. `ScanTriggerRow`) without hiding the card entirely.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalMaterialStyle.current

    if (style == MaterialStyle.NEUMORPH) {
        NeumorphSurface(
            modifier = modifier,
            contentPadding = contentPadding,
            onClick = onClick,
            enabled = enabled
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
        return
    }

    // Batch77 (Cupertino stage 2): same early-return shape as NEUMORPH above
    // — CupertinoSurface is a Composable (needs a shadow layer + separate
    // fill layer + optional hairline, see its doc comment), not a linear
    // Modifier chain like glassPanel/skeuoPanel. Indigo Noir only, as of
    // this batch (see materialStyleFor in MaterialStyle.kt).
    if (style == MaterialStyle.CUPERTINO) {
        CupertinoSurface(
            modifier = modifier,
            contentPadding = contentPadding,
            onClick = onClick,
            enabled = enabled
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
        return
    }

    Box(
        modifier = modifier
            .let {
                when (style) {
                    MaterialStyle.GLASS -> it.glassPanel(shape = shape, elevation = elevation)
                    MaterialStyle.SKEUO_LITE -> it.skeuoPanel()
                    MaterialStyle.NEUMORPH -> it // unreachable — handled by the early return above
                    MaterialStyle.CUPERTINO -> it // unreachable — handled by the early return above
                }
            }
            .let { if (onClick != null) it.clickable(enabled = enabled, onClick = onClick) else it }
            .padding(contentPadding)
    ) {
        // Batch23 readability fix: Box (unlike M3 Surface) never provided
        // LocalContentColor, so any Text() inside GlassCard without an
        // explicit color= fell back to Compose's hard default (Color.Black)
        // — invisible on dark glass panels. This is why titles ("Blurry
        // photos", "Agustus 2026", etc.) rendered black while subtitles
        // stayed readable (those Text() calls already passed an explicit
        // onSurfaceVariant color). Providing onSurface here fixes every
        // GlassCard call site centrally; any Text() that already sets an
        // explicit color is unaffected (explicit color always wins).
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}
