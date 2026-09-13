package com.example.gallerycleaner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

private const val CHANNEL_ID = "trash_expiry"
private const val NOTIFICATION_ID = 1002 // 1001 is CleaningReminderWorker's — must stay distinct

// Batch38 — Audit Gap P0 #3 ("Expiry Trash tidak otomatis mengeksekusi
// deletion"). Duplicated from MainActivity's private ACTION_VIEW_TRASH
// (can't import a `private const val` across files) — same string already
// exists in a 2nd place too (`res/xml/shortcuts.xml`), so this is a 3rd,
// consistent with that existing (imperfect, pre-Batch38) pattern rather
// than a refactor to centralize it, which is out of scope for this fix.
private const val ACTION_VIEW_TRASH = "com.example.gallerycleaner.ACTION_VIEW_TRASH"

/**
 * Periodic, ALWAYS-scheduled (not opt-in, unlike [CleaningReminderWorker])
 * check for trash items that have crossed the retention threshold.
 *
 * **Why this exists — Audit Gap P0 #3**: before this batch, [TrashStore]
 * already computed [TrashStore.expiredItemIdsFlow] correctly, but nothing
 * ever READ that flow unless the user happened to have the app open on
 * `TrashScreen` at the time. If they didn't open the app, expired items
 * just sat there indefinitely with zero surfacing — the audit's exact
 * complaint.
 *
 * **Why this can't be "true silent auto-delete" instead** — this is a
 * platform constraint, not a design choice this batch is working around:
 * [TrashStore]'s own class doc already states it plainly — *"Android's
 * scoped storage requires an interactive system confirmation for every
 * permanent delete (there's no silent background-delete API)"*.
 * `MediaStore.createDeleteRequest()` (used by `performPermanentDeletion`
 * in `MainActivity.kt`) MUST be launched from a foreground `Activity` via
 * `IntentSenderRequest` and MUST show the user a system confirmation
 * dialog on API 30+ — a headless `CoroutineWorker` structurally cannot
 * call it. So the correct, platform-respecting fix for "user never gets
 * prompted" isn't forcing a silent delete (impossible + would defeat the
 * OS's own anti-data-loss protection anyway) — it's making sure the user
 * DOES get prompted even when they haven't opened the app, via a
 * notification. That's what this Worker adds.
 *
 * **Always scheduled, not opt-in**: unlike [CleaningReminderWorker] (a
 * "nice to have" suggestion the user must turn on), trash retention is a
 * core, already-active setting the moment the user swipes their first
 * delete (see `SettingsScreen`'s retention-days picker, always visible) —
 * so surfacing its own expiry isn't an extra feature to opt into, it's
 * completing a promise the Settings screen already makes implicitly.
 * Scheduled once from `MainActivity.onCreate` with `KEEP` policy (see
 * [schedule], same idempotency guarantee [CleaningReminderWorker.schedule]
 * already relies on).
 */
class TrashExpiryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val trashStore = TrashStore(applicationContext)

        val retentionDays = settingsStore.trashRetentionDaysFlow.first()
        val expiredIds = trashStore.expiredItemIdsFlow(retentionDays).first()

        if (expiredIds.isNotEmpty()) {
            postNotification(expiredIds.size)
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
                    "Trash ready to empty",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Lets you know when items in Trash have passed your retention setting and are ready to permanently delete"
                }
            )
        }

        // API 33+ enforces this at the moment of posting, not just when it
        // was granted — same re-check CleaningReminderWorker does, for the
        // same reason (a grant can be revoked between schedule and run).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Deep-links straight to TrashScreen (reuses the existing App
        // Shortcuts routing in MainActivity/AppRoot — no new navigation
        // mechanism introduced) instead of just the bare launch intent
        // CleaningReminderWorker uses, since "review Trash" is a specific
        // actionable destination, not just "open the app".
        val trashIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = ACTION_VIEW_TRASH
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, trashIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val itemWord = if (count == 1) "item" else "items"
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("Trash ready to empty")
            .setContentText("$count $itemWord past your retention setting — tap to review and permanently delete.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Defensive only, mirrors CleaningReminderWorker — a background
            // worker must never crash the app over a notification failing.
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "trash_expiry_check"

        /** KEEP (not REPLACE), same reasoning as
         *  [CleaningReminderWorker.schedule] — calling this on every
         *  `MainActivity.onCreate` (cold start, rotation-survived process)
         *  must not reset an already-ticking daily timer. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashExpiryWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
