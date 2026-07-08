package com.example.gallerycleaner

import android.net.Uri
import android.content.Context

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
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                failed += uri
            }
        }
        return failed
    }
}
