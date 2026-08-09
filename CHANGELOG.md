# CHANGELOG

## v17_Batch17 — 2026-08-09
- Feature (ROADMAP Fase A, item 2): **3rd swipe action "Organize"** — move current photo(s) to a folder of choice, distinct from Keep/Delete.
- **Koreksi audit**: `ROADMAP.md` (Batch15) mengklaim `MediaDataSource` sudah punya primitive `moveTo` — diverifikasi ulang batch ini via grep, klaim itu SALAH (hanya cocok `Cursor.moveToNext()`, API tak terkait). Tidak ada primitive move sebelumnya. Dikoreksi di `ROADMAP.md`, dibangun dari nol di sini.
- NEW `data/media/MoveHelper.kt`: `moveTo(context, item, targetRelativePath)` — API 29+ via update `RELATIVE_PATH` (memindahkan file fisik, bukan cuma metadata); API 24-28 via direct file move (`File.renameTo`/copy+delete fallback) + update kolom `DATA` + `MediaScannerConnection.scanFile`. `RecoverableSecurityException` ditangani identik pola `ImageCompressor`/`DeleteHelper` yang sudah ada.
- `SwipeScreenControls.kt`: `ActionButtonRow` — param baru `onOrganize: (() -> Unit)? = null`, tombol ke-3 (folder icon, size 48dp, di antara Skip dan Keep) muncul hanya bila disediakan. `OrganizeFolderDialog` baru — pilih dari folder existing (radio list) atau ketik folder baru.
- `SwipeScreenGrid.kt`: `GridSelectContent` — param baru `pendingOrganizedIds`/`onOrganizeSelected`, tombol "Organize N" di action bar grid mode (di samping Compress/Delete).
- `SwipeScreen.kt`: state `pendingOrganized`/`organizeTarget` baru — item yang di-organize dikeluarkan dari alur swipe/grid sama seperti `pendingDeletes` (skip, bukan re-review), tapi TIDAK masuk `onFinishWithDeletions` (bukan delete). Dialog dipicu dari tombol swipe tunggal maupun grid multi-select.
- `MainActivity.kt`: `performOrganize(items, targetFolder)` — API 30+ pakai `MediaStore.createWriteRequest` batch (1 dialog sistem untuk seluruh seleksi, pola sama `performCompression`); di bawahnya per-item `RecoverableSecurityException` + retry launcher terpisah (`organizeRequestLauncher`). `applyOrganizeResult` update `relativePath`/`bucketName` item yang berhasil dipindah langsung di `allMedia` (bukan dihapus dari list — organize bukan delete, total library size/count harus tetap sama). `existingFolders` (distinct `relativePath` dari `activeMedia`) diteruskan ke dialog sebagai saran folder.
- Verifikasi: brace/paren balanced 0/0 di 5 file (1 baru + 4 diedit). Protected assets tak tersentuh. Manifest tidak perlu diedit — `WRITE_EXTERNAL_STORAGE` (maxSdk 28) sudah cukup untuk path legacy, RELATIVE_PATH API29+ tidak butuh permission tambahan di luar yang sudah ada.
- Belum disentuh: item 3 (Cleanup goal), item 4 (verifikasi Sort di layar Swipe) — next batch. Filmstrip belum secara visual meredupkan item yang sudah di-organize (kosmetik minor, functional correctness tidak terpengaruh — lihat PROJECT_STATE).

## v16_Batch16 — 2026-08-09
- Feature (ROADMAP Fase A, item 1): **Random clean mode**. `SettingsStore` — `randomModeEnabledFlow`/`setRandomModeEnabled` (default false, persisted). `HomeScreen` top bar — ikon Shuffle toggle cepat (tinted primary saat aktif), berdampingan dengan entry di `SettingsScreen` bagian baru "Swiping" (switch, sama persis setting yang dipersist, sesuai roadmap "toggle di HomeScreen/Settings").
- `MainActivity.kt` — `onGroupClick` sekarang shuffle `group.items` (`MediaGroup.copy(items = ...shuffled())`) sebelum masuk `SwipeScreen` bila mode aktif; `group.key` tidak diubah (ProgressStore tetap jalan by key). Reshuffle terjadi tiap kali folder dibuka (bukan sekali lalu di-cache) — tradeoff sadar: index tersimpan di `ProgressStore` untuk group.key itu hanya akurat dalam 1 sesi acak yang sama, didokumentasikan di doc comment `randomModeEnabledFlow`.
- Verifikasi: brace/paren balanced 0/0 di 4 file yang diedit (`SettingsStore.kt`, `HomeScreen.kt`, `SettingsScreen.kt`, `MainActivity.kt`). Protected assets tak tersentuh. `SwipeDecision`/`MediaModels.kt` tidak diubah — item ini murni ordering, bukan aksi baru (beda dari item 2 roadmap "Organize").

