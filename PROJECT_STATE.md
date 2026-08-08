# PROJECT_STATE — GalleryCleaner

## Versi Saat Ini
v5 — Batch5 (Fix crash logger + Phase-1 package restructure: directory reorg)

## Fix Batch4 Build Failure (dari test-result-main-attempt-1.log kedua)
`CrashLogger.kt:40` — `Unresolved reference: onUncaughtException`. Nama method salah; interface `Thread.UncaughtExceptionHandler` method-nya `uncaughtException`, bukan `onUncaughtException`. DIPERBAIKI.

## Phase-1 (Atomic Change) — Package Restructure per Structure Audit
- Scope batch ini: **reorganisasi direktori fisik saja**, package declaration TIDAK diubah (tetap `com.example.gallerycleaner` flat untuk file lama, `ui.theme`/`ui.components` tetap seperti semula). Kotlin tidak mewajibkan folder = package (beda dari Java) — jadi ini 0% risiko broken import, semua referensi antar-file tetap valid tanpa perlu tambah `import` di mana pun.
- Layout baru:
  - `data/model/` — MediaModels.kt
  - `data/media/` — MediaRepository.kt, DeleteHelper.kt, ImageCompressor.kt
  - `data/local/datastore/` — SettingsStore, StatsStore, TrashStore, ProgressStore, FolderLabelStore
  - `presentation/screen/` — HomeScreen, SwipeScreen, TrashScreen, SettingsScreen, OnboardingScreen, MediaPreview
  - `worker/` — CleaningReminderWorker
  - `core/utils/` — Utils, HapticFeedback, CrashLogger
  - Root (tetap, entry point Android): MainActivity.kt, GalleryCleanerApp.kt — AndroidManifest pakai referensi relatif `.MainActivity`/`.GalleryCleanerApp`, dipindah akan butuh edit manifest (protected asset) tanpa manfaat nyata, jadi sengaja dipertahankan di root.
  - `ui/theme/`, `ui/components/` — TIDAK disentuh, sudah punya package sendiri sejak Batch2/3, sudah sesuai prinsip audit.
- **Phase-1b (belum dikerjakan, next batch)**: split flat package → real sub-package (`com.example.gallerycleaner.data.media`, dst) + tambah `import` di semua pemanggil. Ini butuh compiler nyata untuk validasi tiap langkah (tidak tersedia di environment ini) — akan dikerjakan bertahap per-layer dengan checkpoint CI hijau di antaranya, bukan sekaligus.
- File besar (HomeScreen/SwipeScreen/MediaRepository → Screen/ViewModel/State/Event) BELUM dipecah — itu audit item #3, technically Phase-1 juga tapi butuh perubahan logic nyata (bukan mechanical move), risiko tinggi tanpa compiler → next batch terpisah, bukan bagian atomic move ini.

## Status Build Terakhir
Batch1: FAILED→fixed. Batch2: FAILED(compile)→fixed Batch3. Batch4: FAILED (`onUncaughtException` typo)→fixed Batch5 (ini). Batch5: belum di-run ulang di CI.

## Theme System — AMOLED Hybrid Glassmorphism (compose-amoled-hybrid-glass-final.md)
- Sumber: spec markdown yang diupload user (793 baris, 25 section). Diimplementasikan sebagai arsitektur §23:
  `ui/theme/{Color,Shape,Typography,GlassTokens,TactileTokens,Theme}.kt` + `ui/components/{GlassSurface,GlassCard,TactileButton,TactileSwitch,TactileSlider,GlassNavigation}.kt`.
