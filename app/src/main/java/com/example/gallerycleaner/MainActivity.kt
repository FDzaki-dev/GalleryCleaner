package com.example.gallerycleaner

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gallerycleaner.ui.theme.GalleryCleanerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter

// Matches the <intent android:action="..."> values declared in
// res/xml/shortcuts.xml — these are what tell MainActivity which screen a
// long-press launcher shortcut was meant to open.
private const val ACTION_VIEW_TRASH = "com.example.gallerycleaner.ACTION_VIEW_TRASH"
private const val ACTION_OPEN_SETTINGS = "com.example.gallerycleaner.ACTION_OPEN_SETTINGS"

class MainActivity : ComponentActivity() {

    private lateinit var progressStore: ProgressStore
    private lateinit var trashStore: TrashStore
    private lateinit var statsStore: StatsStore
    private lateinit var folderLabelStore: FolderLabelStore
    private lateinit var settingsStore: SettingsStore

    // A plain Compose MutableState read directly by AppRoot. Because
    // launchMode="singleTask" is set in the manifest, tapping a shortcut
    // while the app is already running reuses this same Activity instance
    // via onNewIntent() rather than creating a new one — mutating this here
    // is enough to trigger recomposition and navigate, no separate event bus
    // needed.
    private var pendingShortcutAction by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutAction = intent.action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingShortcutAction = intent?.action

        // [KOTAK HITAM] 1. Periksa apakah sesi sebelumnya mengalami crash
        val prefs = getSharedPreferences("gallery_cleaner_debug", Context.MODE_PRIVATE)
        val savedCrashLog = prefs.getString("last_crash_log", null)
        if (savedCrashLog != null) {
            // Hapus log setelah dibaca agar tidak muncul terus-menerus
            prefs.edit().remove("last_crash_log").apply()
        }

        // [KOTAK HITAM] 2. Amankan sistem agar jika crash, log diselamatkan ke SharedPreferences
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val fullStackTrace = stringWriter.toString()

            // Simpan paksa ke memori sebelum aplikasi menutup diri
            prefs.edit().putString("last_crash_log", fullStackTrace).commit()
            
            // Biarkan aplikasi menutup diri secara normal setelah data aman
            defaultHandler?.uncaughtException(thread, throwable)
        }

        progressStore = ProgressStore(applicationContext)
        trashStore = TrashStore(applicationContext)
        statsStore = StatsStore(applicationContext)
        folderLabelStore = FolderLabelStore(applicationContext)
        settingsStore = SettingsStore(applicationContext)

        setContent {
            val themeMode by settingsStore.themeModeFlow.collectAsState(initial = ThemeMode.DARK)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            GalleryCleanerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        progressStore = progressStore, 
                        trashStore = trashStore, 
                        statsStore = statsStore,
                        folderLabelStore = folderLabelStore,
                        settingsStore = settingsStore,
                        pendingShortcutAction = pendingShortcutAction,
                        onShortcutActionConsumed = { pendingShortcutAction = null },
                        initialCrashLog = savedCrashLog // Lempar data crash ke UI utama
                    )
                }
            }
        }
    }
}

private sealed class Screen {
    object Permission : Screen()
    object Onboarding : Screen()
    object Trash : Screen()
    object Settings : Screen()
    data class Swipe(val group: MediaGroup) : Screen()
    object Home : Screen()
}

/** Bundles every value derived from (allMedia, trashedIds, expiredIds) that
 *  used to be computed synchronously in composition. Computed together in
 *  one background pass instead — see the LaunchedEffect in AppRoot. */
