package com.example.gallerycleaner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
