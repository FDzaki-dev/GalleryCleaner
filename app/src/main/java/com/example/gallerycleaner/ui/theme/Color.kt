package com.example.gallerycleaner.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces — deep graphite, not pure black, so photos/cards have room to breathe
val GraphiteBg = Color(0xFF14171A)
val GraphiteSurface = Color(0xFF1D2125)
val GraphiteSurfaceRaised = Color(0xFF262B30)
val GraphiteOutline = Color(0xFF32383E)

// Text
val TextPrimary = Color(0xFFF3F5F6)
val TextSecondary = Color(0xFF9BA3AB)
val TextMuted = Color(0xFF6B7278)

// Signature accents: sage (keep) / coral (delete) — a calm "sort, don't scroll" palette
val SageKeep = Color(0xFF8FD9A8)
val SageKeepDim = Color(0xFF3A4A3F)
val CoralDelete = Color(0xFFFF7A68)
val CoralDeleteDim = Color(0xFF4A3230)
val AccentGold = Color(0xFFE0B96D) // used sparingly: progress rings, done states

// ---- Amber Reserve ----
// Warm espresso-charcoal surfaces with brass and oxblood accents — a
// "leather-bound ledger" feel. Deliberately desaturated: brass instead of
// yellow, oxblood instead of red, so it reads as considered rather than
// decorative. Keep/Delete stay semantically distinct (warm-gold-affirm vs
// deep-red-warn) without either one being a loud, literal traffic-light hue.
val EspressoBg = Color(0xFF17140F)
val EspressoSurface = Color(0xFF211C15)
val EspressoSurfaceRaised = Color(0xFF2B241A)
val EspressoOutline = Color(0xFF3D3325)
val IvoryText = Color(0xFFF5F0E6)
val IvoryTextSecondary = Color(0xFFB8AC93)
val BrassKeep = Color(0xFFC9A461)
val BrassKeepDim = Color(0xFF473B26)
val OxbloodDelete = Color(0xFFA85C48)
val OxbloodDeleteDim = Color(0xFF432A22)

val CreamBg = Color(0xFFFAF6EE)
val CreamSurfaceRaised = Color(0xFFF1E9D8)
val CreamOutline = Color(0xFFE3D7BE)
val EspressoTextPrimary = Color(0xFF241F16)
val EspressoTextSecondary = Color(0xFF6B6048)
val BrassKeepOnLight = Color(0xFF8C6D2E)
val OxbloodDeleteOnLight = Color(0xFF8B4331)

// ---- Indigo Noir ----
// Deep indigo-black surfaces with platinum and dusty-rose accents — an
// "evening, not a warning label" feel. Same reasoning as Amber Reserve:
// muted periwinkle instead of a loud blue, dusty rose instead of a loud
// red, kept just distinct enough from each other to still read instantly
// as "keep" vs "delete" alongside the icons that already carry that
// meaning on their own.
val IndigoBg = Color(0xFF12121C)
val IndigoSurface = Color(0xFF1B1B29)
val IndigoSurfaceRaised = Color(0xFF242436)
val IndigoOutline = Color(0xFF35354A)
val PlatinumText = Color(0xFFF1F0F7)
val PlatinumTextSecondary = Color(0xFF9F9DB5)
val PeriwinkleKeep = Color(0xFFA9A6D9)
val PeriwinkleKeepDim = Color(0xFF39374F)
val DustyRoseDelete = Color(0xFFC97A94)
val DustyRoseDeleteDim = Color(0xFF472F3A)

val LilacBg = Color(0xFFF7F6FB)
val LilacSurfaceRaised = Color(0xFFECEAF5)
val LilacOutline = Color(0xFFDAD7EA)
val IndigoTextPrimary = Color(0xFF1C1B29)
val IndigoTextSecondary = Color(0xFF605D78)
val PeriwinkleKeepOnLight = Color(0xFF5C57A8)
val DustyRoseDeleteOnLight = Color(0xFFA14D69)
