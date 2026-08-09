# CHANGELOG

## v10_Batch10 — 2026-08-09
- Refactor (Tactile Component Migration, scope: SettingsScreen.kt saja): 3 pemakaian M3 `Switch(...)` → `TactileSwitch(...)` (§12). API drop-in identik (checked/onCheckedChange/modifier/enabled), 0 perubahan logic, +1 import. Dikonfirmasi `Switch(` sudah 0 pemakaian di seluruh project. `ThemeStyleCard`/`IconButton`/`RadioButton`/`FilterChip` di file yang sama sengaja belum disentuh (butuh perluasan API komponen dulu — lihat PROJECT_STATE).

## v9_Batch9 — 2026-08-09
- Refactor (God File split, scope: SwipeScreen.kt saja): 822 baris → 4 file (`SwipeScreen.kt` 292 baris main composable, `SwipeScreenGrid.kt` 223, `SwipeScreenControls.kt` 158, `SwipeScreenCard.kt` 194). Ekstraksi via exact line-range slicing — isi fungsi 100% identik. 8 `private fun` → `internal fun` (dipanggil lintas file); `SWIPE_CARD_DECODE_SIZE` juga jadi `internal` (dipakai di 2 file); `SWIPE_THRESHOLD_PX`/`MAX_ROTATION_DEG` dipindah ke `SwipeScreenCard.kt` (tetap `private`, dead duplicate di file lama dihapus). Import per file di-trim ke yang benar-benar dipakai.

## v8_Batch8 — 2026-08-09
- Refactor (God File split, scope: HomeScreen.kt saja): 1001 baris → 4 file (`HomeScreen.kt` 361 baris main composable, `HomeScreenSearch.kt` 126, `HomeScreenSections.kt` 384, `HomeScreenFolderRow.kt` 235). Ekstraksi via exact line-range slicing (bukan retyping) — isi fungsi 100% identik, hanya 15 `private fun` → `internal fun` (satu-satunya perubahan kode, wajib karena Kotlin `private` top-level file-scoped).

## v7_Batch7 — 2026-08-09
- Refactor (God File split, scope: MediaRepository.kt saja): 517 baris → 3 file (`MediaRepository.kt` 107 baris facade, `MediaDataSource.kt` 150 baris raw I/O, `MediaScanner.kt` 322 baris analytical scans). Semua fungsi publik 100% copy-paste (bukan ditulis ulang) — `MediaRepository` tetap jadi satu-satunya entry point yang dipanggil `MainActivity`/`CleaningReminderWorker`, 0 file caller diubah.

## v6_Batch6 — 2026-08-09
- Fix: `AndroidManifest.xml` (protected, parsial) — tambah `android:largeHeap="true"`. Root cause dari `crash_20260809_074212_....txt` (runtime OOM, dari CrashLogger produksi user): heap 256MB terlalu kecil untuk grid Compose image-heavy. Aman sekarang karena Coil cache sudah dipin fixed-percentage di `GalleryCleanerApp.kt`.
- Update: `.github/workflows/build.yml` — rename artifact log-fail: `test-result-<branch>-attempt-<attempt>.log` → `log-fail_<branch>_run<run_number>-attempt<attempt>_<short_sha>.log` (unik lintas run, bukan cuma lintas attempt).

## v5_Batch5 — 2026-08-09
- Fix: `core/utils/CrashLogger.kt` — `onUncaughtException` → `uncaughtException` (nama method interface yang benar). Root cause dari `test-result-main-attempt-1.log` (build kedua).
- Refactor (Atomic, Phase-1 partial): reorganisasi 17 file ke direktori `data/model/`, `data/media/`, `data/local/datastore/`, `presentation/screen/`, `worker/`, `core/utils/` sesuai Structure Audit. Package declaration TIDAK diubah (tetap flat) — murni physical move, 0 import rusak.
- Note: `ui/theme/`, `ui/components/` tidak disentuh (sudah real sub-package sejak Batch2/3). `MainActivity.kt`/`GalleryCleanerApp.kt` tetap di root (entry point, terhubung ke AndroidManifest).

## v4_Batch4 — 2026-08-09
- Add: `CrashLogger.kt` — crash logger bawaan (fitur preferensi yang sebelumnya terlewat). MediaStore (API29+) + legacy File fallback (API24-28), FIFO 50 log, metadata Version/OS/Model/Timestamp/Thread/StackTrace.
- Update: `GalleryCleanerApp.kt` (protected, parsial) — hook `CrashLogger.install()` di `onCreate()`.

## v3_Batch3 — 2026-08-09
- Fix: `ui/components/TactileButton.kt`, `ui/components/GlassNavigation.kt` — `CompositionLocalProvider` diimport dari package salah (`material3` → `runtime`), penyebab "Unresolved reference" di CI (lihat `test-result-main-attempt-1.log`).
- Fix: `ui/components/TactileSlider.kt` — tambah `@OptIn(ExperimentalMaterial3Api::class)` untuk parameter `thumb` pada M3 `Slider` (experimental API).

## v2_Batch2 — 2026-08-09
- Add: `.github/workflows/build.yml` — publish signed APK ke **GitHub Release** (`softprops/action-gh-release@v2`, tag `v1.0.<run_number>`), bukan hanya Actions Artifact. `permissions.contents: read → write`.
- Override: `AppTheme.SIGNATURE` (tema default) — implementasi penuh spec "Premium AMOLED Hybrid Glassmorphism + Subtle Midnight Blue + Micro-Skeuomorphism" (`compose-amoled-hybrid-glass-final.md`, 25 section).
- New: `ui/theme/GlassTokens.kt`, `ui/theme/TactileTokens.kt`, `ui/theme/Shape.kt`.
- New: `ui/components/GlassSurface.kt`, `GlassCard.kt`, `TactileButton.kt`, `TactileSwitch.kt`, `TactileSlider.kt`, `GlassNavigation.kt` — arsitektur §23, siap dipakai layar-layar berikutnya.
- Update: `ui/theme/Theme.kt` (SignatureDark/SignatureLight recolor), `SettingsScreen.kt` (theme preview swatch: GraphiteBg → AmoledBlack, deskripsi diperbarui).
- Note: Keep/Delete semantic colors (SageKeep/CoralDelete) sengaja TIDAK diubah — di luar cakupan spec, app-critical UX.

## v1_Batch1 — 2026-08-09
- Fix: `app/build.gradle.kts` missing closing brace `}` di blok validasi `RELEASE_KEY_PASSWORD` (baris 58-62) → menyebabkan `Expecting '}'` di line 123 dan build CI gagal total.
- Add: `.github/workflows/build.yml` step build sekarang menyimpan output ke `test-result-<branch>-attempt-<run_attempt>.log` dan upload otomatis sebagai GitHub Actions artifact jika build gagal (`if: failure()`).
