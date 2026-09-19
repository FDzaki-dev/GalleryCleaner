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

/** Teks tampil (Indonesia) buat key grup sintetis. Key aslinya SENGAJA tetap
 *  literal Inggris — dipakai sebagai id progress/label DataStore dan `when`
 *  di SwipeScreen/MainActivity — jadi cuma dipetakan saat ditampilkan. */
fun displayGroupName(key: String): String = when {
    key == "Largest files" -> "File terbesar"
    key == "Large files (10MB+)" -> "File gede (10MB+)"
    key == "On this day" -> "Hari ini di masa lalu"
    key == "Blurry photos" -> "Foto blur"
    key == "Similar photos" -> "Foto mirip"
    key.startsWith("Similar photos (") -> "Foto mirip" + key.removePrefix("Similar photos")
    key == "Duplicate files" -> "File duplikat"
    key == "Search results" -> "Hasil pencarian"
    key == "Unknown album" -> "Album nggak dikenal"
    else -> key
}
