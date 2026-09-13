# Coil uses reflection to discover decoders/fetchers at runtime — without
# these keep rules, R8 can strip classes that are only referenced by name,
# and image loading (especially the video/gif decoders) breaks silently in
# release builds even though it works fine in debug.
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# Kotlin coroutines + DataStore internals occasionally get flagged by R8's
# stricter release-mode analysis; these are the standard, safe suppressions.
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
-keep class androidx.datastore.** { *; }

# Batch111 (Opsi B, extra protection — isMinifyEnabled/isShrinkResources
# turned on for the first time this project). Every rule below targets a
# SPECIFIC reflection-based risk confirmed by reading this project's own
# code, not a generic copy-paste — each one explains what breaks without it.

# 1) CrashLogger (project's own crash-safety-net feature) writes raw
# stack traces to MediaStore/File. Obfuscation renames classes/methods AND
# strips line numbers by default — without this, every crash report the
# app collects post-release becomes unreadable noise (a real regression to
# an existing safety feature, not a hypothetical one).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*

# 2) WorkManager (CleaningReminderWorker, TrashExpiryWorker): WorkManager
# instantiates Worker subclasses by reflection (Class.forName + the
# (Context, WorkerParameters) constructor) at execution time, days after
# the app was built. The build-time reference —
# PeriodicWorkRequestBuilder<CleaningReminderWorker>(...) — only touches
# `::class.java`, which R8 can see as "used" without ever seeing the
# constructor as called, so the constructor is a real strip candidate.
# General rule (covers both current workers, and any future ones) +
# explicit rule for the two concrete classes as a belt-and-suspenders
# because this exact gotcha causes SILENT failures (job just never runs,
# no crash log) rather than a loud one.
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.gallerycleaner.worker.CleaningReminderWorker { *; }
-keep class com.example.gallerycleaner.worker.TrashExpiryWorker { *; }
-dontwarn androidx.work.**

# 3) BiometricPrompt (App Lock, Batch39): drives its own FragmentManager-
# based dialog internally. Keeping the whole package is cheap (small
# library) and avoids guessing which internal class the next androidx
# release starts reflecting into.
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# 4) OkHttp (in-app update checker/downloader, Batch49): probes for
# optional TLS providers (Conscrypt/BouncyCastle/JDK9 JSSE) via reflection
# at startup. This app never bundles those providers, so R8 just needs to
# stop treating the missing classes as a hard error. Matches OkHttp's own
# published R8 configuration (square.github.io/okhttp/features/r8_proguard).
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# 5) [Batch113 hotfix, missed by the Batch111 audit above] Compose Material
# Icons Extended (`androidx.compose.material:material-icons-extended`).
# CONFIRMED regression (user-reported, real build/device): after Batch111
# turned on isMinifyEnabled/isShrinkResources, exactly 7 icons render blank —
# Shuffle/Folder/Sort/Undo/ViewCarousel/ZoomIn/GridView (HomeScreen.kt,
# HomeScreenFolderRow.kt, SwipeScreen.kt x3, SwipeScreenGrid.kt) — while every
# other icon in the app (Close/Check/ArrowBack/Search/Settings/Edit/Lock/
# PlayArrow/Info/Share/Refresh, all from the small material-icons-core set)
# renders fine. Root cause: material-icons-extended is ~1100+ near-identical
# generated singleton objects, each backed by a private lazily-cached
# `ImageVector?` field filled on first access — R8 full-mode's horizontal
# class-merging + field-value-propagation (only active now that minify is on)
# collapses these structurally-identical classes together and corrupts that
# per-icon cache, so the icon silently renders empty instead of crashing.
# material-icons-core ships pre-built/small and doesn't hit this path, which
# is exactly why core icons were unaffected. Grep-verified this project only
# ever calls `Icons.Filled.*` (0 Outlined/Rounded/Sharp/TwoTone/AutoMirrored
# usage anywhere) — so the keep is scoped to the `filled` package only,
# instead of blanket-keeping all of material-icons-extended.
# [Updated Batch114] The Stage 2 migration this note anticipated HAS now been
# executed: all 7 extended-only icons were moved to local vector drawables
# and `material-icons-extended` was removed from app/build.gradle.kts (see
# Batch114 in PROJECT_STATE.md/CHANGELOG.md). This rule is DELIBERATELY LEFT
# IN PLACE rather than deleted: `androidx.compose.material.icons.filled` is
# the SAME package used by the small material-icons-CORE set (Close/Check/
# ArrowBack/Search/Settings/Edit/Lock/PlayArrow/Info/Share/Refresh — still
# used throughout this app, still on the classpath via material3). Keeping
# ~20 small core-icon classes fully is cheap, and with the ~1100-file
# extended set gone the original class-merging collision risk this rule
# targeted is much smaller anyway — but with 0 compiler/CI access in this
# sandbox to re-verify that removing the rule is safe, the conservative
# choice is to leave a working safety net in place rather than strip it on
# an unverified assumption (Anti-Breaking / Regression-Check guard).
-keep class androidx.compose.material.icons.filled.** { *; }
-dontwarn androidx.compose.material.icons.**

# 6) [Batch122] Media3 ExoPlayer (media3-exoplayer/media3-ui, Batch121) —
# user-reported "Can't play this video" on EVERY video, on the signed
# release APK from GitHub Releases (isMinifyEnabled/isShrinkResources on
# since Batch111). Batch121's own build.gradle.kts comment assumed "0 new
# proguard-rules.pro entry needed — Media3 ships its own consumer rules",
# same class of unverified assumption that already broke once in this exact
# project (material-icons-extended, rule #5 above, Batch111->113). This one
# has independent public precedent too: ExoPlayer's own issue tracker
# (google/ExoPlayer#8709) documents mp4/m4a playback throwing a source/
# extractor error in release-with-minify builds while working fine in
# debug, traced to R8 needing explicit keep rules the bundled consumer
# rules didn't fully cover for that release. Can't reproduce/confirm the
# exact failure in this sandbox (0 Android SDK/emulator/device) — this is
# the same "belum tervalidasi compiler/device asli" position as every other
# batch here, not a confirmed fix, just the standard, low-risk mitigation
# for this documented failure class. Scoped to the packages this app's
# playback path actually touches (local content:// files via
# DefaultExtractorsFactory + platform MediaCodec renderers) — no DASH/HLS/
# RTMP/Cast/session/datasource-okhttp extensions are dependencies of this
# project, so a blanket `androidx.media3.**` keep isn't needed.
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.decoder.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }
-dontwarn androidx.media3.**