- `AppTheme.SIGNATURE` (default aplikasi, tidak berubah — tetap default) di-override total: background=AmoledBlack(#030508), surface=GlassBase(#0A0F16), surfaceVariant=GlassElevated(#101722), outline=GlassBorder(alpha 3.5%), tertiary=AccentBlue(#6670FF) untuk selection/focus/progress (§17).
- Primary(SageKeep)/Secondary(CoralDelete) TIDAK diubah — semantik Keep/Delete swipe adalah app-critical UX, di luar cakupan spec (spec generik, tidak tahu soal keep/delete). Diperlakukan sebagai lapisan terpisah dari AMOLED/Glass/Midnight-Blue/Accent yang murni tentang materi permukaan & functional accent.
- Midnight Blue (§6) diimplementasikan sebagai `midnightAmbientGradient()` brush helper (bukan warna solid ColorScheme) — dipakai opsional lewat `GlassSurface(ambient = true)`, TIDAK dipasang otomatis ke background global (sesuai §6 "Incorrect use": jangan solid background).
- `SignatureLight` (mode terang): spec ini AMOLED-only by definition, tidak mendefinisikan light mode. Diberi tertiary=AccentBlue (darker variant) untuk konsistensi lintas mode, sisanya dipertahankan dari sebelumnya. Catatan: ini adaptasi di luar cakupan literal spec.
- Cascading: HomeScreen/SwipeScreen/TrashScreen/SettingsScreen/MainActivity semua sudah pakai `MaterialTheme.colorScheme` (bukan warna hardcoded) → override Theme.kt otomatis merambat ke seluruh app tanpa perlu edit tiap layar.
- Komponen baru (`GlassSurface`, `GlassCard`, `TactileButton`, `TactileSwitch`, `TactileSlider`, `GlassNavigation`) SUDAH DIBUAT tapi BELUM dipasang menggantikan Box/Card/Button/Switch bawaan Material3 di layar existing — itu §11-15 (tactile buttons/switch/slider per-komponen) masih pakai default M3 look. Batch berikutnya: migrasi pemakaian di HomeScreen/SwipeScreen/SettingsScreen ke komponen baru ini bila ingin 100% tactile-glass look di setiap kontrol (saat ini baru level ColorScheme yang 100% sesuai spec, bukan level component).
- Dead tokens (belum dihapus, nunggu izin): `GraphiteSurface`, `GraphiteSurfaceRaised`, `GraphiteOutline`, `TextSecondary`, `TextMuted` di `Color.kt` sudah tidak direferensikan setelah override ini.

## GitHub Release
- Workflow sekarang publish ke GitHub Release (`softprops/action-gh-release@v2`), bukan cuma Actions Artifact — APK signed akan muncul di sidebar repo (tag `v1.0.<run_number>`).
- `permissions.contents` dinaikkan dari `read` → `write` (wajib untuk membuat Release).

## Protected Assets (jangan hapus/replace penuh)
- app/build.gradle.kts, build.gradle.kts, settings.gradle.kts
- app/src/main/AndroidManifest.xml
- .github/workflows/build.yml
- .gitignore
- release.keystore (tidak disertakan di repo, via secrets)

## Protected Assets (jangan hapus/replace penuh)
- app/build.gradle.kts, build.gradle.kts, settings.gradle.kts
- app/src/main/AndroidManifest.xml
- .github/workflows/build.yml
- .gitignore
- release.keystore (tidak disertakan di repo, via secrets)

## Perubahan Batch Ini
1. Fix `app/build.gradle.kts` — tambah `}` yang hilang pada signingConfigs.release (baris ~61-62), penyebab `Expecting '}'` di line 123.
2. Update `.github/workflows/build.yml` — step build sekarang tee output ke `test-result-<branch>-attempt-<run_attempt>.log` dan upload sebagai artifact HANYA jika job gagal (`if: failure()`), agar log kegagalan berikutnya tinggal diambil dari GitHub Actions Artifacts tanpa perlu re-run.

## Belum Dikerjakan / Catatan
- 4 GradleException guard clause (keystore path/password/alias/key password) sudah benar strukturnya setelah fix Batch1 — perlu verifikasi ulang via CI run berikutnya.
- Migrasi screen-level ke komponen tactile/glass baru (§11-15 per-component look) — lihat catatan Theme System di atas.
- Approval dibutuhkan untuk hapus dead color tokens di Color.kt (lihat daftar di atas).
