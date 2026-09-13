package com.example.gallerycleaner

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.util.Log

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
            } catch (e: RecoverableSecurityException) {
                // Bug fix (found during review, no prior tracker entry): this
                // used to fall through to the generic `catch (e: Exception)`
                // below — RecoverableSecurityException extends
                // SecurityException extends Exception — and got silently
                // added to `failed` like any other failure. That made
                // MainActivity.proceedWithPermanentDeletion's own
                // `catch (e: RecoverableSecurityException)` (which launches
                // the system permission-grant dialog and retries) 100%
                // unreachable: the exception never survived long enough to
                // get there. On API 29 specifically (the only level in this
                // <30 branch where the OS actually throws this exception —
                // scoped storage write/delete permission checks start at
                // API 29), a delete needing that one-time consent just
                // failed silently instead of ever prompting for it. Rethrow
                // so it reaches the handler already written for it — same
                // precedence MoveHelper/ImageCompressor already use
                // (RecoverableSecurityException caught ahead of the generic
                // Exception catch, never folded into it).
                throw e
            } catch (e: Exception) {
                Log.e("DeleteHelper", "Error saat menghapus: $uri", e)
                failed.add(uri)
            }
        }
        return failed
    }
}
