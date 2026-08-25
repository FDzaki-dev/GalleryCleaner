package com.example.gallerycleaner

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Batch49 — In-app update, step 1/2 (check-only; download+install UI wired
 * next batch, see PROJECT_STATE.md Pending Queue).
 *
 * Hits GitHub's `/releases/latest` REST API for this repo and decides
 * whether the release currently published is one the person hasn't
 * seen/installed yet.
 *
 * IMPORTANT — why this compares tag STRINGS, not version NUMBERS:
 * The app's own `BuildConfig.VERSION_CODE`/`VERSION_NAME` come from
 * `git rev-list --count HEAD` (commit count), while the GitHub Release
 * tag (`v1.0.<run_number>`, see `.github/workflows/build.yml`) comes from
 * `GITHUB_RUN_NUMBER` — a different, unrelated counter for the exact same
 * release (already flagged as a known mismatch in PROJECT_STATE.md, e.g.
 * APK showing v1.0.44 next to a GitHub Release already at v1.0.165).
 * Comparing those two numbers against each other is meaningless — a
 * higher run_number does not mean "newer than my commit-count version".
 * Fixing that dual-numbering scheme is out of scope for this task (it
 * touches the protected workflow/build files for an unrelated reason), so
 * instead this class sidesteps it entirely: it remembers the tag_name of
 * the release it last told the person about (SharedPreferences, one
 * string), and only reports "update available" when the API returns a
 * *different* tag than that. First-ever check on a fresh install has
 * nothing to compare against yet, so it takes the current latest tag as
 * its baseline and reports up-to-date rather than guessing.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/FDzaki-dev/GalleryCleaner/releases/latest"
    private const val PREFS_NAME = "update_checker_prefs"
    private const val KEY_LAST_KNOWN_TAG = "last_known_tag"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class UpdateInfo(
        val tagName: String,
        val releaseName: String,
        val releaseNotes: String,
        val apkDownloadUrl: String,
        val apkSizeBytes: Long,
        val apkFileName: String,
    )

    sealed class CheckResult {
        data object UpToDate : CheckResult()
        data class UpdateAvailable(val info: UpdateInfo) : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    suspend fun checkForUpdate(context: Context): CheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github+json")
                // GitHub's REST API rejects requests with no User-Agent.
                .header("User-Agent", "GalleryCleaner-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext CheckResult.Error("GitHub API error: HTTP ${response.code}")
                }

                val bodyString = response.body?.string()
                    ?: return@withContext CheckResult.Error("Empty response body")

                val json = JSONObject(bodyString)
                val tagName = json.optString("tag_name", "")
                if (tagName.isEmpty()) {
                    return@withContext CheckResult.Error("Release has no tag_name")
                }

                val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()
                var apkUrl: String? = null
                var apkSize = 0L
                var apkName = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "").ifEmpty { null }
                        apkSize = asset.optLong("size", 0L)
                        apkName = name
                        break
                    }
                }

                if (apkUrl == null) {
                    return@withContext CheckResult.Error("Latest release has no .apk asset attached")
                }

                val info = UpdateInfo(
                    tagName = tagName,
                    releaseName = json.optString("name", tagName),
                    releaseNotes = json.optString("body", ""),
                    apkDownloadUrl = apkUrl,
                    apkSizeBytes = apkSize,
                    apkFileName = apkName,
                )

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastKnownTag = prefs.getString(KEY_LAST_KNOWN_TAG, null)

                when (lastKnownTag) {
                    null -> {
                        // Fresh install / first check ever: no baseline to
                        // compare against, see class doc. Set it now.
                        prefs.edit().putString(KEY_LAST_KNOWN_TAG, tagName).apply()
                        CheckResult.UpToDate
                    }
                    tagName -> CheckResult.UpToDate
                    else -> CheckResult.UpdateAvailable(info)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Update check failed (network)", e)
            CheckResult.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed (parse)", e)
            CheckResult.Error(e.message ?: "Unexpected error")
        }
    }

    /**
     * Call once a release has been downloaded (or explicitly dismissed by
     * the person) so the *next* check doesn't re-report the same tag.
     */
    fun markTagAsKnown(context: Context, tagName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_KNOWN_TAG, tagName)
            .apply()
    }
}
