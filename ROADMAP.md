# ROADMAP — GalleryCleaner vs "Sponge - Gallery Cleaner"

Dibuat Batch15 (2026-08-09). Tolok ukur: **Sponge - Gallery Cleaner**
(`com.prismtree.sponge`, prismtree, live sejak Juli 2023, ~780rb download,
~510 install/hari, rating 5.0 di listing pihak ketiga). Riset via web
search per tanggal dokumen ini — fitur bisa berubah, cek ulang sebelum
tiap rilis besar.

## 1. Apa yang Sponge tawarkan (fakta, bukan asumsi)
- Swipe kanan=keep, kiri=discard, 1 foto per layar (bukan grid multi-select)
- Pindahkan foto/video ke folder pilihan sambil swipe ("truly organizing")
- Random clean mode
- Sort by size / date / name
- Grouping bulanan + album, checklist-style ("checked off like a to-do list")
- Resume: lanjut dari posisi terakhir
- Statistik: progress per sesi (total lifetime + storage saved + cleanup
  goal disebut developer sebagai "coming soon" per Juli 2026)
- Privacy-first: 100% on-device, tanpa upload, tanpa PII collection
- Monetisasi: Premium **one-time purchase**, bukan subscription
- Multi-bahasa: Inggris + Spanish + Portuguese (BR), terus bertambah
- Android 11+, rating konten Everyone

## 2. Posisi kita sekarang (audit jujur per Batch14)
**Sudah setara atau lebih unggul:**
- Swipe keep/delete ✓ (SwipeScreen, sama persis mekanismenya)
- Resume progress per grup ✓ (`ProgressStore`)
- Stats lifetime (total freed bytes + deleted count) ✓ (`StatsStore`)
- Sort + Group mode (bulan, dll) ✓ (`SortOption`/`GroupMode` di `MediaModels.kt`)
- Privacy on-device 100% ✓ (tidak ada network call ke server sendiri)
- **App lock** (PIN/biometric) — Sponge tidak menyebutkan ini sama sekali
- **Crash logger** bawaan + FIFO retention — diferensiator teknis, bukan
  fitur user-facing tapi menaikkan reliability rating jangka panjang
- **3 gaya tema penuh** (Signature/Amber Reserve/Indigo Noir) + tema
  Skeuomorphism-Dark Midnight Blue — Sponge cuma 1 visual identity
- **Smart category row, On-This-Day, Largest Files card, Expiry banner
  trash** (`HomeScreenSections.kt`) — tidak disebut di listing Sponge

**Gap nyata (Sponge sudah, kita belum):**
- ❌ **Random clean mode** — ✅ shipped Batch16
- ❌ **Move-to-folder saat swipe** — ✅ shipped Batch17 (koreksi: primitive
  `moveTo` yang diklaim "sudah ada" di Batch15 TERNYATA tidak pernah ada;
  dibangun baru dari nol, lihat item 2 Fase A dan `PROJECT_STATE.md`)
- ❌ **Cleanup goal** (target custom, mis. "bebaskan 2GB bulan ini") — ✅
  shipped Batch19 (target byte, slider+preset, progress bar di
  StorageDashboard, tracked terhadap `StatsStore.totalFreedBytesFlow`
  all-time)
- ❌ **Multi-bahasa** — cuma `values/` default (Inggris), belum ada
  `values-es`, `values-pt-rBR`, dst
- ❌ **Sort by size/date/name di layar Swipe** — ✅ shipped Batch20. Audit:
  sort SUDAH sampai ke SwipeScreen sejak awal (`MediaRepository.group()`
  sort dulu baru group, jadi `group.items` selalu tiba pre-sorted). Yang
  betulan hilang: cara GANTI sort tanpa keluar ke Home — sekarang ada
  ikon Sort di top bar SwipeScreen.