## v15_Batch15 — 2026-08-09
- Add: `ROADMAP.md` — roadmap strategis vs kompetitor "Sponge - Gallery Cleaner" (riset web search real per tanggal batch). Gap analysis jujur: fitur kita yang sudah unggul (app lock, crash logger, 3 gaya tema, smart category/on-this-day/largest-files card) vs gap nyata (random mode, 3rd swipe action organize, cleanup goal, multi-bahasa, duplicate detection). 4 fase roadmap + definisi "sukses" yang terukur, bukan checklist kosong.

## v14_Batch14 — 2026-08-09
- Cleanup (approved pending item dari Batch12): hapus 10 dead color token di `Color.kt` (`GraphiteBg`, `GraphiteSurface`, `GraphiteSurfaceRaised`, `GraphiteOutline`, `TextPrimary`, `TextSecondary`, `TextMuted`-lama, `AccentGold`, `SageKeepDim`, `CoralDeleteDim`) — 0 referensi nyata setelah diverifikasi grep lintas seluruh project. `SageKeep`/`CoralDelete` dipertahankan (masih dipakai `Theme.kt` primary/secondary + swatch picker).
- Audit (bukan eksekusi): 17 titik `Button`/`TextButton`/`OutlinedButton` M3 di 8 file diperiksa untuk kandidat cascade `MidnightSkeuoButton` — ditemukan tidak aman untuk swap langsung tanpa extend API (lihat PROJECT_STATE "Belum Dikerjakan"), jadi TIDAK dieksekusi batch ini, menunggu keputusan user.

## v13_Batch13 — 2026-08-09
- FULL Theme Override (klarifikasi user: hapus total, bukan partial): 11 file sistem tema/komponen lama dihapus (`GlassTokens.kt`, `TactileTokens.kt`, `SkeuoTokens.kt`-Cyan, `GlassCard.kt`, `GlassSurface.kt`, `GlassNavigation.kt`, `TactileButton.kt`, `TactileSlider.kt`, `TactileSwitch.kt`, `SkeuoModifier.kt`-lama, `SkeuoDarkButton.kt`-lama). Digantikan 100% oleh spec "Skeuomorphism-Dark Midnight Blue Edition": 4 file baru (`SkeuoMidnightTokens.kt`, `SkeuoMidnightModifier.kt`, `MidnightSkeuoButton.kt`, `MidnightSkeuoSlot.kt`) + `Theme.kt` (`SignatureDark` ditulis ulang total, 0 referensi token lama tersisa).
- Fix: `SettingsScreen.kt` — 3× `TactileSwitch` → M3 `Switch` (spec baru tidak sediakan varian switch), `ThemeStyleCard` direvert dari `GlassCard` ke Row manual (tetap theme-agnostic untuk 3 opsi picker), preview swatch "Signature" pakai `SkeuoMidnightTheme.BaseSurface`.

## v12_Batch12 — 2026-08-09
- Theme Override (Skeuomorphism-Dark, dari upload user `Panduan_Skeuomorphism___Dark_Kotlin.md`): 3 file baru — `ui/theme/SkeuoTokens.kt` (§2 palette + §4 metallic brush), `ui/components/SkeuoModifier.kt` (§3 `Modifier.skeuomorphicDark`, drawBehind+setShadowLayer), `ui/components/SkeuoDarkButton.kt` (§5 tombol contoh) — logic 100% copy dari spec. `Theme.kt` `SignatureDark`: background/surface/surfaceVariant/tertiary di-override ke token baru (DarkShadow/DarkSurface/LightHighlight/AccentNeon); primary/secondary (Keep/Delete) & SignatureLight tidak disentuh. **Catatan cakupan**: komponen Glass/Tactile existing (GlassCard, TactileButton, dst) hardcode token lama langsung — belum ikut berubah visual, itu next batch terpisah (lihat PROJECT_STATE).

## v11_Batch11 — 2026-08-09
- Refactor (GlassSurface API extension, scope: `GlassSurface.kt`+`GlassCard.kt`+`SettingsScreen.kt`): tambah param `borderWidth` ke `GlassSurface`, tambah `shape`/`borderColor`/`borderWidth` ke `GlassCard` (semua default identik behavior lama → 0 breaking change). `ThemeStyleCard` di SettingsScreen dikonversi dari Row manual → `GlassCard`, radius dipertahankan 14dp (bukan default 18dp) untuk 0 regresi visual, selection border (2dp primary vs 1dp GlassBorder) tetap sama persis. Background sekarang pakai token Glass resmi menggantikan `surfaceVariant` hardcode.

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
