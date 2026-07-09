package com.example.gallerycleaner

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
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

class MainActivity : ComponentActivity() {

    private lateinit var progressStore: ProgressStore
    private lateinit var trashStore: TrashStore
    private lateinit var statsStore: StatsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

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

        setContent {
            GalleryCleanerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        progressStore = progressStore, 
                        trashStore = trashStore, 
                        statsStore = statsStore,
                        initialCrashLog = savedCrashLog // Lempar data crash ke UI utama
                    )
                }
            }
        }
    }
}

private sealed class Screen {
    object Permission : Screen()
    object Trash : Screen()
    data class Swipe(val group: MediaGroup) : Screen()
    object Home : Screen()
}

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
    initialCrashLog: String?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    val trashedItems by trashStore.trashedItemsFlow.collectAsState(initial = emptyList())
    val trashedIds = remember(trashedItems) { trashedItems.map { it.id }.toSet() }
    val expiredIds by trashStore.expiredItemIdsFlow.collectAsState(initial = emptySet())

    var isLoadingMore by remember { mutableStateOf(false) }
    LaunchedEffect(hasPermission) {
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

    val activeMedia = remember(allMedia, trashedIds) {
        allMedia.filterNot { it.id in trashedIds }
    }
    val trashItems = remember(allMedia, trashedIds) {
        allMedia.filter { it.id in trashedIds }
    }
    val expiredTrashItems = remember(trashItems, expiredIds) {
        trashItems.filter { it.id in expiredIds }
    }

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

    val currentScreen = when {
        !hasPermission -> Screen.Permission
        showTrash -> Screen.Trash
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
                Screen.Trash -> TrashScreen(
                    items = trashItems,
                    trashedAtMillis = trashedItems.associate { it.id to it.trashedAtMillis },
                    expiryDays = TrashStore.EXPIRY_DAYS,
                    onBack = { showTrash = false },
                    onRestore = { ids -> scope.launch { trashStore.remove(ids) } },
                    onDeletePermanently = { ids ->
                        performPermanentDeletion(trashItems.filter { it.id in ids })
                    }
                )
                is Screen.Swipe -> SwipeScreen(
                    group = screen.group,
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
                    totalLibraryBytes = activeMedia.sumOf { it.sizeBytes },
                    trashReclaimableBytes = trashItems.sumOf { it.sizeBytes },
                    totalFreedBytes = totalFreedBytes,
                    totalDeletedCount = totalDeletedCount,
                    expiredTrashCount = expiredTrashItems.size,
                    expiryDays = TrashStore.EXPIRY_DAYS,
                    onGroupModeChange = { groupMode = it },
                    onSortChange = { sortOption = it },
                    onGroupClick = { selectedGroup = it },
                    onTrashClick = { showTrash = true },
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
