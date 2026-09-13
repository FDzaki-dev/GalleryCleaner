<!--
Ditanamkan permanen ke project Batch45 (sebelumnya cuma ada sebagai upload
chat sesi lama — makanya jadi BLOCKER saat sesi baru mulai dari ZIP hard-reset
tanpa histori chat). Isi di bawah ini VERBATIM dari
`GalleryCleaner_v37_Audit_Gap_Final.md` yang diupload user, tidak diedit.

Status pengerjaan tiap temuan dilacak live di PROJECT_STATE.md, bagian
"AUDIT GAP TRACKER" — file ini murni arsip sumber, bukan tracker.
-->

# GalleryCleaner v37 — Audit Nyata
## Gap Terbesar Menuju 100% Functional & Polished

**Target:** production-grade Gallery Cleaner  
**Audit basis:** `GalleryCleaner_v37_Batch37.zip`  
**Catatan:** audit bersifat static/code-level. Tidak mengklaim runtime/device verification.

---

## VERDICT

**Belum 100% functional & polished.**

Fondasi aplikasi sudah cukup matang, tetapi masih terdapat beberapa gap besar yang secara langsung menghambat level production-grade.

---

# 🔴 P0 — WAJIB DIBERESKAN

## 1. Klaim "photos/videos" tidak sesuai implementasi scanner

- README mengklaim aplikasi memuat **photos + videos**.
- `MediaDataSource.kt` hanya query:
  `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
- Tidak ada query `MediaStore.Video.Media`.
- Manifest hanya meminta `READ_MEDIA_IMAGES`, bukan `READ_MEDIA_VIDEO`.
- `MediaItem` tidak memiliki pembeda media type.
- **Dampak:** video secara praktis belum menjadi bagian library utama.
- **Ini adalah functional gap terbesar.**

## 2. "Trash" bukan trash filesystem

- `TrashStore.kt` hanya menyimpan `id + timestamp` di DataStore.
- Swipe Delete tidak memindahkan file ke trash OS/folder trash.
- Restore hanya menghapus ID dari `TrashStore`; file sebenarnya belum dihapus.
- Secara UX ini lebih tepat disebut **virtual review queue / pending deletion**.
- Jika targetnya cleaner/gallery serius, semantics harus dipertegas atau implementasi trash sebenarnya dibuat.

## 3. Expiry Trash tidak otomatis mengeksekusi deletion

- `expiredItemIdsFlow()` hanya menentukan item yang melewati retention.
- Permanent deletion tetap dipicu UI melalui `performPermanentDeletion(...)`.
- Jika user tidak membuka aplikasi, file tidak diproses.
- Jadi behavior aktual belum sama dengan ekspektasi "automatic cleanup".

## 4. App Lock tidak sesuai klaim README

README mengklaim:

> PIN/biometric

Implementasi aktual menggunakan:

`KeyguardManager.createConfirmDeviceCredentialIntent()`

Tidak ditemukan:

- PIN aplikasi sendiri.
- `BiometricPrompt`.

Yang digunakan adalah **device/screen credential**.

Jadi dokumentasi/UX harus dikoreksi atau fitur lock diperluas.

---

# 🟠 P1 — GAP BESAR MENUJU POLISHED

## 5. Progressive loading melakukan repeated List copy

`MainActivity.kt` menggunakan pola:

`allMedia = allMedia + page`

Setiap page membuat List baru.

Pada library besar:

- repeated allocation,
- repeated copying,
- memory churn,
- scalability menurun.

Untuk ribuan–puluhan ribu media, architecture yang lebih efisien diperlukan.

## 6. Duplicate analysis masih mahal untuk library besar

`findExactDuplicates()` melakukan content hashing terhadap kandidat dengan ukuran sama.

Sudah berjalan di `Dispatchers.IO`, tetapi belum terlihat:

- persistent hash cache,
- incremental scanning,
- progress percentage,
- resume scan,
- cancellation yang benar-benar granular.

**Functional:** bekerja.  
**Production scalability:** belum optimal.

## 7. Near-duplicate algorithm masih heuristik sederhana

Implementasi menggunakan:

- `averageHash()`
- Hamming distance threshold `5`.

Ini valid sebagai heuristic, tetapi bukan duplicate detection yang sangat reliable.

Potensi:

- false positive pada foto yang sangat mirip,
- false negative pada crop/rotation/transformation besar.

Hasil sebaiknya selalu diposisikan sebagai **suggestion**, bukan "confirmed duplicate".

## 8. Blur detector memakai threshold hard-coded

`BLUR_VARIANCE_THRESHOLD = 60.0`

Tidak ada calibration berdasarkan:

- resolusi,
- karakteristik gambar,
- kondisi low-light,
- intentional soft-focus.

Karena hasilnya hanya suggestion, bukan fatal, tetapi belum cukup robust untuk cleaner kelas tinggi.

## 9. Move/organize memiliki partial-success edge case

`MoveHelper.kt` menangani beberapa kondisi MediaStore/API lama.

Namun dapat terjadi:

- file berhasil dipindahkan,
- update MediaStore gagal,
- UI sementara tidak sepenuhnya sinkron.

Perlu **post-move verification/rescan** sebelum operasi dianggap final.

## 10. Batch permission belum benar-benar optimal

`MoveHelper.supportsBatchWriteRequest()` tersedia.

Namun flow organize masih dapat memproses item satu per satu dan berhenti ketika menemui `NeedsPermission`.

Untuk operasi massal, UX belum seefisien batch permission ideal.

---

# 🟡 P2 — POLISH / PRODUCTION QUALITY

## 11. Banyak string UI masih hard-coded

Terlihat pada:

- `HomeScreen.kt`
- `SettingsScreen.kt`
- `TrashScreen.kt`
- `OnboardingScreen.kt`

Padahal `strings.xml` sudah tersedia.

Dampak:

- localization buruk,
- maintenance lebih sulit,
- consistency menurun.

## 12. Pluralization masih manual

Contoh pola:

`photo${if (items.size == 1) "" else "s"}`

Seharusnya menggunakan Android plural resources.

## 13. README sudah divergen dari implementasi

Klaim yang bermasalah:

- photos/videos → implementasi utama images only;
- PIN/biometric → device credential;
- retention cleanup → belum benar-benar automatic.

Dokumentasi yang salah dapat menyebabkan maintenance AI berikutnya mengambil asumsi yang salah.

## 14. Tidak ada Gradle Wrapper

Tidak ditemukan:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper`