private data class DerivedMediaState(
    val activeMedia: List<MediaItem> = emptyList(),
    val trashItems: List<MediaItem> = emptyList(),
    val expiredTrashItems: List<MediaItem> = emptyList(),
    val totalLibraryBytes: Long = 0L,
    val trashReclaimableBytes: Long = 0L
)

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun AppRoot(
    progressStore: ProgressStore, 
    trashStore: TrashStore, 
    statsStore: StatsStore,
    folderLabelStore: FolderLabelStore,
    settingsStore: SettingsStore,
    pendingShortcutAction: String?,
    onShortcutActionConsumed: () -> Unit,
    initialCrashLog: String?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val folderLabels by folderLabelStore.allLabelsFlow.collectAsState(initial = emptyMap())
    val trashRetentionDays by settingsStore.trashRetentionDaysFlow.collectAsState(
        initial = SettingsStore.DEFAULT_TRASH_RETENTION_DAYS
    )
    // Defaults to true (not false) for the brief window before DataStore's
    // real persisted value loads — this only matters for a split second,
    // but which way it's wrong matters: defaulting true means a genuinely
    // new install might flash Home before flipping to Onboarding once, a
    // one-time event. Defaulting false would instead flash Onboarding in
    // front of every returning user on every single app open, which is far
    // more disruptive for the common case.
    val hasSeenOnboarding by settingsStore.hasSeenOnboardingFlow.collectAsState(initial = true)

    // State untuk mengontrol pop-up tampilan error crash
    var activeCrashLog by remember { mutableStateOf(initialCrashLog) }

    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasPermission = results.values.all { it } }

    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var groupMode by remember { mutableStateOf(GroupMode.MONTH) }
    var sortOption by remember { mutableStateOf(SortOption.DATE) }
    var selectedGroup by remember { mutableStateOf<MediaGroup?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Handles both cold start (pendingShortcutAction set from the launch
    // Intent in onCreate) and the app already running (updated via
    // onNewIntent since MainActivity is launchMode="singleTask"). Consuming
    // the action by resetting it to null (below) is what makes repeat taps
    // of the same shortcut keep working — each tap is a null -> action
    // transition, which LaunchedEffect always sees as a real key change,
    // rather than the action value just staying the same between taps.
    LaunchedEffect(pendingShortcutAction) {
        when (pendingShortcutAction) {
            ACTION_VIEW_TRASH -> {
                showSettings = false
                showTrash = true
            }
            ACTION_OPEN_SETTINGS -> {
                showTrash = false
                showSettings = true
            }
            else -> return@LaunchedEffect
        }
        onShortcutActionConsumed()
    }

    val trashedItems by trashStore.trashedItemsFlow.collectAsState(initial = emptyList())
    val trashedIds = remember(trashedItems) { trashedItems.map { it.id }.toSet() }
    val expiredIds by remember(trashRetentionDays) { trashStore.expiredItemIdsFlow(trashRetentionDays) }
        .collectAsState(initial = emptySet())

    var isLoadingMore by remember { mutableStateOf(false) }
    // Bumping this re-runs the effect below even though `hasPermission`
    // hasn't changed — the one way to force a completely fresh MediaStore
    // query on demand (e.g. after renaming a folder in another gallery app,
    // in case its own index was stale rather than ours).
    var refreshTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(hasPermission, refreshTrigger) {
        if (hasPermission) {
            isLoading = true
            allMedia = emptyList()
            var firstPage = true
            withContext(Dispatchers.IO) {
                MediaRepository.loadMediaProgressively(context).collect { page ->
                    withContext(Dispatchers.Main) {
                        allMedia = allMedia + page
                        if (firstPage) {
                            isLoading = false
                            isLoadingMore = true
                            firstPage = false
                        }
                    }
                }
            }
            isLoadingMore = false
        }
    }

    // All of this used to be `remember(allMedia, trashedIds) { ... }` — which
    // avoids recomputing when the keys are unchanged, but still runs the
    // filter/sum synchronously ON the main/composition thread whenever they
    // DO change. During progressive gallery loading, allMedia changes on
    // every single page (every few hundred ms for a large library), so this
    // was doing repeated O(n) work on the UI thread exactly while the user
    // is scrolling — the actual cause of scroll stutter reappearing.
    // Computing it in a background coroutine instead means composition only
    // ever reads an already-computed value; the list never blocks on this.
    var derivedMedia by remember { mutableStateOf(DerivedMediaState()) }
    LaunchedEffect(allMedia, trashedIds, expiredIds) {
        derivedMedia = withContext(Dispatchers.Default) {
            val active = allMedia.filterNot { it.id in trashedIds }
            val trash = allMedia.filter { it.id in trashedIds }
            DerivedMediaState(
                activeMedia = active,
                trashItems = trash,
                expiredTrashItems = trash.filter { it.id in expiredIds },
                totalLibraryBytes = active.sumOf { it.sizeBytes },
                trashReclaimableBytes = trash.sumOf { it.sizeBytes }
            )
        }
    }
    val activeMedia = derivedMedia.activeMedia
    val trashItems = derivedMedia.trashItems
    val expiredTrashItems = derivedMedia.expiredTrashItems

    var groups by remember { mutableStateOf<List<MediaGroup>>(emptyList()) }
    LaunchedEffect(activeMedia, groupMode, sortOption) {
        groups = withContext(Dispatchers.Default) {
            MediaRepository.group(activeMedia, groupMode, sortOption)
        }
    }

    var smartGroups by remember { mutableStateOf<List<MediaGroup>>(emptyList()) }
    LaunchedEffect(activeMedia) {
        val quickCategories = withContext(Dispatchers.Default) {
            MediaRepository.smartCategories(activeMedia)
        }
        smartGroups = quickCategories

        delay(600)
        val duplicates = withContext(Dispatchers.IO) {
            MediaRepository.findExactDuplicates(context, activeMedia)
        }
        if (duplicates.isNotEmpty()) {
            smartGroups = quickCategories + MediaGroup("Duplicate files", duplicates)
        }
    }

    val totalFreedBytes by statsStore.totalFreedBytesFlow.collectAsState(initial = 0L)
    val totalDeletedCount by statsStore.totalDeletedCountFlow.collectAsState(initial = 0)

    var pendingDeleteRetry by remember { mutableStateOf<List<MediaItem>?>(null) }
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val items = pendingDeleteRetry
        if (result.resultCode == android.app.Activity.RESULT_OK && items != null) {
            allMedia = allMedia.filterNot { item -> items.any { it.id == item.id } }
            scope.launch {
                trashStore.remove(items.map { it.id })
                statsStore.recordDeletion(items.sumOf { it.sizeBytes }, items.size)
            }
        } else if (items != null) {
            scope.launch {
                snackbarHostState.showSnackbar("Gagal menghapus file atau izin ditolak")
            }
        }
        pendingDeleteRetry = null
    }

    fun performPermanentDeletion(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val uris = items.map { it.uri }
        if (Build.VERSION.SDK_INT >= 30) {
            pendingDeleteRetry = items
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            deleteRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } else {
            try {
                val failed = DeleteHelper.deleteDirectly(context, uris)
                val deleted = items.filterNot { failed.contains(it.uri) }
                val deletedIds = deleted.map { it.id }
                allMedia = allMedia.filterNot { item -> deletedIds.contains(item.id) }
                scope.launch {
                    trashStore.remove(deletedIds)
                    statsStore.recordDeletion(deleted.sumOf { it.sizeBytes }, deleted.size)
                }
                if (failed.isNotEmpty()) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Gagal menghapus ${failed.size} file. Periksa izin.")
                    }
                }
            } catch (e: RecoverableSecurityException) {
                val sender: IntentSender = e.userAction.actionIntent.intentSender
                pendingDeleteRetry = items
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
        }
    }

    BackHandler(enabled = showTrash) { showTrash = false }
    BackHandler(enabled = showSettings) { showSettings = false }

    val currentScreen = when {
        !hasPermission -> Screen.Permission
        !hasSeenOnboarding -> Screen.Onboarding
        showTrash -> Screen.Trash
        showSettings -> Screen.Settings
        selectedGroup != null -> Screen.Swipe(selectedGroup!!)
        else -> Screen.Home
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(180)) +
                    androidx.compose.animation.slideInHorizontally(tween(220)) { it / 8 })
                    .togetherWith(androidx.compose.animation.fadeOut(tween(120)))
            },
            label = "screen-transition"
        ) { screen ->
            when (screen) {
                Screen.Permission -> PermissionScreen(onRequest = { permissionLauncher.launch(requiredPermissions()) })
                Screen.Onboarding -> OnboardingScreen(
                    onDone = { scope.launch { settingsStore.setHasSeenOnboarding(true) } }
                )
                Screen.Trash -> TrashScreen(
                    items = trashItems,
                    trashedAtMillis = trashedItems.associate { it.id to it.trashedAtMillis },
                    expiryDays = trashRetentionDays,
                    onBack = { showTrash = false },
                    onRestore = { ids -> scope.launch { trashStore.remove(ids) } },
                    onDeletePermanently = { ids ->
                        performPermanentDeletion(trashItems.filter { it.id in ids })
                    }
                )
                Screen.Settings -> SettingsScreen(
                    settingsStore = settingsStore,
                    onBack = { showSettings = false }
                )
                is Screen.Swipe -> SwipeScreen(
                    group = screen.group,
                    // Custom in-app label takes priority over the raw
                    // folder name — the whole point of it is to stand in
                    // for a device Gallery's own naming that we can't read.
                    displayName = folderLabels[screen.group.key] ?: screen.group.key,
                    progressStore = progressStore,
                    onBack = { selectedGroup = null },
                    onFinishWithDeletions = { deletions ->
                        scope.launch {
                            try {
                                trashStore.addToTrash(deletions.map { it.id })
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Gagal memproses data swipe")
                            }
                        }
                    }
                )
                Screen.Home -> HomeScreen(
                    groups = groups,
                    smartGroups = smartGroups,
                    groupMode = groupMode,
                    sortOption = sortOption,
                    progressStore = progressStore,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    trashCount = trashItems.size,
                    totalLibraryBytes = derivedMedia.totalLibraryBytes,
                    trashReclaimableBytes = derivedMedia.trashReclaimableBytes,
                    totalFreedBytes = totalFreedBytes,
                    totalDeletedCount = totalDeletedCount,
                    expiredTrashCount = expiredTrashItems.size,
                    expiryDays = trashRetentionDays,
                    folderLabels = folderLabels,
                    onRenameFolder = { groupKey, newLabel ->
                        scope.launch { folderLabelStore.setLabel(groupKey, newLabel) }
                    },
                    onGroupModeChange = { groupMode = it },
                    onSortChange = { sortOption = it },
                    onGroupClick = { selectedGroup = it },
                    onTrashClick = { showTrash = true },
                    onSettingsClick = { showSettings = true },
                    onRefresh = { refreshTrigger++ },
                    onCleanExpiredTrash = { performPermanentDeletion(expiredTrashItems) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // [KOTAK HITAM] UI Dialog Pop-up pemicu informasi error koding
        if (activeCrashLog != null) {
            AlertDialog(
                onDismissRequest = { activeCrashLog = null },
                title = { Text("Laporan Deteksi Crash 🛠️") },
                text = {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = activeCrashLog!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { activeCrashLog = null }) {
                        Text("Saya Mengerti")
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Clean your gallery",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Gallery Cleaner needs access to your photos to help you swipe through and declutter.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
        ) {
            Text("Grant access", style = MaterialTheme.typography.titleMedium)
        }
    }
}
