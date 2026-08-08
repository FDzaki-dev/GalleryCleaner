package com.example.gallerycleaner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "cleaning_reminders"
private const val NOTIFICATION_ID = 1001
private const val UNIQUE_WORK_NAME = "cleaning_reminder_check"

/** Only bother the user once there's a genuinely useful amount to review —
 *  a notification for "2 screenshots" would feel like noise, not help. */
private const val MIN_ITEMS_TO_NOTIFY = 5

/**
 * Periodic, opt-in check for "you have stuff worth cleaning up" — the same
 * idea as the cleanup-suggestion notifications in Files by Google. Never
 * scheduled unless the user explicitly turns it on in Settings, and
 * re-checks that setting on every single run (not just at schedule time) —
 * that closes a real race: if the user disables the reminder at almost the
 * same moment a run was already queued, cancelling the schedule doesn't
 * retroactively stop a run already in flight, but this check does.
 *
 * Deliberately only counts screenshots + large files, not full duplicate
 * detection — that needs file-content hashing (see
 * MediaRepository.findExactDuplicates), which is too heavy to justify
 * running on a daily background schedule just for a notification.
 */
class CleaningReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        if (!settingsStore.cleaningReminderEnabledFlow.first()) {
            return Result.success()
        }

        val hasReadPermission = ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReadPermission) {
            // Permission was revoked since the user enabled this — nothing
            // safe to check, so skip quietly rather than fail loudly.
            return Result.success()
        }

        val media = MediaRepository.loadAllMedia(applicationContext)
        val cleanupCount = MediaRepository.smartCategories(media).sumOf { it.items.size }

        if (cleanupCount >= MIN_ITEMS_TO_NOTIFY) {
            postNotification(cleanupCount)
        }
        return Result.success()
    }

    private fun postNotification(count: Int) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Cleaning reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Lets you know when there are screenshots or large files worth reviewing"
                }
            )
        }

        // API 33+ enforces this at the moment of posting, not just when it
        // was granted — re-check right here rather than trust an earlier check.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // getLaunchIntentForPackage can return null in edge cases (e.g. the
        // package manager not having fully indexed the app yet) — guard
        // rather than let a null Intent crash PendingIntent.getActivity.
        val pendingIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.let { intent ->
                PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Gallery Cleaner")
            .setContentText("You have $count item(s) worth reviewing — screenshots or large files.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (pendingIntent != null) builder.setContentIntent(pendingIntent)

        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Defensive only — shouldn't happen given the permission check
            // above, but a background worker must never crash the app.
        }
    }

    companion object {
        /** KEEP (not REPLACE) so calling this repeatedly — e.g. every time
         *  Settings recomposes — doesn't reset an already-scheduled timer. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleaningReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
