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
 *  (e.g. Amber Reserve + Light) without a combinatorial enum explosion.
 *  Batch119: SAGE_WASH added — 4th color style, Material 3 foundation
 *  with a calm moss/terracotta palette and a painted/watercolor material
 *  language (see MaterialStyle.PAINTED, PaintedTokens.kt). */
enum class AppTheme { SIGNATURE, AMBER_RESERVE, INDIGO_NOIR, SAGE_WASH }

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val APP_THEME_KEY = stringPreferencesKey("app_theme")
private val TRASH_RETENTION_DAYS_KEY = intPreferencesKey("trash_retention_days")
private val CLEANING_REMINDER_ENABLED_KEY = booleanPreferencesKey("cleaning_reminder_enabled")
private val HAPTIC_FEEDBACK_ENABLED_KEY = booleanPreferencesKey("haptic_feedback_enabled")
private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
private val RANDOM_MODE_ENABLED_KEY = booleanPreferencesKey("random_mode_enabled")
private val RANDOM_COUNT_KEY = intPreferencesKey("random_count")
private val CLEANUP_GOAL_BYTES_KEY = longPreferencesKey("cleanup_goal_bytes")
private val BACKUP_BEFORE_DELETE_ENABLED_KEY = booleanPreferencesKey("backup_before_delete_enabled")
private val GROUP_MODE_KEY = stringPreferencesKey("group_mode")
private val SORT_OPTION_KEY = stringPreferencesKey("sort_option")
private val SORT_ASCENDING_KEY = booleanPreferencesKey("sort_ascending")
private val VIDEO_SOUND_ENABLED_KEY = booleanPreferencesKey("video_sound_enabled")
private val SHARE_TEXT_ENABLED_KEY = booleanPreferencesKey("share_text_enabled")
private val ANIMATE_BUTTONS_ENABLED_KEY = booleanPreferencesKey("animate_buttons_enabled")

/** Default cleanup goal (ROADMAP Fase A item 3): 2 GB. Arbitrary but
 *  reasonable starting target — big enough to feel worth working toward,
 *  small enough that a first cleaning session can make a visible dent
 *  rather than the progress bar looking permanently near-empty. */
const val DEFAULT_CLEANUP_GOAL_BYTES: Long = 2_000_000_000L

/** ROADMAP Fase E, "Cleaning Options" item "Random count" (Sponge parity —
 *  a custom number instead of only on/off). How many items a shuffled
 *  random-mode session (`randomModeEnabledFlow`) draws from a group,
 *  applied as `.take(randomCount)` on top of the existing `.shuffled()`
 *  call sites — group sizes smaller than this are unaffected, `take()`
 *  on a list is already safe when the count exceeds its size. Bounds
 *  chosen for a simple +/- stepper (step 5): low enough to stay a quick
 *  sample, high enough that it's still a meaningful cleaning session. */