**Gap yang TIDAK disebutkan Sponge tapi jadi standar kategori app ini
(peluang untuk melampaui, bukan sekadar menyamai):**
- ❌ Duplicate / near-duplicate photo detection (perceptual hash) —
  kategori "gallery cleaner" umumnya punya ini, Sponge sendiri tidak
  mengiklankannya secara eksplisit di deskripsi resmi
- ❌ Blur/low-quality photo auto-flag
- ❌ Backup-before-delete / export ke folder cadangan sebelum permanent
  delete (mitigasi risiko trash-expiry yang sudah ada)
- ❌ Widget home-screen ("X hari lagi sebelum trash auto-clear", progress
  ring)
- ❌ Monetisasi: belum ada model premium sama sekali di project ini

## 3. Roadmap (goals, bukan jadwal tanggal — tiap fase = beberapa batch)

### Fase A — Tutup gap fungsional inti (prioritas tertinggi)
1. ✅ **Random clean mode** (Batch16) — shuffle urutan `MediaItem` dalam
   `MediaGroup` sebelum masuk SwipeScreen, toggle di HomeScreen (ikon
   Shuffle top bar) + Settings (section "Swiping"), persisted via
   `SettingsStore.randomModeEnabledFlow`. Detail: `PROJECT_STATE.md`.
2. ✅ **3rd swipe action "Organize"** (Batch17) — **koreksi audit**: klaim
   Batch15 bahwa `MediaDataSource` sudah punya primitive `moveTo` TERNYATA
   SALAH (grep hanya menemukan `Cursor.moveToNext()`, API tak terkait,
   bukan primitive move). Dibangun dari nol: `MoveHelper.kt` (RELATIVE_PATH
   update API 29+, direct-file move API 24-28, permission-request dance
   identik `ImageCompressor`), tombol "Organize" ke-3 di `ActionButtonRow`
   + aksi bulk "Organize N" di grid mode, `OrganizeFolderDialog` (pilih
   folder existing atau buat baru). Detail: `PROJECT_STATE.md`.
3. ✅ **Cleanup goal** (Batch19) — target byte via
   `SettingsStore.cleanupGoalBytesFlow` (default 2GB), dialog slider +
   preset chips (500MB/1/2/5/10GB), progress bar di `StorageDashboard`
   (HomeScreen), tracked terhadap `StatsStore.totalFreedBytesFlow`
   all-time (bukan reset per periode — konsisten dengan stat lain yang
   memang all-time). Kita duluan ship ini sebelum Sponge (per riset
   Batch15, mereka baru rencanakan per Juli 2026) — selling point nyata,
   bukan cuma catch-up. Detail: `PROJECT_STATE.md`.
4. ✅ **Sort (size/date/name) di layar Swipe** (Batch20) — **FASE A
   SELESAI 4/4**. Audit: sort sudah otomatis berlaku di SwipeScreen sejak
   awal (`MediaRepository.group()` sort items sebelum grouping — tidak
   ada gap fungsional yang perlu diperbaiki). Yang ditambahkan: ikon Sort
   di top bar SwipeScreen supaya bisa ganti sort tanpa keluar ke Home;
   `MediaRepository.sortItems` dibuka jadi public biar SwipeScreen pakai
   logic sort yang sama persis (bukan duplikat). Ganti sort mid-session
   reset posisi swipe ke awal (posisi lama tidak lagi valid di urutan
   baru) — didokumentasikan di `PROJECT_STATE.md`.

**Fase A (tutup gap fungsional inti vs Sponge) selesai 4/4 di Batch20.**
Lanjut Fase B.

### Fase B — Diferensiasi lewat AI/on-device intelligence
5. ✅ **Duplicate & near-duplicate detection** (audit Batch25: SUDAH ada
   sejak batch sebelumnya, luput ditandai di roadmap ini — koreksi
   dokumentasi, bukan fitur baru). Average hashing (aHash) 64-bit,
   Hamming-distance clustering, 100% on-device, tanpa ML model besar.
   `MediaScanner.findNearDuplicates()`, dipicu manual dari tombol "Scan"
   di kartu "Similar photos" (Smart Detection, HomeScreen).
