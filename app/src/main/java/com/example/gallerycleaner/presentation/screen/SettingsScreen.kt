package com.example.gallerycleaner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.gallerycleaner.ui.theme.DustyRoseDelete
import com.example.gallerycleaner.ui.theme.IndigoBg
import com.example.gallerycleaner.ui.theme.Neumorph
import com.example.gallerycleaner.ui.theme.OxbloodDelete
import com.example.gallerycleaner.ui.theme.PeriwinkleKeep
import com.example.gallerycleaner.ui.theme.SageKeep
import com.example.gallerycleaner.ui.theme.CoralDelete
import com.example.gallerycleaner.ui.theme.MidnightGlass
import kotlinx.coroutines.launch
import java.io.File

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
    val hapticsEnabled by settingsStore.hapticFeedbackEnabledFlow.collectAsState(initial = true)
    val randomModeEnabled by settingsStore.randomModeEnabledFlow.collectAsState(initial = false)
    val appLockEnabled by settingsStore.appLockEnabledFlow.collectAsState(initial = false)
    val backupBeforeDeleteEnabled by settingsStore.backupBeforeDeleteEnabledFlow.collectAsState(initial = false)
    val isDeviceSecure = remember {
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isDeviceSecure == true
    }

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

    // Batch50: in-app update (UpdateChecker/ApkDownloader are Batch49).
    // One state machine drives both the Settings row subtitle and the
    // dialog below — Idle/UpToDate/Error are dismissible-by-tap-again,
    // Available/Downloading/ReadyToInstall keep the dialog open.
    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    // Batch51 — installed version, read once via PackageManager (not
    // BuildConfig: buildConfig feature isn't enabled in app/build.gradle.kts,
    // and this avoids touching that protected file for this task).
    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: PackageManager.NameNotFoundException) {
            "?"
        }
    }

    // Returned from the "Allow from this source" system settings screen
    // (only reached on API26+ when the permission isn't granted yet) — if
    // the person granted it, retry the install immediately instead of
    // making them tap "Install" a second time.
    val installSourcePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val current = updateState
        if (current is UpdateUiState.ReadyToInstall &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls())
        ) {
            installDownloadedApk(context, current.file)
        }
    }

    fun onCheckForUpdate() {
        updateState = UpdateUiState.Checking
        scope.launch {
            updateState = when (val result = UpdateChecker.checkForUpdate(context)) {
                is UpdateChecker.CheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateChecker.CheckResult.UpdateAvailable -> UpdateUiState.Available(result.info)
                is UpdateChecker.CheckResult.Error -> UpdateUiState.Error(result.message)
            }
        }
    }

    fun onDownloadUpdate(info: UpdateChecker.UpdateInfo) {
        scope.launch {
            updateState = UpdateUiState.Downloading(info, 0f)
            val result = ApkDownloader.download(
                context = context,
                url = info.apkDownloadUrl,
                fileName = info.apkFileName,
                expectedSizeBytes = info.apkSizeBytes
            ) { bytesRead, totalBytes ->
                // Written from Dispatchers.IO inside ApkDownloader — safe:
                // Compose's snapshot state system is thread-safe for
                // writes and schedules the recomposition on the main
                // thread itself, no manual withContext(Main) needed here.
                updateState = UpdateUiState.Downloading(
                    info,
                    if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                )
            }
            updateState = when (result) {
                is ApkDownloader.DownloadResult.Success -> {
                    // Mark known now (on successful download, not on
                    // confirmed install) — see UpdateChecker's class doc
                    // for why. Known limitation: if the person downloads
                    // but backs out without installing, leaving Settings
                    // resets this screen's local state, and the next
                    // "Check for update" will say up-to-date even though
                    // the already-downloaded APK was never installed. The
                    // file itself stays on disk either way; not fixed here
                    // to keep this batch to 3 files.
                    UpdateChecker.markTagAsKnown(context, info.tagName)
                    UpdateUiState.ReadyToInstall(result.file, info.tagName)
                }
                is ApkDownloader.DownloadResult.Error -> UpdateUiState.Error(result.message)
            }
        }
    }

    fun onInstallUpdate(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            installSourcePermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
        } else {
            installDownloadedApk(context, file)
        }
    }

    Scaffold(
        // Transparent (Batch22) — see matching comment in HomeScreen.kt.
        // contentColor (Batch24 fix): M3 Scaffold derives its default
        // contentColor from containerColor via contentColorFor(); a
        // transparent container isn't a themed color so that lookup
        // returns Unspecified, which Text() resolves to hard-default
        // black instead of the theme's text color. Every Text()/label in
        // this screen without its own explicit color= (radio row labels,
        // color-style card titles, toggle titles) was rendering black on
        // the dark glass background. Set explicitly to onBackground.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
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
                // Batch26 — rearsitektur: 3-way RadioButton (Match system /
                // Light / Dark, one-directional single-select) diganti 2
                // toggle Switch "disama ratakan" ke pola Row+Switch yang
                // sudah dipakai konsisten di semua section lain di layar
                // ini (Backup/Notifications/Swiping/Feedback/Privacy di
                // bawah) — sebelumnya Appearance adalah satu-satunya
                // section berbentuk radio-list, bukan toggle, jadi terasa
                // beda sendiri di tengah layar yang isinya toggle semua.
                //
                // "Match system" ON  -> ThemeMode.SYSTEM, brightness ikut
                //   `isSystemInDarkTheme()` live (MainActivity sudah baca
                //   ini persis sama seperti sebelumnya, lihat komentar di
                //   Theme.kt). Toggle "Dark mode" di bawahnya jadi
                //   read-only (enabled=false) tapi tetap mencerminkan
                //   status sistem saat ini secara real-time — bukan
                //   disembunyikan — supaya user tetap lihat "oh sekarang
                //   lagi dark karena sistem", bukan tiba-tiba blank.
                // "Match system" OFF -> resolve ke ThemeMode konkret
                //   (DARK/LIGHT) berdasarkan status sistem SAAT toggle
                //   dimatikan, supaya tidak ada lompatan visual mendadak
                //   di momen switch-off; setelah itu user bebas atur
                //   manual lewat toggle "Dark mode" di bawahnya.
                val systemDark = isSystemInDarkTheme()
                val matchSystem = themeMode == ThemeMode.SYSTEM
                val resolvedDark = if (matchSystem) systemDark else themeMode == ThemeMode.DARK

                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Match system", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Follow this device's light/dark setting automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = matchSystem,
                            onCheckedChange = { on ->
                                scope.launch {
                                    settingsStore.setThemeMode(
                                        if (on) ThemeMode.SYSTEM
                                        else if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
                                    )
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (matchSystem) "Currently following the system setting."
                                else "Off uses light appearance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = resolvedDark,
                            enabled = !matchSystem,
                            onCheckedChange = { on ->
                                scope.launch {
                                    settingsStore.setThemeMode(if (on) ThemeMode.DARK else ThemeMode.LIGHT)
                                }
                            }
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
                    modifier = Modifier.padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp)
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
            item { SettingsSectionLabel("Backup") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Backup before delete", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Copy each photo/video to Pictures or Movies > GalleryCleaner > Backup before it's permanently deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = backupBeforeDeleteEnabled,
                        onCheckedChange = { scope.launch { settingsStore.setBackupBeforeDeleteEnabled(it) } }
                    )
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

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("Swiping") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Random clean mode", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Shuffle a folder's photos into random order each time you open it, instead of date order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = randomModeEnabled,
                        onCheckedChange = { scope.launch { settingsStore.setRandomModeEnabled(it) } }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("Feedback") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Swipe haptics", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "A short vibration when you keep or delete a photo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { scope.launch { settingsStore.setHapticFeedbackEnabled(it) } }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("Privacy") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("App lock", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (isDeviceSecure) {
                                "Require your screen lock (PIN, pattern, or biometric) to open the app."
                            } else {
                                "Set a screen lock on this device first (Settings > Security)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appLockEnabled,
                        enabled = isDeviceSecure,
                        onCheckedChange = { scope.launch { settingsStore.setAppLockEnabled(it) } }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionLabel("About") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable(
                            enabled = updateState is UpdateUiState.Idle ||
                                updateState is UpdateUiState.UpToDate ||
                                updateState is UpdateUiState.Error
                        ) { onCheckForUpdate() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Check for update", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        val subtitle = when (val current = updateState) {
                            UpdateUiState.Idle -> "Tap to check GitHub for a newer release."
                            UpdateUiState.Checking -> "Checking…"
                            UpdateUiState.UpToDate -> "You're on the latest version."
                            is UpdateUiState.Available -> "Version ${current.info.tagName} is available."
                            is UpdateUiState.Downloading -> "Downloading… ${(current.progress * 100).toInt()}%"
                            is UpdateUiState.ReadyToInstall -> "Downloaded — tap to install."
                            is UpdateUiState.Error -> "Couldn't check for updates: ${current.message}"
                        }
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (updateState is UpdateUiState.Error) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    when (updateState) {
                        UpdateUiState.Checking -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        is UpdateUiState.Downloading -> CircularProgressIndicator(
                            progress = (updateState as UpdateUiState.Downloading).progress,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        else -> {}
                    }
                }
            }
        }
    }

    // Dialog for Available/Downloading/ReadyToInstall — Idle/UpToDate/Error
    // stay inline in the row above, no dialog needed for those.
    val dialogState = updateState
    if (dialogState is UpdateUiState.Available ||
        dialogState is UpdateUiState.Downloading ||
        dialogState is UpdateUiState.ReadyToInstall
    ) {
        val isDownloading = dialogState is UpdateUiState.Downloading
        val newTagName = when (dialogState) {
            is UpdateUiState.Available -> dialogState.info.tagName
            is UpdateUiState.Downloading -> dialogState.info.tagName
            is UpdateUiState.ReadyToInstall -> dialogState.tagName
            else -> ""
        }
        val releaseName = when (dialogState) {
            is UpdateUiState.Available -> dialogState.info.releaseName
            is UpdateUiState.Downloading -> dialogState.info.releaseName
            is UpdateUiState.ReadyToInstall -> dialogState.tagName
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { if (!isDownloading) updateState = UpdateUiState.Idle },
            properties = DialogProperties(
                dismissOnBackPress = !isDownloading,
                dismissOnClickOutside = !isDownloading
            ),
            title = { Text(releaseName) },
            text = {
                Column {
                    // Batch51 — installed vs incoming version, shown across
                    // all three dialog states (Available/Downloading/
                    // ReadyToInstall) so it stays visible through the flow.
                    Text(
                        "Installed $currentVersionName → New $newTagName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    when (dialogState) {
                        is UpdateUiState.Available -> {
                            Text(
                                dialogState.info.shortSummary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is UpdateUiState.Downloading -> {
                            LinearProgressIndicator(
                                progress = dialogState.progress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${(dialogState.progress * 100).toInt()}% — keep this open until it finishes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is UpdateUiState.ReadyToInstall -> {
                            Text(
                                "Downloaded. Android will ask you to confirm the install next.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                when (dialogState) {
                    is UpdateUiState.Available -> TextButton(
                        onClick = { onDownloadUpdate(dialogState.info) }
                    ) { Text("Download") }
                    is UpdateUiState.ReadyToInstall -> TextButton(
                        onClick = { onInstallUpdate(dialogState.file) }
                    ) { Text("Install") }
                    else -> {}
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { updateState = UpdateUiState.Idle }) { Text("Cancel") }
                }
            }
        )
    }
}

/** Batch50 — one state machine drives the "Check for update" row and its
 *  dialog together (see call-site in [SettingsScreen]). */
private sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class Available(val info: UpdateChecker.UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateChecker.UpdateInfo, val progress: Float) : UpdateUiState()
    data class ReadyToInstall(val file: File, val tagName: String) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

/** Batch50 — hands the downloaded APK (app-specific external storage, see
 *  ApkDownloader.kt) to the system installer via a FileProvider content://
 *  URI, since a plain file:// Intent data throws FileUriExposedException
 *  on API24+ and installers can't read an app-private path directly. */
private fun installDownloadedApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
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
        description = "Midnight-blue glassmorphism — frosted panels over a deep navy glow.",
        previewBg = MidnightGlass.NavyCore,
        swatchKeep = SageKeep,
        swatchDelete = CoralDelete
    ),
    ThemeStyle(
        appTheme = AppTheme.AMBER_RESERVE,
        label = "Amber Reserve",
        // Batch36 redesign: was "Espresso skeuomorphism-lite — raised
        // brass-bevel panels, not glass." (Batch27/28) — now pure Soft UI.
        description = "Deep navy neumorphism — soft dual-shadow panels, no border, no glass.",
        previewBg = Neumorph.DeepNavy,
        swatchKeep = Neumorph.ClassicBrass,
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
    // Theme-agnostic by design: this card is used to pick between Signature
    // (Midnight-Blue Glassmorphism), Amber Reserve, and Indigo Noir, so it
    // stays plain M3 (colorScheme-driven) rather than any one theme's own
    // components — using GlassCard here would look wrong once Amber/Indigo
    // is selected. Manual clip+background+border Row, same technique kept
    // since before Batch10's short-lived Tactile/Glass migration.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
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
