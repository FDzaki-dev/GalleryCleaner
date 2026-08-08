package com.example.gallerycleaner

import kotlin.math.ln
import kotlin.math.pow

/** Formats a byte count as a human-readable size, e.g. 1536 -> "1.5 KB". */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return if (digitGroups == 0) "$bytes B" else "%.1f %s".format(value, units[digitGroups])
}
