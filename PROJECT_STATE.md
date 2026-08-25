# PROJECT_STATE — GalleryCleaner

## 🔗 Rilis Terbaru
- GitHub Release (APK signed, siap install, muncul di sidebar repo): **https://github.com/FDzaki-dev/GalleryCleaner/releases/latest**
- Publish otomatis tiap push ke `main` lewat `.github/workflows/build.yml` (`softprops/action-gh-release@v2`, tag `v1.0.<run_number>`) — bukan cuma Actions Artifact, `permissions.contents: write`.

## Versi Saat Ini
v57 — Batch57 (Rebranding tahap 3/3 — SELESAI: judul RELEASE_SIGNING.md + entry CHANGELOG.md baru, 2 file)

## Belum Dikerjakan (Prioritas Berikutnya)
- **REBRANDING Gallery Cleaner → Snaply — ✅ SELESAI (Batch55-57, kosmetik only)** — permintaan user eksplisit: "kosmetik only, haram hukumnya jikalau sampai mengacaukan workflow termux". Keputusan scope (assumption, belum dikonfirmasi user secara literal per-item, tapi konsisten sama instruksi "kosmetik only" + Protected Files + Termux gag-order), diarsipkan di sini buat referensi batch depan:
  - ✅ **DIGANTI** (7 file total lintas Batch55-57): `strings.xml` (`app_name`), `MainActivity.kt` (biometric title, permission rationale, lock screen text), `CleaningReminderWorker.kt` (notifikasi), `HomeScreen.kt` (top bar), `README.md`/`ROADMAP.md`/`RELEASE_SIGNING.md` (judul H1), `CHANGELOG.md` (entry baru).
  - ❌ **TETAP `GalleryCleaner`/`com.example.gallerycleaner` — SENGAJA TIDAK diganti** (ini alasan kenapa rebrand ini "kosmetik", bukan total):
    - **Package ID / applicationId** (`com.example.gallerycleaner`) — ganti ini = tiap file `.kt` kena sentuh (package declaration + import), risiko masif, 0 manfaat user-facing.
    - **Nama folder Termux/repo GitHub** (`GalleryCleaner`, PascalCase tanpa hyphen) — persis yang diperingatkan section 6 project ini: folder WAJIB statis, `-iname` di script Termux nyari nama ini. Ganti = script generate folder BARU salah (riwayat insiden serupa sudah pernah kejadian soal hyphen-mismatch, lihat catatan Batch46 di Riwayat Batch).
    - **`rootProject.name`** di `settings.gradle.kts` (protected file) — identifier Gradle-internal, gak pernah muncul ke user, gak ada manfaat ganti.
    - **Nama kelas/fungsi/resource internal**: `GalleryCleanerApp` (class), `GalleryCleanerTheme` (fun), `Theme.GalleryCleaner`/`Theme.GalleryCleaner.Splash` (style resource) — 0 visibility ke user, tapi kalau diganti WAJIB sinkron ke `AndroidManifest.xml` (protected) + `Theme.kt` + `themes.xml` sekaligus dalam 1 batch (pas 3 file, 0 slack buat typo). Ditunda tanpa izin eksplisit user.
    - **Folder penyimpanan on-device** (`BackupHelper.APP_FOLDER`/`CrashLogger.APP_FOLDER` = `"GalleryCleaner"`, dipakai buat path nyata `Pictures|Movies/GalleryCleaner/Backup` & `Documents/GalleryCleaner/logs/`) — ganti nama folder ini artinya user yang UPDATE (bukan install baru) bakal punya backup/log LAMA nyangkut di folder nama lama, gak ke-pindah otomatis. `SettingsScreen.kt`'s teks "...Movies > GalleryCleaner > Backup..." SENGAJA tetap match nama folder asli ini. STABILITY WINS.
    - `AUDIT_GAP.md` — filenya sendiri eksplisit bilang VERBATIM/tidak diedit dari upload asli user, gak disentuh sama sekali.
    - **Riwayat Batch lama** di `PROJECT_STATE.md`/`CHANGELOG.md` — fakta historis TIDAK ditulis ulang; cuma entry BARU ke depan yang pakai nama baru.
    - Nama kompetitor **"Sponge - Gallery Cleaner"** di `ROADMAP.md` — nama listing Play Store asli pihak ketiga, bukan brand kita, dibiarkan verbatim.
    - `ApkDownloader.kt`/`UpdateChecker.kt` HTTP `User-Agent: "GalleryCleaner-App"` — string internal ke GitHub API, 0 visibility user, ditunda (boleh nyusul kalau user minta eksplisit).
  - **Kalau user mau lanjut ke rebrand PENUH** (package ID/repo/folder ikut berubah) di masa depan — itu keputusan sadar terpisah, bukan bagian "kosmetik" ini, dan perlu langkah manual tambahan di Termux (rename repo GitHub + folder lokal) yang di luar cakupan Bash immutable script project ini.

- **AUDIT GAP TRACKER (mulai Batch38)** — 20 temuan P0/P1/P2. Sumber sekarang **`AUDIT_GAP.md` di root repo** (ditanamkan permanen Batch45 — sebelumnya cuma ada sebagai upload chat sesi lama, itu sebabnya sempat jadi BLOCKER di awal sesi ini). Dikerjakan bertahap per batch (bukan 1 batch raksasa — di luar batas 3-file/batch project ini). Status:
  - ✅ **P0 #3** (retention gak auto-eksekusi) — **Batch38**: `TrashExpiryWorker` baru, notifikasi saat item lewat retensi. **Catatan penting**: silent background delete TERBUKTI mustahil di scoped storage (`TrashStore`'s doc sendiri sudah bilang ini sejak sebelum Batch38) — `MediaStore.createDeleteRequest()` WAJIB dari foreground Activity + WAJIB dialog konfirmasi user di API30+. Jadi fix-nya notifikasi proaktif, bukan silent-delete (yang memang gak mungkin). Audit-nya benar soal gejala, tapi solusi "truly automatic" di deskripsi audit gak feasible di platform ini — didokumentasikan biar gak diulang gagal-paham di batch depan.
  - ✅ **P0 #4** (App Lock klaim vs implementasi) — **Batch39**: implementasi lama pakai `KeyguardManager.createConfirmDeviceCredentialIntent()` (deprecated, `@Suppress("DEPRECATION")`) yang cuma nunjukin layar PIN/pattern/password device — "biometric" di README/Settings selama ini cuma klaim, bukan fitur nyata. Fix: ganti ke `androidx.biometric.BiometricPrompt` dengan `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` — sekarang beneran nunjukin prompt sidik jari/wajah dulu, fallback otomatis ke screen lock kalau biometric gak ke-enroll/gagal. Ini fix nyata (bukan cuma benerin dokumentasi), karena implementasi lama emang gak sesuai klaim. Detail lengkap di Riwayat Batch di bawah.
  - ✅ **P0 #1** (video belum ke-scan) — **Batch40**: `MediaDataSource` sekarang query `MediaStore.Video.Media` selain `Images.Media`, `MediaItem` punya field `mediaType`/`durationMillis`, manifest+runtime permission `READ_MEDIA_VIDEO` ditambahkan, thumbnail video jalan lewat Coil `VideoFrameDecoder` (didaftarkan sekali di `GalleryCleanerApp`, otomatis berlaku di semua layar via `MediaPreview.kt`), play-badge ditambahkan di `MediaPreview.kt` sebagai satu-satunya penanda visual video. Detail lengkap + apa yang SENGAJA belum dikerjakan (duration label, tap-to-play, filter by type) di Riwayat Batch di bawah.
  - ✅ **P0 #2** ("Trash" bukan trash filesystem) — **Batch42**: investigasi dulu sebelum coding (konsisten pola P0 #3/#4). Cek 3 tempat yang mungkin overclaim: (1) `TrashScreen.kt` copy — udah akurat ("Trash (N)", "Empty Trash", "Delete permanently", warning eksplisit sebelum aksi final, gak pernah klaim file dipindah ke OS trash), (2) `SettingsScreen.kt` retention copy — udah akurat ("flag items in Trash for cleanup after:", bukan "auto-delete"), (3) `README.md` — 0 hit kata "trash" sama sekali, gak ada klaim yang perlu dikoreksi. Kesimpulan: gap-nya murni di level dokumentasi INTERNAL/arsitektur (`TrashStore.kt`'s doc comment), bukan di UX yang dilihat user — pattern "review queue + countdown + restore" yang dipakai app ini sama persis kayak "Recently Deleted" di app galeri lain, jadi bukan bug semantik. Fix: perkuat doc comment `TrashStore.kt` biar eksplisit sebut ini bukan `MediaStore.createTrashRequest()` (API30+, real OS trash), dan jelasin kenapa itu SENGAJA belum diadopsi (butuh re-query trashed items dari MediaStore via `MATCH_TRASHED`, flow untrash terpisah, dan penanganan item yang di-trash app lain muncul di queue — perubahan arsitektur, bukan fix kecil). Didokumentasikan sebagai kandidat batch terpisah kalau OS-level trash jadi requirement produk nyata, BUKAN dikerjakan sekarang biar gak "greedy".
  - ✅ **P1 #5** (progressive loading repeated List copy) — **Batch44**: `MainActivity`'s `allMedia = allMedia + page` bikin FULL copy list yang udah ke-load tiap 1 page baru masuk — O(n) per page, O(n²) total buat seluruh loading di library besar (ribuan-puluhan ribu item, per kata audit). Fix: `allMedia` tipe-nya jadi `PersistentList<MediaItem>` (kotlinx.collections.immutable) — `.addAll(page)` pakai structural sharing, bukan full-copy, jadi cuma O(page.size) per page bukan O(total size so far). Compose reactivity (LaunchedEffect keying, recomposition) TIDAK perlu diubah sama sekali — PersistentList tetep `List`, dan Compose bandingin value pakai `equals()` structural by default, jadi behavior lama 100% preserved, cuma cara nyimpen datanya yang lebih efisien. Detail lengkap (termasuk kenapa pin ke versi 0.3.8, bukan versi terbaru) di Riwayat Batch di bawah.
  - ✅ **P1 #6** (duplicate scan mahal untuk library besar) — **SELESAI, stage 2b Batch52** (final stage). Riwayat: stage 1 (Batch45, persistent hash cache) → stage 2a (Batch47, `yield()` tiap 20 item + `onProgress` callback backend) → **stage 2b (Batch52, 3 file)**: `MainActivity.kt` — duplicate-scan dicabut dari auto-scan `LaunchedEffect(activeMedia)` (dulu nyatu ke Quick Clean "Duplicate files" tiap kali `activeMedia` berubah), diganti manual (`duplicateScanState`/`duplicateScanProgress`/`duplicateScanJob` + `scanDuplicates()`/`cancelDuplicateScan()`, `Job` disimpan biar bisa `.cancel()`). `HomeScreen.kt` — 4 param baru (`duplicateScanState`, `duplicateScanProgress`, `onScanDuplicates`, `onCancelDuplicateScan`), row baru "Duplicate files" di section SMART DETECTION (sejajar Blurry/Similar photos). `HomeScreenSections.kt` — composable baru `CancellableScanTriggerRow` (progress% + tombol Cancel saat scanning), SENGAJA bukan nambah param opsional ke `ScanTriggerRow` yang udah ada (biar 2 call-site lama blur/near-dup 0 disentuh, sesuai Zero-Unnecessary-Refactor). `ScanState<T>` sepanjang `MediaModels.kt` TIDAK diubah (progress disimpan state terpisah `Float`, bukan payload di `ScanState.Scanning`) — jaga di 3 file, hindari resiko ke blur/near-dup yang pakai class sama. Verifikasi: brace/paren balanced 3 file, `quickCategories`/`smartGroups` (dipakai section lain) gak kesenggol saat cabut blok duplicate dari LaunchedEffect, import `kotlinx.coroutines.Job` ditambah di `MainActivity.kt`.
  - ✅ **P1 #7** (near-duplicate cuma heuristik aHash, potensi false positive/negative) — **Batch54**: audit-nya minta hasil "selalu diposisikan sebagai suggestion, bukan confirmed duplicate" — bukan minta algoritma baru (aHash+Hamming-5 tetap, itu domain terpisah/lebih besar dari 1 batch 3-file). Fix: `SwipeScreen.kt` — banner `GlassCard` (komponen shared yang sama dipakai `SwipeScreenGrid.kt`/`SwipeScreenControls.kt`, jadi konsisten iOS-look/glassmorphism, 0 komponen baru) muncul di atas konten review (berlaku utk Swipe MAUPUN Grid view mode, ditaruh sebelum percabangan viewMode) HANYA saat `group.key == "Similar photos"` — string literal yang sama persis dipakai `HomeScreen.kt` bikin grup ini (pola yang sama juga dipakai "Blurry photos"/"Duplicate files", jadi bukan pola baru). Sengaja key di `group.key`, bukan `displayName` (yang bisa ketiban custom folder label dari `folderLabels`) — biar notice ini gak pernah bisa ke-suppress. Teks: "Suggested matches, not confirmed duplicates — grouped by visual similarity. Review each photo before deleting." "Duplicate files" (`findExactDuplicates`, byte-for-byte hash asli) SENGAJA tidak dapat banner ini — itu memang confirmed, beda kelas dari near-dup. **Catatan efek samping**: nemu `.dp` (androidx.compose.ui.unit.dp) belum pernah dipakai di `SwipeScreen.kt` sebelum batch ini (semua dimension di file itu selama ini datang dari composable lain/tanpa literal dp langsung) — jadi importnya belum ada sama sekali; ditambahkan karena kode baru butuh, bukan refactor tak perlu. Verifikasi: brace/paren balanced (114/114, 193/193), GlassCard/Icon/Row/Spacer semua sudah ke-cover wildcard import yang ada (`androidx.compose.foundation.layout.*`, `androidx.compose.material3.*`) kecuali `Icons.Filled.Info` (baru) dan `GlassCard`+`dp` (baru, sudah ditambah). 0 file lain disentuh — P1 #8/9/10 (verification post-move, batch permission) beda domain sama sekali, gak numpang di batch ini.
  - ⏳ P1 #8-10, P2 #11-20 — belum disentuh, menyusul (urutan sesuai "PRIORITAS FIX" di `AUDIT_GAP.md`). **Catatan dari Batch40**: P2 #11 (hardcoded strings) sekarang juga mencakup kata "photo"/"foto" yang dipakai generik untuk semua item termasuk video di beberapa layar (mis. hitungan "X photos" di Home) — belum diubah, dicatat biar gak kelupaan saat P2 #11 dikerjakan. **Catatan dari Batch42**: kalau OS-level trash (`MediaStore.createTrashRequest()`) jadi requirement produk nyata di masa depan, itu batch tersendiri (lihat detail Batch42 di Riwayat Batch), bukan bagian dari P1/P2 manapun.- **Cleanup pending approval (Batch36)** — `SkeuoLiteTokens.kt` + `SkeuoModifier.kt` sekarang 100% unreferenced oleh `MaterialStyle`/tema manapun (`AMBER_RESERVE` pindah ke `NEUMORPH`), tapi TIDAK dihapus batch ini (butuh izin eksplisit user per aturan project). Ikut jadi dead-once-approved: 13 `val` warna lama di `Color.kt` (`EspressoSurface`, `EspressoOutline`, `IvoryText`, `IvoryTextSecondary`, `BrassKeep`, `BrassKeepDim`, `BrassKeepOnLight`, `CreamBg`, `CreamSurfaceRaised`, `CreamOutline`, `EspressoTextPrimary`, `EspressoTextSecondary`, `EspressoBg`) yang cuma dipakai `SkeuoLiteTokens.kt`. Juga ditemukan (bukan dari batch ini, sudah mati dari sebelumnya): `EspressoSurfaceRaised`, `OxbloodDeleteDim` — 0 referensi di manapun. Semua di-flag di sini, tunggu izin user sebelum hapus.
- **ROADMAP Fase C item 9** — Phase-1b package restructure (flat → real sub-package `com.example.gallerycleaner.data.media` dst.) — masih pending, butuh compiler/CI nyata per-layer, tidak tersedia di sandbox ini.
- **ROADMAP Fase C item 8** (keputusan cascade `MidnightSkeuoButton`) — **superseded/tidak relevan lagi**: sistem Midnight Skeuo dihapus total sejak Batch21 (diganti Glassmorphism), lalu Batch27 memperkenalkan `SkeuoLite` terpisah untuk Amber Reserve. Tidak perlu keputusan lanjutan.
- **ROADMAP Fase D (belum dimulai)**: multi-bahasa (minimal ES + PT-BR), monetisasi premium one-time-purchase, Play Store readiness (privacy policy URL, Data Safety form, screenshot set, ASO description).
- Filmstrip (`SwipeScreenGrid.kt`) belum secara visual meredupkan item yang sudah di-organize — kosmetik minor, terbuka sejak Batch17.
- Belum ada test end-to-end manual di device asli untuk jalur legacy Organize (API 24-28) — belum ada emulator/compiler di sandbox ini.
- Temuan lama, belum ditindak (bukan diminta user): nama file APK (`VERSION_NAME` dari `git rev-list --count HEAD`) dan nomor tag GitHub Release (`github.run_number`) pakai 2 skema angka berbeda — dibiarkan sampai ada instruksi eksplisit.

## Protected Assets (jangan hapus/replace penuh)
- app/build.gradle.kts, build.gradle.kts, settings.gradle.kts
- app/src/main/AndroidManifest.xml
- .github/workflows/build.yml
- .gitignore
- release.keystore (tidak disertakan di repo, via secrets)

## Riwayat Batch (terbaru di atas)

### Batch57 — Rebranding tahap 3/3, SELESAI (2 file)
Penutup Batch55-56. Diganti:
- `RELEASE_SIGNING.md` — judul H1 "GalleryCleaner release signing" → "Snaply release signing". Sisa isi file (nama secret, penjelasan keystore) generik, gak nyebut brand — tidak disentuh.
- `CHANGELOG.md` — entry baru `## v56_Batch56` di paling atas (di bawah "Rilis Terbaru", di atas entry lama `v48_Batch48`) yang meringkas seluruh rebrand Batch55-56. Entry-entry lama TIDAK ditulis ulang (fakta historis).
Total rebrand lintas 3 batch: 7 file diganti (`strings.xml`, `MainActivity.kt`, `CleaningReminderWorker.kt`, `HomeScreen.kt`, `README.md`, `ROADMAP.md`, `RELEASE_SIGNING.md`) + 1 file entry baru (`CHANGELOG.md`). Daftar lengkap apa yang diganti vs SENGAJA tidak (package ID, folder Termux/repo, storage path on-device, class/theme internal, AUDIT_GAP.md, nama kompetitor) diarsipkan permanen di "Belum Dikerjakan" bagian atas file ini buat referensi kalau user minta rebrand lanjutan di masa depan.