const val DEFAULT_RANDOM_COUNT: Int = 20
const val MIN_RANDOM_COUNT: Int = 5
const val MAX_RANDOM_COUNT: Int = 100
const val RANDOM_COUNT_STEP: Int = 5

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

    /** Companion setting to [randomModeEnabledFlow] — see DEFAULT_RANDOM_COUNT's
     *  doc comment above for what this bounds and why. Only meaningful while
     *  random mode itself is on; left persisted (not reset) while off, same
     *  as every other preference in this store that has an "off" state. */
    val randomCountFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[RANDOM_COUNT_KEY] ?: DEFAULT_RANDOM_COUNT
    }

    suspend fun setRandomCount(count: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[RANDOM_COUNT_KEY] = count.coerceIn(MIN_RANDOM_COUNT, MAX_RANDOM_COUNT)
        }
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

    /** Batch29 polish: Home's "GROUP BY"/"SORT BY" filter row (`FilterRow`
     *  in HomeScreenSections.kt) was previously backed by plain
     *  `remember { mutableStateOf(...) }` in MainActivity — a real app
     *  would expect this choice to survive a relaunch, same as every other
     *  preference on this screen. Defaults match the previous in-memory
     *  defaults exactly (MONTH/DATE) so existing behavior is unchanged
     *  until someone actually picks something else. */
    val groupModeFlow: Flow<GroupMode> = context.settingsDataStore.data.map { prefs ->
        prefs[GROUP_MODE_KEY]?.let { raw ->
            runCatching { GroupMode.valueOf(raw) }.getOrNull()
        } ?: GroupMode.MONTH
    }

    suspend fun setGroupMode(mode: GroupMode) {
        context.settingsDataStore.edit { prefs -> prefs[GROUP_MODE_KEY] = mode.name }
    }

    val sortOptionFlow: Flow<SortOption> = context.settingsDataStore.data.map { prefs ->
        prefs[SORT_OPTION_KEY]?.let { raw ->
            runCatching { SortOption.valueOf(raw) }.getOrNull()
        } ?: SortOption.DATE
    }

    suspend fun setSortOption(option: SortOption) {
        context.settingsDataStore.edit { prefs -> prefs[SORT_OPTION_KEY] = option.name }
    }

    /** ROADMAP Fase E, "Cleaning Options" item "Default sort + arah" — the
     *  *field* (DATE/SIZE/NAME) was already persisted above via
     *  [sortOptionFlow] since Batch20; this is the missing *direction*
     *  half (`MediaRepository.sortItems` used to hardcode
     *  `sortedByDescending` for DATE/SIZE and ascending for NAME with no
     *  way to flip it). Named "ascending" for the setting's own identity,
     *  but consumed by `sortItems` as "reverse whichever order is natural
     *  for the current field" (Newest->Oldest, Largest->Smallest,
     *  A-Z->Z-A) rather than a literal ascending/descending comparator
     *  swap — that keeps its meaning correct no matter which SortOption is
     *  active, instead of requiring a per-field special case here.
     *  Defaults to false so existing sort behavior is 100% unchanged for
     *  every user until they explicitly flip it. */
    val sortAscendingFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SORT_ASCENDING_KEY] ?: false
    }

    suspend fun setSortAscending(ascending: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[SORT_ASCENDING_KEY] = ascending }
    }

    /** [Batch129, default corrected Batch132] Whether videos play with
     *  sound during swipe/grid review. First item off the "Cleaning
     *  Options" backlog (ROADMAP — gap found against reference app in the
     *  same category, which groups this exact toggle plus ~8 siblings
     *  under one Settings section; the rest are intentionally NOT bundled
     *  into this same change, see ROADMAP for the full list and per-item
     *  risk notes).
     *
     *  [Batch132] Defaults to **true** — Batch129 originally defaulted
     *  this to false on "unexpected sound is unwelcome" reasoning, but
     *  that reasoning only holds for a setting that's ADDING a new
     *  capability. This one isn't: before Batch129, VideoPlayerSurface set
     *  no `volume` at all, so ExoPlayer's own default (1f, full volume)
     *  applied and every video played with sound, same as opening it in
     *  any other player. Defaulting the new toggle to false silently
     *  muted every video that used to have sound — a genuine regression
     *  (user-reported, "regresi pada output Audio"), not a neutral opt-in
     *  choice, because the toggle's mere EXISTENCE changed prior working
     *  behavior for everyone who never touched Settings. True restores
     *  the pre-Batch129 behavior exactly; the toggle still exists for
     *  anyone who wants to mute. Consumed directly inside
     *  VideoPlayerSurface (SwipeScreenCard.kt) — see that file's own
     *  comment for why it constructs this store locally instead of
     *  threading a parameter through SwipeScreen.kt/SwipeScreenGrid.kt. */
    val videoSoundEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[VIDEO_SOUND_ENABLED_KEY] ?: true
    }

    suspend fun setVideoSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[VIDEO_SOUND_ENABLED_KEY] = enabled }
    }

    /** [Batch131] Whether the share sheet (SwipeScreen.kt's toolbar Share
     *  action) attaches a text extra alongside the media file — 2nd item
     *  off the Fase E "Cleaning Options" backlog (ROADMAP.md #15), same
     *  shape/risk class as Batch129's videoSoundEnabledFlow above (self-
     *  contained toggle, 1 consumer file, no core swipe-logic changes).
     *  Defaults to false: the "text" is the file's own display name
     *  (SwipeScreen.kt decides the exact string, not this store) — for a
     *  renamed/personal file that can leak more than the recipient needs,
     *  so opt-in rather than opt-out, same off-by-default reasoning as
     *  videoSoundEnabledFlow/cleaningReminderEnabledFlow. */
    val shareTextEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SHARE_TEXT_ENABLED_KEY] ?: false
    }

    suspend fun setShareTextEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[SHARE_TEXT_ENABLED_KEY] = enabled }
    }

    /** [Batch138] Whether buttons get a subtle press-scale shrink layered
     *  on top of each MaterialStyle's own existing press feedback — 4th
     *  item off the Fase E "Cleaning Options" backlog (ROADMAP.md #14,
     *  "Animate on buttons"). Defaults to true, same reasoning as
     *  hapticFeedbackEnabledFlow above: a subtle per-tap visual cue most
     *  people expect from a modern app and would likely never think to
     *  turn on if it defaulted off (unlike shareTextEnabledFlow's opt-in
     *  case, there's no privacy/behavior-change reason to default this
     *  off). Consumed directly inside GlassButton.kt, which constructs
     *  this store locally the same way VideoPlayerSurface
     *  (SwipeScreenCard.kt) reads videoSoundEnabledFlow — GlassButton has
     *  6 call sites and no existing settings parameter threaded through
     *  any of them, so a local read avoids touching a single consumer.
     *  Turning this off does NOT touch each style's own pressed-state
     *  feedback (glow/shadow/dim swap, see GlassButton.kt's doc comment)
     *  — only the added scale-shrink stacked on top of it. */
    val animateButtonsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[ANIMATE_BUTTONS_ENABLED_KEY] ?: true
    }

    suspend fun setAnimateButtonsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[ANIMATE_BUTTONS_ENABLED_KEY] = enabled }
    }
}
