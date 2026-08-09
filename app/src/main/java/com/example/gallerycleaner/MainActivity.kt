package com.example.gallerycleaner

import android.Manifest
import android.app.KeyguardManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
            val appTheme by settingsStore.appThemeFlow.collectAsState(initial = AppTheme.SIGNATURE)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            GalleryCleanerTheme(darkTheme = darkTheme, appTheme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // initial = null, not false: collectAsState briefly
                    // returns the initial value before DataStore's first
                    // real emission arrives. Defaulting to false would mean
                    // a person with app lock genuinely enabled could see a
                    // flash of AppRoot (their actual gallery) for that
                    // brief window before recomposition catches up to the
                    // true value — a real, if narrow, privacy leak for a
                    // security feature. null is treated as "don't know
                    // yet" below and renders neither screen until resolved,
                    // which fails closed instead of open.
                    val appLockEnabled by settingsStore.appLockEnabledFlow.collectAsState(initial = null)
                    // rememberSaveable, not remember: on rotation the whole
                    // Activity is torn down and recreated (no
                    // android:configChanges override in the manifest), so a
                    // plain `remember` would reset to locked on every
                    // rotation. rememberSaveable survives that via the
                    // normal save/restore Bundle, while still resetting on
                    // real process death — which is fine, a fresh process
                    // start is exactly when re-locking is wanted.
                    var isUnlocked by rememberSaveable { mutableStateOf(false) }
                    val keyguardManager = remember {
                        getSystemService(KeyguardManager::class.java)
                    }
                    val confirmCredentialLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) isUnlocked = true
                    }

                    // Re-lock exactly on a genuine backgrounding (home
                    // button, app switcher, etc.) — never on a rotation.
                    // isChangingConfigurations is true only during a
                    // config-change-triggered onStop, which is the same
                    // guard MainActivity already relies on elsewhere
                    // (performPermanentDeletion's retry state) for the
                    // identical "was this a real stop or just a rotation"
                    // distinction.
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner, appLockEnabled) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP && appLockEnabled == true && !isChangingConfigurations) {
                                isUnlocked = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    when {
                        // Real value not resolved yet — render nothing
                        // rather than guess. See the collectAsState comment
                        // above for why this matters for a security gate
                        // specifically (fails closed, not open).
                        appLockEnabled == null -> Unit
                        appLockEnabled == true && !isUnlocked -> {
                            LaunchedEffect(Unit) {
                                val km = keyguardManager
                                if (km != null && km.isDeviceSecure) {
                                    @Suppress("DEPRECATION")
                                    val intent = km.createConfirmDeviceCredentialIntent(
                                        "Unlock GalleryCleaner",
                                        "Confirm your screen lock to continue"
                                    )
                                    if (intent != null) confirmCredentialLauncher.launch(intent)
                                } else {
                                    // The device's screen lock was removed
                                    // after this setting was turned on —
                                    // there's no valid credential left to
                                    // authenticate against, so holding the
                                    // gallery locked here would strand the
                                    // person with no way back in. Fail
                                    // open here specifically, unlike the
                                    // "unknown state" branch above: this is
                                    // a *known*, resolved state (lock is on,
                                    // but unusable), not an ambiguous one.
                                    isUnlocked = true
                                }
                            }
                            AppLockScreen(
                                onUnlockClick = {
                                    val km = keyguardManager
                                    if (km != null && km.isDeviceSecure) {
                                        @Suppress("DEPRECATION")
                                        val intent = km.createConfirmDeviceCredentialIntent(
                                            "Unlock GalleryCleaner",
                                            "Confirm your screen lock to continue"
                                        )
                                        if (intent != null) confirmCredentialLauncher.launch(intent)
                                    }
                                }
                            )
                        }
                        else -> {
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
    val hapticsEnabled by settingsStore.hapticFeedbackEnabledFlow.collectAsState(initial = true)
    val randomModeEnabled by settingsStore.randomModeEnabledFlow.collectAsState(initial = false)
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
    var onThisDayItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(activeMedia) {
        // Debounced, not immediate. MediaRepository.loadMediaProgressively
        // emits a new page roughly every few hundred ms while a large
        // gallery streams in, and `activeMedia` (this effect's key) changes
        // on every single one of those emissions — restarting this effect
        // each time. Running smartCategories() synchronously on every
        // restart meant rescanning the *entire* active list (screenshot +
        // large-file filters) again and again as it grew, for a section the
        // user isn't even looking at yet (Quick Clean sits below the
        // dashboard, off-screen during initial load). A short debounce here
        // means only the "settled" state after a burst of pages actually
        // pays for a scan — cheap, and zero UX cost since this section was
        // never part of the "instant first paint" the progressive loader is
        // optimizing for in the first place (see MediaRepository.kt).
        delay(200)
        val quickCategories = withContext(Dispatchers.Default) {
            MediaRepository.smartCategories(activeMedia)
        }
        smartGroups = quickCategories
        // Same in-memory pass, same debounce window — onThisDay() is just as
        // cheap as smartCategories() (a single filter over data already
        // loaded), so it doesn't need a dispatcher hop or delay of its own.
        onThisDayItems = withContext(Dispatchers.Default) {
            MediaRepository.onThisDay(activeMedia)
        }

        delay(600)
        val duplicates = withContext(Dispatchers.IO) {
            MediaRepository.findExactDuplicates(context, activeMedia)
        }
        if (duplicates.isNotEmpty()) {
            smartGroups = quickCategories + MediaGroup("Duplicate files", duplicates)
        }
    }

    // Blur/near-duplicate detection: on-demand only (see MediaRepository's
    // doc comments on findBlurryPhotos/findNearDuplicates for why) — state
    // starts Idle and only moves once the person actually taps "Scan".
    var blurryScanState by remember { mutableStateOf<ScanState<List<MediaItem>>>(ScanState.Idle) }
    var nearDupScanState by remember { mutableStateOf<ScanState<List<MediaGroup>>>(ScanState.Idle) }

    fun scanBlurryPhotos() {
        blurryScanState = ScanState.Scanning
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                MediaRepository.findBlurryPhotos(context, activeMedia)
            }
            blurryScanState = ScanState.Done(result)
        }
    }

    fun scanNearDuplicates() {
        nearDupScanState = ScanState.Scanning
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                MediaRepository.findNearDuplicates(context, activeMedia)
            }
            nearDupScanState = ScanState.Done(result)
        }
    }

    val totalFreedBytes by statsStore.totalFreedBytesFlow.collectAsState(initial = 0L)
    val totalDeletedCount by statsStore.totalDeletedCountFlow.collectAsState(initial = 0)

    // Surface the biggest space hogs directly on the dashboard. This turns
    // storage pressure into an immediately actionable list instead of making
    // the user hunt through folders or the generic Large Files category.
    val largestItems = remember(activeMedia) {
        activeMedia.sortedByDescending { it.sizeBytes }.take(5)
    }

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

    // Compression's own launcher+pending-state pair, separate from delete's
    // above — reusing one launcher for two different pending actions would
    // mean its callback couldn't tell which kind of request it's resuming
    // after the system dialog closes.
    var pendingCompressRetry by remember { mutableStateOf<List<MediaItem>?>(null) }
    val compressRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val items = pendingCompressRetry
        pendingCompressRetry = null
        if (result.resultCode == android.app.Activity.RESULT_OK && items != null) {
            // Write access for the whole batch was just granted — every
            // compressInPlace() call below should now succeed without
            // hitting RecoverableSecurityException again.
            scope.launch(Dispatchers.IO) {
                var saved = 0L
                items.forEach { item ->
                    val result = ImageCompressor.compressInPlace(context, item)
                    if (result is ImageCompressor.Result.Success) saved += result.savedBytes
                }
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        if (saved > 0) "Compressed — saved ${formatBytes(saved)}" else "Nothing to compress"
                    )
                }
            }
        } else if (items != null) {
            scope.launch { snackbarHostState.showSnackbar("Compression permission denied") }
        }
    }

    /** Compresses [items] in place at a balanced quality. On Android 11+
     *  (where MediaStore.createWriteRequest exists), asks for write access
     *  to the whole batch up front — one system dialog for the selection,
     *  not one per photo. Below that, compresses directly and stops at the
     *  first RecoverableSecurityException rather than juggling a queue of
     *  per-item consent dialogs, which would be a jarring, repeated-prompt
     *  experience on those older OS versions. */
    fun performCompression(items: List<MediaItem>) {
        if (items.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            if (ImageCompressor.supportsBatchWriteRequest()) {
                try {
                    val uris = items.map { it.uri }
                    val pending = MediaStore.createWriteRequest(context.contentResolver, uris)
                    withContext(Dispatchers.Main) {
                        pendingCompressRetry = items
                        compressRequestLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Gagal meminta izin kompresi")
                    }
                }
            } else {
                var saved = 0L
                var needsPermission = false
                for (item in items) {
                    when (val result = ImageCompressor.compressInPlace(context, item)) {
                        is ImageCompressor.Result.Success -> saved += result.savedBytes
                        is ImageCompressor.Result.NeedsPermission -> {
                            needsPermission = true
                            break
                        }
                        else -> Unit
                    }
                }
                withContext(Dispatchers.Main) {
                    when {
                        needsPermission -> snackbarHostState.showSnackbar(
                            "Beberapa foto butuh izin tambahan — coba lagi satu per satu"
                        )
                        saved > 0 -> snackbarHostState.showSnackbar("Compressed — saved ${formatBytes(saved)}")
                        else -> snackbarHostState.showSnackbar("Nothing to compress")
                    }
                }
            }
        }
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
                    hapticsEnabled = hapticsEnabled,
                    onCompressRequest = ::performCompression,
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
                    onThisDayItems = onThisDayItems,
                    blurryScanState = blurryScanState,
                    onScanBlurry = ::scanBlurryPhotos,
                    nearDupScanState = nearDupScanState,
                    onScanNearDuplicates = ::scanNearDuplicates,
                    groupMode = groupMode,
                    sortOption = sortOption,
                    progressStore = progressStore,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    trashCount = trashItems.size,
                    totalLibraryBytes = derivedMedia.totalLibraryBytes,
                    trashReclaimableBytes = derivedMedia.trashReclaimableBytes,
                    largestItems = largestItems,
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
                    onGroupClick = { group ->
                        // Reshuffled fresh on every entry rather than once and
                        // cached — see randomModeEnabledFlow's doc comment for
                        // why that's the deliberate tradeoff (ProgressStore's
                        // saved index is only meaningful within one shuffled
                        // session, not across re-entries).
                        selectedGroup = if (randomModeEnabled) {
                            group.copy(items = group.items.shuffled())
                        } else {
                            group
                        }
                    },
                    onTrashClick = { showTrash = true },
                    onSettingsClick = { showSettings = true },
                    onRefresh = { refreshTrigger++ },
                    onCleanExpiredTrash = { performPermanentDeletion(expiredTrashItems) },
                    randomModeEnabled = randomModeEnabled,
                    onRandomModeToggle = { enabled ->
                        scope.launch { settingsStore.setRandomModeEnabled(enabled) }
                    }
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

@Composable
private fun AppLockScreen(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("GalleryCleaner is locked", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Confirm your screen lock to continue.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onUnlockClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
        ) {
            Text("Unlock", style = MaterialTheme.typography.titleMedium)
        }
    }
}