### Batch56 — Rebranding tahap 2/N: Home top bar + judul README/ROADMAP (3 file)
Lanjutan Batch55. Diganti (3 file):
- `HomeScreen.kt` — `Text("Gallery Cleaner"...)` di top bar Home → "Snaply". Ini string UI in-app terakhir yang tersisa (grep ulang setelah batch ini bersih, 0 sisa "Gallery Cleaner"/"GalleryCleaner" di kode `.kt` yang benar-benar dirender ke user).
- `README.md` — judul H1 doc. Instruksi setup (`git clone`, `unzip`, nama folder lokal, URL repo) TIDAK disentuh — itu semua nama repo GitHub asli (`GalleryCleaner`), yang sengaja tetap sesuai keputusan scope Batch55.
- `ROADMAP.md` — judul H1 doc. **Perhatian khusus**: judul lama nyebut 2 hal beda yang keduanya mengandung kata "Gallery Cleaner" — project KITA (`GalleryCleaner`, no-space) vs nama listing app kompetitor pihak ketiga **"Sponge - Gallery Cleaner"** (ada space, nama Play Store asli mereka, muncul lagi di baris 9 sebagai tolok ukur riset). Cuma yang pertama diganti; nama kompetitor dibiarkan verbatim di kedua titik kemunculannya (mengubahnya akan jadi salah kutip nama app orang lain).
Verifikasi: grep manual tiap sisa "Gallery Cleaner"/"GalleryCleaner" di README.md/ROADMAP.md setelah edit — 3 kategori tersisa, semua by-design: URL repo GitHub, nama kompetitor "Sponge - Gallery Cleaner", dan folder storage on-device `Pictures|Movies/GalleryCleaner/Backup` (functional path, keputusan Batch55). `HomeScreen.kt` brace/paren balanced (97/97, 174/174). Sisa antrian: `RELEASE_SIGNING.md` + catatan `CHANGELOG.md` — lihat "Belum Dikerjakan".

### Batch55 — Rebranding tahap 1/N: Gallery Cleaner → Snaply (3 file)
User minta rebrand total tapi "kosmetik only, haram hukumnya jikalau sampai mengacaukan workflow termux". Scope decision lengkap (apa yang diganti vs sengaja TIDAK — package ID, folder Termux/repo, rootProject.name, class/theme internal, folder backup/log on-device, AUDIT_GAP.md, riwayat batch lama) didokumentasikan di "Belum Dikerjakan" bagian atas file ini, bukan diulang di sini.
Diganti batch ini (3 file, semua string yang benar-benar tampil ke user di layar/notifikasi):
- `strings.xml` — `app_name`: "Gallery Cleaner" → "Snaply" (ini yang muncul sebagai label launcher/nama app di system Settings).
- `MainActivity.kt` (protected, edit parsial, 3 titik): judul `BiometricPrompt` ("Unlock GalleryCleaner" → "Unlock Snaply"), teks alasan minta izin foto, teks layar "GalleryCleaner is locked" → "Snaply is locked".
- `CleaningReminderWorker.kt` — `setContentTitle` notifikasi reminder pembersihan.
Verifikasi: brace/paren balanced kedua file `.kt` (278/278, 504/504 dan 16/16, 49/49). Sisa antrian (Home top bar title, README/ROADMAP/RELEASE_SIGNING.md, catatan CHANGELOG) — lihat "Belum Dikerjakan".

### Batch54 — Audit Gap P1 #7: banner suggestion di near-duplicate review (1 file)
`SwipeScreen.kt` — banner `GlassCard` "Suggested matches, not confirmed duplicates — grouped by visual similarity. Review each photo before deleting." muncul di atas konten review (Swipe & Grid view mode) hanya saat `group.key == "Similar photos"` (persis string yang dipakai `HomeScreen.kt` bikin grup ini). Detail lengkap + kenapa "Duplicate files" sengaja TIDAK dapat banner ini + catatan import `.dp` yang baru ditambah, ada di tracker "AUDIT GAP TRACKER" di atas.

