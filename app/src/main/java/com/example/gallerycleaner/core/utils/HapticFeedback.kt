package com.example.gallerycleaner

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Direct Vibrator calls instead of Compose's LocalHapticFeedback.
 * performHapticFeedback(). The Compose/View API routes through
 * View.isHapticFeedbackEnabled() and the system's per-type haptic
 * settings, both of which some OEM skins mute by default outside of
 * standard system widgets — which is exactly why it went in silently and
 * produced nothing. VibrationEffect goes straight to the vibration motor
 * and only depends on the VIBRATE permission (normal, auto-granted).
 *
 * Batch106: minSdk is now 31 = Build.VERSION_CODES.S (Batch105), so the
 * legacy pre-S VibratorManager fallback and the pre-O VibrationEffect
 * fallback below were structurally unreachable — removed as part of the
 * dead-code cleanup that minSdk 31 enabled. `Build` import dropped too
 * (no longer referenced anywhere in this file).
 */

private fun vibrator(context: Context): Vibrator? = try {
    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    manager?.defaultVibrator
} catch (e: Exception) {
    null
}

/** [pattern]: alternating off/on ms, starting with an initial delay —
 *  e.g. [0, 12] = no delay, vibrate 12ms. */
private fun vibrate(context: Context, pattern: LongArray) {
    try {
        val v = vibrator(context) ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } catch (e: Exception) {
        // Never worth crashing or interrupting a swipe over this.
    }
}

/** One light, short tick — Keep. */
fun hapticKeep(context: Context) = vibrate(context, longArrayOf(0, 15))

/** Two quick pulses — Delete. Distinct rhythm from Keep. */
fun hapticDelete(context: Context) = vibrate(context, longArrayOf(0, 12, 40, 12))
