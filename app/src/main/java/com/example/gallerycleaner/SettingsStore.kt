package com.example.gallerycleaner

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

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

    /** Whether the first-launch onboarding/tutorial has already been shown.
     *  Defaults to false so a fresh install always sees it once. */
    val hasSeenOnboardingFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[HAS_SEEN_ONBOARDING_KEY] = seen }
    }
}
