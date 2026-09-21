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

// Batch145 (Notifications split): pengingat sesi belum kelar punya channel &
// ID sendiri (1001 = bulanan, 1002 = TrashExpiryWorker) supaya user bisa
// matiin/atur masing-masing dari pengaturan sistem.
private const val IN_PROGRESS_CHANNEL_ID = "in_progress_reminders"
private const val IN_PROGRESS_NOTIFICATION_ID = 1003
private const val MONTHLY_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000
private const val IN_PROGRESS_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000

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
 * Batch145: sekarang 2 jenis pengingat independen dalam SATU worker harian
 * (jadwal jalan kalau salah satu toggle nyala; cadence tiap jenis di-throttle
 * lewat timestamp "terakhir terkirim" di SettingsStore) — bulanan (cek
 * screenshot/file gede, max 30 hari sekali) dan sesi belum kelar (grup yang
 * udah mulai digeser tapi belum selesai, max 3 hari sekali).
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
        val monthlyEnabled = settingsStore.cleaningReminderEnabledFlow.first()
        val inProgressEnabled = settingsStore.inProgressReminderEnabledFlow.first()
        // Cek ULANG tiap run (bukan cuma pas dijadwalin) — alasan race-nya
        // sama kayak yang dijelasin di class doc.
        if (!monthlyEnabled && !inProgressEnabled) {
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

        val now = System.currentTimeMillis()
        val monthlyDue = monthlyEnabled &&
            now - settingsStore.lastMonthlyReminderMillisFlow.first() >= MONTHLY_INTERVAL_MS
        val inProgressDue = inProgressEnabled &&
            now - settingsStore.lastInProgressReminderMillisFlow.first() >= IN_PROGRESS_INTERVAL_MS
        // Belum ada yang jatuh tempo -> jangan load seluruh library cuma buat
        // nggak ngapa-ngapain (worker ini jalan tiap hari).
        if (!monthlyDue && !inProgressDue) {
            return Result.success()
        }

        val media = MediaRepository.loadAllMedia(applicationContext)

        if (monthlyDue) {
            val cleanupCount = MediaRepository.smartCategories(media).sumOf { it.items.size }
            if (cleanupCount >= MIN_ITEMS_TO_NOTIFY) {
                val posted = postNotification(
                    channelId = CHANNEL_ID,
                    channelName = "Pengingat bulanan",
                    channelDescription = "Sebulan sekali ngasih tahu kalau ada screenshot atau file gede yang layak dicek",
                    notificationId = NOTIFICATION_ID,
                    text = "Ada $cleanupCount item yang layak dicek — screenshot atau file gede."
                )
                if (posted) settingsStore.setLastMonthlyReminderMillis(now)
            }
        }

        // Mode bersih acak nyala -> index progress cuma bermakna di dalam 1
        // sesi shuffle (lihat MainActivity), jadi "belum kelar" nggak bisa
        // dihitung dengan jujur — dilewati, bukan ditebak.
        if (inProgressDue && !settingsStore.randomModeEnabledFlow.first()) {
            val progressStore = ProgressStore(applicationContext)
            var bestName: String? = null
            var bestDone = 0
            var bestTotal = 0
            for (group in MediaRepository.group(media, settingsStore.groupModeFlow.first(), SortOption.DATE)) {
                val done = progressStore.progressFlow(group.key).first()
                val total = group.items.size
                // 0 < done < total = sudah mulai tapi belum selesai. Pilih grup
                // yang paling mendekati selesai (paling "sayang" ditinggal).
                if (done in 1 until total &&
                    (bestName == null || done.toFloat() / total > bestDone.toFloat() / bestTotal)
                ) {
                    bestName = displayGroupName(group.key)
                    bestDone = done
                    bestTotal = total
                }
            }
            if (bestName != null) {
                val posted = postNotification(
                    channelId = IN_PROGRESS_CHANNEL_ID,
                    channelName = "Pengingat sesi belum kelar",
                    channelDescription = "Ngingetin kalau ada folder yang udah mulai kamu geser tapi belum kelar",
                    notificationId = IN_PROGRESS_NOTIFICATION_ID,
                    text = "Lanjutin bersih-bersih $bestName yuk — baru $bestDone dari $bestTotal yang kamu cek."
                )
                if (posted) settingsStore.setLastInProgressReminderMillis(now)
            }
        }
        return Result.success()
    }

    /** @return true cuma kalau notifikasinya benar-benar di-post — dipakai
     *  pemanggil buat nentuin apakah timestamp "terakhir dikirim" boleh
     *  ditulis. */
    private fun postNotification(
        channelId: String,
        channelName: String,
        channelDescription: String,
        notificationId: Int,
        text: String
    ): Boolean {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = channelDescription
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
            return false
        }

        // getLaunchIntentForPackage can return null in edge cases (e.g. the
        // package manager not having fully indexed the app yet) — guard
        // rather than let a null Intent crash PendingIntent.getActivity.
        val pendingIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.let { intent ->
                PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Snaply")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (pendingIntent != null) builder.setContentIntent(pendingIntent)

        return try {
            manager.notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            // Defensive only — shouldn't happen given the permission check
            // above, but a background worker must never crash the app.
            false
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
