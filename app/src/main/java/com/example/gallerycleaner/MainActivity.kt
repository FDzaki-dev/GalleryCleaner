package com.example.gallerycleaner

import android.Manifest
import android.app.RecoverableSecurityException
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gallerycleaner.ui.theme.GalleryCleanerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var progressStore: ProgressStore
    private lateinit var trashStore: TrashStore
    private lateinit var statsStore: StatsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        progressStore = ProgressStore(applicationContext)
        trashStore = TrashStore(applicationContext)
        statsStore = StatsStore(applicationContext)

        setContent {
            GalleryCleanerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(progressStore = progressStore, trashStore = trashStore, statsStore = statsStore)
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
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun AppRoot(progressStore: ProgressStore, trashStore: TrashStore, statsStore: StatsStore) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
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

    val trashedIds by trashStore.trashedIdsFlow.collectAsState(initial = emptySet())

    // Reload media whenever permission is granted — off the main thread so a
    // large gallery (thousands of items) never blocks the UI.
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            allMedia = withContext(Dispatchers.IO) { MediaRepository.loadAllMedia(context) }
            isLoading = false
        }
    }

    // Trashed items are excluded from the active gallery so they don't show
    // up again in swipe sessions while sitting in the trash.
    val activeMedia = remember(allMedia, trashedIds) {
        allMedia.filterNot { it.id in trashedIds }
    }
    val trashItems = remember(allMedia, trashedIds) {
        allMedia.filter { it.id in trashedIds }
    }

    // Grouping/sorting a large list is also non-trivial work, so it's computed
    // off the main thread too, and only replaces `groups` once ready — this
    // avoids janking the UI thread on every filter change.
    var groups by remember { mutableStateOf<List<MediaGroup>>(emptyList()) }
    LaunchedEffect(activeMedia, groupMode, sortOption) {
        groups = withContext(Dispatchers.Default) {
            MediaRepository.group(activeMedia, groupMode, sortOption)
        }
    }

    // "Quick Clean" shortcuts (Screenshots, Videos, Large files, possible
    // duplicates) — same MediaGroup shape as regular groups, so they can
    // reuse SwipeScreen with no changes.
    var smartGroups by remember { mutableStateOf<List<MediaGroup>>(emptyList()) }
    LaunchedEffect(activeMedia) {
        smartGroups = withContext(Dispatchers.Default) {
            MediaRepository.smartCategories(activeMedia)
        }
    }

    val totalFreedBytes by statsStore.totalFreedBytesFlow.collectAsState(initial = 0L)
    val totalDeletedCount by statsStore.totalDeletedCountFlow.collectAsState(initial = 0)

    // Delete-request launcher (Android 11+ batch delete confirmation) — this
    // is only ever triggered from the Trash screen's "Delete permanently",
    // never automatically when finishing a swipe session.
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
            } catch (e: RecoverableSecurityException) {
                val sender: IntentSender = e.userAction.actionIntent.intentSender
                pendingDeleteRetry = items
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
        }
    }

    // Without this, the back gesture/button falls through to the system and
    // closes the app entirely instead of navigating within it. Trash is
    // simple state, safe to pop directly — but SwipeScreen has its own
    // BackHandler below since a raw pop here would skip saving progress and
    // moving pending items to trash.
    BackHandler(enabled = showTrash) { showTrash = false }

    val currentScreen = when {
        !hasPermission -> Screen.Permission
        showTrash -> Screen.Trash
        selectedGroup != null -> Screen.Swipe(selectedGroup!!)
        else -> Screen.Home
    }

    // Screens here are just conditional composables, not a real navigation
    // stack — switching them instantly (the previous behavior) is what made
    // the app feel unfinished. A short cross-fade + slide is the standard,
    // cheap way most polished apps mask that same underlying pattern.
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
                // A "delete" swipe moves the item into the trash — nothing is
                // removed from the device until the user empties the trash.
                onFinishWithDeletions = { deletions ->
                    scope.launch { trashStore.addToTrash(deletions.map { it.id }) }
                }
            )
            Screen.Home -> HomeScreen(
                groups = groups,
                smartGroups = smartGroups,
                groupMode = groupMode,
                sortOption = sortOption,
                progressStore = progressStore,
                isLoading = isLoading,
                trashCount = trashItems.size,
                totalLibraryBytes = activeMedia.sumOf { it.sizeBytes },
                trashReclaimableBytes = trashItems.sumOf { it.sizeBytes },
                totalFreedBytes = totalFreedBytes,
                totalDeletedCount = totalDeletedCount,
                onGroupModeChange = { groupMode = it },
                onSortChange = { sortOption = it },
                onGroupClick = { selectedGroup = it },
                onTrashClick = { showTrash = true }
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
            "Gallery Cleaner needs access to your photos and videos to help you swipe through and declutter.",
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
