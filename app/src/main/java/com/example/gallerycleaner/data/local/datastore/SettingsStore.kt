package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "gallery_cleaner_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** A curated color style, independent of light/dark — ThemeMode decides
 *  brightness, AppTheme decides *character*. Kept as a separate axis
 *  instead of folding into ThemeMode so the two can be combined freely
 *  (e.g. Amber Reserve + Light) without a combinatorial enum explosion. */
enum class AppTheme { SIGNATURE, AMBER_RESERVE, INDIGO_NOIR }

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val APP_THEME_KEY = stringPreferencesKey("app_theme")
private val TRASH_RETENTION_DAYS_KEY = intPreferencesKey("trash_retention_days")
private val CLEANING_REMINDER_ENABLED_KEY = booleanPreferencesKey("cleaning_reminder_enabled")
private val HAPTIC_FEEDBACK_ENABLED_KEY = booleanPreferencesKey("haptic_feedback_enabled")
private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
private val RANDOM_MODE_ENABLED_KEY = booleanPreferencesKey("random_mode_enabled")
private val CLEANUP_GOAL_BYTES_KEY = longPreferencesKey("cleanup_goal_bytes")
private val BACKUP_BEFORE_DELETE_ENABLED_KEY = booleanPreferencesKey("backup_before_delete_enabled")

/** Default cleanup goal (ROADMAP Fase A item 3): 2 GB. Arbitrary but
 *  reasonable starting target — big enough to feel worth working toward,
 *  small enough that a first cleaning session can make a visible dent
 *  rather than the progress bar looking permanently near-empty. */
const val DEFAULT_CLEANUP_GOAL_BYTES: Long = 2_000_000_000L

/** Everything the user can configure about how the app behaves, kept in one
 *  place the way a Settings screen in any polished app would. */
class SettingsStore(private val context: Context) {

    companion object {
        const val DEFAULT_TRASH_RETENTION_DAYS = 30
        val RETENTION_OPTIONS = listOf(7, 14, 30, 60, 90)
    }

    val themeModeFlow: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY]?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()
        } ?: ThemeMode.DARK // matches the app's original always-dark behavior for existing installs
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }

    /** Defaults to SIGNATURE — the app's original sage/coral look — so
     *  nothing changes visually for anyone until they deliberately opt into
     *  one of the other styles in Settings. */
    val appThemeFlow: Flow<AppTheme> = context.settingsDataStore.data.map { prefs ->
        prefs[APP_THEME_KEY]?.let { raw ->
            runCatching { AppTheme.valueOf(raw) }.getOrNull()
        } ?: AppTheme.SIGNATURE
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.settingsDataStore.edit { prefs -> prefs[APP_THEME_KEY] = theme.name }
    }

    /** How many days an item sits in Trash before it's flagged for
     *  auto-cleanup — user-adjustable instead of a fixed 30 days. */
    val trashRetentionDaysFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[TRASH_RETENTION_DAYS_KEY] ?: DEFAULT_TRASH_RETENTION_DAYS
    }

    suspend fun setTrashRetentionDays(days: Int) {
        context.settingsDataStore.edit { prefs -> prefs[TRASH_RETENTION_DAYS_KEY] = days }
    }

    /** Whether the periodic "you have items to clean up" notification is on.
     *  Defaults to false until the user explicitly opts in — notifications
     *  the user didn't ask for are exactly the kind of thing that makes a
     *  cleaner app feel like it's nagging rather than helping. */
    val cleaningReminderEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[CLEANING_REMINDER_ENABLED_KEY] ?: false
    }

    suspend fun setCleaningReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[CLEANING_REMINDER_ENABLED_KEY] = enabled }
    }

    /** Whether swipe decisions (Keep/Delete) give a short haptic tick.
     *  Defaults to true — unlike the cleaning reminder notification (which
     *  is genuinely intrusive if unwanted), this is a subtle per-gesture
     *  touch most people expect from a swipe-card interaction and would
     *  likely never discover if it defaulted off. */
    val hapticFeedbackEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[HAPTIC_FEEDBACK_ENABLED_KEY] ?: true
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[HAPTIC_FEEDBACK_ENABLED_KEY] = enabled }
    }

    /** Whether the app requires the device's screen lock (PIN/pattern/
     *  biometric — whatever the person already has set up) before showing
     *  any content. Defaults to false — this gates access to someone's
     *  photos, a much bigger behavior change than a swipe haptic, so it
     *  should be an opt-in the person deliberately turns on, never a
     *  surprise. The caller (SettingsScreen) is responsible for checking
     *  KeyguardManager.isDeviceSecure before allowing this to be turned on
     *  at all — enabling it on a device with no screen lock configured
     *  would have no valid credential to authenticate against, locking the
     *  person out of their own gallery with no way back in. */
    val appLockEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[APP_LOCK_ENABLED_KEY] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[APP_LOCK_ENABLED_KEY] = enabled }
    }

    /** Whether the first-launch onboarding/tutorial has already been shown.
     *  Defaults to false so a fresh install always sees it once. */
    val hasSeenOnboardingFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[HAS_SEEN_ONBOARDING_KEY] = seen }
    }

    /** Whether entering a folder for review shuffles its photos into random
     *  order instead of the default (date/name/size, per SortOption). A
     *  quick way to sample across a large folder rather than always seeing
     *  the same items first — matches the "random clean mode" competitors
     *  in this category offer. Reshuffled fresh each time a folder is
     *  opened (not persisted per-folder), so ProgressStore's saved index
     *  for that group.key is only meaningful within one shuffled session;
     *  that's an accepted tradeoff of random mode, not a bug. Defaults to
     *  false — off until the user opts in, same as other behavior-changing
     *  toggles in this store. */
    val randomModeEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[RANDOM_MODE_ENABLED_KEY] ?: false
    }

    suspend fun setRandomModeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[RANDOM_MODE_ENABLED_KEY] = enabled }
    }

    /** Cleanup goal (ROADMAP Fase A item 3): a target number of bytes to
     *  free, tracked against `StatsStore.totalFreedBytesFlow` (all-time
     *  cumulative, not reset per period — a goal that silently reset would
     *  be confusing since nothing else about "all time" stats resets
     *  either). Kept in `SettingsStore` rather than `StatsStore` since it's
     *  a user-set preference, not a derived/recorded stat — same
     *  separation of concerns the rest of this store already follows. */
    val cleanupGoalBytesFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[CLEANUP_GOAL_BYTES_KEY] ?: DEFAULT_CLEANUP_GOAL_BYTES
    }

    suspend fun setCleanupGoalBytes(bytes: Long) {
        context.settingsDataStore.edit { prefs -> prefs[CLEANUP_GOAL_BYTES_KEY] = bytes.coerceAtLeast(1L) }
    }

    /** ROADMAP Fase B item 7 — copy each item to a local
     *  `Pictures|Movies/GalleryCleaner/Backup/` folder (see `BackupHelper`)
     *  right before it's permanently deleted. Defaults to false: this
     *  trades disk space for a safety net, and unlike Trash's own
     *  short-lived retention window, a backup copy sticks around
     *  indefinitely until the person clears it themselves — real
     *  storage-usage behavior that should be something the person opts
     *  into, never a silent default. */
    val backupBeforeDeleteEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[BACKUP_BEFORE_DELETE_ENABLED_KEY] ?: false
    }

    suspend fun setBackupBeforeDeleteEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[BACKUP_BEFORE_DELETE_ENABLED_KEY] = enabled }
    }
}