Ini melemahkan reproducibility build, terutama untuk workflow Termux/AI.

## 15. Versioning CI tidak konsisten

APK menggunakan:

`versionName = 1.0.<git commit count>`

Release tag menggunakan:

`v1.0.<github.run_number>`

Akibatnya version APK dan release tag dapat berbeda.

## 16. Release build masih `minifyEnabled = false`

Bukan functional bug, tetapi untuk production release:

- R8,
- resource shrinking,

layak dipertimbangkan setelah rules diuji.

## 17. `largeHeap=true` merupakan red flag

Manifest menggunakan:

`android:largeHeap="true"`

Untuk gallery cleaner, ini dapat menjadi safety net terhadap memory pressure.

Target production sebaiknya tidak bergantung pada large heap sebagai solusi utama.

## 18. Exact duplicate belum memiliki persistent hash cache

File yang sudah pernah di-hash berpotensi di-hash ulang.

Pada library besar ini membuang:

- CPU,
- I/O,
- battery,
- waktu scanning.

## 19. Test suite destructive masih kurang

Belum terlihat coverage instrumentation yang memadai untuk:

- delete permission flow,
- `RecoverableSecurityException`,
- restore,
- move,
- duplicate detection,
- corrupted media,
- revoked permission,
- MediaStore inconsistency.

Untuk cleaner, operasi destructive justru harus mendapat coverage tertinggi.

## 20. Belum ada verifikasi end-to-end lintas Android version

Manifest + code mencoba menangani API 24–35.

Namun tanpa device/instrumentation matrix, compatibility tersebut masih:

**static confidence, bukan runtime proof.**

---

# PRIORITAS FIX

## P0

1. Tambahkan video secara benar atau hapus semua klaim video.
2. Tentukan semantics Trash secara eksplisit.
3. Benahi retention behavior: automatic vs reminder.
4. Benahi App Lock claim/implementation.

## P1

5. Optimalkan progressive media loading.
6. Buat duplicate scan incremental/cacheable + cancellable.
7. Perkuat verification setelah Move/Delete.
8. Jadikan batch operation benar-benar batch.

## P2

9. Pindahkan UI strings ke `strings.xml`.
10. Gunakan plural resources.
11. Tambahkan Gradle Wrapper + reproducible build.
12. Sinkronisasi versioning CI/release.
13. Kurangi ketergantungan `largeHeap`.
14. Tambahkan instrumentation test untuk operasi destructive.
15. Audit accessibility + touch target + loading/error/empty states.

---

# FINAL VERDICT

**Kode ini bukan proyek rusak.**

Fondasi yang sudah benar mencakup:

- MediaStore,
- RecoverableSecurityException,
- progressive loading,
- DataStore,
- backup-before-delete,
- cancellation scan,
- separation `MediaDataSource → MediaScanner → Repository`.

Namun untuk standar **"100% functional & polished, sekelas Gallery Cleaner production-grade"**, belum GO.

### Gap terbesar bukan UI visual.

Prioritas sebenarnya adalah:

**MediaStore coverage → deletion/trash semantics → retention automation → scalability → destructive-operation reliability → baru visual polishing.**

Setelah area tersebut dibereskan, barulah project layak masuk fase **final polish / release hardening**.
