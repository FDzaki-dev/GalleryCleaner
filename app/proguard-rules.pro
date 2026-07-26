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
