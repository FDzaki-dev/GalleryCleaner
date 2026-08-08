package com.example.gallerycleaner

import android.content.Context
import android.os.Build
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
 */

private fun vibrator(context: Context): Vibrator? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
} catch (e: Exception) {
    null
}

/** [pattern]: alternating off/on ms, starting with an initial delay —
 *  e.g. [0, 12] = no delay, vibrate 12ms. */
private fun vibrate(context: Context, pattern: LongArray) {
    try {
        val v = vibrator(context) ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    } catch (e: Exception) {
        // Never worth crashing or interrupting a swipe over this.
    }
}

/** One light, short tick — Keep. */
fun hapticKeep(context: Context) = vibrate(context, longArrayOf(0, 15))

/** Two quick pulses — Delete. Distinct rhythm from Keep. */
fun hapticDelete(context: Context) = vibrate(context, longArrayOf(0, 12, 40, 12))