6. ✅ **Blur/low-quality auto-flag** (audit Batch25: SUDAH ada, sama
   seperti item 5 — koreksi dokumentasi). Laplacian-variance blur metric
   pada grayscale sample kecil, 100% on-device. `MediaScanner.findBlurryPhotos()`,
   dipicu manual dari tombol "Scan" di kartu "Blurry photos". Konsisten
   dengan filosofi "you decide, we just help" — hasil scan cuma suggestion
   list, tidak ada auto-delete.
7. ✅ **Backup-before-permanent-delete** (Batch25) — opsional, default OFF
   (`SettingsStore.backupBeforeDeleteEnabledFlow`), toggle di Settings >
   Backup. Saat aktif, tiap item disalin ke `Pictures|Movies/GalleryCleaner/
   Backup/` (MediaStore, API 29+, fallback File I/O API 24-28) SEBELUM
   permanent-delete benar-benar berjalan — satu-satunya titik `uri` sumber
   dijamin masih bisa dibaca, baik di jalur system-dialog (API 30+) maupun
   direct-delete (API <30). Best-effort per item (satu file gagal backup
   tidak pernah memblokir delete yang diminta user). Detail lengkap +
   trade-off yang didokumentasikan: `PROJECT_STATE.md` Batch25.

**Fase B (diferensiasi AI/on-device) selesai 3/3 per Batch25.** Lanjut
Fase C.

### Fase C — Reliability & visual (lanjutan kerja yang sudah jalan)
8. Selesaikan keputusan `MidnightSkeuoButton` cascade (parameter warna
   opsional vs biarkan tombol semantik tetap M3) — pending dari Batch14.
9. Phase-1b package restructuring (butuh compiler nyata / CI, bukan
   sandbox ini).
10. Pastikan Batch10-14 hijau di CI sebelum numpuk fitur baru di atas
    fondasi yang belum tervalidasi build-nya.

### Fase D — Jangkauan pasar (menyamai skala 780rb download Sponge)
11. **Multi-bahasa**: minimal Spanish + Portuguese (BR) — dua bahasa yang
    sama seperti Sponge tambahkan duluan, karena itu sinyal pasar yang
    sudah terbukti diminati (developer Sponge menyebutnya eksplisit di
    changelog mereka).
12. **Monetisasi one-time-purchase premium** (bukan subscription) — ini
    justru salah satu alasan review positif Sponge ("i instantly bought
    premium... i love that it's not a subscription"). Tentukan dulu apa
    yang jadi fitur premium (mis. random mode, unlimited undo, tema
    ekstra) sebelum implementasi billing.
13. **Play Store readiness**: privacy policy URL, Data Safety form (jujur
    — app ini genuinely on-device, mudah diisi), screenshot set,
    short/long description dengan ASO keyword yang sama seperti Sponge
    kejar ("gallery cleaner", "swipe photos", "declutter").

## 4. Definisi "sukses setara/melampaui Sponge"
Bukan cuma feature-parity checklist — sukses berarti:
- Semua gap Fase A tertutup DAN teruji (bukan cuma tertulis di kode)
- Minimal 1 fitur Fase B (duplicate detection ATAU cleanup goal) live
  SEBELUM Sponge merilisnya secara resmi (cleanup goal masih "coming
  soon" per riset Juli 2026 — window peluang nyata)
- CI hijau konsisten (bukan "belum dikonfirmasi" berulang tiap batch)
- Privacy posture minimal sama kuat: 100% on-device, dinyatakan jujur di
  Data Safety form, bukan cuma diklaim di README

## 5. Catatan proses
- Roadmap ini dokumen hidup — update tiap kali riset kompetitor diulang
  atau gap baru ditemukan saat audit batch. Jangan biarkan basi.
- Setiap fase dipecah jadi batch ≤10 file (kecuali Atomic Change dengan
  justifikasi), sesuai Batch Limit yang berlaku di seluruh project ini.
