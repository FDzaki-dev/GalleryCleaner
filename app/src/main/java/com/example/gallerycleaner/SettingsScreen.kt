package com.example.gallerycleaner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gallerycleaner.ui.theme.BrassKeep
import com.example.gallerycleaner.ui.theme.DustyRoseDelete
import com.example.gallerycleaner.ui.theme.EspressoBg
import com.example.gallerycleaner.ui.theme.IndigoBg
import com.example.gallerycleaner.ui.theme.OxbloodDelete
import com.example.gallerycleaner.ui.theme.PeriwinkleKeep
import com.example.gallerycleaner.ui.theme.SageKeep
import com.example.gallerycleaner.ui.theme.CoralDelete
import com.example.gallerycleaner.ui.theme.GraphiteBg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by settingsStore.themeModeFlow.collectAsState(initial = ThemeMode.DARK)
    val appTheme by settingsStore.appThemeFlow.collectAsState(initial = AppTheme.SIGNATURE)
    val retentionDays by settingsStore.trashRetentionDaysFlow.collectAsState(
        initial = SettingsStore.DEFAULT_TRASH_RETENTION_DAYS
    )
    val reminderEnabled by settingsStore.cleaningReminderEnabledFlow.collectAsState(initial = false)

    // Only reached on API 33+ when the toggle is turned on and permission
    // isn't already granted. On denial we deliberately do nothing — the
    // Flow-backed `reminderEnabled` stays false since we never persisted
    // true in that branch, so the switch snaps back on its own.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { settingsStore.setCleaningReminderEnabled(true) }
            CleaningReminderWorker.schedule(context)
        }
    }

    fun onReminderToggle(enabled: Boolean) {
        if (enabled) {
            val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            if (needsRuntimePermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scope.launch { settingsStore.setCleaningReminderEnabled(true) }
                CleaningReminderWorker.schedule(context)
            }
        } else {
            scope.launch { settingsStore.setCleaningReminderEnabled(false) }
            CleaningReminderWorker.cancel(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { SettingsSectionLabel("Appearance") }
            item {
                Column(Modifier.fillMaxWidth()) {
                    ThemeMode.values().forEach { mode ->
                        SettingsRadioRow(
                            label = when (mode) {
                                ThemeMode.SYSTEM -> "Match system"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            selected = themeMode == mode,
                            onClick = { scope.launch { settingsStore.setThemeMode(mode) } }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("Color style") }
            item {
                Text(
                    "Applies to both Light and Dark above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, bottom = 8.dp)
                )
            }
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    THEME_STYLES.forEach { style ->
                        ThemeStyleCard(
                            style = style,
                            selected = appTheme == style.appTheme,
                            onClick = { scope.launch { settingsStore.setAppTheme(style.appTheme) } }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("Trash") }
            item {
                Text(
                    "Automatically flag items in Trash for cleanup after:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsStore.RETENTION_OPTIONS.forEach { days ->
                        FilterChip(
                            selected = retentionDays == days,
                            onClick = { scope.launch { settingsStore.setTrashRetentionDays(days) } },
                            label = { Text("$days days") }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("Notifications") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Cleaning reminders", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "A once-a-day nudge if there are screenshots or large files worth reviewing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = ::onReminderToggle)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Describes one selectable color style for the swatch card below. The two
 *  swatch colors shown are each style's actual Keep/Delete accents (dark
 *  variant) — previewing the real palette rather than a generic labeled
 *  list is what makes the picker itself feel considered rather than a
 *  bare settings toggle. */
private class ThemeStyle(
    val appTheme: AppTheme,
    val label: String,
    val description: String,
    val previewBg: Color,
    val swatchKeep: Color,
    val swatchDelete: Color
)

private val THEME_STYLES = listOf(
    ThemeStyle(
        appTheme = AppTheme.SIGNATURE,
        label = "Signature",
        description = "Graphite, sage & coral — the original look.",
        previewBg = GraphiteBg,
        swatchKeep = SageKeep,
        swatchDelete = CoralDelete
    ),
    ThemeStyle(
        appTheme = AppTheme.AMBER_RESERVE,
        label = "Amber Reserve",
        description = "Espresso surfaces, brass & oxblood accents.",
        previewBg = EspressoBg,
        swatchKeep = BrassKeep,
        swatchDelete = OxbloodDelete
    ),
    ThemeStyle(
        appTheme = AppTheme.INDIGO_NOIR,
        label = "Indigo Noir",
        description = "Deep indigo, platinum & dusty rose accents.",
        previewBg = IndigoBg,
        swatchKeep = PeriwinkleKeep,
        swatchDelete = DustyRoseDelete
    )
)

@Composable
private fun ThemeStyleCard(style: ThemeStyle, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small preview swatch: the style's actual background with its two
        // accent dots overlapping on top — a miniature of what the app will
        // actually look like, not just a color name.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(style.previewBg)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .clip(CircleShape)
                    .background(style.swatchKeep)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .background(style.swatchDelete)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(style.label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                style.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
