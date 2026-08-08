package com.example.gallerycleaner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Consistent rounded geometry per spec §19 — don't mix unrelated radii. */
val ShapeSmallControl = RoundedCornerShape(11.dp)   // 10-12dp
val ShapeStandardControl = RoundedCornerShape(14.dp) // 12-16dp
val ShapeCard = RoundedCornerShape(18.dp)            // 16-20dp
val ShapeLargeSurface = RoundedCornerShape(22.dp)    // 20-24dp

val GalleryShapes = Shapes(
    extraSmall = ShapeSmallControl,
    small = ShapeSmallControl,
    medium = ShapeStandardControl,
    large = ShapeCard,
    extraLarge = ShapeLargeSurface
)
