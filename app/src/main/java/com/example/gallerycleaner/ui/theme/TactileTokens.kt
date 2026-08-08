package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tactile / micro-skeuomorphic tokens (§10-§13, §20). Applied ONLY to
 * interactive controls (buttons, switches, sliders, knobs, selected
 * controls) — never to structural elements (cards, containers, toolbars,
 * nav shells, static panels), which stay primarily glassmorphic.
 */

// §11 TACTILE BUTTON
const val ButtonPressedScale = 0.98f
const val ButtonRestScale = 1f
val ButtonRestElevation: Dp = 4.dp
val ButtonPressedElevation: Dp = 1.dp

// §12 TACTILE SWITCH
const val SwitchPressedScale = 0.96f
val SwitchOffElevation: Dp = 0.dp   // recessed
val SwitchOnElevation: Dp = 3.dp    // subtly elevated

// §13 TACTILE SLIDER
val SliderKnobElevation: Dp = 3.dp
val SliderKnobPressedElevation: Dp = 1.dp

// §20 MOTION — short, immediate, no exaggerated bounce
const val TactileAnimationDurationMs = 120
const val TactileReleaseAnimationDurationMs = 160
