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