### Batch51 — In-app update dialog: compare versi + ringkasan singkat (2 file)
Diminta user: dialog update sekarang nunjukin compare versi terpasang vs versi baru, plus info singkat soal isi update — bukan nyuruh user lihat log/link changelog buat detail.
- `UpdateChecker.kt` — `UpdateInfo` dapat field baru `shortSummary`. Dibangun dari `buildShortSummary()` (fungsi baru, private): parse raw `body` dari GitHub `/releases/latest` (auto-generated lewat `generate_release_notes: true` di `build.yml`), buang baris `## ` header, buang suffix `by @user in <url>` per baris, buang baris `**Full Changelog**: <compare-url>` (link itu yang sebelumnya jadi satu-satunya "detail" yang ditawarkan — sekarang dibuang, diganti ringkasan bullet asli). Dibatasi 5 bullet / 320 karakter, fallback "New release available." kalau body kosong atau gak ada yang lolos filter. `releaseNotes` (raw) TETAP ada di data class (gak dihapus, cuma gak dipakai lagi di UI) — non-breaking, cuma 1 titik konstruksi `UpdateInfo` (di file ini sendiri), aman nambah field.
- `SettingsScreen.kt` — 2 perubahan: (1) `currentVersionName` baru, dibaca sekali via `context.packageManager.getPackageInfo(...).versionName` (bukan `BuildConfig.VERSION_NAME` — `buildFeatures.buildConfig` belum di-enable di `app/build.gradle.kts`, sengaja dihindari biar gak nyentuh protected file itu untuk task ini). (2) Dialog update (Available/Downloading/ReadyToInstall) sekarang nunjukin baris "Installed X → New Y" di atas, konsisten muncul di ketiga state pakai `newTagName` (pattern sama kayak `releaseName` yang udah ada). Body dialog state `Available` ganti dari raw `releaseNotes` ke `shortSummary` yang baru.
- **Sengaja TIDAK diubah**: baris subtitle inline di row "Check for update" (`"Version ${current.info.tagName} is available."`) — compare version paling relevan di dialog detail (tempat user mutusin download), bukan di baris ringkas Settings; nambahin di 2 tempat sekaligus dianggap scope creep di luar yang diminta.
- **Sengaja TIDAK diubah**: logic pembanding tag (`UpdateChecker.checkForUpdate`'s tag-string comparison) — itu udah didokumentasikan sengaja gak pakai perbandingan angka versi (lihat class doc `UpdateChecker.kt`, versionCode/tag pakai skema angka beda). Task ini murni soal apa yang DITAMPILKAN ke user, bukan logic "ada update atau nggak".
- Verifikasi: brace/paren balanced kedua file, 1 titik konstruksi `UpdateInfo` (aman nambah field non-optional), `currentVersionName` dibungkus try/catch `PackageManager.NameNotFoundException` (fallback "?", walau query package sendiri praktis gak pernah gagal).

## Riwayat Batch (terbaru di atas)

### Batch53 — In-app update disempurnakan (3 file)
Diminta user: "sempurnakan fitur update langsung pada aplikasi" — mengangkat item yang sebelumnya di-flag "opsional, belum diminta user" di Pending Queue (lihat Batch50/52).
- **Bug yang diperbaiki**: kalau orang download APK terus keluar dari Settings sebelum tap "Install", file `.apk` udah kepegang di disk dan tag-nya udah ke-mark known (`markTagAsKnown` dipanggil saat download SUKSES, bukan saat install SUKSES — sengaja, lihat class doc `UpdateChecker.kt`). Buka lagi Settings → tap "Check for update" → API bilang tag itu masih sama dengan `lastKnownTag` → "up to date", padahal belum pernah ke-install.
- `UpdateChecker.kt` — fungsi baru `getLastKnownTag(context)`: read-only getter ke pref yang sama yang ditulis `markTagAsKnown`.
- `ApkDownloader.kt` — fungsi baru `findDownloadedApk(context)`: scan folder `updates/`, hapus file `.part` yang nyangkut (selalu gak lengkap/gak kepake, biasanya udah dihapus di jalur error `download()` tapi bisa nyangkut kalau proses ke-kill duluan), lalu antara sisa file `.apk` ambil yang `lastModified()`-nya paling baru dan hapus yang lain (cegah numpuk kalau ada lebih dari 1 percobaan download yang ditinggal).
- `SettingsScreen.kt` — 3 perubahan:
  1. `LaunchedEffect(Unit)` baru di awal composable: begitu Settings dibuka, cek `findDownloadedApk` + `getLastKnownTag` — kalau dua-duanya ada, `updateState` langsung di-set `ReadyToInstall` (dialog "tap to install" otomatis muncul, pola yang sama kayak pas download baru selesai — bukan behavior baru).
  2. **Regresi yang HAMPIR kejadian, keburu ke-catch pas verifikasi**: kalau file di `updates/` gak pernah dibersihkan setelah instalasi BENERAN sukses, poin (1) di atas bakal nge-prompt "tap to install" TERUS-TERUSAN setiap buka Settings, walau versi baru udah keinstall — jauh lebih annoying dibanding bug aslinya. App ini gak punya cara handal deteksi "install sukses dikonfirmasi" (sudah didokumentasikan sebagai limitation arsitektur, lihat komentar `onDownloadUpdate` lama). Solusi pragmatis: `launchInstall()` (fungsi baru, dipakai di KEDUA jalur trigger install — tap langsung & jalur "izin install-source baru granted") jalanin intent install SEPERTI BIASA, lalu `scope.launch { delay(5000); file.delete() }` — delay (bukan hapus langsung) supaya system installer sempat baca file lewat FileProvider `content://` URI dulu sebelum sumbernya dihapus. Trade-off yang diterima: kalau orang batalin dialog konfirmasi install bawaan Android SETELAH titik ini, mereka perlu tap "Check for update" lagi buat re-download — minor, dan jauh lebih baik dibanding re-prompt permanen.
  3. Komentar lama di `onDownloadUpdate` yang nyebut limitation ini ("Known limitation... tidak diperbaiki di batch ini") diupdate — limitation-nya udah gak berlaku lagi, komentar sekarang nunjuk ke `LaunchedEffect(Unit)` yang baru.
- **Sengaja TIDAK diubah**: `installDownloadedApk()` (fungsi private yang benar-benar fire intent) — 0 logic-nya disentuh, cuma dibungkus lewat `launchInstall()` yang baru dari 2 call-site lama.
- Verifikasi: brace/paren balanced 3 file, `installDownloadedApk(...)` sekarang cuma 1 titik panggil (di dalam `launchInstall`) + 1 definisi — 2 call-site lama (`onInstallUpdate` else-branch, `installSourcePermissionLauncher` callback) udah dialihkan ke `launchInstall`, gak ada yang kelewat. Import `kotlinx.coroutines.delay` ditambah di `SettingsScreen.kt`.

## ⚠️ Insiden Operasional (permanen — bukan bagian Riwayat Batch, gak ada kode berubah)

### Batch46 — Termux PROJ_DIR case-mismatch bikin folder baru salah, bukan nemu folder existing
Setelah Batch45, command Termux yang dikasih ke user pakai `-iname "gallery-cleaner"` (lowercase-kebab-case) buat cari folder project lokal. Nama repo GitHub project ini ASLI-nya `GalleryCleaner` — PascalCase, TANPA hyphen (lihat URL rilis di paling atas file ini: `github.com/FDzaki-dev/GalleryCleaner`). `find -iname` cuma case-insensitive (huruf besar/kecil), BUKAN hyphen-insensitive — jadi `"gallery-cleaner"` (ada hyphen) tetap gak pernah match folder asli `GalleryCleaner` (gak ada hyphen), berapa pun kombinasi besar/kecil hurufnya. Pencarian gagal → fallback `mkdir -p ~/projects/gallery-cleaner` bikin folder BARU yang salah, bukan masuk ke folder project yang sudah ada.

**Root cause**: instruksi umum project ("Variabel [NamaFolderProyek] WAJIB 100% lowercase kebab-case", lihat bagian Termux Automation Commands) SALAH diterapkan ke project yang REPO-nya sudah ada dan sudah punya nama sendiri. Aturan kebab-case itu maksudnya buat project BARU (saat `gh repo create` pertama kali) — begitu repo sudah exist dengan nama tertentu (apa pun casing-nya), nama itu jadi fakta yang harus diikuti apa adanya, bukan diseragamkan ulang ke kebab-case.

**Dampak**: kemungkinan ada folder residu `~/projects/gallery-cleaner` di device user. Skrip daily-update TIDAK pernah nge-set git remote (`git remote add origin` cuma ada di skrip INITIAL SETUP) — jadi `git push` di folder salah ini pasti gagal duluan sebelum sempat nyentuh GitHub. Kesimpulan: 0 dampak ke remote/GitHub, tapi folder lokal residu tetap perlu dibersihkan manual (skrip pembersihan diberikan terpisah di chat, bukan bagian permanen file ini karena itu perintah housekeeping sekali-jalan, bukan source project).

**Fix permanen ke depan (WAJIB diikuti tiap generate command Termux buat project INI)**: pakai nama folder/repo PERSIS `GalleryCleaner` (PascalCase, tanpa hyphen) di setiap `-iname` dan fallback path — JANGAN otomatis di-kebab-case-kan lagi. Precedence: nama repo GitHub yang sudah eksis > aturan gaya penamaan umum di instruksi project.

## Riwayat Batch (terbaru → terlama)
Detail Batch2–4 belum granular di file ini — lihat `CHANGELOG.md` (urutan sama, terbaru di atas).

### Batch50 — In-app update, tahap 2/2: manifest + FileProvider + UI Settings (3 file)
Lanjutan Batch49 — sekarang fitur update jalan end-to-end dari tap sampai install.

File diubah/baru:
- `AndroidManifest.xml` (protected, edit parsial) — tambah `<uses-permission INTERNET>` (sebelumnya manifest ini 0 punya izin internet sama sekali — baru kepakai sekarang lewat `UpdateChecker`/`ApkDownloader`) + `<uses-permission REQUEST_INSTALL_PACKAGES>` + deklarasi `<provider android:name="androidx.core.content.FileProvider">` (`authorities="${applicationId}.fileprovider"`, `exported=false`, `grantUriPermissions=true`, meta-data nunjuk `@xml/file_paths`).
- `res/xml/file_paths.xml` (baru) — `<external-files-path name="updates" path="updates/" />`, persis map ke `ApkDownloader.UPDATE_SUBDIR` Batch49 (`getExternalFilesDir(null)/updates/`).
- `SettingsScreen.kt` — section baru "About" di bawah "Privacy" (row "Check for update", ikutin pola Row+Column+trailing-element yang sudah dipakai semua section lain di layar ini, BUKAN bikin komponen baru). State machine `UpdateUiState` (Idle/Checking/UpToDate/Available/Downloading/ReadyToInstall/Error) private di bawah file. Dialog pakai `AlertDialog` M3 biasa (BUKAN `ModalBottomSheet` — grep dulu sebelum nulis, seluruh project ini 0 pemakaian bottom sheet, semua dialog existing `AlertDialog`/`Dialog`, rounded-corner "iOS look" udah dateng otomatis dari `Theme.kt`/`Shape.kt` global, bukan per-dialog manual) — nampilin release notes saat `Available`, progress bar saat `Downloading` (`LinearProgressIndicator(progress = Float, ...)`, overload lama, dipilih SENGAJA di atas API lambda `progress = {...}` yang baru ada di Material3 versi lebih baru dari `compose-bom 2024.06.00` yang project ini pin — resiko gak-compile lebih rendah), tombol Install saat `ReadyToInstall`.
- Install flow: `installDownloadedApk()` (private, bawah file) — `FileProvider.getUriForFile()` → `Intent(ACTION_VIEW)` + `FLAG_GRANT_READ_URI_PERMISSION`. API 26+ dicek `canRequestPackageInstalls()` dulu; kalau belum, buka `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` lewat `rememberLauncherForActivityResult`, begitu balik dari situ langsung retry install otomatis kalau izin udah dikasih — user gak perlu tap "Install" 2x.

**Progress callback dari thread IO ke Compose state**: `ApkDownloader.download`'s `onProgress` jalan di `Dispatchers.IO` (lihat Batch49), tapi langsung nulis ke `updateState` (Compose `mutableStateOf`) tanpa `withContext(Main)` — ini AMAN, snapshot state Compose thread-safe buat write dan otomatis jadwalin recomposition di main thread sendiri; bukan kelalaian, sengaja dihindarin `withContext(Main)` per-tick biar gak nambah overhead di loop 8KB-per-chunk.

**Keterbatasan yang didokumentasikan, bukan bug**: `UpdateChecker.markTagAsKnown()` dipanggil begitu DOWNLOAD sukses (bukan begitu INSTALL sukses beneran, gak ada cara diverifikasi app ini balik lagi setelah installer system selesai). Kalau user download tapi batal install & keluar dari Settings, `updateState` (local `remember`, gak persisted) reset ke `Idle` pas screen ke-buka lagi — "Check for update" berikutnya bakal bilang up-to-date walau belum keinstall. File `.apk`-nya sendiri tetep ada di disk. Dicatat di Pending Queue sebagai polish opsional, bukan dikerjakan sekarang (di luar scope "tambahkan fitur update" yang diminta, dan bakal butuh file ke-4 buat batch ini).

Verifikasi: brace/paren balance `SettingsScreen.kt` 145/145 & 336/336 (dihitung ulang setelah edit, cocok). Manifest well-formed (provider nested benar di dalam `<application>`, sebelum `</application>`). FileProvider authority di manifest (`${applicationId}.fileprovider`) dan di kode (`"${context.packageName}.fileprovider"`) sama-sama resolve ke `com.example.gallerycleaner.fileprovider` (applicationId = packageName, project ini gak pakai product flavor/suffix). Compile sesungguhnya nunggu CI run berikutnya (gak ada compiler di sandbox ini).

### Batch49 — In-app update, tahap 1/2: UpdateChecker + ApkDownloader (3 file: 2 baru + 1 dependency)
Task baru dari user: "tambahkan fitur update langsung dari aplikasi". Fitur besar, HARD CAP 3-file/batch memaksa dipecah 2 tahap — tahap ini murni logic layer (network check + streaming download), BELUM bisa dipicu dari UI dan BELUM bisa trigger install (butuh `AndroidManifest.xml` + `FileProvider` + tombol UI, masuk Pending Queue di bawah, next batch).

File baru:
- `data/update/UpdateChecker.kt` — `suspend fun checkForUpdate(context)` hit `GET api.github.com/repos/FDzaki-dev/GalleryCleaner/releases/latest`, parse pakai `org.json` bawaan Android (bukan nambah Gson/Moshi, minim dependency). Cari asset pertama yang namanya berakhiran `.apk`, ambil `browser_download_url` + `size`.
- `data/update/ApkDownloader.kt` — `suspend fun download(...)` streaming APK ke `getExternalFilesDir(null)/updates/` **chunk-by-chunk pakai Okio** (`BufferedSource.read(Buffer, 8KB)` → `BufferedSink.write`, loop) — sesuai rule "Release Downloader (Anti-OOM)": **tidak pernah** panggil `.bytes()`/`.readBytes()` yang akan nge-load seluruh APK ke RAM sekaligus. Tulis ke `<nama>.apk.part` dulu, baru rename ke nama final setelah panjang yang ketulis match `Content-Length` — download yang keputus (timeout/koneksi drop/coroutine dibatalkan) gak pernah ninggalin file yang KELIHATAN lengkap padahal enggak.

Dependency:
- `app/build.gradle.kts` (protected, edit parsial — cuma nambah 1 baris `implementation`) — `com.squareup.okhttp3:okhttp:4.12.0`, bawa Okio secara transitif. `OkHttpClient` di kedua file di atas eksplisit set `connectTimeout`/`readTimeout`/`writeTimeout` + `followRedirects(true)` (GitHub `browser_download_url` redirect ke S3, WAJIB diikuti) sesuai rule.

**Keputusan desain penting — kenapa compare tag STRING bukan angka versi**: `BuildConfig.VERSION_NAME` app ini asalnya dari `git rev-list --count HEAD` (commit count), sedangkan tag GitHub Release asalnya dari `GITHUB_RUN_NUMBER` — dua counter independen buat rilis yang SAMA (ini persis temuan lama yang udah didokumentasikan di baris "Belum Dikerjakan" file ini: APK sempat nunjukin v1.0.44 sementara Release-nya udah v1.0.165). Karena itu, banding angka versi app vs angka tag itu gak bermakna — nomor run yang lebih besar BUKAN berarti "lebih baru dari versi commit-count saya". Betulin skema dual-counter itu di luar scope task ini (nyentuh workflow file buat alasan yang gak relevan ke fitur update). Solusinya: `UpdateChecker` simpen `tag_name` rilis terakhir yang udah dikasih tau ke user (SharedPreferences, 1 string) dan cuma bilang "ada update" kalau API balikin tag yang BEDA dari itu. First-check di install baru gak punya baseline buat dibandingin, jadi tag terbaru saat itu langsung dijadiin baseline (dianggap up-to-date, bukan asal tebak).

Verifikasi: brace/paren balanced kedua file baru, import Okio (`okio.Buffer`, `okio.buffer`, `okio.sink`) dan OkHttp (`OkHttpClient`, `Request`) sesuai API `okhttp3:4.12.0`. `UpdateChecker`/`ApkDownloader` belum ada call-site (belum dipanggil dari mana pun) — sengaja, murni tambahan additive, 0 file lama yang berperilaku beda.

### Batch48 — HOTFIX: CI build gagal dari Batch47 (2 file, 1 baris tiap file)
User upload log CI gagal (`log-fail_main_run170-attempt1_1f96cac.log`). Root cause: `onProgress: (checked: Int, total: Int) -> Unit = {}` — Kotlin TIDAK auto-infer arity buat lambda literal kosong `{}` di posisi **default parameter value** kalau functional type-nya >1 parameter (beda dari lambda biasa di call-site, yang memang boleh `{}` kosong tanpa peduli arity selama gak dipakai). Compiler error persis nunjuk ke `{` itu sendiri di kedua file: `e: ...MediaRepository.kt:34:128 Expected 2 parameters of types Int, Int` dan `e: ...MediaScanner.kt:97:58 Expected 2 parameters of types Int, Int` — murni salah tebak sintaks Kotlin waktu Batch47 ditulis tanpa compiler untuk validasi (pola sama kayak hotfix Batch41).

**Fix**: `{}` → `{ _, _ -> }` (underscore = parameter sengaja gak dipakai, arity dinyatakan eksplisit) di kedua file — `MediaScanner.kt` baris 97, `MediaRepository.kt` baris 34. Tidak ada perubahan lain — logic yield/cancellation/cache Batch47 lainnya sudah benar dan tidak disentuh.

**Verifikasi manual**: brace/paren balance `MediaScanner.kt` 78/78 & 175/175, `MediaRepository.kt` 19/19 & 54/54 (angka sama persis kayak sebelum hotfix — cuma isi `{}` yang berubah, jumlah bracket gak nambah/kurang). Compile sesungguhnya nunggu CI run berikutnya.

### Batch47 — Audit Gap P1 #6 stage 2a: cancellable exact-dup scan + progress callback (2 file)
User pilih lanjut #6 stage 2 ("apapun, yang penting gak asal jadi"). Full stage 2 (on-demand+cancel+progress UI) butuh 4 file (`MediaScanner.kt`, `MediaRepository.kt`, `MainActivity.kt`, `HomeScreen.kt`) — tabrak HARD CAP 3-file/batch. Dipecah jadi 2a (backend, batch ini) dan 2b (UI, next batch) — pola sama persis kayak split stage 1/2 di Batch45.

**Root cause yang ditemukan saat baca kode** (bukan cuma dari deskripsi audit): `findExactDuplicates()` ditandai `suspend fun` tapi badan fungsinya 100% kerja sinkron (baca file + MD5 loop) TANPA satu pun titik suspensi — artinya walau `MainActivity` manggilnya lewat `withContext(Dispatchers.IO)` di dalam `LaunchedEffect(activeMedia)`, Job effect itu SECARA TEKNIS gak pernah bisa membatalkan scan yang lagi jalan. Saat `activeMedia` berubah lagi (lumrah terjadi berkali-kali selama progressive loading gallery besar), `LaunchedEffect` restart → coroutine lama di-cancel(), tapi karena gak ada suspension point buat cancellation exception nyangkut, scan lama tetap jalan sampai kelar di background — kerja I/O sia-sia untuk hasil yang gak dipakai. Ini persis gejala "cancellation belum granular" di audit #6, plus temuan tambahan (waste I/O) yang gak eksplisit disebut auditor.

**Fix**: `kotlinx.coroutines.yield()` tiap 20 item diperiksa (`YIELD_EVERY`, konstanta yang sudah ada, dipakai ulang — bukan bikin baru) — pola identik `findBlurryPhotos` yang sudah terbukti jalan sejak batch lama. `yield()` di coroutine yang Job-nya sudah di-cancel akan throw `CancellationException` di titik itu juga, jadi scan lama beneran berhenti secepat checkpoint berikutnya, bukan nunggu selesai sendiri.

**`onProgress: (checked: Int, total: Int) -> Unit = {}`** ditambah ke signature `MediaScanner.findExactDuplicates` DAN facade `MediaRepository.findExactDuplicates` (delegator 1-baris, tinggal forward parameter) — default no-op bikin satu-satunya caller sekarang (`MainActivity.kt` baris ~589, `MediaRepository.findExactDuplicates(context, activeMedia)`) tetap compile PERSIS TANPA DIUBAH. 0 risiko, 0 behavior change untuk caller yang sudah ada — infra ini nganggur (belum ada yang isi callback beneran) sampai stage 2b pasang UI-nya.

**Cache-persist-on-cancel**: `updatedCache` yang tadinya di-`saveAll()` di akhir fungsi (cuma kepanggil kalau fungsi selesai NORMAL) sekarang dibungkus `try { ... } finally { cacheStore.saveAll(updatedCache) }` — kalau scan dibatalkan di tengah jalan (`CancellationException` dari `yield()`), `finally` TETAP jalan sebelum exception dilempar ke atas, jadi hash yang sempat dihitung sebelum cancel gak hilang. Efek nyata: kombinasi sama stage 1 (Batch45)'s hash cache, ini bikin scan berikutnya (baik yang auto masih jalan sekarang, atau yang manual pas stage 2b nanti) makin murah tiap kali di-restart — **BUKAN** "resume dari posisi persis terakhir" (gak ada state posisi yang disimpan), sekadar efek samping jujur dari arsitektur cache-nya, jangan dioverklaim jadi "resume scan" penuh di komunikasi ke user manapun ke depan.

**Kenapa cuma 2 file (di bawah cap 3, bukan dipepetin ke 3)**: `MainActivity.kt` SENGAJA tidak disentuh batch ini — kalau ikut diubah tanpa `HomeScreen.kt` juga (UI trigger), constructor/state baru bakal jadi dead code (dipanggil tapi gak ada tombol yang manggil), atau kalau nekat ganti `LaunchedEffect` auto-scan jadi manual TANPA UI pengganti, fitur "Duplicate files" di Quick Clean bakal hilang dari user sampai batch berikutnya — regresi UX sementara yang gak perlu. Backend murni (2 file) adalah potongan terkecil yang genuinely berdiri sendiri dan 0 risiko UI.

**Verifikasi manual**: brace/paren balance `MediaScanner.kt` 78/78 & 175/175, `MediaRepository.kt` 19/19 & 54/54. Grep dikonfirmasi: `findExactDuplicates` cuma 1 call-site di seluruh project (`MainActivity.kt` baris ~589) dan itu tetap valid tanpa edit. `YIELD_EVERY` dipakai ulang (bukan konstanta baru) — dideklarasikan sebagai member `object MediaScanner`, urutan deklarasi dalam file gak masalah di Kotlin. Compile sesungguhnya nunggu CI seperti biasa (belum ada compiler di sandbox ini).

**Belum dikerjakan (stage 2b, lihat "Belum Dikerjakan" di atas)**: tombol Scan on-demand + Cancel + progress% UI (`MainActivity.kt` + `HomeScreen.kt`), ganti `LaunchedEffect` auto-scan jadi trigger manual pola `ScanState` sama kayak blur/near-dup.

### Batch45 — Audit file ditanamkan permanen + Audit Gap P1 #6 stage 1: persistent hash cache (2 file + 1 doc baru)
User upload ulang `GalleryCleaner_v37_Audit_Gap_Final.md` di sesi ini setelah ZIP hard-reset bikin file itu jadi BLOCKER (cuma pernah ada sebagai upload chat sesi lama, gak pernah ikut ke dalam project — begitu histori chat gak kebawa, isinya hilang). User eksplisit minta "tanamkan+adaptasi permanen", jadi 2 task digabung 1 batch (instruksi user > batasan 1-task/batch).

**Task 1 — embed permanen**: `AUDIT_GAP.md` baru di root (sejajar README/ROADMAP/CHANGELOG, pola dokumen root yang sudah ada) — isi VERBATIM dari file upload, ditambah 1 comment block di atas yang jelasin kenapa file ini ada. Sekarang audit source jadi bagian ZIP, bukan lagi bergantung ke histori chat — hard-reset ZIP manapun ke depan akan selalu bawa file ini.

**Task 2 — P1 #6 stage 1**: audit #6 = "`findExactDuplicates()`... belum terlihat: persistent hash cache, incremental scanning, progress percentage, resume scan, cancellation granular" — 5 sub-gap. Stage 1 ini cuma yang pertama (persistent hash cache), yang paling berdampak dan paling rendah risiko (0 perubahan UI/state Compose).

**Kenapa bisa 0 sentuh `MainActivity.kt`/`MediaRepository.kt`**: dicek dulu signature `MediaRepository.findExactDuplicates(context, items)` — tetap sama persis, cuma delegator 1-baris ke `MediaScanner`. Cache dibuat & dipakai murni di dalam `MediaScanner.findExactDuplicates` sendiri (`HashCacheStore(context)` diinstansiasi lokal di situ) — jadi 0 downstream call site yang perlu ikut berubah.

**Kenapa `HashCacheStore` baca/tulis SEKALI per scan (bukan per item)**: pola persis sama seperti `FolderLabelStore`/`ProgressStore` (DataStore Preferences per-key) DITOLAK untuk kasus ini — cache bisa berisi ribuan entry (semua size-candidate), dan `edit{}` DataStore selalu nulis ulang SELURUH file tiap dipanggil. Nulis per-item di library besar bakal jadi masalah O(n) yang sama persis kayak `allMedia + page` yang baru dibenerin Batch44, cuma pindah lokasi. Fix: 1 key berisi JSON array (pakai `org.json` built-in Android, 0 dependency baru — beda dari Batch44 yang butuh riset versi package baru, di sini gak perlu sama sekali), dibaca sekali di awal scan jadi `Map<Long, Entry>`, ditulis sekali di akhir.

**Cache-invalidation**: key = media id, entry nyimpen `sizeBytes` + `dateModifiedMillis` di saat item itu di-hash. Cache hit hanya valid kalau KEDUA nilai itu masih sama persis dengan item saat ini — jadi file yang diedit/diganti isinya di path yang sama (id sama, konten beda) tetap kepaksa di-hash ulang, gak salah-percaya ke hash lama.

**Pruning otomatis**: `updatedCache` yang ditulis balik cuma berisi entry dari size-candidate scan SAAT INI (bukan union mentah sama cache lama) — jadi entry item yang udah dihapus, atau yang size-nya udah gak collide sama item lain, otomatis ke-drop, gak menumpuk selamanya.

**File (2 kode + 1 doc)**: `data/local/datastore/HashCacheStore.kt` (baru), `data/media/MediaScanner.kt` (`findExactDuplicates` ditulis ulang, `hashContent`/fungsi lain di file ini tidak disentuh), `AUDIT_GAP.md` (baru, root, dokumen bukan kode — di luar hitungan cap 3-file kode).

**Verifikasi manual**: brace/paren balance `MediaScanner.kt` 73/73 & 159/159, `HashCacheStore.kt` 10/10 & 36/36. Grep dicek: nama DataStore baru (`gallery_cleaner_hash_cache`) 0 bentrok dengan 5 DataStore lain yang sudah ada (trash/settings/stats/folder_labels/progress). `org.json` tidak butuh entry baru di `build.gradle.kts` (API bawaan Android runtime, bukan library eksternal). Compile sesungguhnya nunggu CI seperti biasa.

**Belum dikerjakan (stage 2, lihat "Belum Dikerjakan" di atas)**: incremental scanning, progress percentage, resume scan, cancellation granular — 4 sub-gap sisa dari temuan #6, butuh perubahan `MainActivity.kt` (state UI, kemungkinan `ScanState` seperti pola blur/near-dup) yang sengaja dipisah dari stage 1 ini.

### Batch44 — Audit Gap P1 #5: progressive loading O(n²) copy (2 file)
Mulai P1 setelah semua P0 (4/4) tuntas. P1 #5: "`allMedia = allMedia + page` — setiap page bikin List baru... repeated allocation, repeated copying, memory churn, scalability menurun."

**Analisis akar masalah**: operator `+` di List Kotlin selalu bikin `ArrayList` baru berisi SEMUA elemen kedua operand — dipanggil tiap page (400 item/page, lihat `PAGE_SIZE` di `MediaDataSource`), total kerja copy tumbuh O(n²) seiring jumlah page (mis. library 20rb item / 400 per page = 50 page, total elemen ke-copy berulang ≈ 400×(1+2+...+50) ≈ 510rb — bukan sekali jalan O(n), tapi O(n²)).

**Kenapa gak pakai `SnapshotStateList` (opsi lain yang lebih "native" Compose)**: sempat dipertimbangkan tapi DITOLAK — `mutableStateListOf()` punya reference OBJEK yang stabil selamanya (gak pernah ganti), padahal `LaunchedEffect(allMedia, trashedIds, expiredIds)` di baris bawahnya BUTUH `allMedia` berubah REFERENSI/VALUE tiap page biar re-trigger recompute (total bytes, active/trash split, dst). Kalau dipaksa pakai SnapshotStateList, effect itu cuma jalan SEKALI di awal — butuh proxy key manual (`allMedia.size`, dst) yang riskan lupa di-update di call site baru nanti, DAN gak cover semua kasus mutasi (mis. `.map{}` di Organize handler yang ganti isi tanpa ganti `size`). Risiko regresi silent terlalu tinggi buat state management paling sensitif di seluruh app, tanpa compiler/device buat validasi — jadi TIDAK dipilih.

**Fix yang dipilih**: `PersistentList<MediaItem>` (`kotlinx.collections.immutable`) — struktur data immutable yang `.addAll()`-nya pakai *structural sharing* (cuma elemen baru yang benar-benar "kerja baru", bukan full-copy semua yang udah ada), TAPI tetap `List` biasa dan tetap bikin VALUE baru tiap dipanggil (beda dari `SnapshotStateList`) — jadi `LaunchedEffect(allMedia, ...)`'s key-comparison (`equals()` structural, default Compose `mutableStateOf` policy) tetep kedeteksi berubah, TANPA perlu diubah sama sekali. Ini alasan kenapa PersistentList dipilih di atas SnapshotStateList: dapet efisiensi structural-sharing TANPA mengorbankan correctness/robustness keying yang udah ada.

**5 titik reassignment `allMedia` di `MainActivity.kt`, semua sudah dicek & diubah**:
1. Init loading (`allMedia = emptyList()` → `persistentListOf()`)
2. **Inti fix**: page-append (`allMedia + page` → `allMedia.addAll(page)`)
3. Delete-after-consent-dialog handler (`.filterNot{}.toPersistentList()`)
4. Delete-directly handler (`.filterNot{}.toPersistentList()`)
5. Organize/move handler (`.map{}.toPersistentList()`)

3 titik terakhir butuh `.toPersistentList()` karena `filterNot`/`map` stdlib Kotlin generik selalu return `List` biasa (gak tau soal PersistentList), sementara `allMedia`-nya sendiri sekarang bertipe `PersistentList<MediaItem>` — tanpa konversi ini bakal type-mismatch. Titik BACA (`allMedia.filterNot{}`/`.filter{}` di dalam `LaunchedEffect` buat `DerivedMediaState`) TIDAK perlu diubah — hasilnya cuma dibaca jadi `List<MediaItem>` biasa (tipe field `DerivedMediaState`), bukan di-assign balik ke `allMedia`.

**Kenapa pin ke `0.3.8`, bukan versi terbaru (`0.5.1`, Jul 2026)**: dicek dulu via web search sebelum nulis dependency (pelajaran dari hotfix Batch41 — jangan nebak nama/versi package tanpa verifikasi). `0.5.1` butuh Kotlin ≥2.3.0, `0.4.0` butuh Kotlin ≥2.1.20 — project ini di Kotlin **1.9.24** (`build.gradle.kts` root), jadi KEDUANYA bakal gagal resolve dependency. `0.3.8` (rilis Sep 2024) cuma butuh Kotlin ≥1.9.21 — kompatibel.

**File (2)**: `MainActivity.kt` (state type + 5 titik reassignment + import), `app/build.gradle.kts` (protected, edit parsial: +1 dependency).

**Verifikasi manual**: brace/paren balance `MainActivity.kt` 272/272, 494/494; `build.gradle.kts` 22/22, 59/59. Grep dicek: 0 sisa `allMedia + page` atau assignment tipe `List<MediaItem>` polos tanpa `.toPersistentList()`. Dicek juga: `DerivedMediaState`'s field (`activeMedia`/`trashItems`/`expiredTrashItems`) tetap `List<MediaItem>` biasa, gak perlu ikut diubah ke PersistentList (cuma dibaca, gak pernah jadi target assignment `allMedia`). Dicek: `allMedia` gak pernah dipassing langsung sebagai argumen ke fungsi lain di file ini (cuma dipakai sebagai LaunchedEffect key + dibaca di body-nya sendiri) — jadi 0 downstream call site lain yang perlu disentuh. Compile sesungguhnya nunggu CI seperti biasa.

### Batch43 — Fix nama file APK Release + akar masalah versi "stuck" (1 file protected asset, edit parsial)
Permintaan user (2 screenshot GitHub Release: `GalleryCleaner v1.0.165` filename APK `GalleryCleaner-v1.0.44-Release.apk`, dibandingkan contoh app lain "PromptVault" yang filename-nya `PromptVault-v7.2.0.apk` — polos, tanpa suffix "-Release", dan angka versinya SAMA PERSIS dengan tag release).

**Konfirmasi akar masalah** (ini yang user duga "manual version bump stuck" — sudah pernah di-flag Batch20 tapi sengaja gak disentuh waktu itu karena di luar permintaan saat itu): `.github/workflows/build.yml` punya DUA skema angka versi berbeda buat 1 hal yang sama:
- Tag/nama GitHub Release: `v1.0.${{ github.run_number }}` — auto-increment per run CI.
- Nama file APK (`VERSION_NAME` di step "Rename APK"): `1.0.$(git rev-list --count HEAD)` — total commit count.

Dua counter ini BUKAN manual/stuck, tapi auto-generate dari sumber berbeda yang secara alami saling drift (re-run CI, workflow_dispatch, atau banyak commit di-batch jadi 1 push semua bikin `run_number` maju lebih cepat dari `commit count`) — makanya tag bisa di v1.0.165 sementara nama file APK-nya masih v1.0.44.

**Fix**: `VERSION_NAME` sekarang pakai `$GITHUB_RUN_NUMBER` (env var bawaan Actions, sumber yang SAMA kayak tag) — bukan `git rev-list --count HEAD` lagi. Jadi nama file APK & tag/nama release SELALU sama persis dari sekarang, permanen (bukan cuma dibenerin sekali). Suffix "-Release" dihapus total, hasil akhir: `GalleryCleaner-v1.0.${{ run_number }}.apk` — pola persis `{App}-v{version}.apk` kayak contoh PromptVault, dan angka run number itu sendiri yang jadi pembeda tiap build (dulu tugas ini dipegang kata "Release" yang statis/gak informatif).

**Riwayat terkait**: ini iterasi ke-2 dari penamaan file APK — Batch20 sebelumnya sudah pernah ganti dari `-{SHORT_SHA hash acak}.apk` → `-Release.apk` (permintaan user waktu itu: predictable/readable, bukan hash acak). Batch20 JUGA udah nemuin mismatch VERSION_NAME vs tag di catatannya sendiri, tapi sengaja gak diutak-atik karena di luar scope permintaan saat itu — baru dikerjakan sekarang karena user eksplisit minta.

**File**: `.github/workflows/build.yml` (protected, edit parsial — cuma step "Rename APK", 2 baris logic diganti + komentar penjelas; build steps lain, secrets, keystore, signature verification, Release publishing sama sekali tidak disentuh).

**Verifikasi manual**: grep bersih, 0 sisa referensi `git rev-list --count` atau `-Release.apk` di workflow. `$GITHUB_RUN_NUMBER` sudah proven-pattern di file ini sendiri (dipakai identik di step "Build signed release APK" buat nama `LOG_FILE`, jadi bukan syntax baru yang belum teruji). Efek baru baru kelihatan di run CI berikutnya (nama file APK & tag release berikutnya bakal sama persis, contoh: `GalleryCleaner-v1.0.166.apk` + tag `v1.0.166`).

### Batch42 — Audit Gap P0 #2: "Trash" bukan trash filesystem — investigasi + klarifikasi (1 file)
Stage 4 dari audit tracker. P0 #2: "`TrashStore.kt` hanya nyimpen id+timestamp di DataStore... secara UX ini lebih tepat disebut virtual review queue / pending deletion... semantics harus dipertegas atau implementasi trash sebenarnya dibuat."

**Investigasi sebelum coding** (pola sama kayak P0 #3/#4 — cek klaim aktual sebelum nentuin fix): audit ngasih 2 pilihan solusi — pertegas semantics ATAU bangun trash beneran. Dicek 3 tempat yang berpotensi overclaim ke user:
1. `TrashScreen.kt` — copy-nya udah akurat: "Trash (N)", "Empty Trash", "Delete permanently" (implisit: item di Trash BELUM ke-delete permanen), dialog konfirmasi eksplisit sebelum hapus final. Gak ada klaim "file dipindah ke OS trash" di mana pun.
2. `SettingsScreen.kt` — retention copy: "Automatically flag items in Trash for cleanup after:" — udah pakai kata "flag", bukan "auto-delete" (ini hasil fix Batch38, sudah benar sejak itu).
3. `README.md` — grep "trash": 0 hit. README gak pernah nyebut kata "trash" sama sekali, jadi gak ada klaim README yang perlu dikoreksi.

**Kesimpulan**: gap-nya BUKAN di UX yang dilihat user (semua copy sudah jujur soal apa yang sebenarnya terjadi), tapi di level dokumentasi internal/arsitektur — `TrashStore.kt`'s doc comment lama menjelaskan CARA KERJA-nya tapi gak eksplisit bilang ini beda dari `MediaStore.createTrashRequest()` (API 30+, real OS-level trash: nge-flag kolom `IS_TRASHED` di MediaStore, disembunyikan dari query normal, bisa direstore lewat sistem). Pattern "review queue + countdown + restore, semuanya app-managed" yang dipakai app ini persis sama kayak "Recently Deleted" di Google Photos/iOS Photos — konvensi UX yang sudah umum dipahami user, bukan bug semantik.

**Fix**: perkuat doc comment di `TrashStore.kt` — sekarang eksplisit menyebut nama API OS yang beneran ada (`MediaStore.createTrashRequest()`), dan jelasin KENAPA itu sengaja belum diadopsi: butuh re-query trashed items dari MediaStore pakai `MATCH_TRASHED` (`TrashScreen` sekarang baca dari `TrashStore` lokal, bukan dari MediaStore), flow untrash yang terpisah dari restore-di-DataStore, dan story buat item yang di-trash dari app LAIN (Files/Photos bawaan) muncul gak terduga di queue app ini. Ini genuinely perubahan arsitektur (query layer + sync state), bukan tweak kecil — didokumentasikan sebagai kandidat upgrade masa depan kalau OS-level trash integration jadi requirement produk nyata, TIDAK dikerjakan sekarang (instruksi user: "jangan greedy").

**Kenapa cuma 1 file, bukan lebih**: gak ada kode yang perlu diubah (UI copy sudah benar, tracking logic sudah benar) — murni memperjelas niat desain di titik yang paling relevan (`TrashStore.kt`, tempat implementasi virtual-queue-nya ada) biar auditor/maintainer berikutnya gak salah paham ini "belum sempat" padahal ini pilihan desain sadar.

**Verifikasi manual**: brace/paren balance `TrashStore.kt` 24/24, 61/61.

### Batch41 — HOTFIX: CI build gagal dari Batch40 (1 file)
User upload log CI gagal (`log-fail_main_run163-attempt1_594aa57.log`). Root cause tunggal: `GalleryCleanerApp.kt` import `coil.video.VideoFrameDecoder` — package itu SALAH untuk Coil 2.x. Dicek via web search ke dokumentasi/changelog resmi Coil: kelas ini ada di `coil.decode.VideoFrameDecoder` (di bawah `coil-video` artifact, tapi package-nya tetap `coil.decode`, bukan `coil.video`). Ini murni salah tebak nama package waktu Batch40 ditulis tanpa compiler untuk validasi — dependency `coil-video:2.6.0` di `build.gradle.kts` sendiri sudah benar, cuma importnya yang keliru.

**Fix**: `import coil.video.VideoFrameDecoder` → `import coil.decode.VideoFrameDecoder`. Cuma 1 baris, 1 file. Tidak ada perubahan lain — dependency, registrasi `add(VideoFrameDecoder.Factory())`, dan seluruh logika Batch40 lainnya sudah benar dan tidak disentuh.

**Verifikasi manual**: brace balance `GalleryCleanerApp.kt` 7/7. Grep bersih: 0 sisa referensi `coil.video` di seluruh project. Package `coil.decode.VideoFrameDecoder` dikonfirmasi dari dokumentasi API resmi Coil (coil-kt.github.io) dan changelog resmi — bukan tebakan kedua tanpa sumber.

### Batch40 — Audit Gap P0 #1: Video jadi bagian nyata library (8 file)
Stage 3 dari audit tracker. P0 #1: "video secara praktis 0% didukung" — `MediaDataSource` cuma query `MediaStore.Images.Media`, manifest cuma minta `READ_MEDIA_IMAGES`, `MediaItem` gak punya pembeda tipe media.

**Fix**: `MediaDataSource` sekarang query `MediaStore.Video.Media` juga (paging independen per koleksi, di-merge per-round di `loadMediaProgressively`; urutan interleave gak sempurna tapi gak masalah karena `MediaRepository.group()` re-sort ulang seluruh list di setiap emission — lihat komentar di kode). `MediaItem` dapat field `mediaType: MediaType` (enum IMAGE/VIDEO, default IMAGE) dan `durationMillis: Long` (0 untuk foto). Manifest + `requiredPermissions()` MainActivity nambah `READ_MEDIA_VIDEO` (API33+). Thumbnail video: Coil `VideoFrameDecoder` (dependency baru `coil-video:2.6.0`) didaftarkan sekali di `GalleryCleanerApp`'s shared `ImageLoader` — otomatis berlaku di SEMUA layar yang render `MediaItem` (home cover, swipe card, trash grid, filmstrip, fullscreen viewer) karena semuanya lewat satu composable `MediaPreview.kt`, jadi cukup 1 file UI yang disentuh (bukan 7). Play-badge (lingkaran semi-transparan + ikon PlayArrow, center-aligned) ditambahkan di `MediaPreview.kt` sebagai satu-satunya penanda visual video vs foto.

**Kenapa gak perlu ubah DeleteHelper/TrashStore/MoveHelper/SwipeScreen share-intent**: sudah type-agnostic dari awal (verified via grep — 0 hit MIME/BitmapFactory di file-file itu kecuali `SwipeScreen.kt`'s share intent yang sudah query `contentResolver.getType()` secara dinamis, bukan hardcode "image/*"). Delete/trash/organize/share via content URI generik, gak peduli MIME — video otomatis ikut jalur yang sama tanpa modifikasi.

**MediaScanner**: `findExactDuplicates` (hash byte konten mentah) generic, otomatis jalan untuk video juga tanpa perubahan. `findBlurryPhotos`/`findNearDuplicates` (keduanya decode bitmap) ditambah filter `mediaType == IMAGE` di awal — bukan syarat correctness (exception handling yang sudah ada bikin video gagal-decode secara aman/silent), murni biar gak buang I/O buka file video yang pasti gagal di-decode sebagai bitmap.

**SENGAJA belum dikerjakan batch ini** (biar gak melebihi batas file/batch, exact per instruksi "jangan greedy"):
- Duration label di thumbnail (field `durationMillis` sudah ada di model & data source, tinggal dipakai di UI)
- Tap-to-play / pemutaran video penuh di swipe/fullscreen viewer (saat ini cuma nampilin 1 frame statis via VideoFrameDecoder, sama kayak foto)
- Filter "hanya foto" / "hanya video" di UI (belum ada toggle)
- Wording "photo"/"foto" generik di beberapa layar (mis. hitungan "X photos" di Home) yang sekarang bisa termasuk video — dicatat ulang di P2 #11 tracker di atas, bukan diperbaiki di sini

**Verifikasi manual**: brace/paren balance semua 8 file dicek, hasil balance semua. Grep bersih: publik API `MediaRepository` (dipanggil `MainActivity`, `CleaningReminderWorker`) TIDAK berubah signature — cuma implementasi internal `MediaDataSource` yang berubah, jadi 0 caller lain perlu disentuh. 1 satu-satunya real construction site `MediaItem(...)` (di `MediaDataSource`) sudah pakai named args, field baru berdefault jadi non-breaking. `minSdk=24` kompatibel dengan `coil-video:2.6.0` & `MediaStore.Video.VideoColumns.DURATION` (tersedia sejak API lama, khusus di-query dari koleksi Video saja — bukan koleksi Images — biar gak mengandalkan OEM mentolerir kolom asing). Compile sesungguhnya nunggu CI seperti biasa.

### Batch39 — Audit Gap P0 #4: BiometricPrompt sungguhan untuk App Lock (4 file: 3 edit protected asset parsial + 1 edit)
Lanjutan tracker audit dari Batch38. Ini stage 2: P0 #4 ("App Lock tidak sesuai klaim README").

**Investigasi sebelum coding**: baca `MainActivity.kt` — App Lock lama pakai `KeyguardManager.createConfirmDeviceCredentialIntent()`, method ini di-`@Suppress("DEPRECATION")` (artinya sudah deprecated di Android SDK). Method ini cuma nunjukin layar konfirmasi PIN/pattern/password bawaan device — TIDAK ada opsi biometric independen di dalamnya (walau device punya fingerprint/face, layar ini tetap minta credential penuh, bukan quick-biometric). Audit benar: README bilang "PIN/biometric" dan Settings bilang "PIN, pattern, or biometric", tapi implementasi aktual cuma satu jalur (device credential screen), bukan biometric prompt beneran. Beda dari P0 #3 (yang solusinya "audit-nya gak feasible, benerin dokumentasi"), di sini implementasi LAMA yang memang belum sesuai klaim — jadi fix-nya nambah biometric beneran, bukan nurunin klaim dokumentasi.

**Solusi**: ganti ke `androidx.biometric.BiometricPrompt` (library resmi Jetpack, bukan deprecated) dengan `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` — kombinasi ini bikin sistem nunjukin prompt fingerprint/face dulu (kalau ada & ke-enroll), otomatis fallback ke device screen lock (PIN/pattern/password) kalau biometric gak tersedia/gagal/dibatalkan. Satu API, dua jalur — persis yang diklaim dokumentasi dari awal.

**Kenapa `FragmentActivity`, bukan `ComponentActivity`**: `BiometricPrompt`'s constructor butuh host `FragmentActivity` (auth UI-nya jalan sebagai `DialogFragment` internal). `androidx.fragment.app.FragmentActivity` sudah extend `androidx.activity.ComponentActivity` sejak Fragment 1.3.0 — jadi ganti base class `MainActivity : ComponentActivity()` → `MainActivity : FragmentActivity()` murni WIDENING, semua API ComponentActivity yang sudah dipakai (`setContent`, `rememberLauncherForActivityResult`, `getSystemService`, dst — dipakai di banyak tempat lain di file ini untuk permission/delete flow) tetap jalan tanpa perubahan. Diverifikasi: 0 file lain di project reference `ComponentActivity` secara eksplisit (grep bersih), jadi 0 risiko broken cast di tempat lain.

**Fail-open dipertahankan** (bukan fitur baru, port dari logic lama): kalau `BiometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` bukan `BIOMETRIC_SUCCESS` (device gak punya screen lock sama sekali, atau hardware biometric gak ada dan gak ada PIN/pattern/password), `isUnlocked` langsung `true` — sama rationale kayak `km.isDeviceSecure == false` di implementasi lama: gak ada credential valid buat autentikasi = mengunci gallery di sini akan strand user tanpa jalan balik.

**File (4)**: `app/build.gradle.kts` (protected, edit parsial: +1 dependency `androidx.biometric:biometric:1.1.0`), `app/src/main/AndroidManifest.xml` (protected, edit parsial: +1 permission normal `USE_BIOMETRIC`, granted otomatis di install, gak ada runtime prompt), `MainActivity.kt` (ganti base class + blok App Lock: `KeyguardManager`+`ActivityResultContracts.StartActivityForResult` diganti `BiometricManager`+`BiometricPrompt`), `README.md` (baris fitur App Lock diperjelas match implementasi baru).

**Tidak diubah**: `SettingsScreen.kt` — copy "Require your screen lock (PIN, pattern, or biometric) to open the app." sudah akurat untuk implementasi baru (malah lebih akurat sekarang karena biometric-nya beneran ada), gak perlu edit.

**Verifikasi manual** (belum ada compiler di sandbox): brace/paren balance `MainActivity.kt` 272/272, 480/480 (dicek ulang setelah semua edit). Grep bersih: 0 reference `KeyguardManager`/`confirmCredentialLauncher`/`createConfirmDeviceCredentialIntent`/`ComponentActivity` tersisa di kode aktif (cuma di komentar penjelas). 0 file lain di project pakai `ComponentActivity` secara eksplisit. `minSdk=24` project ini di atas syarat minimum `androidx.biometric:1.1.0` (minSdk 23) — kompatibel. Compile sesungguhnya nunggu CI seperti biasa.

### Batch38 — Audit Gap P0 #3: TrashExpiryWorker (2 file: 1 baru + 1 edit)
User upload `GalleryCleaner_v37_Audit_Gap_Final.md` — audit eksternal 20 temuan (P0×4, P1×6, P2×10), minta dikerjakan **bertahap sampai tuntas**. Ini stage 1: P0 #3 ("Expiry Trash tidak otomatis mengeksekusi deletion").

**Investigasi sebelum coding** (bukan langsung asumsi audit benar 100%): baca `TrashStore.kt` — class doc-nya SUDAH menjelaskan sejak sebelum batch ini kenapa silent auto-delete gak dipakai: *"Android's scoped storage requires an interactive system confirmation for every permanent delete (there's no silent background-delete API)"*. Dicek ke `MainActivity.performPermanentDeletion` — betul, jalur delete pakai `MediaStore.createDeleteRequest()` yang WAJIB `IntentSenderRequest` dari Activity foreground + WAJIB dialog konfirmasi user di API30+. Kesimpulan: audit BENAR soal gejala (user yang gak buka app gak pernah diproses), tapi solusi tersirat di audit ("automatic cleanup" = silent delete) **gak feasible secara platform**, bukan cuma "belum diimplementasi". Fix yang benar: notifikasi proaktif saat item expired, bukan silent-delete.

**Solusi**: `TrashExpiryWorker.kt` (baru) — `CoroutineWorker` periodik harian, mirip pola `CleaningReminderWorker.kt` yang sudah ada (dependency `androidx.work` sudah terpasang, 0 perubahan `build.gradle.kts`). Cek `settingsStore.trashRetentionDaysFlow` + `trashStore.expiredItemIdsFlow(retentionDays)`, kalau ada yang expired → notifikasi channel baru (`trash_expiry`, id `1002`, beda dari punya `CleaningReminderWorker` yang `1001`) dengan `PendingIntent` yang deep-link langsung ke `TrashScreen` (reuse mekanisme `ACTION_VIEW_TRASH` App Shortcut yang sudah ada di `MainActivity`, bukan mekanisme navigasi baru — string action di-duplikasi sebagai `private const` di file baru ini karena gak bisa import `private const` lintas file; sudah ada 2 duplikasi serupa sebelumnya di `shortcuts.xml`, jadi ini konsisten pola lama, bukan technical debt baru).

**Selalu terjadwal (bukan opt-in)** — beda dari `CleaningReminderWorker` yang perlu toggle di Settings. Retention days sudah jadi fitur aktif sejak swipe-delete pertama (picker-nya selalu kelihatan di Settings), jadi notifikasi expiry-nya bukan fitur tambahan yang perlu di-opt-in, melainkan melengkapi janji yang implisit sudah dibuat retention picker itu sendiri. Di-schedule sekali dari `MainActivity.onCreate` (`KEEP` policy, idempotent — dipanggil ulang tiap cold start gak reset timer harian yang udah jalan).

**File (2)**: `worker/TrashExpiryWorker.kt` (baru), `MainActivity.kt` (+3 baris: panggil `TrashExpiryWorker.schedule(applicationContext)` di `onCreate`, sebelum `setContent`).

**Tidak diubah**: `AndroidManifest.xml` (0 permission baru — `POST_NOTIFICATIONS` sudah ada dari `CleaningReminderWorker`), `build.gradle.kts` (0 dependency baru — `androidx.work` sudah ada). 0 protected asset disentuh.

**Belum/tidak dikerjakan batch ini** (di luar scope P0 #3 spesifik, dicatat biar gak dianggap kelupaan): dedup notifikasi harian selama item masih expired-dan-belum-diaksi (v1 ini notif ulang tiap hari kalau masih ada yang expired — sama seperti banyak app serupa Google Photos/Files, dianggap cukup untuk v1, bukan bug).

**Verifikasi manual** (belum ada compiler di sandbox): brace/paren balance `TrashExpiryWorker.kt` 12/12, 53/53. `MainActivity.kt` re-checked 268/268, 467/467. Compile sesungguhnya nunggu CI seperti biasa.

### Batch37 — Fix Build Failure run160 (1 file: NeumorphSurface.kt)
User upload log CI: `log-fail_main_run160-attempt1_4a1b7b9_log.zip`. `:app:compileReleaseKotlin` FAILED, 3× "Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: public abstract fun Modifier.matchParentSize(): Modifier defined in androidx.compose.foundation.layout.BoxScope" — persis di 3 pemanggilan `matchParentSize()` dalam Batch36 (`NeumorphSurface.kt:108,120,133`).

**Root cause**: `matchParentSize()` dideklarasikan DI DALAM `interface BoxScope` sebagai `fun Modifier.matchParentSize(): Modifier` — perlu DUA hal sekaligus: extension receiver `Modifier` (harus ditulis eksplisit `Modifier.matchParentSize()`, sama seperti modifier lain) DAN dispatch receiver implisit `BoxScope` (otomatis tersedia karena dipanggil di dalam `Box { }`). Batch36 salah asumsi — dokumentasi internal (comment) yang saya tulis sendiri bilang "dipanggil bare karena implicit BoxScope receiver", tapi itu cuma benar untuk separuh syarat (dispatch receiver), bukan separuhnya lagi (extension receiver `Modifier` tetap wajib eksplisit). Fix: `matchParentSize()` → `Modifier.matchParentSize()` di ketiga titik pemanggilan, comment diperbaiki supaya gak mengulang kesalahan yang sama di batch depan.

**Tidak ada perubahan lain** — 3 layer shadow/fill di `NeumorphSurface.kt` (dark shadow, light shadow, flat fill) strukturnya sama persis, cuma prefix `Modifier.` yang ditambahkan. 0 file lain disentuh, 0 protected asset disentuh.

**Verifikasi manual** (belum ada compiler di sandbox ini): brace/paren balance re-checked (6/6, 54/54 — naik dari 52 karena 2 karakter tambahan `Modifier.` × 3, wajar). Compile sesungguhnya masih nunggu CI — kalau masih merah, upload log-fail berikutnya.

### Batch36 — Amber Reserve: Skeuomorphism-lite → Pure Neumorphism (8 file: 2 baru + 6 edit)
Permintaan eksplisit user: redesign Amber Reserve jadi **"eksplisit" Neumorphism murni, tanpa rekayasa ngide sendiri, tanpa hybrid baseline bersama theme lain**, dengan komposisi warna WCAG-compliant yang sudah ditentukan (60% `#0F172A` Deep Navy / 30% `#1E293B` Navy Card / 10% `#D4AF37` Classic Brass, teks `#F8FAFC` di atas Navy, teks `#0F172A` di dalam tombol Brass).

**Kepatuhan ke "tanpa ngide sendiri"**: semua warna baru berasal dari SALAH SATU dari (a) hex persis yang diberikan user, tanpa modifikasi, atau (b) derivasi mekanis dari hex itu (alpha-blend untuk teks sekunder, HLS-lightness-shift untuk varian pressed/light-mode — formula didokumentasikan per-value di `NeumorphTokens.kt`), atau (c) hitam/putih murni untuk pasangan shadow (teknik neumorphism standar, bukan hue baru). 0 warna dipilih bebas.

**Kepatuhan ke "tanpa hybrid baseline"**: `NeumorphTokens.kt` (baru) TIDAK meng-alias satupun token dari `SkeuoLiteTokens.kt`/`Color.kt`-nya Amber Reserve lama (beda dari `SkeuoLite` yang dulu alias `AccentBrass = BrassKeep`) — 100% standalone. Teknik render (`NeumorphSurface.kt`, baru) juga resep berbeda total: dual shadow independen (bukan 1 ambient shadow), fill flat solid (bukan gradient), TANPA border/bevel sama sekali (beda dari `glassPanel`/`skeuoPanel` yang selalu punya border) — lihat tabel perbandingan di doc comment `NeumorphSurface.kt`.

**Kendala teknis yang mendorong desain**: `Modifier.shadow()` (primitif yang dipakai `glassPanel`/`skeuoPanel`) cuma bisa 1 shadow dari elevasi Z, TIDAK bisa offset X/Y independen — secara struktural gak bisa bikin 2 shadow neumorphism (terang kiri-atas + gelap kanan-bawah). Solusi: 2 layer `Box` terpisah, masing-masing `.offset()` + `.shadow()` sendiri — primitif yang SAMA yang sudah dipakai `glassPanel`/`skeuoPanel`, cuma disusun sebagai 2 layer bukan 1 chain. Konsekuensi: `NeumorphSurface` adalah `@Composable` (seperti `GlassCard`), BUKAN `Modifier.neumorphPanel()` extension (beda dari `glassPanel`/`skeuoPanel`) — gak bisa masuk pola `.let { when(style) ... }` yang sama, jadi 3 call site (`GlassCard`, `GlassButton`, `InfoChip`) branch NEUMORPH via early-`return` SEBELUM masuk modifier-chain lama, bukan di dalam `when` yang sama.

**Caveat platform, didokumentasikan bukan disembunyikan**: `ambientColor`/`spotColor` custom di `Modifier.shadow()` cuma render sesuai tint di API 28+; di API24-27 fallback ke shadow hitam default. Untuk `glassPanel`/`skeuoPanel` ini tak kasat mata (tint mereka udah dekat-hitam). Untuk shadow terang (putih, neumorphism ini) di API24-27 akan salah render jadi shadow gelap kedua — degradasi visual kecil di device di bawah `minSdk=24`... eh, di ATAS `minSdk` tapi di bawah API28 (rentang shrinking di 2026). Flagged, tidak memblokir implementasi (`glassPanel`/`skeuoPanel` sudah terima tradeoff API-level yang serupa).

**File (8)**:
1. `ui/theme/NeumorphTokens.kt` (BARU) — palet + shadow pair + turunan pressed/light-mode, semua terderivasi/eksplisit (lihat di atas).
2. `ui/components/NeumorphSurface.kt` (BARU) — composable dual-shadow, no-border, no-gradient.
3. `ui/theme/MaterialStyle.kt` — tambah `MaterialStyle.NEUMORPH`, `AMBER_RESERVE` pindah dari `SKEUO_LITE`→`NEUMORPH`. `SKEUO_LITE` TETAP ada di enum (unused, bukan dihapus).
4. `ui/theme/Theme.kt` — `AmberReserveDark`/`AmberReserveLight` baca dari `Neumorph.*`. `secondary`/`error` (Oxblood/Delete) TIDAK diubah — di luar cakupan spec user, konsisten aturan project (Keep/Delete semantic color selalu di luar spec visual manapun).
5. `ui/components/GlassCard.kt` — early-return branch NEUMORPH → `NeumorphSurface`.
6. `ui/components/GlassButton.kt` — early-return branch NEUMORPH → `NeumorphSurface` dengan `fillColor=ClassicBrass` (CTA), teks selalu `TextOnBrass` (sesuai rule WCAG user, tidak ada swap warna teks saat pressed, cuma fill yang swap).
7. `presentation/screen/SwipeScreenControls.kt` (`InfoChip`) — early-return branch NEUMORPH, sama pola dengan `GlassCard`.
8. `presentation/screen/SettingsScreen.kt` — deskripsi + preview swatch theme picker Amber Reserve diupdate ke Neumorphism; import `BrassKeep`/`EspressoBg` yang jadi tak terpakai dihapus.

**Light mode**: TIDAK diberikan spec-nya oleh user (spec cuma untuk dark). Didekati mekanis: hue yang sama dari 3 hex yang diberikan, lightness dinaikkan di ruang HLS (teknik yang SAMA yang project ini sudah 3× pakai untuk pasangan dark/light tema lain: Espresso→Cream, Indigo→Lilac, Void→Ice) — bukan palet baru yang tak terkait. Ditandai sebagai default, terbuka dikoreksi kalau user kasih spec light mode sendiri.

**Verifikasi kontras (dihitung, bukan estimasi)**: TextPrimary vs DeepNavy = 17.06:1. TextPrimary vs NavyCard = 13.98:1. TextOnBrass vs ClassicBrass = 8.49:1. Semua jauh di atas AA (4.5:1), bahkan lolos AAA (7:1) — klaim WCAG di spec user terverifikasi benar.

**Tidak diverifikasi (tidak ada compiler di sandbox ini, konsisten batch-batch sebelumnya)**: build aktual. Verifikasi lewat GitHub Actions CI setelah push — cek brace/paren balance + exhaustiveness `when(style)` sudah dilakukan manual (lihat commit ini), tapi type-check Compose sesungguhnya nunggu CI.

### Batch35 — Isi Repo Asli ke Shortcut GitHub Release (4 file)
User konfirmasi repo: `https://github.com/FDzaki-dev/GalleryCleaner`. Placeholder `OWNER` (sengaja generik di Batch34 karena repo belum tentu dibuat) diganti `FDzaki-dev` di README.md/CHANGELOG.md/PROJECT_STATE.md/ROADMAP.md — 4 link sekarang mengarah ke `github.com/FDzaki-dev/GalleryCleaner/releases/latest` yang valid begitu rilis pertama ter-publish. 0 file kode disentuh.

### Batch34 — Rapikan Dokumentasi + Shortcut GitHub Release (4 file: README.md, CHANGELOG.md, PROJECT_STATE.md, ROADMAP.md)
Permintaan user: rapikan dokumentasi sampai "welcome able", terbaru wajib di urutan paling atas di setiap lini dokumentasi, tambah shortcut ke GitHub Release APK terbaru.

- **README.md**: ditulis ulang — shortcut GitHub Release (`/releases/latest`) sekarang paragraf ke-2, sebelum penjelasan build manual. Bagian "starter project, tidak bisa dikompilasi di sandbox" (basi, sudah v33+ berjalan penuh lewat CI) diganti ringkasan fitur terkini + link ke CHANGELOG/PROJECT_STATE. Blurb "Build note (fixed v2)" dan "v1.1.0 user-facing update" yang tadinya nyangkut di paling bawah (padahal itu update RELATIF baru) dihapus sebagai teks berdiri sendiri — isinya sudah representatif di README versi baru + CHANGELOG.
- **CHANGELOG.md**: shortcut GitHub Release ditambah di baris ke-2. **Bug urutan diperbaiki**: entri `v23`/`v24`/`v25` sebelumnya nyasar di PALING BAWAH file (setelah v1) alih-alih di posisi kronologisnya — dipindah ke antara v26 dan v22, header dinormalisasi ke pola `vNN_BatchNN — tanggal` (sebelumnya `## v23 — Batch23` tanpa tanggal). Urutan sekarang strict descending v33→v1 tanpa pengecualian.
- **PROJECT_STATE.md**: restrukturisasi total. Root cause lama: dokumen ini tumbuh sebagai stream tempel-tambah tanpa aturan urutan, sehingga muncul 2× header `## Batch26` identik berdampingan, 2× section `Protected Assets` identik, beberapa `## Versi Historis`/`## Versi Saat Ini (historis)` yang isinya cuma duplikat 1-baris dari section tepat di bawahnya (0 informasi unik), dan 1 baris paragraf yang ke-parse sebagai heading (`## Catatan koreksi (dari Batch32): header...`) karena kebetulan diawali pola mirip heading. Semua batch (1-33) disusun ulang ketat descending, header duplikat & divider murni-redundan di-dedup (0 kehilangan informasi substantif — diverifikasi word-count sebelum/sesudah selisih hanya dari elemen yang didup, bukan dari isi teknis). Ditambah: shortcut GitHub Release + "Belum Dikerjakan" yang di-refresh (menggantikan 3 snapshot basi yang tersebar di tengah dokumen — snapshot lama tetap diarsipkan di dalam section batch terkait, tidak dihapus).
- **ROADMAP.md**: shortcut GitHub Release + baris "Status Ringkas" ditambah di atas, supaya status Fase A-D langsung kebaca tanpa scroll ke section 3.
- **Tidak diubah**: `RELEASE_SIGNING.md` (sudah ringkas & evergreen, tidak butuh restrukturisasi), seluruh kode `.kt`/protected assets (murni pekerjaan dokumentasi, 0 file kode disentuh).
- Verifikasi: `OWNER` di URL shortcut GitHub Release adalah placeholder yang sengaja dibiarkan generik (repo belum tentu sudah dibuat/nama akun belum diketahui saat batch ini ditulis) — ganti manual setelah `gh repo create` di Kotak A, atau update lagi di batch berikutnya begitu username diketahui.

### Batch33 — Folder-Context Clarity (2 file: HomeScreenFolderRow.kt, HomeScreenSections.kt)
User kirim screenshot "Biggest space hogs" (nampilin subtitle nama folder
per file, mis. "Private"/"GIF"/"Camera") vs "All Photos" grouped by Month
(cuma nampilin "135 items", tanpa folder). Nanya kenapa beda. Dijelasin
dulu (bukan bug — beda level data: file individual vs kumpulan lintas
folder), lalu diminta "Dirombak. Agar user awam pun tetap tahu fungsinya".

**`HomeScreenFolderRow.kt` `GroupRow`**: tambah baris folder-summary di
bawah "N items" — HANYA muncul kalau `group.items.map{it.bucketName}
.distinct().size > 1` (jadi di mode Album selalu skip, karena judul baris
sudah = nama folder itu sendiri, redundant kalau ditambah lagi). Format:
ikon folder kecil + "Camera, WhatsApp Images" (≤2 folder) atau "Camera,
WhatsApp Images +3 more" (>2, `maxLines=1` + ellipsis, row height tidak
berubah walau foldernya banyak). Ini langsung menjawab pertanyaan user di
UI-nya sendiri — row Bulan sekarang KELIATAN kalau isinya lintas folder,
bukan diam-diam beda tanpa penjelasan.

**`HomeScreenSections.kt` `FilterRow`**: caption 1 baris di bawah label
"GROUP BY", teks berubah sesuai mode aktif ("One row per month, pooling
photos from every folder" / "One row per folder, exactly as it exists on
your device") — supaya konsepnya kejelasan DI DEPAN, sebelum user perlu
menebak-nebak dari hasil scroll-nya sendiri. Sengaja tidak menyebut
istilah teknis ("bucketName"/"MediaStore") — bahasa awam sesuai
permintaan.

**Tidak disentuh**: `SmartCategoryRow` (Quick Clean: Screenshots/Large
files) — juga lintas folder secara alami, tapi nama kategorinya sendiri
("Screenshots") sudah cukup menjelaskan diri, di luar cakupan pertanyaan
user (yang spesifik soal Month vs Biggest space hogs).

**Verifikasi**: brace/paren balanced 0/0 full sweep. 1 typo paren di
comment (bukan kode — tidak mempengaruhi kompilasi) ketauan lewat sweep
ini juga, dibetulkan sebelum commit.

### Batch32 — OOM Crash Fix + Success/Undo Snackbar Polish (3 file)
**Catatan versi (ditulis awal Batch32):**
tertinggal di v28 walau `CHANGELOG.md` dan kode sebenarnya sudah di v31
(Batch29 Share+persist fix, Batch30 DangerButton refactor, Batch31
permission dead-end fix — commit di luar chat ini via Termux, dokumentasi
header-nya saja yang tidak ke-update). Dikonfirmasi lewat kode (grep
`DangerButton`/`shouldShowRequestPermissionRationale`/`ACTION_SEND` — semua
sudah ada) sebelum lanjut, supaya batch ini tidak menimpa balik pekerjaan
v29-v31. Penomoran versi diloncat ke v32 (bukan v29) untuk menghindari
tabrakan dengan section "Batch29" historis yang sudah ada di bawah.

User upload crash log `crash_20260810_134626_b98c4a79...txt` +
`GalleryCleaner-main.zip`, minta fokus "debugging, polish UI/UX, detail
kecil aplikasi generik yang belum diterapkan". Debug Priority diikuti:
crash log dianalisis dulu sebelum minta Logcat/ADB (tidak perlu, log-nya
cukup).

**Bug #1 (root cause OOM, `ImageCompressor.kt`)** — Stack trace crash
sendiri (`com.mediatek.boostfwk...FrameIdentify`, alokasi 32 byte) BUKAN
penyebabnya — itu cuma alokasi kecil apa saja yang kebetulan jalan
persis setelah heap sudah penuh. Penyebab sebenarnya:
`compressInPlace()` decode JPEG di RESOLUSI PENUH (`BitmapFactory.
decodeStream(stream)` tanpa `Options` sama sekali) ke `ARGB_8888` (4
byte/pixel). Device di crash log (Infinix X6855, MediaTek) kemungkinan
kamera 108MP — satu foto = ~12000x9000 = ~430MB SATU bitmap, cukup
sendirian menghabiskan heap 512MB (`largeHeap`). Ditambah lagi
`OutOfMemoryError` `extends Error` bukan `Exception`, jadi `catch (e:
Exception)` yang ada TIDAK PERNAH menangkapnya — begitu terjadi, app
langsung crash total, persis seperti log.
- Fix: baca `bounds` dulu (`inJustDecodeBounds=true`, murah, tidak alokasi
  pixel buffer). Kalau pixel count > 24 megapixel
  (`LARGE_IMAGE_PIXEL_THRESHOLD`), decode pakai `RGB_565` (2 byte/pixel)
  bukan `ARGB_8888` — tetap RESOLUSI PENUH (janji dokumentasi lama "tidak
  downscale" tetap dipegang), cuma bit-depth yang turun untuk kasus
  ekstrem, imperceptible setelah re-encode JPEG quality 80.
- Fix #2 (independen dari #1, sama pentingnya): `catch (e:
  OutOfMemoryError)` ditambahkan eksplisit di 2 titik (decode + write) —
  device dengan heap lebih kecil atau file rusak/header bogus tetap bisa
  OOM walau sudah di-cap; sekarang gagal per-foto (`Result.Failed`) bukan
  crash total.
- Hardening tambahan (`MediaScanner.kt` `decodeSampledBitmap` — pola
  BitmapFactory tanpa OOM-guard yang sama, dipakai blur/near-dup scan):
  tambah `catch (e: OutOfMemoryError)`. Bukan penyebab crash ini (sudah
  pakai `inSampleSize`, jauh lebih aman), murni defensive backstop untuk
  header korup.

**Polish — Success/Undo feedback (`MainActivity.kt`, "detail kecil
aplikasi generik")**: audit `showSnackbar` (13 titik) menemukan SEMUANYA
cuma pesan GAGAL — 0 konfirmasi sukses di seluruh app untuk delete
permanen, organize/move, restore dari trash, maupun commit swipe→trash.
Silent success itu sendiri adalah gap generik dibanding app file-manager
manapun (Google Photos/Files by Google selalu kasih konfirmasi + Undo
kalau reversible). Ditambahkan di 5 titik:
- `deleteRequestLauncher` (API 30+) & `proceedWithPermanentDeletion`
  (legacy, pre-30): "Deleted N photos — freed X MB" (irreversible, tidak
  ada Undo — MediaStore tidak punya un-delete).
- 2 jalur "Organize"/move (`organizeRequestLauncher` API30+ & loop legacy
  di `performOrganize`): "Moved N photos to <folder>", pesan gagal-parsial
  lama tetap tampil menyusul kalau ada yang gagal.
- `onRestore` (TrashScreen "Restore"): "N photos restored" (tidak perlu
  Undo — restore sendiri sudah jadi undo-nya trash).
- `onFinishWithDeletions` (commit sesi Swipe → trash) — SATU-SATUNYA yang
  dikasih tombol **Undo** di Snackbar-nya (`actionLabel="Undo"`,
  `SnackbarDuration.Long`): ini aksi reversible (beda dari delete
  permanen), `trashStore.remove(ids)` adalah invers persis dari
  `addToTrash(ids)` barusan, `activeMedia`/`trashItems` sudah reaktif
  jadi tidak perlu patch `allMedia` manual.

**Tidak disentuh**: per-swipe "Undo last swipe" (`SwipeScreen.kt`, tombol
↩ di action bar) sudah ada sejak lama — dicek dulu supaya tidak
duplikat/konflik dengan Undo Snackbar baru (dua hal beda: satu untuk 1
swipe yang belum di-commit, satu untuk seluruh sesi yang SUDAH di-commit
ke trash).

**Verifikasi**: brace/paren balanced 0/0 di SEMUA file `.kt` (full sweep).
Protected assets tidak tersentuh. Codebase asal (`GalleryCleaner-main.zip`
baru, v28) dipakai sebagai source of truth — bukan sesi lama di chat ini
(Hirarki Konteks: Chat Saat Ini > PROJECT_STATE.md), karena repo GitHub
sudah maju lebih jauh (v28: tema Skeuomorphism-lite) lewat commit di luar
chat ini via Termux.

### Batch31 — Permanently-denied Permission Fix (1 file)
(Android won't re-show its own dialog on re-request once denied).
`MainActivity.kt`: `ActivityCompat.shouldShowRequestPermissionRationale`
check after the launcher result → `permissionPermanentlyDenied` flag →
`PermissionScreen` switches to "Open Settings"
(`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`). `ON_RESUME` observer
added (same pattern as app-lock's) to re-check permission state when
returning from that Settings screen, so granting it there is picked up
immediately, no relaunch needed.

### Batch30 — DangerButton Extraction — Audit Correction (4 file)
ulang menemukan Compress/Organize/Restore semua sudah `GlassButton`. Sisa
3 `Button` mentah (Clean up / Delete N selected / Delete permanently)
dikonfirmasi SENGAJA solid `colorScheme.secondary` (prinsip sama PillChip
"selected" — destructive action butuh sinyal tegas, bukan kaca), bukan
oversight. Diekstrak jadi `DangerButton` (ui/components) untuk hilangkan
3x duplikasi identik. 0 perubahan visual, murni DRY. `MainActivity.kt`
crash dialog TIDAK termasuk (pola beda, sengaja dibiarkan simpel).

### Batch29 — Debugging + UX Polish Pass (4 file)
"detail kecil aplikasi generik yang belum diterapkan". Grep sweep: TODO/FIXME
(0 hasil), empty catch block (0), `contentDescription = null` (5, semua
diaudit satu-satu), Share/ACTION_SEND (0 — gap nyata), pull-to-refresh (0 —
gap nyata tapi tidak dikerjakan, lihat alasan di bawah), sort/group state
(ditemukan: in-memory only, bug nyata).

Dikerjakan:
1. `SettingsStore.kt` + `MainActivity.kt` — `groupMode`/`sortOption` sekarang
   persist ke DataStore (sebelumnya reset ke Month/Date tiap relaunch).
2. `SwipeScreen.kt` — tombol Share (baru, 0 sebelumnya), pakai `MediaItem.uri`
   langsung, tidak perlu FileProvider.
3. `SwipeScreenCard.kt` — `FileInfoDialog` diperkaya (Album/Date taken/
   Dimensions/Path ditambah, `ID` mentah dihapus), pakai field `MediaItem`
   yang sudah ada.
4. `SwipeScreenCard.kt` — `FullscreenViewer` image `contentDescription`
   null → `item.displayName` (aksesibilitas).

Sengaja belum dikerjakan (didokumentasikan, bukan terlewat):
- **Pull-to-refresh gesture** di Home — API M3 `PullToRefreshBox` stabil
  butuh `compose-bom` > `2024.06.00` (versi project saat ini). Tidak
  dipasang tanpa bisa compile-test di sandbox (no network/gradle) — risiko
  break build lebih besar dari manfaatnya untuk 1 gesture tambahan padahal
  tombol refresh manual sudah ada sebagai fallback yang berfungsi.
- `GlassButton` cascade ke seluruh `Button`/`TextButton` M3 — item lama
  sejak Batch14/21, masih terbuka, di luar fokus "debugging + detail kecil"
  batch ini (itu perubahan visual besar, bukan polish/bugfix).

### Batch28 — Skeuo-lite Visibility Fix (2 file: SkeuoLiteTokens.kt, SkeuoModifier.kt)
Permintaan user (dengan screenshot Amber Reserve hasil Batch27): "Efek
timbul skeuomorphism-lite gak kerasa sama sekali. Malah lebih mirip ganti
pallet warna murahan seperti yang saya sebut tadi".

**Diagnosis root cause** (dari screenshot, bukan tebakan): `PanelFill`
Batch27 adalah SATU warna flat (`EspressoSurfaceRaised`), dan
`ShadowColor` adalah near-black yang di-drop-shadow di atas backdrop
espresso yang JUGA near-black — shadow gelap di atas background gelap
= nyaris tidak terlihat sama sekali secara visual. Yang tersisa sebagai
"beda" secara visual hanya warna border — persis "pallet warna murahan"
yang user sebut, meskipun secara ARSITEKTUR (`MaterialStyle`/`skeuoPanel`/
`GlassCard` theme-aware) sudah benar-benar berbeda dari glass sejak
Batch27. Diagnosis: benar secara arsitektur, salah tuning nilai visual.

**Fix (3 bagian, tetap "shadow+fill+border", tidak nambah teknik/`Modifier.blur`
baru)**:
1. `PanelFillGradient` — fill sekarang gradient diagonal (terang di
   pojok kiri-atas → gelap di pojok kanan-bawah), bukan flat color.
   Ini cue paling besar yang hilang: permukaan objek timbul sendiri
   punya gradasi cahaya lintas permukaannya, terlepas dari shadow di
   bawahnya.
2. `SpecularHighlight` — BARU: glow lembut brass/putih di pojok
   kiri-atas panel (`Brush.radialGradient`, dilapis sebagai
   `.background(brush=specular, shape=shape)` KEDUA setelah fill dasar,
   sebelum border, di `skeuoPanel`). Ini cue yang SAMA SEKALI HILANG di
   Batch27 — fill+shadow doang selalu terbaca "kotak dengan border",
   corner-catch inilah yang terbaca "permukaan melengkung/timbul yang
   memantulkan cahaya".
3. `BevelGradient` kontras dinaikkan ~2x (highlight alpha 0x8A→0xF0,
   shadow alpha 0x66→0xB3) + `borderWidth` default `1.5.dp`→`2.dp` di
   `SkeuoModifier.kt`, supaya bevel edge langsung terbaca, bukan cuma
   outline tipis generik.

**Pressed/inset state** (`skeuoInset`): fill gradient dan bevel SAMA-SAMA
dibalik arahnya (bukan cuma warna beda), plus specular DIHILANGKAN sama
sekali (slot terbenam tidak memantulkan cahaya ke pengamat) — 3 pembalikan
sekaligus inilah yang menjual "ketekan masuk", bukan cuma "warna beda".

**Tidak diubah**: `MaterialStyle.kt` (axis GLASS/SKEUO_LITE, mapping
per-AppTheme), `GlassCard.kt`/`GlassButton.kt`/`SwipeScreenControls.kt`
`InfoChip` (semua sudah theme-aware sejak Batch27, tetap panggil
`skeuoPanel()`/`skeuoInset()` dengan API yang sama — HANYA isi token di
dalamnya yang di-retune, jadi 0 file caller lain perlu disentuh lagi).
`ShadowColor` dipertahankan (masih berkontribusi sebagai cue sekunder di
siluet bawah-kanan panel), tapi didokumentasikan eksplisit BUKAN lagi
sinyal utama "timbul" — itu sekarang dibawa oleh gradient fill + specular
+ bevel bersama-sama.

### Batch27 — Amber Reserve → Skeuomorphism-lite (Atomic Change — 8 file)
Permintaan user: "ganti total 'Amber Reserve' jadi theme 'Skeuomorphism-lite'
yang bisa jadi baseline theme seperti 'Glassmorphism' default tanpa
menghapus yang sudah ada (wajib berubah konfigurasi nya. Bukan pallet
warna murahan)". Melebihi batch limit 10 file dikecualikan sebagai bukan
masalah (8 file, di bawah limit) tapi tetap 1 Atomic Change kohesif: satu
axis arsitektur baru (`MaterialStyle`) yang WAJIB ditambahkan bersamaan
supaya konsisten di semua komponen sekaligus.

**Root masalah yang diperbaiki**: sebelum batch ini, `AppTheme` (Signature/
Amber Reserve/Indigo Noir) HANYA mengontrol `ColorScheme` M3 — ketiganya
memakai `GlassCard`/`GlassButton`/`Modifier.glassPanel()` yang SAMA persis
(translucent frosted glass). Jadi "Amber Reserve" secara arsitektur adalah
Signature yang dicat ulang espresso/brass — persis "pallet warna murahan"
yang disebut user di prompt, bukan theme berbeda sungguhan.

**Perbaikan (axis baru, terpisah dari warna)**: file baru
`MaterialStyle.kt` — enum `MaterialStyle { GLASS, SKEUO_LITE }` +
`fun materialStyleFor(appTheme): MaterialStyle` (SIGNATURE→GLASS,
AMBER_RESERVE→SKEUO_LITE, INDIGO_NOIR→GLASS — Indigo Noir SENGAJA
dipertahankan GLASS, tidak diminta user, 0 regresi) + CompositionLocal
`LocalMaterialStyle` (default GLASS, supaya composable manapun yang belum
sempat baca provider tetap render persis seperti sebelum batch ini).
`Theme.kt` → `GalleryCleanerTheme` wrap `content` dengan
`CompositionLocalProvider(LocalMaterialStyle provides materialStyleFor(appTheme))`
di SATU titik, bukan di-pass manual ke tiap composable.

**Token & modifier baru (paralel ke `MidnightGlassTokens.kt`/
`GlassModifier.kt`, BUKAN mengedit keduanya)**: `SkeuoLiteTokens.kt`
(object `SkeuoLite` — fill OPAQUE/matte pakai `EspressoSurfaceRaised`
yang sudah ada, bukan gradient translucent; shadow warna hangat
near-black `0xFF0B0906` bukan `VoidDeep` biru dingin milik glass; bevel
border 2-stop brass-highlight→shadow, plus varian `*Pressed` yang
ARAH-nya dibalik untuk efek deboss, bukan cuma gelapin warna). Varian
light-mode disertakan (`PanelFillLight` dst, pola sama seperti
`MidnightGlass.Ice*`) untuk konsistensi meski belum ada call site yang
threading dark/light flag ke component (keterbatasan yang sama persis
sudah ada di sistem glass — didokumentasikan di komentar `GlassModifier.kt`
lama, tidak diperbaiki di batch ini karena di luar cakupan permintaan).
`SkeuoModifier.kt` — `Modifier.skeuoPanel()`/`Modifier.skeuoInset()`,
teknik SAMA (shadow+fill+border, tanpa `Modifier.blur`, tetap
`minSdk=24`-safe) tapi nilai beda: shape default `12.dp` (vs glass
`18.dp`, kartu ledger lebih "tegas" dari kaca), fill `Color` solid (bukan
`Brush`), border lebih tebal (`1.5.dp` vs `1.dp`).

**Bukan resurrection sistem lama**: `SkeuoLiteTokens.kt`/`SkeuoModifier.kt`
BUKAN mengembalikan `SkeuoMidnightTokens.kt`/`SkeuoMidnightModifier.kt`/
`MidnightSkeuoButton.kt`/`MidnightSkeuoSlot.kt` yang dihapus permanen
Batch21 (metallic multi-layer, debossed slot berat) — "lite" di namanya
sengaja: 1 fill solid + 1 shadow arah + 1 border gradient, budget
komponen SAMA seperti glassPanel, cuma beda nilai.

**Komponen dibuat theme-aware (bukan di-duplicate per tema)**:
- `GlassCard.kt` — baca `LocalMaterialStyle.current`, branch
  `.glassPanel(...)` vs `.skeuoPanel()`. API publik TIDAK berubah (masih
  `GlassCard(modifier, shape, elevation, contentPadding, onClick, enabled) { }`)
  — SEMUA 6 file caller (`HomeScreenSections.kt`, `HomeScreenFolderRow.kt`,
  `SwipeScreenGrid.kt`, `TrashScreen.kt`, dll.) 0 perubahan, otomatis ikut
  material style aktif.
- `GlassButton.kt` — branch penuh dua render path. Mekanisme feedback
  tekan BEDA, bukan cuma warna: Glass = glow label + border lebih terang
  (tidak berubah). Skeuo-lite = swap `skeuoPanel()`→`skeuoInset()` penuh
  saat `isPressed` (shadow hilang, fill+bevel berbalik arah — deboss
  sungguhan, bukan simulasi opacity). API publik tidak berubah, 7 call
  site existing 0 perubahan.
- `SwipeScreenControls.kt` `InfoChip` — satu-satunya raw
  `Modifier.glassPanel()` call site DI LUAR GlassCard/GlassButton (chip
  kecil di atas foto preview, override elevation/border sendiri, lihat
  Batch22). Dibuat theme-aware juga (branch style sama), supaya Amber
  Reserve benar-benar 100% material-swap, bukan "kartu & tombol diganti,
  1 chip kecil kelewatan tetap kaca".

**Tidak diubah (di luar cakupan, sengaja)**: `SettingsStore.kt`/`AppTheme`
enum (masih `SIGNATURE, AMBER_RESERVE, INDIGO_NOIR`, 0 migrasi data),
`Theme.kt` `colorSchemeFor`/`AmberReserveDark`/`AmberReserveLight` (M3
`ColorScheme` Amber Reserve tetap sama — dipakai untuk fallback
Card/Sheet M3 biasa, terpisah dari `SkeuoLite` object), `MainActivity.kt`
root `Surface`/`glassBackdrop` logic (Amber Reserve sudah `glassBackdrop
== null` sejak Batch22 — flat backdrop ini justru PAS untuk skeuomorphic:
panel timbul butuh kanvas matte datar, bukan glow gradient, jadi tidak
perlu disentuh). `SettingsScreen.kt` — hanya 1 baris deskripsi kartu
Amber Reserve diupdate ("Espresso skeuomorphism-lite — raised brass-bevel
panels, not glass."), swatch warna/label/preview color tidak diubah.

### Batch26 — Appearance Toggle Rearchitecture (1 file: SettingsScreen.kt)
Permintaan user (via screenshot Settings): "Rombak arsitektur di sektor
theme. Dari yang awalnya button 1 arah jadi toggle on-off (semua opsi
disama ratakan) yang menyesuaikan dengan mode 'light/dark system'".

**Sebelum**: `ThemeMode.values().forEach { SettingsRadioRow(...) }` — 3
RadioButton (Match system/Light/Dark) single-select, satu-satunya section
berbentuk radio-list di layar Settings (semua section lain di bawahnya
pakai `Row` + `Switch`), jadi menonjol berbeda ("button 1 arah").

**Sesudah**: 2 `Row`+`Switch` toggle, "disama ratakan" ke pola visual yang
sama dengan Backup/Notifications/Swiping/Feedback/Privacy:
- **"Match system"** — `checked = (themeMode == ThemeMode.SYSTEM)`. ON →
  `setThemeMode(SYSTEM)`, brightness ikut `isSystemInDarkTheme()` LIVE
  (recompose otomatis saat sistem ganti tema, resolusi sama seperti
  `MainActivity.kt` sudah lakukan sejak awal — tidak diubah). OFF →
  resolve ke `DARK` atau `LIGHT` konkret berdasarkan status sistem SAAT
  toggle dimatikan (`systemDark` dibaca di composition scope yang sama),
  supaya tidak ada lompatan visual mendadak di momen switch-off.
- **"Dark mode"** — `checked = resolvedDark` (`systemDark` kalau Match
  system ON, else `themeMode == DARK`). `enabled = !matchSystem`: saat
  Match system ON, toggle ini nonaktif TAPI tetap mencerminkan status
  sistem real-time (bukan disembunyikan) — subtitle berubah jadi
  "Currently following the system setting." Saat Match system OFF, toggle
  aktif dan langsung `setThemeMode(DARK/LIGHT)`.

**Dihapus**: composable `SettingsRadioRow` (private fun, satu-satunya
caller adalah section Appearance yang baru saja diganti — di-grep dulu
sebelum hapus, tidak dipakai di file lain kecuali `RadioButton` murni di
`SwipeScreenControls.kt` yang tidak tersentuh/berbeda konteks).

**Tidak diubah**: `SettingsStore.kt` (`ThemeMode` enum tetap SYSTEM/LIGHT/
DARK, key DataStore & default `DARK` untuk install lama — persis sama),
`MainActivity.kt` (resolusi `themeModeFlow` → `darkTheme: Boolean` untuk
`GalleryCleanerTheme` tidak disentuh), `Theme.kt` (tidak disentuh sama
sekali). Ini murni perubahan presentasi UI di satu file, 0 perubahan
skema/behavior data layer.

### Batch25 — Backup-before-delete (ROADMAP Fase B item 7, 4 file)
auto-flag) TERNYATA sudah diimplementasi sejak batch sebelumnya
(`MediaScanner.findNearDuplicates`/`findBlurryPhotos`, terpasang di
HomeScreen section "Smart Detection" — persis yang terlihat di screenshot
user sebelumnya). Roadmap tidak pernah diupdate untuk mencatat ini —
dikoreksi jadi ✅ di Batch25 ini (dokumentasi, bukan kerja baru).

Item 7 (Backup-before-permanent-delete) dibangun baru:
- **`BackupHelper.kt`** (baru) — copy tiap item ke
  `Pictures|Movies/GalleryCleaner/Backup/` sebelum delete. MediaStore
  insert (API 29+, pola sama `CrashLogger`), fallback File I/O langsung
  (API 24-28, pola sama `MoveHelper`/`CrashLogger` legacy path). Deteksi
  video vs image via `contentResolver.getType(uri)` (bukan field baru di
  `MediaItem` — tidak menyentuh model data). Best-effort per item
  (try/catch, gagal 1 file tidak pernah membatalkan delete keseluruhan).
- **`SettingsStore.kt`** — `backupBeforeDeleteEnabledFlow` +
  `setBackupBeforeDeleteEnabled`, default false (opt-in, bukan silent
  default — storage-usage behavior harus sepengetahuan user).
- **`SettingsScreen.kt`** — section baru "Backup" (antara Trash dan
  Notifications), 1 toggle row, pola identik toggle lain di layar ini.
- **`MainActivity.kt`** (edit parsial, protected asset) — `performPermanentDeletion`
  dipecah: fungsi baru `proceedWithPermanentDeletion` (badan asli, tidak
  diubah logikanya) DIDEKLARASIKAN DULU, baru `performPermanentDeletion`
  (nama publik yang dipanggil TrashScreen tetap sama — 0 breaking change
  di call site) yang sekarang cek `backupBeforeDeleteEnabled`: kalau aktif,
  jalankan `BackupHelper.backupBeforeDelete` di `Dispatchers.IO` dulu, baru
  lanjut ke `proceedWithPermanentDeletion` di Main thread; kalau tidak
  aktif, langsung lanjut seperti sebelumnya. Urutan deklarasi ini SENGAJA
  mengikuti pelajaran Batch18 (`applyOrganizeResult`/`organizeRequestLauncher`):
  local function di Kotlin harus sudah dideklarasikan SEBELUM titik
  pemakaiannya — termasuk di dalam lambda bersarang — kalau tidak jadi
  "Unresolved reference" saat build.

**Trade-off yang didokumentasikan (bukan bug)**: backup dijalankan SEBELUM
delete request API 30+ diluncurkan (bukan setelah user konfirmasi), karena
itu satu-satunya titik source `uri` dijamin masih terbaca — begitu delete
sukses, sumbernya sudah hilang. Konsekuensinya: kalau user cancel dialog
sistem, salinan backup tetap ada (dianggap tidak berbahaya — cadangan
ekstra yang tidak dipakai, bukan kerugian).

### Batch24 — Scaffold contentColor Fix (5 file)
Batch22 mengubah SEMUA Scaffold jadi `containerColor = Color.Transparent`
supaya ambient gradient tembus, tapi M3 Scaffold menurunkan `contentColor`
default-nya dari `contentColorFor(containerColor)` — dan warna transparan
bukan warna bertema, jadi hasilnya `Color.Unspecified`, yang pada akhirnya
di-resolve `Text()` sebagai HITAM (bukan warna teks tema). Ini kena semua
`Text()` di 5 layar yang TIDAK set `color=` eksplisit dan TIDAK dibungkus
GlassCard: radio row label ("Match system"/"Light"/"Dark"), judul kartu
color-style ("Signature"/"Amber Reserve"/"Indigo Noir" — `ThemeStyleCard`
pakai `Modifier.background()` manual, bukan `Surface`, jadi juga tidak
dapat contentColor sendiri), dan judul toggle Settings ("Cleaning
reminders"/"Random clean mode"/"Swipe haptics"/"App lock").
Fix: tambah `contentColor = MaterialTheme.colorScheme.onBackground` di
tiap Scaffold (SettingsScreen, HomeScreen, OnboardingScreen, SwipeScreen,
TrashScreen) berdampingan dengan `containerColor = Color.Transparent` yang
sudah ada. Text yang sudah set warna eksplisit (subtitle onSurfaceVariant,
dll) tidak terpengaruh. GlassCard.kt (Batch23) tetap diperlukan terpisah
karena `Box` internalnya tidak mewarisi contentColor otomatis dari Scaffold
manapun — dua fix independen, saling melengkapi, 0 tumpang tindih.

### Batch23 — GlassCard Readability Fix (1 file)
`Surface`), sehingga tidak pernah menyediakan `LocalContentColor` ke children.
Semua `Text()` di dalam GlassCard yang TIDAK set `color=` eksplisit (judul
"Blurry photos"/"Similar photos"/nama bulan "Agustus 2026" dst.) jatuh ke
default keras Compose Material3 `LocalContentColor = Color.Black` — hitam di
atas panel kaca gelap, persis bug readability di screenshot user. Text yang
SUDAH set warna eksplisit (subtitle `onSurfaceVariant`, label "Scan"
`primary`) sudah benar sejak awal — itu sebabnya sebagian teks di kartu yang
sama terlihat OK sementara judulnya hilang.
Fix: satu titik pusat di `GlassCard.kt`, bungkus `content()` dengan
`CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface)`.
Tidak menyentuh file layar manapun — semua GlassCard call site (HomeScreen,
TrashScreen, SwipeScreen, SettingsScreen) otomatis ikut fix tanpa edit
per-lokasi. `GlassButton`/`InfoChip` tidak disentuh (sudah set warna eksplisit
sejak awal, tidak terpengaruh bug ini).

### Batch22 — Glassmorphism Component Cascade (Atomic Change — 9 file)
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

### Batch21 — Theme Rewrite: Glassmorphism Midnight Blue Edition (Atomic Change)
- `AppTheme.SIGNATURE` (default) = `ui/theme/MidnightGlassTokens.kt` (`MidnightGlass`) + `ui/theme/Theme.kt` (`SignatureDark`/`SignatureLight`) + `ui/components/{GlassModifier,GlassCard,GlassButton}.kt`.
- ColorScheme level (background/surface/surfaceVariant/tertiary/outline) = 100% rewritten, applies automatically everywhere via `MaterialTheme.colorScheme`. `MainActivity.kt` root `Surface` also paints the ambient gradient backdrop (Signature only).
- Component level (NOT yet done, next batch if wanted): dashboard/list `Card`s in HomeScreen/SwipeScreen/TrashScreen still use plain M3 `Card` — swap to `GlassCard` per-screen for a fully "kaca" look on every panel, not just the ColorScheme base. `GlassButton` similarly not yet cascaded to the app's ~17 M3 `Button`/`TextButton` call sites (same open item as Batch14's audit, now against Glass API instead of Skeuo).
- Amber Reserve / Indigo Noir themes: untouched by this rewrite, still their original flat-color style (out of scope — user asked specifically about the default/Signature theme).
- No `Modifier.blur`/RenderEffect anywhere — deliberate, see `GlassModifier.kt` doc comment (API31+ only, `minSdk=24`).


**Sistem sebelumnya (AMOLED, digantikan batch ini):**
- Sumber: spec markdown yang diupload user (793 baris, 25 section). Diimplementasikan sebagai arsitektur §23:
  `ui/theme/{Color,Shape,Typography,GlassTokens,TactileTokens,Theme}.kt` + `ui/components/{GlassSurface,GlassCard,TactileButton,TactileSwitch,TactileSlider,GlassNavigation}.kt`.
- `AppTheme.SIGNATURE` (default aplikasi, tidak berubah — tetap default) di-override total: background=AmoledBlack(#030508), surface=GlassBase(#0A0F16), surfaceVariant=GlassElevated(#101722), outline=GlassBorder(alpha 3.5%), tertiary=AccentBlue(#6670FF) untuk selection/focus/progress (§17).
- Primary(SageKeep)/Secondary(CoralDelete) TIDAK diubah — semantik Keep/Delete swipe adalah app-critical UX, di luar cakupan spec (spec generik, tidak tahu soal keep/delete). Diperlakukan sebagai lapisan terpisah dari AMOLED/Glass/Midnight-Blue/Accent yang murni tentang materi permukaan & functional accent.
- Midnight Blue (§6) diimplementasikan sebagai `midnightAmbientGradient()` brush helper (bukan warna solid ColorScheme) — dipakai opsional lewat `GlassSurface(ambient = true)`, TIDAK dipasang otomatis ke background global (sesuai §6 "Incorrect use": jangan solid background).
- `SignatureLight` (mode terang): spec ini AMOLED-only by definition, tidak mendefinisikan light mode. Diberi tertiary=AccentBlue (darker variant) untuk konsistensi lintas mode, sisanya dipertahankan dari sebelumnya. Catatan: ini adaptasi di luar cakupan literal spec.
- Cascading: HomeScreen/SwipeScreen/TrashScreen/SettingsScreen/MainActivity semua sudah pakai `MaterialTheme.colorScheme` (bukan warna hardcoded) → override Theme.kt otomatis merambat ke seluruh app tanpa perlu edit tiap layar.
- Komponen baru (`GlassSurface`, `GlassCard`, `TactileButton`, `TactileSwitch`, `TactileSlider`, `GlassNavigation`) SUDAH DIBUAT tapi BELUM dipasang menggantikan Box/Card/Button/Switch bawaan Material3 di layar existing — itu §11-15 (tactile buttons/switch/slider per-komponen) masih pakai default M3 look. Batch berikutnya: migrasi pemakaian di HomeScreen/SwipeScreen/SettingsScreen ke komponen baru ini bila ingin 100% tactile-glass look di setiap kontrol (saat ini baru level ColorScheme yang 100% sesuai spec, bukan level component).
- Dead tokens (belum dihapus, nunggu izin): `GraphiteSurface`, `GraphiteSurfaceRaised`, `GraphiteOutline`, `TextSecondary`, `TextMuted` di `Color.kt` sudah tidak direferensikan setelah override ini.

### Batch20 — ROADMAP Fase A Selesai 4/4 — Sort di Swipe + Fix Nama APK Release
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

**Fix nama file APK Release (permintaan user, batch sama):**
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

**Arsip catatan pending (ditulis sekitar batch ini):**
- ~~ROADMAP Fase A item 4~~ — ✅ shipped Batch20, Fase A selesai 4/4. Lanjut Fase B (AI on-device: duplicate/blur detection, backup-before-delete) — belum dimulai.
- Filmstrip belum secara visual meredupkan item yang sudah di-organize (Batch17, kosmetik minor, masih terbuka).
- Belum ada test end-to-end nyata untuk Organize (no emulator di sandbox) — sudah lolos 1x CI fix (Batch18), masih belum dikonfirmasi manual di device asli terutama jalur legacy API 24-28.
- ~~Batch10-19 belum ada 1 run CI hijau yang terkonfirmasi user~~ — ✅ terkonfirmasi Batch20: user melampirkan screenshot GitHub Release v1.0.143 sukses (APK 11.2MB ter-publish, signed, run142-ish). CI hijau sejak fix Batch18.
- **Item lama, masih menunggu keputusan user**: (a) cascade `MidnightSkeuoButton`/`MidnightSkeuoSlot` — butuh keputusan extend-warna vs cascade-parsial (detail lengkap di section Batch14 di bawah); (b) Phase-1b flat→sub-package restructure — masih butuh compiler nyata per-layer, tidak tersedia di sandbox.
- **Temuan baru (bukan diminta, sekadar dicatat)**: `VERSION_NAME` di nama file APK (dari `git rev-list --count HEAD`, mis. "1.0.22") tidak sinkron dengan nomor tag GitHub Release (dari `github.run_number`, mis. "v1.0.143") — dua skema angka berbeda dalam 1 workflow. Belum diminta user untuk disatukan, dibiarkan sampai ada instruksi eksplisit.

### Batch19 — Cleanup Goal
Mengeksekusi item 3 Fase A di `ROADMAP.md` — item terakhir yang kompetitor
(Sponge) sendiri belum ship per riset Batch15, jadi ini genuinely "duluan"
bukan cuma catch-up.
- `SettingsStore.kt`: `cleanupGoalBytesFlow`/`setCleanupGoalBytes(Long)`, key `cleanup_goal_bytes`. `DEFAULT_CLEANUP_GOAL_BYTES = 2_000_000_000L` (top-level const, dipakai juga sebagai default param di `HomeScreen`/`StorageDashboard` biar konsisten kalau flow belum ke-collect). Setter coerce `≥1L` — melindungi progress-bar division (`totalFreedBytes / cleanupGoalBytes`) dari divide-by-zero kalau user entah bagaimana set 0.
- `HomeScreenSections.kt` — `StorageDashboard` diperluas: baris "Cleanup goal" (tap → buka dialog) + `LinearProgressIndicator` modern (`progress: () -> Float` lambda API, sesuai compose-bom 2024.06.00 / Material3 1.2.x — bukan overload Float lama yang deprecated). Warna primary + pesan "Goal reached!" saat progress ≥100%. `CleanupGoalDialog` (private, sama file): slider 100MB..20GB + 5 preset chip (500MB/1/2/5/10GB), preset ke-highlight kalau slider persis di situ.
- `HomeScreen.kt`/`MainActivity.kt`: parameter tambahan diteruskan end-to-end (`cleanupGoalBytes`, `onCleanupGoalChange`), collect di `AppRoot` sejajar `totalFreedBytes`/`totalDeletedCount` yang sudah ada.
- **Desain sadar**: goal ditrack terhadap `totalFreedBytes` ALL-TIME (bukan per-bulan/per-minggu). Tidak ada auto-reset. Kalau user mau "goal baru bulan ini", mereka set ulang manual — konsisten dengan baris "All time: X freed" yang sudah lebih dulu ada di dashboard yang sama (kalau goal tracked periodik tapi baris di sebelahnya all-time, dua angka storage yang bersebelahan tapi beda basis waktu akan membingungkan).
- Verifikasi: brace/paren balanced 0/0 di 4 file. Single call-site untuk `StorageDashboard(`/`HomeScreen(`.

### Batch18 — Fix Build Failure (applyOrganizeResult forward-reference)
- Error: `MainActivity.kt:629:21 Unresolved reference: applyOrganizeResult`, task `:app:compileReleaseKotlin` FAILED.
- Sebab: di Batch17, `fun applyOrganizeResult(...)` didefinisikan SETELAH `organizeRequestLauncher` — padahal callback lambda `organizeRequestLauncher` memanggilnya. Local function di Kotlin (beda dari top-level function) harus sudah ada di scope pada titik pemakaian, termasuk di dalam lambda yang baru dieksekusi belakangan — urutan deklarasi tekstual tetap dicek compiler.
- Fix: pindahkan blok `applyOrganizeResult` ke atas, sebelum `pendingOrganizeRetry`/`organizeRequestLauncher`/`performOrganize`. Isi fungsi tidak diubah sama sekali, murni reorder.
- Verifikasi: brace/paren balanced 0/0 di `MainActivity.kt`. Grep manual seluruh local fun lain (`performCompression`, `performPermanentDeletion`, `performOrganize`) — tidak ada pola forward-reference serupa di tempat lain.
- Log CI cuma menunjukkan 1 error (compiler Kotlin berhenti di error pertama untuk file itu) — tidak ada error kedua yang perlu diantisipasi setelah fix ini, tapi tetap perlu 1x run CI nyata untuk konfirmasi hijau (sesuai item "Batch10-14 belum dikonfirmasi hijau" — sekarang bertambah "Batch15-18 juga belum").

### Batch17 — Organize — 3rd Swipe Action
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

### Batch16 — Random Clean Mode
**Catatan status saat ditulis:**
- **ROADMAP Fase A item 3 — Cleanup goal**: ✅ shipped Batch19, lihat section di atas.
- **ROADMAP Fase A item 4 — verifikasi Sort di layar Swipe**: masih pending, lihat section "Belum Dikerjakan" teratas.

Mengeksekusi item pertama Fase A di `ROADMAP.md` ("tutup gap fungsional inti").
- `SettingsStore.kt`: `randomModeEnabledFlow`/`setRandomModeEnabled(Boolean)` — key baru `random_mode_enabled`, default `false`.
- `HomeScreen.kt`: param baru `randomModeEnabled: Boolean = false`, `onRandomModeToggle: (Boolean) -> Unit = {}`. Ikon Shuffle di top bar (antara Refresh dan Settings), tint primary saat aktif — quick toggle tanpa masuk Settings.
- `SettingsScreen.kt`: section baru "Swiping" (di atas "Feedback") dengan `Switch` yang bind ke setting yang sama persis — dua entry point, satu sumber kebenaran (DataStore), konsisten dengan pola existing (haptics, app lock, dll).
- `MainActivity.kt`: `onGroupClick` di `HomeScreen(...)` sekarang cek `randomModeEnabled` — bila aktif, `selectedGroup = group.copy(items = group.items.shuffled())` sebelum masuk `SwipeScreen`; bila tidak, group asli tanpa diubah. `randomModeEnabledFlow` di-collect di `AppRoot`, diteruskan ke `HomeScreen` + dipakai di shuffle logic.
- **Tradeoff sadar (didokumentasikan di doc comment `randomModeEnabledFlow`)**: reshuffle terjadi tiap kali folder dibuka, bukan sekali lalu dipersist per-folder. `ProgressStore` menyimpan index integer per `group.key` (bukan per-item), jadi resume setelah keluar-masuk ulang sebuah folder di mode random akan menempatkan index yang sama tapi urutan item yang berbeda (karena reshuffle baru). Ini disengaja — mode random secara sifat adalah "sampling ulang", bukan "lanjutkan urutan tetap"; behavior identik saat mode OFF (urutan asli, resume akurat) tidak berubah sama sekali.
- Verifikasi: brace/paren balanced 0/0 di 4 file yang disentuh. `group.key` tidak diubah oleh `.copy(items=...)` — semua fitur lain yang bergantung ke key (folder label, progress, trash) tidak terpengaruh.

### Batch15 — ROADMAP.md Dibuat (riset kompetitif vs Sponge)
- File baru: `ROADMAP.md` (root) — riset kompetitif "Sponge - Gallery Cleaner" (web search real, bukan asumsi) + audit jujur fitur project ini yang sudah setara/lebih unggul vs yang masih gap.
- 4 Fase: (A) tutup gap fungsional inti — random mode, 3rd swipe action "organize", cleanup goal (window peluang: Sponge sendiri baru rencanakan ini per Juli 2026); (B) diferensiasi AI on-device — duplicate detection, blur detection, backup-before-delete; (C) lanjutan kerja existing — keputusan MidnightSkeuoButton cascade, Phase-1b, CI hijau; (D) jangkauan pasar — multi-bahasa, monetisasi one-time-purchase, Play Store readiness.
- Lihat `ROADMAP.md` untuk detail lengkap + sumber riset. **Catatan (Batch17): item "backend moveTo sudah ada" di paragraf ini adalah klaim yang ternyata salah, dikoreksi di Batch17 — lihat section "Organize" di atas.**

### Batch14 — Dead Token Cleanup (Color.kt)
User approve pending item dari Batch12 ("Approval dibutuhkan untuk hapus dead color tokens"). Diverifikasi ulang dulu (grep lintas SELURUH project, bukan cuma app/src) karena token dead ini terakumulasi dari 2 override tema berturut-turut (AMOLED Batch2, lalu Midnight Batch13) yang tidak pernah membersihkan sisa palet "Graphite" original:
- Dihapus (0 referensi nyata, hanya deklarasi diri sendiri di `Color.kt`): `GraphiteBg`, `GraphiteSurface`, `GraphiteSurfaceRaised`, `GraphiteOutline`, `TextPrimary`, `TextSecondary`, `TextMuted` (versi top-level lama — beda dari `SkeuoMidnightTheme.TextMuted` yang masih dipakai penuh), `AccentGold`, `SageKeepDim`, `CoralDeleteDim`.
- Dipertahankan (masih dipakai `Theme.kt`/`SettingsScreen.kt`): `SageKeep`, `CoralDelete` (primary/secondary Signature + swatch picker) — sengaja tidak disentuh, app-critical Keep/Delete semantic, precedent sejak Batch2.
- `Color.kt`: 73 baris → 46 baris. Palet Amber Reserve & Indigo Noir (2 theme style lain) TIDAK disentuh — semua tokennya masih aktif dipakai `Theme.kt`.
- Verifikasi: brace/paren balanced 0/0 di seluruh `app/src/**/*.kt` (bukan cuma file yang diedit), grep ulang pasca-hapus mengonfirmasi `SageKeep`/`CoralDelete` masih wired penuh.

**Arsip pending saat itu:**
- **ROADMAP Fase A item 2 — 3rd swipe action "Organize"**: `moveTo` primitive sudah ada di `MediaDataSource`, tapi belum diekspos ke `SwipeDecision` (baru `Keep`/`Delete`) atau UI (`SwipeScreenControls`/`SwipeCard`). Butuh: extend `SwipeDecision` sealed class, folder-picker dialog, wiring swipe-up gesture atau tombol ke-3. Lebih invasif dari random mode (Batch16) — batch terpisah.
- **ROADMAP Fase A item 3 — Cleanup goal**: target storage/jumlah foto + progress bar di HomeScreen. Belum ada model data untuk goal tersimpan (perlu `SettingsStore` key baru + UI slider/input + progress calculation dari `StatsStore`).
- **ROADMAP Fase A item 4 — verifikasi Sort di layar Swipe**: `SortOption` dipakai di Home, belum dicek/dipasang eksplisit di `SwipeScreen`/`Filmstrip`.
- **Cascade `MidnightSkeuoButton`/`MidnightSkeuoSlot` ke layar lain** — diaudit ulang Batch14: TERNYATA sebagian besar `Button(`/`TextButton(` di HomeScreen/SwipeScreen/TrashScreen/OnboardingScreen (17 titik, 8 file) TIDAK cocok jadi swap langsung. Alasan: (1) banyak adalah `TextButton`/`OutlinedButton` kecil di dalam AlertDialog (Cancel/OK/Reset) — mengubahnya jadi tombol skeuomorphic timbul 56dp akan merusak proporsi dialog; (2) satu `Button` di `HomeScreenSections.kt` (tombol "Clean up") sengaja pakai `colorScheme.secondary` (CoralDelete) untuk makna semantik delete — `MidnightSkeuoButton` dari spec tidak punya parameter warna (hardcode `RaisedGradient`+`TextMuted`/`ElectricCyan`), swap paksa akan menghilangkan sinyal warna Keep/Delete yang app-critical. Kesimpulan: cascade literal spec (tanpa extend API) TIDAK aman untuk 5+ dari 17 titik ini — butuh keputusan user dulu: (a) extend `MidnightSkeuoButton` dengan parameter warna opsional (di luar cakupan spec asli), atau (b) cascade hanya ke situs yang benar-benar netral/non-semantik. BELUM dieksekusi, menunggu arahan.
- Phase-1b (flat package → real sub-package) — masih butuh compiler nyata per-layer, tidak tersedia di environment ini.
- `IconButton`/`RadioButton`/`FilterChip` di `SettingsScreen.kt` masih M3 default — spec Midnight tidak menyediakan varian untuk itu.
- Batch10-14 belum dikonfirmasi hijau di CI — perlu push & cek run berikutnya.

### Batch13 — FULL Theme Override — Skeuomorphism-Dark Midnight Blue Edition
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

### Batch12 — Theme Override — Skeuomorphism-Dark
Sumber: `Panduan_Skeuomorphism___Dark_Kotlin.md` (165 baris, 6 section) diupload user, permintaan eksplisit: override tema sekarang, 100% sesuai spec markdown.
- **File baru (3), logic 100% copy dari spec, tidak ditulis ulang:**
  - `ui/theme/SkeuoTokens.kt` — §2 palette (`DarkSurface` #1E1F22, `DarkShadow` #0C0D0F, `LightHighlight` #2E3136, `AccentNeon` #00FFCC) + §4 `metallicDarkBrush` (procedural gradient, bukan bitmap texture).
  - `ui/components/SkeuoModifier.kt` — §3 `Modifier.skeuomorphicDark(cornerRadius, elevation)`, drawBehind + `setShadowLayer` 2x (drop shadow gelap bawah-kanan, highlight terang atas-kiri) via Canvas native — GPU-accelerated, bukan tumpukan Box+blur (§6.1/§6.2). Satu-satunya perubahan dari spec: `DarkShadow`/`LightHighlight` diimpor dari `ui.theme` (spec asli 1 file, project ini pisah token dari komponen, sesuai struktur `ui/theme/` vs `ui/components/` yang sudah ada).
  - `ui/components/SkeuoDarkButton.kt` — §5 `SkeuoDarkButton`, gabungan modifier+brush di atas. Catatan spec asli dipertahankan: `isPressed` masih placeholder (belum ada `pointerInput` ACTION_DOWN/UP nyata) — bukan bug baru, itu keterbatasan yang sudah ada di spec sumber.
- **`Theme.kt` — `SignatureDark` override:** `background`→`DarkShadow`, `surface`→`DarkSurface`, `surfaceVariant`→`LightHighlight`, `tertiary`→`AccentNeon` (accent/indikator, gantikan `AccentBlue`). `primary`/`secondary` (SageKeep/CoralDelete, Keep/Delete semantic) TIDAK diubah — aturan project yang sama sejak override tema pertama (Batch2), di luar cakupan spec visual manapun. `SignatureLight` TIDAK disentuh — spec ini "Dark" by name/definisi, sama seperti precedent AMOLED sebelumnya.
- **⚠️ PENTING — batas cakupan batch ini (jujur, bukan 100% visual cascade):** Override `ColorScheme` HANYA mengubah komponen yang baca dari `MaterialTheme.colorScheme` (Scaffold, TopAppBar, Text default, dll). `GlassCard`/`GlassSurface`/`TactileButton`/`TactileSwitch`/`TactileSlider`/`GlassNavigation` (dipakai di HomeScreen/SwipeScreen/SettingsScreen/TrashScreen) HARDCODE token dari `GlassTokens.kt` (`GlassBase`/`GlassElevated`/`GlassBorder`/`AccentBlue`) langsung, BUKAN lewat `colorScheme` — jadi visual translucent-glass-blur pada komponen itu TIDAK otomatis berubah jadi skeuomorphic solid-material+drawn-shadow dari batch ini saja. Ini bukan oversight — swap teknik render (translucent alpha-layer vs solid material+Canvas-drawn shadow/highlight) di 6 komponen bersama yang dipakai di semua layar adalah perubahan fondasi visual berisiko tinggi tanpa compiler nyata untuk verifikasi; dipecah jadi batch terpisah (lihat Belum Dikerjakan).
- Verifikasi: brace/paren balanced 0/0 di 4 file (3 baru + Theme.kt), tidak ada import yatim, `GlassTokens.kt` tidak disentuh (masih dipakai penuh oleh 6 komponen glass di atas).

### Batch11 — GlassSurface API Extension + ThemeStyleCard Migration
Scope batch ini: 3 file — `GlassSurface.kt`, `GlassCard.kt` (perluasan API), `SettingsScreen.kt` (migrasi `ThemeStyleCard`).
- `GlassSurface`: tambah param `borderWidth: Dp = 1.dp` (sebelumnya hardcoded `1.dp` di `.border(...)`). Default identik, jadi 0 perubahan visual untuk semua caller existing (`GlassCard`, `GlassNavigationBar` — keduanya pakai named args, tidak kena positional-arg breakage dari param baru).
- `GlassCard`: tambah 3 param opsional — `shape` (default tetap `ShapeCard`/18dp), `borderColor` (default tetap `GlassBorder`), `borderWidth` (default tetap `1.dp`) — semua diteruskan ke `GlassSurface`. Karena semua ada default value yang match behavior lama, 0 breaking change untuk pemanggil manapun (saat ini belum ada pemanggil selain batch ini sendiri).
- `SettingsScreen.kt` — `ThemeStyleCard` dikonversi dari Row manual (`clip`+`background(surfaceVariant alpha .5f)`+`border` M3 biasa) → `GlassCard` (shape dipertahankan `RoundedCornerShape(14.dp)` biar radius visual TIDAK berubah, bukan default `ShapeCard` 18dp — sengaja, supaya 0 regresi visual radius). Selection state (`selected`) tetap sama persis: `borderColor` primary vs `GlassBorder`, `borderWidth` 2dp vs 1dp — hanya sumber warna default (`GlassBorder`) yang sekarang dari token tema resmi, bukan `MaterialTheme.colorScheme.outline` lama. Background sekarang pakai token Glass resmi (`GlassBase`, via `GlassSurface` level 1) menggantikan `surfaceVariant.copy(alpha=0.5f)` — sesuai §14 "glass surfaces first", konsisten dengan sisa app.
- Isi Row internal (swatch dots, label, description, check icon) 100% tidak diubah — hanya dipindah ke dalam `content` lambda `GlassCard`, `Modifier` chain di Row itu sendiri dikosongkan (background/border/clip/clickable/padding lama dihapus dari Row, sekarang jadi tanggung jawab `GlassCard`).
- Import dibersihkan: `androidx.compose.foundation.border` dihapus (sudah tidak dipakai di file ini setelah migrasi), `GlassCard`+`GlassBorder` ditambah.
- Verifikasi: brace/paren balanced 0/0 di ketiga file, `GlassNavigationBar` (satu-satunya caller `GlassSurface` lain) dicek pakai named-args sehingga tidak kena breaking change dari param baru.

### Batch10 — Tactile Component Migration (SettingsScreen.kt)
Scope batch ini: HANYA 3 pemakaian M3 `Switch(...)` di `SettingsScreen.kt` → `TactileSwitch(...)` (§12, `ui/components/TactileSwitch.kt`).
- API drop-in identik: `checked`, `onCheckedChange`, `modifier`, `enabled` — 0 perubahan logic, hanya nama composable + 1 import baru (`com.example.gallerycleaner.ui.components.TactileSwitch`).
- Diverifikasi: `Switch(` sudah 0 pemakaian tersisa di seluruh project (`grep -rl` across semua screen mengonfirmasi `SettingsScreen.kt` adalah satu-satunya file yang pernah pakai `Switch`), brace/paren balanced (0/0).
- `ThemeStyleCard` (custom Row+clip+background+border+clickable di file yang sama) SENGAJA belum dikonversi ke `GlassCard` — `GlassCard`/`GlassSurface` tidak punya parameter border-color/width dinamis untuk state "selected" (border primary 2dp vs outline 1dp yang dipakai sekarang), jadi konversi paksa akan menghilangkan visual selection indicator yang sudah berfungsi. Butuh perluasan API `GlassSurface` dulu (tambah `borderWidth` param) sebelum migrasi ini aman — next batch, bukan bagian atomic ini.
- `IconButton`/`RadioButton`/`FilterChip` di file yang sama TIDAK diubah — belum ada varian tactile/glass untuk itu di `ui/components/` (baru ada Button/Card/Switch/Slider/Surface/Navigation), migrasi butuh komponen baru dulu.
- File lain yang pakai `Card(`/`Button(` M3 asli (HomeScreen*, SwipeScreen*, TrashScreen, OnboardingScreen) belum diaudit di batch ini — next batch per-file, pola sama.

### Batch9 — God File Split — SwipeScreen.kt
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

**Status build (arsip, snapshot hingga Batch9):**
Batch1: FAILED→fixed. Batch2: FAILED(compile)→fixed Batch3. Batch4: FAILED(`onUncaughtException` typo)→fixed Batch5. Batch6: OK, build hijau (dikonfirmasi user). Batch7: OK, build hijau (dikonfirmasi user). Batch8+Batch9: OK, build hijau (dikonfirmasi user). Batch10, Batch11, Batch12 (ini): belum ter-CI.

### Batch8 — God File Split — HomeScreen.kt
1001 baris → 4 file, teknik sama seperti Batch7 (extract by exact line range, tidak ada logic ditulis ulang):
- `HomeScreen.kt` (361 baris) — composable utama saja (Scaffold, search state, LazyColumn orchestration).
- `HomeScreenSearch.kt` (126 baris) — SearchResultsContent, SearchPhotoGrid.
- `HomeScreenSections.kt` (384 baris) — ExpiryBanner, SectionLabel, LargestFilesCard, StorageDashboard, OnThisDayRow, ScanTriggerRow, SmartCategoryRow, FilterRow, PillChip.
- `HomeScreenFolderRow.kt` (235 baris) — GroupRow, RenameFolderDialog, CoverThumbnail, ProgressRing.
- Semua 15 sub-composable diubah `private fun` → `internal fun` (Kotlin: `private` top-level = file-scoped, jadi wajib `internal` biar bisa dipanggil lintas file dalam 1 module — ini SATU-SATUNYA perubahan kode selain lokasi file; isi fungsi 100% identik).
- Verifikasi: 16/16 fungsi (1 utama + 15 sub) terkonfirmasi ada, brace/paren balanced per file, call-graph silang (SectionLabel/GroupRow/PillChip/CoverThumbnail/ProgressRing/RenameFolderDialog dipanggil lintas file baru) dicek manual — semua sudah `internal`. Dicek juga: tidak ada file LAIN (SettingsScreen, MainActivity, dst) yang bergantung pada nama-nama ini (false positive `SettingsSectionLabel` dikecualikan).
- SwipeScreen.kt (822 baris) — SELESAI di Batch9 (lihat section di atas).

### Batch7 — God File Split — MediaRepository.kt
Scope batch ini: HANYA `MediaRepository.kt` (517 baris). HomeScreen(1001)/SwipeScreen(822) belum — itu Compose state extraction, jauh lebih berisiko tanpa compiler nyata, next batch terpisah.
- `MediaDataSource.kt` (baru, 150 baris) — raw MediaStore paging I/O: `loadAllMedia`, `loadMediaProgressively`, `queryMediaPage`.
- `MediaScanner.kt` (baru, 322 baris) — analytical/CPU-heavy scans: `smartCategories`, `onThisDay`, `findExactDuplicates`, `findBlurryPhotos`, `findNearDuplicates` + semua private helper (hash/decode/laplacian/aHash).
- `MediaRepository.kt` (107 baris) — jadi **facade tipis**: `group`/`sortItems`/`monthKey` tetap di sini (orkestrasi), 8 fungsi publik lain jadi one-line delegator ke MediaDataSource/MediaScanner.
- **Kenapa facade, bukan pindah caller**: semua caller existing (`MainActivity.kt`, `CleaningReminderWorker.kt`) tetap manggil `MediaRepository.xxx(...)` tanpa perubahan sama sekali — 0 file lain disentuh, 0 risiko missed call-site. Isi fungsi 100% copy-paste (bukan ditulis ulang) dari file lama, jadi behavior dijamin identik.
- Verifikasi: brace/paren balanced per file, 8/8 fungsi publik asli masih ada & bisa dipanggil dengan signature sama persis.

### Batch6 — largeHeap Fix (OOM Root Cause) + CI Artifact Rename
`java.lang.OutOfMemoryError` saat Compose recomposition di LazyColumn (grid HomeScreen/TrashScreen), heap target hanya 256MB (`android:largeHeap` belum diset). Titik crash (`MutableObjectIntMap.initializeStorage`, alokasi 40 byte) cuma korban terakhir — bukan penyebab asli; tekanan memori kumulatif dari bitmap cache + LazySaveableStateHolder yang menahan state item off-screen.
Fix: `AndroidManifest.xml` (protected, edit parsial) — tambah `android:largeHeap="true"`. Aman dilakukan sekarang karena `GalleryCleanerApp.kt` sudah pin Coil memory/disk cache ke `maxSizePercent` tetap (0.15/0.02), BUKAN ke memori "available" versi `ActivityManager` — jadi alasan lama untuk menghindari largeHeap (cache ikut membesar) sudah tidak berlaku (lihat komentar existing di file itu).
`MediaPreview.kt` / decode size / `lowMemory=true` di semua grid call-site sudah benar sejak awal — bukan bagian dari masalah.

Nama artifact log kegagalan build diubah agar lebih informatif & unik per-run:
- Sebelum: `test-result-<branch>-attempt-<run_attempt>.log`
- Sesudah: `log-fail_<branch>_run<run_number>-attempt<run_attempt>_<short_sha>.log`
- Alasan: `run_number` + `short_sha` membuat tiap artifact unik lintas run (bukan hanya lintas attempt dalam 1 run), memudahkan lacak balik ke commit persis yang gagal.

### Batch5 — Fix onUncaughtException + Phase-1 Package Restructure
`CrashLogger.kt:40` — `Unresolved reference: onUncaughtException`. Nama method salah; interface `Thread.UncaughtExceptionHandler` method-nya `uncaughtException`, bukan `onUncaughtException`. DIPERBAIKI.

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

### Batch1 — Fix Gradle Brace + CI Log Artifact on Failure
1. Fix `app/build.gradle.kts` — tambah `}` yang hilang pada signingConfigs.release (baris ~61-62), penyebab `Expecting '}'` di line 123.
2. Update `.github/workflows/build.yml` — step build sekarang tee output ke `test-result-<branch>-attempt-<run_attempt>.log` dan upload sebagai artifact HANYA jika job gagal (`if: failure()`), agar log kegagalan berikutnya tinggal diambil dari GitHub Actions Artifacts tanpa perlu re-run.
- 4 GradleException guard clause (keystore path/password/alias/key password) — diverifikasi struktur benar sejak fix Batch1; masih perlu 1x CI run hijau sebagai bukti final.

(Lihat "Belum Dikerjakan" di bagian atas file ini untuk daftar pending terkini — item lama di sini sudah diproses/superseded.)
