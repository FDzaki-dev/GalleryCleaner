package com.example.gallerycleaner

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Batch49 — In-app update, step 1/2 (download-only; install-trigger UI
 * wired next batch, see PROJECT_STATE.md Pending Queue).
 *
 * Streams the release APK to app-specific external storage
 * (`getExternalFilesDir(null)/updates/`) strictly chunk-by-chunk via Okio —
 * project rule "Release Downloader (Anti-OOM)": never call `.bytes()` /
 * `.readBytes()` on the response body, which would materialize the whole
 * APK (tens of MB) in RAM at once. Each loop iteration reads at most
 * [CHUNK_SIZE] bytes into a small Okio [Buffer] and immediately drains it
 * to disk, so peak memory use stays flat regardless of APK size.
 *
 * Writes to a `.part` file first and only renames to the final file name
 * once the full expected length has actually been written — an
 * interrupted download (timeout, connection drop, cancelled coroutine)
 * never leaves behind a file that looks complete but isn't.
 */
object ApkDownloader {

    private const val TAG = "ApkDownloader"
    private const val UPDATE_SUBDIR = "updates"
    private const val CHUNK_SIZE = 8L * 1024L // 8 KB per read/write cycle

    private val client = OkHttpClient.Builder()
        // Explicit timeouts per project rule. No overall callTimeout cap —
        // APK size (and therefore total transfer time) varies by release;
        // connect/read timeouts below are what actually guard against a
        // stalled connection, not a fixed wall-clock ceiling.
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    suspend fun download(
        context: Context,
        url: String,
        fileName: String,
        expectedSizeBytes: Long,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), UPDATE_SUBDIR).apply { mkdirs() }
        val partFile = File(dir, "$fileName.part")
        val finalFile = File(dir, fileName)

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GalleryCleaner-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Error("HTTP ${response.code}")
                }

                val body = response.body
                    ?: return@withContext DownloadResult.Error("Empty response body")

                val totalBytes = body.contentLength().takeIf { it > 0 } ?: expectedSizeBytes
                var bytesRead = 0L
                val source = body.source()
                val buffer = Buffer()

                partFile.sink().buffer().use { sink ->
                    while (true) {
                        val read = source.read(buffer, CHUNK_SIZE)
                        if (read == -1L) break
                        sink.write(buffer, read)
                        bytesRead += read
                        onProgress(bytesRead, totalBytes)
                    }
                }

                if (totalBytes > 0 && bytesRead < totalBytes) {
                    partFile.delete()
                    return@withContext DownloadResult.Error(
                        "Incomplete download: got $bytesRead of $totalBytes bytes"
                    )
                }

                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    return@withContext DownloadResult.Error("Could not finalize downloaded file")
                }

                DownloadResult.Success(finalFile)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Download failed", e)
            partFile.delete()
            DownloadResult.Error(e.message ?: "Network error during download")
        }
    }

    /** Clears any previously downloaded/partial APKs from the updates folder. */
    fun clearDownloadedFiles(context: Context) {
        File(context.getExternalFilesDir(null), UPDATE_SUBDIR)
            .listFiles()
            ?.forEach { it.delete() }
    }
}
