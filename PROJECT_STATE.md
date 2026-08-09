# PROJECT_STATE — GalleryCleaner

## Versi Saat Ini
v22 — Batch22 (Atomic Change: Glassmorphism component cascade — Card+Button, seluruh app)

## Glassmorphism Component Cascade (Batch22, Atomic Change — 9 file)
Permintaan user: 1 batch atomic change berisi SEMUA bagian yang belum
terjangkau rewrite Batch21 ("Component level NOT yet done" — lihat section
Batch21 di bawah), plus home screen wajib terlihat glass bukan flat seperti
di screenshot yang dilampirkan user. Melebihi batch limit 10 file/1 modul
biasa — dikecualikan sesuai aturan "Atomic Change" (1 perubahan visual
kohesif, sama pola `Surface(color=colorScheme.surface/surfaceVariant,
shape=RoundedCornerShape(20.dp))` diulang di banyak file, harus konsisten
diganti bersamaan atau tidak sama sekali; setengah-setengah akan membuat
sebagian layar glass dan sebagian flat, terlihat seperti bug bukan desain).

**Root cause temuan (bukan cuma "komponen belum dipasang")**: `Scaffold`
tiap layar (`HomeScreen`/`TrashScreen`/`SettingsScreen`/`SwipeScreen`/
`OnboardingScreen`) pakai `containerColor = MaterialTheme.colorScheme.background`
SOLID. `MainActivity.kt` root `Surface` sudah melukis ambient gradient
(`MidnightGlass.AmbientGradient`) sejak Batch21, TAPI Scaffold di atasnya
mengecat solid tepat di atas gradient itu — jadi gradient tidak pernah
benar-benar terlihat di balik konten, persis seperti yang ditunjukkan
screenshot user (background rata gelap, bukan glow biru). Ini penyebab
utama kenapa app "masih terlihat flat" walau ColorScheme sudah 100% diganti
Batch21 — bukan sekadar Card/Button yang belum di-cascade. Diperbaiki di
SEMUA Scaffold: `containerColor = Color.Transparent` (root gradient kini
tembus), `TopAppBar` `containerColor` → `colorScheme.background.copy(alpha
= 0.72f)` (translucent, meniru toolbar kaca iOS tanpa `Modifier.blur` —
tetap konsisten `minSdk=24`, lihat doc comment `MidnightGlassTokens`).
Untuk tema Amber Reserve/Indigo Noir (tidak dapat gradient dari root,
`glassBackdrop == null` sehingga root `Surface` tetap solid
`colorScheme.background`), hasil render identik dengan sebelumnya — 0
regresi visual di 2 tema itu.

**GlassCard.kt — extend API (non-breaking)**: tambah `onClick: (() ->
Unit)? = null` + `enabled: Boolean = true`. `clickable` diterapkan SETELAH
`.glassPanel(...)` di modifier chain (bukan digabung ke `modifier` yang
di-pass caller, yang diterapkan SEBELUM glassPanel) — pola yang sama persis
sudah dipakai `GlassButton` sejak Batch21, supaya ripple/indication kelihatan
di ATAS lapisan kaca, bukan tertimbun di bawahnya (kalau clickable duluan,
background glass akan menggambar ulang di atas ripple dan menyembunyikannya).

**8 titik `Surface(color=surface/surfaceVariant, shape=RoundedCornerShape(20.dp))`
→ `GlassCard`** (semua diberi `contentPadding = 0.dp` karena Column/Row di
dalamnya sudah punya padding manual sendiri — set 0 di GlassCard mencegah
double-padding, bukan oversight):
- `HomeScreenSections.kt`: `LargestFilesCard`, `StorageDashboard`,
  `ScanTriggerRow` (`enabled = !scanning` diteruskan ke `GlassCard`, ganti
  `.clickable(enabled=...)` lama), `SmartCategoryRow`.
- `HomeScreenFolderRow.kt`: `GroupRow` — ini row bulan/folder yang persis
  terlihat di screenshot 1 user ("Agustus 2026" dst).
- `SwipeScreenControls.kt`: `FinishedPanel` stat row (Surface tanpa
  `fillMaxWidth`, sengaja dipertahankan wrap-content sama seperti aslinya).
- `SwipeScreenGrid.kt`: bottom action bar (Compress/Organize/Delete).
- `TrashScreen.kt`: bottom action bar (Restore/Delete permanently).

**1 titik chip kecil → `Modifier.glassPanel()` langsung (bukan GlassCard)**:
`SwipeScreenControls.kt` `InfoChip` — border/elevasi lebih tipis
(`elevation=3.dp`, `borderWidth=0.5.dp`) daripada default GlassCard, karena
menumpuk di atas foto preview, bukan panel berdiri sendiri. Pakai
`glassPanel` mentah (bukan GlassCard) supaya bisa override kedua parameter
itu tanpa nambah lagi parameter opsional ke GlassCard yang tidak akan
dipakai di tempat lain.

**7 titik `Button`/`OutlinedButton` non-semantik → `GlassButton`** (dari 26
total titik Button di seluruh app — sisanya SENGAJA tidak disentuh, lihat
"Tidak disentuh" di bawah):
- `OnboardingScreen.kt` — "Next"/"Get Started" (CTA utama flow onboarding).
- `SwipeScreenControls.kt` — "Continue" di `FinishedPanel`.
- `SwipeScreenGrid.kt` — "Compress N", "Organize N" (OutlinedButton → GlassButton).
- `TrashScreen.kt` — "Restore" (OutlinedButton → GlassButton, bottom bar).
- `MainActivity.kt` — "Grant access" (`PermissionScreen`), "Unlock"
  (`AppLockScreen`) — dua CTA full-width, `modifier = Modifier.fillMaxWidth()`
  diteruskan ke `GlassButton` (sudah punya `height(52.dp)` bawaan sendiri,
  `shape`/`height` manual lama di kedua situs ini dihapus, sudah didup GlassButton).

**Tidak disentuh (disengaja, didokumentasikan — 19 dari 26 titik Button,
plus beberapa Card/Surface)**:
- **10 tombol AlertDialog** (Save/Reset/Cancel/Set goal/OK/Move/Delete All,
  dst.) — precedent Batch14: tombol kecil di dalam dialog akan rusak
  proporsinya kalau diganti komponen "timbul"/kaca berukuran penuh; berlaku
  sama untuk GlassButton (dirancang 52dp height, bukan untuk dialog compact).
- **4 TextButton link gaya top-bar** (`HomeScreen` "Trash", `TrashScreen`
  "Empty Trash"/"Select all", `SwipeScreenGrid` "Select all") — ini teks-link
  minimalis khas toolbar iOS, bukan tombol kartu; menjadikannya GlassButton
  akan terlihat berlebihan di app bar yang sudah translucent.
- **3 tombol warna semantik delete** (`HomeScreenSections` "Clean up",
  `SwipeScreenGrid` "Delete N selected", `TrashScreen` "Delete permanently")
  — pakai `colorScheme.secondary` (CoralDelete) sengaja, precedent sejak
  Batch12/14: `GlassButton` tidak punya parameter warna, swap paksa akan
  menghilangkan sinyal delete yang app-critical.
