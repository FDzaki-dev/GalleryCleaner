# CHANGELOG

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
