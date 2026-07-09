package com.example.gallerycleaner

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * Wraps the platform-specific dance required to delete MediaStore items
 * once scoped storage is in effect (Android 10+).
 */
object DeleteHelper {

    /**
     * Attempts to delete all [uris]. On Android 11+ this should normally be
     * routed through [MediaStore.createDeleteRequest] (see MainActivity),
     * which shows a single system confirmation for the whole batch.
     * This direct-delete path is the fallback for older OS versions.
     */
    fun deleteDirectly(context: Context, uris: List<Uri>): List<Uri> {
        val failed = mutableListOf<Uri>()
        for (uri in uris) {
            try {
                val rowsDeleted = context.contentResolver.delete(uri, null, null)
                if (rowsDeleted == 0) {
                    failed.add(uri)
                }
            } catch (e: Exception) {
                Log.e("DeleteHelper", "Error saat menghapus: $uri", e)
                failed.add(uri)
            }
        }

        // AMAN: Dibungkus Handler & Looper agar tidak memicu Force Close
        if (failed.isNotEmpty()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Gagal menghapus ${failed.size} file. Periksa izin akses.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return failed
    }
}