- **`RoundActionButton`** (Keep ✓ / Delete ✕ / Skip ⏭ / Organize 🗂 di
  `SwipeScreenControls.kt`) — custom `Box`+`Canvas`, bukan `Button()`,
  warnanya semantik Keep/Delete/neutral; di luar cakupan grep Button( dan
  memang tidak boleh disentuh (precedent Keep/Delete sejak Batch2).
  circular action button ini sendiri sudah cukup "melayang" secara visual
  (bulat, shadow) — tidak butuh treatment glass tambahan.
- **`ExpiryBanner`, `PillChip`/`FilterChip`** — tetap `Surface`, warna
  semantik (secondary-tint warning / selected-state chip), bukan kandidat
  Card generik.
- **`SettingsScreen.kt` `ThemeStyleCard`** — masih pending (item lama sejak
  Batch11), butuh extend `GlassCard` dengan parameter `borderColor`/
  `borderWidth` dinamis untuk state "selected" sebelum aman dikonversi
  (persis alasan Batch11 kenapa `GlassSurface` diberi param border
  tambahan) — di luar cakupan permintaan user kali ini (fokus: home
  screen + cascade Card/Button generik), next batch kalau diminta.

**Verifikasi**: brace/paren balanced 0/0 di SEMUA file `.kt` project (sweep
penuh, bukan cuma file yang disentuh). Single call-site check: `GlassCard(`
8 titik pemanggilan (+1 definisi), `GlassButton(` 7 titik pemanggilan (+1
definisi) — sesuai rencana, tidak ada situs yang kelewat/dobel. Protected
assets (3 gradle, manifest, workflow, .gitignore) tidak tersentuh sama
sekali — 0 permission baru dibutuhkan, murni perubahan Compose UI.

## Versi Historis
v20 — Batch20 (ROADMAP Fase A item 4: Sort di layar Swipe — shipped. **FASE A SELESAI 4/4.**) + fix nama APK release

## Sort di Layar Swipe (Batch20) — Fase A selesai
Item terakhir Fase A di `ROADMAP.md`.

**Audit finding**: tidak seperti kasus `moveTo` (Batch17, klaim salah), kali
ini klaim roadmap ("perlu diverifikasi") memang perlu verifikasi murni —
dan hasilnya: sort SUDAH bekerja benar di SwipeScreen sejak awal. Alurnya:
`MainActivity`'s `LaunchedEffect(activeMedia, groupMode, sortOption)`
memanggil `MediaRepository.group(activeMedia, groupMode, sortOption)`, dan
`group()` MEMANGGIL `sortItems()` SEBELUM melakukan `groupBy` — jadi setiap
`MediaGroup.items` yang terbentuk sudah dalam urutan sortOption yang aktif
saat itu, SEBELUM pernah sampai ke `SwipeScreen`. Tidak ada bug, tidak ada
kode yang hilang. Diverifikasi dengan membaca `MediaRepository.kt` baris ke
baris, bukan asumsi/grep-dangkal (pelajaran dari kesalahan audit Batch15).

**Yang genuinely hilang**: kemampuan mengganti sort SAAT SEDANG di dalam
SwipeScreen, tanpa mundur ke Home dulu. Itu yang dibangun batch ini:
- `data/media/MediaRepository.kt`: `sortItems()` diubah dari `private` ke
  public — satu-satunya perubahan di file ini. `SwipeScreen` sekarang
  memanggil fungsi yang SAMA PERSIS yang dipakai `group()`, menghindari
  risiko dua implementasi sort yang perlahan-lahan drift beda hasil.
- `presentation/screen/SwipeScreen.kt`: param baru `sortOption: SortOption
  = SortOption.DATE`, `onSortChange: (SortOption) -> Unit = {}`.
  `val sortedItems = remember(group.items, sortOption) { MediaRepository.sortItems(group.items, sortOption) }`
  — SEMUA 13 referensi `group.items` di file ini diganti jadi `sortedItems`
  (grid multi-select, filmstrip, `currentItem`/`skipIds` lookup, info bar
  posisi, finished-panel reviewed count, prefetch 2-ahead). Ikon Sort baru
  di top bar (tersedia di kedua view mode Swipe & Grid) — `DropdownMenu`
  3 opsi dengan centang di opsi yang sedang aktif.
- **Reset posisi saat ganti sort mid-session (desain sadar)**: `index`
  adalah integer posisi ke dalam list. Kalau urutan list berubah (mis.
  dari Date ke Size), posisi lama menunjuk ke foto yang beda — tidak bisa
  dipertahankan begitu saja. `LaunchedEffect(sortOption)` dengan tracker
  `lastAppliedSort` (state terpisah dari prop `sortOption`) mendeteksi
  PERUBAHAN sebenarnya (bukan initial composition, yang nilainya sama
  dengan `lastAppliedSort` sehingga tidak memicu reset) dan reset
  `index`/`lastDecision`/progress ke 0. `pendingDeletes`/`pendingOrganized`
  TIDAK direset — keduanya `Set<Long>` berbasis id, bukan posisi, jadi
  aman dari reshuffle urutan apa pun.
- `MainActivity.kt`: `sortOption`/`onSortChange` diteruskan ke
  `SwipeScreen` menggunakan STATE GLOBAL yang sama dengan sort menu di
  Home (bukan state lokal terpisah untuk SwipeScreen) — ganti sort dari
  dalam SwipeScreen juga mengubah apa yang Home tampilkan berikutnya,
  konsisten dengan pola `groupMode`/`randomModeEnabled` yang sudah lebih
  dulu ada di app ini.
- Verifikasi: brace/paren balanced 0/0 di 3 file (`SwipeScreen.kt`,
  `MediaRepository.kt`, `MainActivity.kt`). Grep ulang `group\.items` di
  `SwipeScreen.kt` — 0 sisa referensi fungsional (2 match tersisa cuma
  komentar penjelasan). Single call-site check: `GridSelectContent(`/
  `Filmstrip(`/`SwipeScreen(` masing-masing 1 tempat pemanggilan.

**🎉 ROADMAP Fase A (tutup gap fungsional inti vs Sponge) SELESAI 4/4**:
Random clean mode (16), Organize/3rd swipe action (17, fix 18), Cleanup
goal (19), Sort di Swipe (20). Lanjut Fase B (AI on-device: duplicate
detection, blur detection, backup-before-delete) di batch berikutnya.

## Fix Nama File APK Release (Batch20, permintaan user)
User minta hash commit acak di nama file APK Release diganti kata
"Release". Sebelum: `GalleryCleaner-v1.0.22-3e0649f.apk` (lihat screenshot
GitHub Release v1.0.143 yang dilampirkan user — ironisnya versionName di
nama APK, `1.0.22`, juga tidak sinkron dengan nomor tag release
`v1.0.143`; itu 2 skema angka berbeda — `VERSION_NAME` dari
`git rev-list --count HEAD` vs tag dari `github.run_number` — TAPI ini
DI LUAR permintaan user, tidak disentuh, hanya dicatat sebagai temuan).
- `.github/workflows/build.yml` step "Rename APK": `OUT_NAME` sebelumnya
  `GalleryCleaner-v${VERSION_NAME}-${SHORT_SHA}.apk` (SHORT_SHA dari
  `git rev-parse --short HEAD`) → sekarang
  `GalleryCleaner-v${VERSION_NAME}-Release.apk`. Baris `SHORT_SHA=...`
  yang cuma dipakai di situ ikut dihapus (sudah tidak terpakai di step
  ini — `SHORT_SHA` di step "Build signed release APK" untuk nama
  `LOG_FILE` adalah variable shell LOKAL berbeda, terpisah, tidak
  tersentuh oleh perubahan ini).
- Protected asset (`.github/workflows/*`) — perubahan MINIMAL, cuma 1
  baris nama file, sisanya (build steps, secrets, keystore, signature
  verification, Release publishing) sama sekali tidak disentuh.
- Verifikasi: brace/paren balanced 0/0 di `build.yml`.
- **Catatan untuk verifikasi user**: efek baru terlihat di run CI
  berikutnya (release berikutnya akan bernama
  `GalleryCleaner-v1.0.144-Release.apk` atau serupa, bukan lagi diakhiri
  hash commit).

## Cleanup Goal (Batch19)
Mengeksekusi item 3 Fase A di `ROADMAP.md` — item terakhir yang kompetitor
(Sponge) sendiri belum ship per riset Batch15, jadi ini genuinely "duluan"
bukan cuma catch-up.
- `SettingsStore.kt`: `cleanupGoalBytesFlow`/`setCleanupGoalBytes(Long)`, key `cleanup_goal_bytes`. `DEFAULT_CLEANUP_GOAL_BYTES = 2_000_000_000L` (top-level const, dipakai juga sebagai default param di `HomeScreen`/`StorageDashboard` biar konsisten kalau flow belum ke-collect). Setter coerce `≥1L` — melindungi progress-bar division (`totalFreedBytes / cleanupGoalBytes`) dari divide-by-zero kalau user entah bagaimana set 0.
- `HomeScreenSections.kt` — `StorageDashboard` diperluas: baris "Cleanup goal" (tap → buka dialog) + `LinearProgressIndicator` modern (`progress: () -> Float` lambda API, sesuai compose-bom 2024.06.00 / Material3 1.2.x — bukan overload Float lama yang deprecated). Warna primary + pesan "Goal reached!" saat progress ≥100%. `CleanupGoalDialog` (private, sama file): slider 100MB..20GB + 5 preset chip (500MB/1/2/5/10GB), preset ke-highlight kalau slider persis di situ.
- `HomeScreen.kt`/`MainActivity.kt`: parameter tambahan diteruskan end-to-end (`cleanupGoalBytes`, `onCleanupGoalChange`), collect di `AppRoot` sejajar `totalFreedBytes`/`totalDeletedCount` yang sudah ada.
- **Desain sadar**: goal ditrack terhadap `totalFreedBytes` ALL-TIME (bukan per-bulan/per-minggu). Tidak ada auto-reset. Kalau user mau "goal baru bulan ini", mereka set ulang manual — konsisten dengan baris "All time: X freed" yang sudah lebih dulu ada di dashboard yang sama (kalau goal tracked periodik tapi baris di sebelahnya all-time, dua angka storage yang bersebelahan tapi beda basis waktu akan membingungkan).
- Verifikasi: brace/paren balanced 0/0 di 4 file. Single call-site untuk `StorageDashboard(`/`HomeScreen(`.

## Belum Dikerjakan (masih tertunda, prioritas berikutnya) — snapshot Batch18, LIHAT BAGIAN ATAS untuk status terkini
- ~~ROADMAP Fase A item 4~~ — ✅ shipped Batch20, Fase A selesai 4/4. Lanjut Fase B (AI on-device: duplicate/blur detection, backup-before-delete) — belum dimulai.
- Filmstrip belum secara visual meredupkan item yang sudah di-organize (Batch17, kosmetik minor, masih terbuka).
- Belum ada test end-to-end nyata untuk Organize (no emulator di sandbox) — sudah lolos 1x CI fix (Batch18), masih belum dikonfirmasi manual di device asli terutama jalur legacy API 24-28.
- ~~Batch10-19 belum ada 1 run CI hijau yang terkonfirmasi user~~ — ✅ terkonfirmasi Batch20: user melampirkan screenshot GitHub Release v1.0.143 sukses (APK 11.2MB ter-publish, signed, run142-ish). CI hijau sejak fix Batch18.
- **Item lama, masih menunggu keputusan user**: (a) cascade `MidnightSkeuoButton`/`MidnightSkeuoSlot` — butuh keputusan extend-warna vs cascade-parsial (detail lengkap di section Batch14 di bawah); (b) Phase-1b flat→sub-package restructure — masih butuh compiler nyata per-layer, tidak tersedia di sandbox.
- **Temuan baru (bukan diminta, sekadar dicatat)**: `VERSION_NAME` di nama file APK (dari `git rev-list --count HEAD`, mis. "1.0.22") tidak sinkron dengan nomor tag GitHub Release (dari `github.run_number`, mis. "v1.0.143") — dua skema angka berbeda dalam 1 workflow. Belum diminta user untuk disatukan, dibiarkan sampai ada instruksi eksplisit.

## Fix Batch18 Build Failure (dari log-fail_main_run141-attempt1_9282172.log, user)
- Error: `MainActivity.kt:629:21 Unresolved reference: applyOrganizeResult`, task `:app:compileReleaseKotlin` FAILED.
- Sebab: di Batch17, `fun applyOrganizeResult(...)` didefinisikan SETELAH `organizeRequestLauncher` — padahal callback lambda `organizeRequestLauncher` memanggilnya. Local function di Kotlin (beda dari top-level function) harus sudah ada di scope pada titik pemakaian, termasuk di dalam lambda yang baru dieksekusi belakangan — urutan deklarasi tekstual tetap dicek compiler.
- Fix: pindahkan blok `applyOrganizeResult` ke atas, sebelum `pendingOrganizeRetry`/`organizeRequestLauncher`/`performOrganize`. Isi fungsi tidak diubah sama sekali, murni reorder.
- Verifikasi: brace/paren balanced 0/0 di `MainActivity.kt`. Grep manual seluruh local fun lain (`performCompression`, `performPermanentDeletion`, `performOrganize`) — tidak ada pola forward-reference serupa di tempat lain.
- Log CI cuma menunjukkan 1 error (compiler Kotlin berhenti di error pertama untuk file itu) — tidak ada error kedua yang perlu diantisipasi setelah fix ini, tapi tetap perlu 1x run CI nyata untuk konfirmasi hijau (sesuai item "Batch10-14 belum dikonfirmasi hijau" — sekarang bertambah "Batch15-18 juga belum").

## Organize — 3rd Swipe Action (Batch17, kode — lihat fix di atas untuk build error-nya)
Mengeksekusi item 2 Fase A di `ROADMAP.md`.

**Koreksi penting terhadap riset Batch15**: `ROADMAP.md` sebelumnya menyatakan
`MediaDataSource` "sudah punya primitive `moveTo`, tinggal expose ke UI".
Diverifikasi ulang di batch ini dengan `grep -rn "moveTo" .` — satu-satunya
match adalah `Cursor.moveToNext()` di `MediaDataSource.kt`/`CrashLogger.kt`,
API Android bawaan untuk iterasi cursor, sama sekali tidak terkait dengan
memindahkan file. Tidak ada primitive move yang pernah ada di project ini
sebelum batch ini. Kesalahan riset Batch15 kemungkinan dari pattern-match
nama "moveTo" tanpa verifikasi isi function-nya. Sudah dikoreksi di
`ROADMAP.md`; catatan ini didokumentasikan agar tidak terulang.

**File baru:**
- `data/media/MoveHelper.kt` — `moveTo(context, item, targetRelativePath): Result`. Dua jalur:
  - API 29+ (`Build.VERSION_CODES.Q`): update kolom `RELATIVE_PATH` via `ContentResolver.update` — di scoped storage, ini benar-benar memindahkan file fisik, bukan cuma metadata (perilaku terdokumentasi Android, bukan asumsi).
  - API 24-28 (pre-scoped-storage, `WRITE_EXTERNAL_STORAGE` maxSdk 28 sudah ada di manifest): `File.renameTo` dengan fallback copy+delete lintas filesystem, lalu update kolom `DATA` + `MediaScannerConnection.scanFile` supaya gallery app lain langsung lihat lokasi baru.
  - `RecoverableSecurityException` ditangkap di kedua jalur, sealed `Result.NeedsPermission(sender)` — pola identik `ImageCompressor.compressInPlace`/`DeleteHelper`.
  - `supportsBatchWriteRequest()`: `SDK_INT >= 30`, sama cutoff `MediaStore.createWriteRequest`.

**File diedit:**
- `SwipeScreenControls.kt`: `ActionButtonRow` param baru `onOrganize: (() -> Unit)? = null` — tombol ke-3 (📁, 48dp) antara Skip dan Keep, muncul hanya kalau caller menyediakan (nullable, bukan breaking change untuk siapa pun yang belum pakai). `OrganizeFolderDialog` baru: radio list folder existing (dari `existingFolders`, dibatasi tampil 6 pertama) + text field folder baru, tombol Move disabled sampai ada target valid.
- `SwipeScreenGrid.kt`: `GridSelectContent` param baru `pendingOrganizedIds: Set<Long> = emptySet()`, `onOrganizeSelected: (() -> Unit)? = null` — item yang sudah di-organize ikut disaring dari `visibleItems` (sama seperti `pendingDeleteIds`), tombol "Organize N" muncul di action bar bila `onOrganizeSelected` disediakan.
- `SwipeScreen.kt`: state baru `pendingOrganized` (SnapshotStateList, sejajar `pendingDeletes`) + `organizeTarget` (item yang sedang menunggu pilihan folder di dialog). `currentItem`/`pendingDeleteIds` logic diperluas jadi `skipIds = pendingDeleteIds + pendingOrganizedIds` supaya alur swipe skip item yang sudah di-organize, sama seperti item yang sudah di-delete. **Sengaja TIDAK masuk `pendingDeletes`/`onFinishWithDeletions`** — organize bukan delete, kontrak `onFinishWithDeletions` (dipakai `MainActivity` untuk `trashStore.addToTrash`) khusus untuk item yang benar-benar akan ditrash. Param baru `existingFolders: List<String> = emptyList()`, `onOrganizeRequest: (List<MediaItem>, String) -> Unit = { _, _ -> }` (default no-op, non-breaking).
- `MainActivity.kt`:
  - `existingFolders` — `activeMedia.map{it.relativePath}.distinct().sorted()`, diteruskan ke `SwipeScreen` sebagai saran folder di dialog.
  - `performOrganize(items, targetFolder)` — API 30+: `MediaStore.createWriteRequest` untuk seluruh batch (1 dialog sistem, pola identik `performCompression`), retry lewat `organizeRequestLauncher` (launcher terpisah dari delete/compress — tiga pending-state independen, konsisten dengan alasan kenapa compress sudah punya launcher sendiri: satu launcher untuk dua state tidak bisa tahu sedang resume yang mana). API <30: loop per-item, stop di `RecoverableSecurityException` pertama, sisa item (yang belum sempat dicoba) dibawa ke retry setelah user grant izin — bukan retry seluruh batch dari awal (menghindari re-attempt item yang sudah berhasil).
  - `applyOrganizeResult(movedIds, targetFolder)` — update `relativePath`/`bucketName` item yang berhasil pindah langsung di `allMedia` in-place (`.map` + `.copy`), BUKAN filter-out seperti delete. Ini penting: organize tidak mengurangi total library, cuma pindah folder — kalau memakai pola delete (`filterNot`) maka `totalLibraryBytes`/dashboard stats akan salah turun padahal foto masih ada.

**Verifikasi:** brace/paren balanced 0/0 di 5 file (1 baru: `MoveHelper.kt`; 4 diedit: `SwipeScreenControls.kt`, `SwipeScreenGrid.kt`, `SwipeScreen.kt`, `MainActivity.kt`). Grep ulang memastikan `GridSelectContent(`/`ActionButtonRow(`/`SwipeScreen(` masing-masing cuma 1 call site (tidak ada caller lama yang kelewat di-update). Protected assets (manifest, 3 gradle, workflow, .gitignore) tidak tersentuh — tidak perlu permission baru.

**Belum sempurna (minor, next batch kalau perlu):**
- `Filmstrip` (di `SwipeScreenGrid.kt`) belum secara visual meredupkan/mencoret item yang sudah di-organize — functional correctness tetap benar (swipe flow `skipIds` sudah skip item itu), ini murni kosmetik, beda dari item yang sudah di-delete yang juga belum ditandai di situ (pre-existing, bukan regresi batch ini).
- Belum ada test end-to-end nyata (tidak ada emulator/compiler di environment ini) — perlu 1x build + manual test di device sebelum dianggap benar-benar solid, terutama jalur legacy (API 24-28) yang lebih jarang teruji di ekosistem modern.

## Belum Dikerjakan (masih tertunda, prioritas berikutnya) — snapshot Batch16, LIHAT BAGIAN ATAS untuk status terkini
- **ROADMAP Fase A item 3 — Cleanup goal**: ✅ shipped Batch19, lihat section di atas.
- **ROADMAP Fase A item 4 — verifikasi Sort di layar Swipe**: masih pending, lihat section "Belum Dikerjakan" teratas.
Mengeksekusi item pertama Fase A di `ROADMAP.md` ("tutup gap fungsional inti").
- `SettingsStore.kt`: `randomModeEnabledFlow`/`setRandomModeEnabled(Boolean)` — key baru `random_mode_enabled`, default `false`.
- `HomeScreen.kt`: param baru `randomModeEnabled: Boolean = false`, `onRandomModeToggle: (Boolean) -> Unit = {}`. Ikon Shuffle di top bar (antara Refresh dan Settings), tint primary saat aktif — quick toggle tanpa masuk Settings.
- `SettingsScreen.kt`: section baru "Swiping" (di atas "Feedback") dengan `Switch` yang bind ke setting yang sama persis — dua entry point, satu sumber kebenaran (DataStore), konsisten dengan pola existing (haptics, app lock, dll).
- `MainActivity.kt`: `onGroupClick` di `HomeScreen(...)` sekarang cek `randomModeEnabled` — bila aktif, `selectedGroup = group.copy(items = group.items.shuffled())` sebelum masuk `SwipeScreen`; bila tidak, group asli tanpa diubah. `randomModeEnabledFlow` di-collect di `AppRoot`, diteruskan ke `HomeScreen` + dipakai di shuffle logic.
- **Tradeoff sadar (didokumentasikan di doc comment `randomModeEnabledFlow`)**: reshuffle terjadi tiap kali folder dibuka, bukan sekali lalu dipersist per-folder. `ProgressStore` menyimpan index integer per `group.key` (bukan per-item), jadi resume setelah keluar-masuk ulang sebuah folder di mode random akan menempatkan index yang sama tapi urutan item yang berbeda (karena reshuffle baru). Ini disengaja — mode random secara sifat adalah "sampling ulang", bukan "lanjutkan urutan tetap"; behavior identik saat mode OFF (urutan asli, resume akurat) tidak berubah sama sekali.
- Verifikasi: brace/paren balanced 0/0 di 4 file yang disentuh. `group.key` tidak diubah oleh `.copy(items=...)` — semua fitur lain yang bergantung ke key (folder label, progress, trash) tidak terpengaruh.

## Roadmap Baru (Batch15, historis — lihat ROADMAP.md untuk status terkini)
- File baru: `ROADMAP.md` (root) — riset kompetitif "Sponge - Gallery Cleaner" (web search real, bukan asumsi) + audit jujur fitur project ini yang sudah setara/lebih unggul vs yang masih gap.
- 4 Fase: (A) tutup gap fungsional inti — random mode, 3rd swipe action "organize", cleanup goal (window peluang: Sponge sendiri baru rencanakan ini per Juli 2026); (B) diferensiasi AI on-device — duplicate detection, blur detection, backup-before-delete; (C) lanjutan kerja existing — keputusan MidnightSkeuoButton cascade, Phase-1b, CI hijau; (D) jangkauan pasar — multi-bahasa, monetisasi one-time-purchase, Play Store readiness.
- Lihat `ROADMAP.md` untuk detail lengkap + sumber riset. **Catatan (Batch17): item "backend moveTo sudah ada" di paragraf ini adalah klaim yang ternyata salah, dikoreksi di Batch17 — lihat section "Organize" di atas.**

## Versi Saat Ini (historis)
v14 — Batch14 (Cleanup: hapus 10 dead color token yang disetujui user, verifikasi 0 referensi)

## Dead Token Cleanup — Color.kt (Batch14)
User approve pending item dari Batch12 ("Approval dibutuhkan untuk hapus dead color tokens"). Diverifikasi ulang dulu (grep lintas SELURUH project, bukan cuma app/src) karena token dead ini terakumulasi dari 2 override tema berturut-turut (AMOLED Batch2, lalu Midnight Batch13) yang tidak pernah membersihkan sisa palet "Graphite" original:
- Dihapus (0 referensi nyata, hanya deklarasi diri sendiri di `Color.kt`): `GraphiteBg`, `GraphiteSurface`, `GraphiteSurfaceRaised`, `GraphiteOutline`, `TextPrimary`, `TextSecondary`, `TextMuted` (versi top-level lama — beda dari `SkeuoMidnightTheme.TextMuted` yang masih dipakai penuh), `AccentGold`, `SageKeepDim`, `CoralDeleteDim`.
- Dipertahankan (masih dipakai `Theme.kt`/`SettingsScreen.kt`): `SageKeep`, `CoralDelete` (primary/secondary Signature + swatch picker) — sengaja tidak disentuh, app-critical Keep/Delete semantic, precedent sejak Batch2.
- `Color.kt`: 73 baris → 46 baris. Palet Amber Reserve & Indigo Noir (2 theme style lain) TIDAK disentuh — semua tokennya masih aktif dipakai `Theme.kt`.
- Verifikasi: brace/paren balanced 0/0 di seluruh `app/src/**/*.kt` (bukan cuma file yang diedit), grep ulang pasca-hapus mengonfirmasi `SageKeep`/`CoralDelete` masih wired penuh.

## Belum Dikerjakan (masih tertunda, prioritas berikutnya)
- **ROADMAP Fase A item 2 — 3rd swipe action "Organize"**: `moveTo` primitive sudah ada di `MediaDataSource`, tapi belum diekspos ke `SwipeDecision` (baru `Keep`/`Delete`) atau UI (`SwipeScreenControls`/`SwipeCard`). Butuh: extend `SwipeDecision` sealed class, folder-picker dialog, wiring swipe-up gesture atau tombol ke-3. Lebih invasif dari random mode (Batch16) — batch terpisah.
- **ROADMAP Fase A item 3 — Cleanup goal**: target storage/jumlah foto + progress bar di HomeScreen. Belum ada model data untuk goal tersimpan (perlu `SettingsStore` key baru + UI slider/input + progress calculation dari `StatsStore`).
- **ROADMAP Fase A item 4 — verifikasi Sort di layar Swipe**: `SortOption` dipakai di Home, belum dicek/dipasang eksplisit di `SwipeScreen`/`Filmstrip`.
- **Cascade `MidnightSkeuoButton`/`MidnightSkeuoSlot` ke layar lain** — diaudit ulang Batch14: TERNYATA sebagian besar `Button(`/`TextButton(` di HomeScreen/SwipeScreen/TrashScreen/OnboardingScreen (17 titik, 8 file) TIDAK cocok jadi swap langsung. Alasan: (1) banyak adalah `TextButton`/`OutlinedButton` kecil di dalam AlertDialog (Cancel/OK/Reset) — mengubahnya jadi tombol skeuomorphic timbul 56dp akan merusak proporsi dialog; (2) satu `Button` di `HomeScreenSections.kt` (tombol "Clean up") sengaja pakai `colorScheme.secondary` (CoralDelete) untuk makna semantik delete — `MidnightSkeuoButton` dari spec tidak punya parameter warna (hardcode `RaisedGradient`+`TextMuted`/`ElectricCyan`), swap paksa akan menghilangkan sinyal warna Keep/Delete yang app-critical. Kesimpulan: cascade literal spec (tanpa extend API) TIDAK aman untuk 5+ dari 17 titik ini — butuh keputusan user dulu: (a) extend `MidnightSkeuoButton` dengan parameter warna opsional (di luar cakupan spec asli), atau (b) cascade hanya ke situs yang benar-benar netral/non-semantik. BELUM dieksekusi, menunggu arahan.
- Phase-1b (flat package → real sub-package) — masih butuh compiler nyata per-layer, tidak tersedia di environment ini.
- `IconButton`/`RadioButton`/`FilterChip` di `SettingsScreen.kt` masih M3 default — spec Midnight tidak menyediakan varian untuk itu.
- Batch10-14 belum dikonfirmasi hijau di CI — perlu push & cek run berikutnya.

## FULL Theme Override — Skeuomorphism-Dark Midnight Blue Edition (Batch13)
Klarifikasi user atas Batch12: "override" = hapus SEMUA konfigurasi tema lama, timpa 100% dengan 1 spec baru — bukan partial (ColorScheme saja, komponen lama dipertahankan berdampingan) seperti Batch12.
Sumber: `Panduan_Skeuomorphism_Midnight_Blue_Kotlin.md` (§1-5) diupload user.

**Dihapus total (11 file — bukan sekadar tidak dipakai, benar-benar dihapus dari repo):**
- `ui/theme/GlassTokens.kt`, `ui/theme/TactileTokens.kt`, `ui/theme/SkeuoTokens.kt` (versi Cyan Batch12)
- `ui/components/GlassCard.kt`, `GlassSurface.kt`, `GlassNavigation.kt`, `TactileButton.kt`, `TactileSlider.kt`, `TactileSwitch.kt`, `SkeuoModifier.kt` (lama), `SkeuoDarkButton.kt` (lama)
- Diverifikasi dulu (grep lintas seluruh project) sebelum hapus: hanya `SettingsScreen.kt` yang pernah memakai (`GlassCard`, `TactileSwitch`) — HomeScreen/SwipeScreen/TrashScreen/OnboardingScreen tidak pernah migrasi ke sistem lama ini, jadi penghapusan 0 breaking change di file-file itu.

**Dibuat baru (4 file, logic 100% copy dari spec §2-4, hanya package diadaptasi):**
- `ui/theme/SkeuoMidnightTokens.kt` — `object SkeuoMidnightTheme`: BaseSurface #0F172A, DarkShadow #050B14, LightHighlight #23324D, InnerShadowDark #070D18, InnerShadowLight #1E293B, ElectricCyan #00E5FF, TextMuted #94A3B8, TextBright #F8FAFC, + RaisedGradient/InsetGradient (Brush.linearGradient).
- `ui/components/SkeuoMidnightModifier.kt` — §3 `Modifier.skeuoMidnightRaised()` (dual setShadowLayer: DarkShadow bawah-kanan + LightHighlight atas-kiri) dan `Modifier.skeuoMidnightDebossed()` (InnerShadowDark/InnerShadowLight, arah dibalik untuk efek cekung) via Canvas native.
- `ui/components/MidnightSkeuoButton.kt` — §4A tombol timbul, elevasi 8dp→2dp saat ditekan, warna teks TextMuted→ElectricCyan saat pressed, ripple dimatikan (`indication = null`).
- `ui/components/MidnightSkeuoSlot.kt` — §4B container cekung/inset untuk slot nilai/input.

**`Theme.kt` — `SignatureDark` ditulis ulang total:** `background`→`DarkShadow`, `surface`→`BaseSurface`, `surfaceVariant`/`outline`→`LightHighlight`, `tertiary`→`ElectricCyan`, `onBackground`/`onSurface`→`TextBright`, `onSurfaceVariant`→`TextMuted`. Tidak ada satupun referensi token lama (`AmoledBlack`/`GlassBase`/`AccentBlue`/`AccentNeon`/dst) tersisa. `primary`/`secondary` (SageKeep/CoralDelete) TETAP tidak diubah — precedent sama sejak Batch2. `SignatureLight` TIDAK disentuh (spec Dark-only by definition).

**`SettingsScreen.kt` — 3 titik yang bergantung ke sistem lama, diperbaiki agar tetap kompilasi + konsisten:**
- 3× `TactileSwitch(...)` → M3 `Switch(...)` biasa (spec Midnight tidak menyediakan komponen switch — jujur mengikuti cakupan spec, bukan mengarang komponen baru; warna tetap otomatis ikut `colorScheme` yang sudah di-override).
- `ThemeStyleCard` (dipakai untuk memilih di antara Signature/Amber Reserve/Indigo Noir — bukan cuma Midnight) direvert dari `GlassCard` ke `Row` manual (`clip`+`background(colorScheme.surface)`+`border`+`clickable`) — SENGAJA tidak pakai `MidnightSkeuoSlot` di sini karena komponen itu visualnya spesifik-Midnight dan akan salah tampil saat user memilih Amber/Indigo.
- `previewBg` untuk opsi "Signature" di theme picker: `AmoledBlack` (dihapus) → `SkeuoMidnightTheme.BaseSurface`, deskripsi diperbarui.

**Verifikasi:** grep lintas seluruh project untuk 20+ nama token/komponen lama → 0 referensi kode tersisa (hanya komentar dokumentasi yang menyebut nama lama secara historis). Brace/paren balanced 0/0 di 6 file (4 baru + Theme.kt + SettingsScreen.kt). Protected assets (manifest, 3 gradle, workflow, .gitignore) utuh tak tersentuh.

**Belum dikerjakan (sama seperti sebelumnya, next batch):** `MidnightSkeuoButton`/`MidnightSkeuoSlot` belum dipasang menggantikan `Button`/`Card` M3 biasa di HomeScreen/SwipeScreen/TrashScreen/OnboardingScreen — baru level ColorScheme yang 100% ter-cascade otomatis ke semua layar (karena semua layar sudah pakai `MaterialTheme.colorScheme`, bukan warna hardcoded).

## Theme Override — Skeuomorphism-Dark (Batch12)
Sumber: `Panduan_Skeuomorphism___Dark_Kotlin.md` (165 baris, 6 section) diupload user, permintaan eksplisit: override tema sekarang, 100% sesuai spec markdown.
- **File baru (3), logic 100% copy dari spec, tidak ditulis ulang:**
  - `ui/theme/SkeuoTokens.kt` — §2 palette (`DarkSurface` #1E1F22, `DarkShadow` #0C0D0F, `LightHighlight` #2E3136, `AccentNeon` #00FFCC) + §4 `metallicDarkBrush` (procedural gradient, bukan bitmap texture).
  - `ui/components/SkeuoModifier.kt` — §3 `Modifier.skeuomorphicDark(cornerRadius, elevation)`, drawBehind + `setShadowLayer` 2x (drop shadow gelap bawah-kanan, highlight terang atas-kiri) via Canvas native — GPU-accelerated, bukan tumpukan Box+blur (§6.1/§6.2). Satu-satunya perubahan dari spec: `DarkShadow`/`LightHighlight` diimpor dari `ui.theme` (spec asli 1 file, project ini pisah token dari komponen, sesuai struktur `ui/theme/` vs `ui/components/` yang sudah ada).
  - `ui/components/SkeuoDarkButton.kt` — §5 `SkeuoDarkButton`, gabungan modifier+brush di atas. Catatan spec asli dipertahankan: `isPressed` masih placeholder (belum ada `pointerInput` ACTION_DOWN/UP nyata) — bukan bug baru, itu keterbatasan yang sudah ada di spec sumber.
- **`Theme.kt` — `SignatureDark` override:** `background`→`DarkShadow`, `surface`→`DarkSurface`, `surfaceVariant`→`LightHighlight`, `tertiary`→`AccentNeon` (accent/indikator, gantikan `AccentBlue`). `primary`/`secondary` (SageKeep/CoralDelete, Keep/Delete semantic) TIDAK diubah — aturan project yang sama sejak override tema pertama (Batch2), di luar cakupan spec visual manapun. `SignatureLight` TIDAK disentuh — spec ini "Dark" by name/definisi, sama seperti precedent AMOLED sebelumnya.
- **⚠️ PENTING — batas cakupan batch ini (jujur, bukan 100% visual cascade):** Override `ColorScheme` HANYA mengubah komponen yang baca dari `MaterialTheme.colorScheme` (Scaffold, TopAppBar, Text default, dll). `GlassCard`/`GlassSurface`/`TactileButton`/`TactileSwitch`/`TactileSlider`/`GlassNavigation` (dipakai di HomeScreen/SwipeScreen/SettingsScreen/TrashScreen) HARDCODE token dari `GlassTokens.kt` (`GlassBase`/`GlassElevated`/`GlassBorder`/`AccentBlue`) langsung, BUKAN lewat `colorScheme` — jadi visual translucent-glass-blur pada komponen itu TIDAK otomatis berubah jadi skeuomorphic solid-material+drawn-shadow dari batch ini saja. Ini bukan oversight — swap teknik render (translucent alpha-layer vs solid material+Canvas-drawn shadow/highlight) di 6 komponen bersama yang dipakai di semua layar adalah perubahan fondasi visual berisiko tinggi tanpa compiler nyata untuk verifikasi; dipecah jadi batch terpisah (lihat Belum Dikerjakan).
- Verifikasi: brace/paren balanced 0/0 di 4 file (3 baru + Theme.kt), tidak ada import yatim, `GlassTokens.kt` tidak disentuh (masih dipakai penuh oleh 6 komponen glass di atas).

## GlassSurface API Extension + ThemeStyleCard Migration (Batch11)
Scope batch ini: 3 file — `GlassSurface.kt`, `GlassCard.kt` (perluasan API), `SettingsScreen.kt` (migrasi `ThemeStyleCard`).
- `GlassSurface`: tambah param `borderWidth: Dp = 1.dp` (sebelumnya hardcoded `1.dp` di `.border(...)`). Default identik, jadi 0 perubahan visual untuk semua caller existing (`GlassCard`, `GlassNavigationBar` — keduanya pakai named args, tidak kena positional-arg breakage dari param baru).
- `GlassCard`: tambah 3 param opsional — `shape` (default tetap `ShapeCard`/18dp), `borderColor` (default tetap `GlassBorder`), `borderWidth` (default tetap `1.dp`) — semua diteruskan ke `GlassSurface`. Karena semua ada default value yang match behavior lama, 0 breaking change untuk pemanggil manapun (saat ini belum ada pemanggil selain batch ini sendiri).
- `SettingsScreen.kt` — `ThemeStyleCard` dikonversi dari Row manual (`clip`+`background(surfaceVariant alpha .5f)`+`border` M3 biasa) → `GlassCard` (shape dipertahankan `RoundedCornerShape(14.dp)` biar radius visual TIDAK berubah, bukan default `ShapeCard` 18dp — sengaja, supaya 0 regresi visual radius). Selection state (`selected`) tetap sama persis: `borderColor` primary vs `GlassBorder`, `borderWidth` 2dp vs 1dp — hanya sumber warna default (`GlassBorder`) yang sekarang dari token tema resmi, bukan `MaterialTheme.colorScheme.outline` lama. Background sekarang pakai token Glass resmi (`GlassBase`, via `GlassSurface` level 1) menggantikan `surfaceVariant.copy(alpha=0.5f)` — sesuai §14 "glass surfaces first", konsisten dengan sisa app.
- Isi Row internal (swatch dots, label, description, check icon) 100% tidak diubah — hanya dipindah ke dalam `content` lambda `GlassCard`, `Modifier` chain di Row itu sendiri dikosongkan (background/border/clip/clickable/padding lama dihapus dari Row, sekarang jadi tanggung jawab `GlassCard`).
- Import dibersihkan: `androidx.compose.foundation.border` dihapus (sudah tidak dipakai di file ini setelah migrasi), `GlassCard`+`GlassBorder` ditambah.
- Verifikasi: brace/paren balanced 0/0 di ketiga file, `GlassNavigationBar` (satu-satunya caller `GlassSurface` lain) dicek pakai named-args sehingga tidak kena breaking change dari param baru.

## Tactile Component Migration — SettingsScreen.kt (Batch10)
Scope batch ini: HANYA 3 pemakaian M3 `Switch(...)` di `SettingsScreen.kt` → `TactileSwitch(...)` (§12, `ui/components/TactileSwitch.kt`).
- API drop-in identik: `checked`, `onCheckedChange`, `modifier`, `enabled` — 0 perubahan logic, hanya nama composable + 1 import baru (`com.example.gallerycleaner.ui.components.TactileSwitch`).
- Diverifikasi: `Switch(` sudah 0 pemakaian tersisa di seluruh project (`grep -rl` across semua screen mengonfirmasi `SettingsScreen.kt` adalah satu-satunya file yang pernah pakai `Switch`), brace/paren balanced (0/0).
- `ThemeStyleCard` (custom Row+clip+background+border+clickable di file yang sama) SENGAJA belum dikonversi ke `GlassCard` — `GlassCard`/`GlassSurface` tidak punya parameter border-color/width dinamis untuk state "selected" (border primary 2dp vs outline 1dp yang dipakai sekarang), jadi konversi paksa akan menghilangkan visual selection indicator yang sudah berfungsi. Butuh perluasan API `GlassSurface` dulu (tambah `borderWidth` param) sebelum migrasi ini aman — next batch, bukan bagian atomic ini.
- `IconButton`/`RadioButton`/`FilterChip` di file yang sama TIDAK diubah — belum ada varian tactile/glass untuk itu di `ui/components/` (baru ada Button/Card/Switch/Slider/Surface/Navigation), migrasi butuh komponen baru dulu.
- File lain yang pakai `Card(`/`Button(` M3 asli (HomeScreen*, SwipeScreen*, TrashScreen, OnboardingScreen) belum diaudit di batch ini — next batch per-file, pola sama.


## God File Split — SwipeScreen.kt (Batch9)
822 baris → 4 file, teknik identik Batch7/8 (extract by exact line range, tidak ada logic ditulis ulang):
- `SwipeScreen.kt` (292 baris) — composable utama saja (top bar, state, orchestration Swipe/Grid mode).
- `SwipeScreenGrid.kt` (223 baris) — GridSelectContent, Filmstrip.
- `SwipeScreenControls.kt` (158 baris) — InfoBar, InfoChip, ActionButtonRow, RoundActionButton, FinishedPanel, StatColumn.
- `SwipeScreenCard.kt` (194 baris) — SwipeCard, FullscreenViewer, FileInfoDialog.
- 8 fungsi `private fun` → `internal fun` (dipanggil lintas file baru): GridSelectContent, Filmstrip, InfoBar, ActionButtonRow, FinishedPanel, SwipeCard, FullscreenViewer, FileInfoDialog. Sisanya (InfoChip, RoundActionButton, StatColumn, `private enum class SwipeViewMode`) tetap `private` — hanya dipanggil dalam file yang sama.
- `SWIPE_CARD_DECODE_SIZE` (const, dipakai di `SwipeScreen.kt` & `SwipeScreenCard.kt`) → `private` jadi `internal` karena lintas file. `SWIPE_THRESHOLD_PX`/`MAX_ROTATION_DEG` dipindah penuh ke `SwipeScreenCard.kt` (tetap `private`, hanya dipakai di situ) — duplikat lama di `SwipeScreen.kt` dihapus.
- Verifikasi: 11/11 fungsi (1 utama + 10 sub) terkonfirmasi ada, brace/paren balanced per file (0/0 di keempatnya), call-graph silang dicek manual, import per file di-trim ke yang benar-benar dipakai (bukan copy blok penuh) — dicek via analisis simbol otomatis lalu direview manual.
- Caller eksternal (`MainActivity.kt` — `SwipeScreen(...)`) tidak disentuh, signature publik `fun SwipeScreen(...)` identik.
- God file split sekarang selesai untuk kedua target awal (HomeScreen Batch8, SwipeScreen Batch9). Sisa file besar lain (bila ada) belum diaudit ulang.

## God File Split — HomeScreen.kt (Batch8)
1001 baris → 4 file, teknik sama seperti Batch7 (extract by exact line range, tidak ada logic ditulis ulang):
- `HomeScreen.kt` (361 baris) — composable utama saja (Scaffold, search state, LazyColumn orchestration).
- `HomeScreenSearch.kt` (126 baris) — SearchResultsContent, SearchPhotoGrid.
- `HomeScreenSections.kt` (384 baris) — ExpiryBanner, SectionLabel, LargestFilesCard, StorageDashboard, OnThisDayRow, ScanTriggerRow, SmartCategoryRow, FilterRow, PillChip.
- `HomeScreenFolderRow.kt` (235 baris) — GroupRow, RenameFolderDialog, CoverThumbnail, ProgressRing.
- Semua 15 sub-composable diubah `private fun` → `internal fun` (Kotlin: `private` top-level = file-scoped, jadi wajib `internal` biar bisa dipanggil lintas file dalam 1 module — ini SATU-SATUNYA perubahan kode selain lokasi file; isi fungsi 100% identik).
- Verifikasi: 16/16 fungsi (1 utama + 15 sub) terkonfirmasi ada, brace/paren balanced per file, call-graph silang (SectionLabel/GroupRow/PillChip/CoverThumbnail/ProgressRing/RenameFolderDialog dipanggil lintas file baru) dicek manual — semua sudah `internal`. Dicek juga: tidak ada file LAIN (SettingsScreen, MainActivity, dst) yang bergantung pada nama-nama ini (false positive `SettingsSectionLabel` dikecualikan).
- SwipeScreen.kt (822 baris) — SELESAI di Batch9 (lihat section di atas).

## God File Split — MediaRepository.kt (Batch7)
Scope batch ini: HANYA `MediaRepository.kt` (517 baris). HomeScreen(1001)/SwipeScreen(822) belum — itu Compose state extraction, jauh lebih berisiko tanpa compiler nyata, next batch terpisah.
- `MediaDataSource.kt` (baru, 150 baris) — raw MediaStore paging I/O: `loadAllMedia`, `loadMediaProgressively`, `queryMediaPage`.
- `MediaScanner.kt` (baru, 322 baris) — analytical/CPU-heavy scans: `smartCategories`, `onThisDay`, `findExactDuplicates`, `findBlurryPhotos`, `findNearDuplicates` + semua private helper (hash/decode/laplacian/aHash).
- `MediaRepository.kt` (107 baris) — jadi **facade tipis**: `group`/`sortItems`/`monthKey` tetap di sini (orkestrasi), 8 fungsi publik lain jadi one-line delegator ke MediaDataSource/MediaScanner.
- **Kenapa facade, bukan pindah caller**: semua caller existing (`MainActivity.kt`, `CleaningReminderWorker.kt`) tetap manggil `MediaRepository.xxx(...)` tanpa perubahan sama sekali — 0 file lain disentuh, 0 risiko missed call-site. Isi fungsi 100% copy-paste (bukan ditulis ulang) dari file lama, jadi behavior dijamin identik.
- Verifikasi: brace/paren balanced per file, 8/8 fungsi publik asli masih ada & bisa dipanggil dengan signature sama persis.

## Root Cause Crash Log (crash_20260809_074212_...txt, dari CrashLogger produksi user)
`java.lang.OutOfMemoryError` saat Compose recomposition di LazyColumn (grid HomeScreen/TrashScreen), heap target hanya 256MB (`android:largeHeap` belum diset). Titik crash (`MutableObjectIntMap.initializeStorage`, alokasi 40 byte) cuma korban terakhir — bukan penyebab asli; tekanan memori kumulatif dari bitmap cache + LazySaveableStateHolder yang menahan state item off-screen.
Fix: `AndroidManifest.xml` (protected, edit parsial) — tambah `android:largeHeap="true"`. Aman dilakukan sekarang karena `GalleryCleanerApp.kt` sudah pin Coil memory/disk cache ke `maxSizePercent` tetap (0.15/0.02), BUKAN ke memori "available" versi `ActivityManager` — jadi alasan lama untuk menghindari largeHeap (cache ikut membesar) sudah tidak berlaku (lihat komentar existing di file itu).
`MediaPreview.kt` / decode size / `lowMemory=true` di semua grid call-site sudah benar sejak awal — bukan bagian dari masalah.

## GitHub Actions — Artifact Log-Fail Rename
Nama artifact log kegagalan build diubah agar lebih informatif & unik per-run:
- Sebelum: `test-result-<branch>-attempt-<run_attempt>.log`
- Sesudah: `log-fail_<branch>_run<run_number>-attempt<run_attempt>_<short_sha>.log`
- Alasan: `run_number` + `short_sha` membuat tiap artifact unik lintas run (bukan hanya lintas attempt dalam 1 run), memudahkan lacak balik ke commit persis yang gagal.

## Status Build Terakhir
Batch1: FAILED→fixed. Batch2: FAILED(compile)→fixed Batch3. Batch4: FAILED(`onUncaughtException` typo)→fixed Batch5. Batch6: OK, build hijau (dikonfirmasi user). Batch7: OK, build hijau (dikonfirmasi user). Batch8+Batch9: OK, build hijau (dikonfirmasi user). Batch10, Batch11, Batch12 (ini): belum ter-CI.


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

## Theme System — CURRENT (Batch21): Glassmorphism, Midnight Blue Edition
- `AppTheme.SIGNATURE` (default) = `ui/theme/MidnightGlassTokens.kt` (`MidnightGlass`) + `ui/theme/Theme.kt` (`SignatureDark`/`SignatureLight`) + `ui/components/{GlassModifier,GlassCard,GlassButton}.kt`.
- ColorScheme level (background/surface/surfaceVariant/tertiary/outline) = 100% rewritten, applies automatically everywhere via `MaterialTheme.colorScheme`. `MainActivity.kt` root `Surface` also paints the ambient gradient backdrop (Signature only).
- Component level (NOT yet done, next batch if wanted): dashboard/list `Card`s in HomeScreen/SwipeScreen/TrashScreen still use plain M3 `Card` — swap to `GlassCard` per-screen for a fully "kaca" look on every panel, not just the ColorScheme base. `GlassButton` similarly not yet cascaded to the app's ~17 M3 `Button`/`TextButton` call sites (same open item as Batch14's audit, now against Glass API instead of Skeuo).
- Amber Reserve / Indigo Noir themes: untouched by this rewrite, still their original flat-color style (out of scope — user asked specifically about the default/Signature theme).
- No `Modifier.blur`/RenderEffect anywhere — deliberate, see `GlassModifier.kt` doc comment (API31+ only, `minSdk=24`).

## Theme System — HISTORICAL (superseded Batch21): AMOLED Hybrid Glassmorphism (compose-amoled-hybrid-glass-final.md)
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

## Perubahan Batch Ini (Batch1, historis)
1. Fix `app/build.gradle.kts` — tambah `}` yang hilang pada signingConfigs.release (baris ~61-62), penyebab `Expecting '}'` di line 123.
2. Update `.github/workflows/build.yml` — step build sekarang tee output ke `test-result-<branch>-attempt-<run_attempt>.log` dan upload sebagai artifact HANYA jika job gagal (`if: failure()`), agar log kegagalan berikutnya tinggal diambil dari GitHub Actions Artifacts tanpa perlu re-run.
- 4 GradleException guard clause (keystore path/password/alias/key password) — diverifikasi struktur benar sejak fix Batch1; masih perlu 1x CI run hijau sebagai bukti final.

(Lihat "Belum Dikerjakan" di bagian atas file ini untuk daftar pending terkini — item lama di sini sudah diproses/superseded.)
