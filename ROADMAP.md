# ROADMAP — Snaply vs "Sponge - Gallery Cleaner"

## 🔗 Rilis Terbaru
APK signed terbaru: **https://github.com/FDzaki-dev/GalleryCleaner/releases/latest**

## Status Ringkas (terbaru)
✅ Fase A (gap fungsional inti) selesai 4/4 — Batch20. ✅ Fase B (AI on-device) selesai 3/3 — Batch25. ⏳ Fase C (reliability/visual) & Fase D (jangkauan pasar) belum dimulai. 🆕 Fase E (gap fungsional lanjutan, ditemukan Batch129 re-cek Sponge): item #14 "Cleaning Options" 2/9 sub-item selesai (Sound for Videos — Batch129, Enable share text — Batch131), item #16-20 (History/Home dashboard/Customize view/Notifications split/My Account) BELUM digarap. Lihat `PROJECT_STATE.md` → "Belum Dikerjakan" untuk detail terkini.

Tolok ukur: **Sponge - Gallery Cleaner** (`com.prismtree.sponge`,
~780rb download, ~510 install/hari, rating 5.0). Cek ulang sebelum
rilis besar — fitur kompetitor bisa berubah.

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

### 1b. Re-cek Batch129 (2026-09-14, screen recording user langsung — bukan asumsi/memori lama)
Section 1 di atas TIDAK salah, tapi TIDAK LENGKAP — fitur baru berikut
kepakai fisik di screen recording (72 detik), 0 ada di Section 1 lama:
- **Home dashboard**: greeting + streak ("You are a cleaning star!"),
  bintang rating "Share your experience", lifetime counter ("reviewed 302
  items, saved 870 MB"), progress "2% done", CTA "Start cleaning", resume
  card "In progress: <bulan> · Just now · View all"
- **History screen (nav item terpisah, BUKAN sub-Settings)**: tabel
  per-bulan, 3 angka + label MB per baris (mis. "November 2025: 73 / 56 /
  575", "Mei 2025: 154 / 123 / 62 MB") — semantik pasti 3 angka itu
  (reviewed/kept/deleted vs reviewed/deleted/MB) BELUM bisa dipastikan
  cuma dari rekaman, perlu klarifikasi kalau mau ditiru persis
- **Settings > My Account**: Sign In/Sign Up ("sign in or create an
  account") — indikasi ada akun/cloud-sync, sebelumnya 0 tercatat
- **Settings > Notifications**: TERNYATA 2 toggle terpisah — "In progress
  reminder" + "Monthly reminder" (Section 1 lama cuma nyebut 1 gabungan)
- **Settings > Cleaning Options** (section baru, 9 item, 0 satupun ada di
  project ini — dicek langsung ke source Batch129, lihat Section 2b):
  Manage move-to albums, Personalize your cleaning screen, Swipe direction
  for delete (Left/Right), Enable Sound for Videos, Animate on buttons,
  Default sort (mis. "Date: Oldest to Latest" — field+ARAH, bukan cuma
  field), Random count (angka custom, bukan cuma on/off), Enable share
  text, Manage albums (include/exclude album dari sesi cleaning)
- **Settings > Reset app** (reset data + stats — 0 ada di project ini)
- **Swipe screen > "Customize view" (ikon expand di top bar)**: bottom
  sheet 4 toggle — Top media strip (thumbnail strip di atas), File info,
  Proceed button on top, Organize mode (tampilkan folder tujaun pindah)
- Video/GIF card di swipe screen nampilin overlay ukuran file + index
  ("15 MB · GIF · 1/53") langsung di kartu, bukan cuma pas di-tap

## 2. Posisi kita sekarang
**Setara atau lebih unggul:**
- Swipe keep/delete ✓ (`SwipeScreen`)
- Resume progress per grup ✓ (`ProgressStore`)
- Stats lifetime (total freed bytes + deleted count) ✓ (`StatsStore`)
- Sort + Group mode (bulan, dll) ✓ (`SortOption`/`GroupMode` di `MediaModels.kt`)
- Privacy on-device 100% ✓
- **App lock** (PIN/biometric) — Sponge tidak punya
- **Crash logger** bawaan + FIFO retention
- **3 gaya tema penuh** (Signature/Amber Reserve/Indigo Noir) + Midnight Blue
- **Smart category row, On-This-Day, Largest Files card, Expiry banner
  trash** (`HomeScreenSections.kt`) — tidak ada di Sponge

**Gap vs Sponge — semua closed:**
- ✅ Random clean mode (Batch16)
- ✅ Move-to-folder saat swipe (Batch17, `MoveHelper.kt`)
- ✅ Cleanup goal (Batch19, `SettingsStore.cleanupGoalBytesFlow`)
- ❌ Multi-bahasa — cuma `values/` default (Inggris)
- ✅ Sort by size/date/name di layar Swipe (Batch20)

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

### 2b. Gap baru dari re-cek Batch129 (lihat Section 1b untuk fakta lengkap)
- ✅ **Sound for Videos** (Batch129, SELESAI) — `SettingsStore.videoSoundEnabledFlow`
  + `SettingsScreen.kt` section "Cleaning Options" (section baru) +
  `VideoPlayerSurface` (`SwipeScreenCard.kt`) baca setting itu buat
  `exoPlayer.volume`. Default OFF (beda dari Sponge yang defaultnya ON di
  rekaman) — alasan: konsisten sama pola off-by-default project ini utk
  toggle yang mengubah perilaku "mengganggu" (lihat doc comment di
  `SettingsStore.kt`), BUKAN niru Sponge 1:1 buta.
- ✅ **Enable share text** (Batch131, SELESAI) — `SettingsStore.shareTextEnabledFlow`
  + toggle di section "Cleaning Options" yang sama + `SwipeScreen.kt`
  share intent nambahin `EXTRA_TEXT` (nama file) kalau enabled. Default
  OFF juga (alasan beda dari sound: nama file bisa bocorin info personal
  kalau user rename sendiri, opt-in lebih aman) — 1 file consumer
  (`SwipeScreen.kt`), pola identik Batch129.
- ❌ 7 sisa item "Cleaning Options" Sponge (Manage move-to albums,
  Personalize cleaning screen, Swipe direction, Animate on buttons,
  Default sort+arah, Random count, Manage albums) —
  BELUM digarap, per-item beda kompleksitas (beberapa cuma toggle+wiring
  ringan mirip Batch129/131, beberapa — swipe direction, manage albums —
  nyentuh logic inti `SwipeScreenCard.kt`/`SwipeScreen.kt` lebih dalam,
  resiko regresi lebih tinggi, HARUS batch terpisah per STABILITY WINS)
- ❌ History screen (nav item terpisah, statistik bulanan) — 0 ada sama
  sekali di project ini (cuma ada Trash). Butuh data-layer baru (belum
  ada yang nyatet reviewed/kept/deleted per bulan hari ini,
  `StatsStore` yang ada cuma lifetime total, bukan per-bulan) — lebih
  besar dari sekadar 1 UI screen, JANGAN diremehin jadi "quick add"
- ❌ Home dashboard model Sponge (streak/rating-prompt/resume-card) —
  `HomeScreen.kt` project ini sekarang perannya folder/album browser,
  BUKAN dashboard statistik — beda arsitektur, bukan sekadar tambah widget
- ❌ "Customize view" bottom sheet di swipe screen (4 toggle layout)
- ❌ Settings > My Account (sign in/up) — indikasi fitur akun/cloud, scope
  jauh lebih besar dari toggle biasa (butuh auth+backend), TIDAK
  direkomendasikan dikejar tanpa keputusan produk eksplisit dari user
- ❌ Notifications displit jadi 2 toggle (in-progress + monthly) — saat
  ini cuma 1 (`cleaningReminderEnabledFlow`)

## 3. Roadmap (goals, bukan jadwal tanggal — tiap fase = beberapa batch)

### Fase A — Tutup gap fungsional inti (SELESAI 4/4, Batch20)
1. ✅ Random clean mode (Batch16) — `SettingsStore.randomModeEnabledFlow`
2. ✅ 3rd swipe action "Organize" (Batch17) — `MoveHelper.kt`,
   `OrganizeFolderDialog`
3. ✅ Cleanup goal (Batch19) — `SettingsStore.cleanupGoalBytesFlow`,
   progress bar di `StorageDashboard`
4. ✅ Sort (size/date/name) di layar Swipe (Batch20) — ikon Sort di top
   bar, `MediaRepository.sortItems` public

Detail implementasi tiap item: `PROJECT_STATE.md`.

### Fase B — Diferensiasi AI/on-device (SELESAI 3/3, Batch25)
5. ✅ Duplicate & near-duplicate detection — `MediaScanner.findNearDuplicates()`
   (aHash 64-bit, on-device)
6. ✅ Blur/low-quality auto-flag — `MediaScanner.findBlurryPhotos()`
   (Laplacian-variance, on-device)
7. ✅ Backup-before-permanent-delete — `SettingsStore.backupBeforeDeleteEnabledFlow`,
   opt-in, `Pictures|Movies/GalleryCleaner/Backup/`

Detail implementasi tiap item: `PROJECT_STATE.md`.

### Fase C — Reliability & visual (lanjutan kerja yang sudah jalan)
8. Selesaikan keputusan `MidnightSkeuoButton` cascade (parameter warna
   opsional vs biarkan tombol semantik tetap M3) — pending dari Batch14.
9. Phase-1b package restructuring (butuh compiler nyata / CI, bukan
   sandbox ini).
10. Pastikan Batch10-14 hijau di CI sebelum numpuk fitur baru di atas
    fondasi yang belum tervalidasi build-nya.

### Fase E — Gap fungsional lanjutan (ditemukan Batch129, re-cek Sponge)
Diselipkan SEBELUM Fase D karena ini "gap fungsional" (semangat sama
Fase A/B), bukan "jangkauan pasar" — urutan/prioritas per-item TETAP
keputusan user per-batch (bukan diasumsikan sistem), lihat catatan resiko
masing-masing di Section 2b.
14. "Cleaning Options" (9 sub-item, per-item checklist — tiap item batch
    terpisah, kompleksitas beda-beda, lihat Section 2b):
    - ✅ Sound for Videos (Batch129) — `SettingsStore.videoSoundEnabledFlow`
    - ✅ Enable share text (Batch131) — `SettingsStore.shareTextEnabledFlow`,
      dipakai `SwipeScreen.kt` share intent (`EXTRA_TEXT` = nama file)
    - ❌ Manage move-to albums
    - ❌ Personalize cleaning screen
    - ❌ Swipe direction for delete (Left/Right)
    - ❌ Animate on buttons
    - ❌ Default sort + arah (field-nya sudah ada via `sortOptionFlow`,
      ARAH asc/desc belum — `MediaRepository.sortItems` sekarang selalu
      `sortedByDescending`, hardcoded)
    - ❌ Random count (0 ada konsep "count" sama sekali hari ini, random
      mode sekarang cuma shuffle flag on/off, bukan cuma nambah angka ke
      yang udah ada)
    - ❌ Manage albums (include/exclude dari sesi cleaning)
16. ❌ History screen — perlu data-layer baru (stats per-bulan, bukan cuma
    lifetime), BUKAN quick-add
17. ❌ Home dashboard model Sponge (streak/resume-card) — `HomeScreen.kt`
    project ini peran beda (folder browser), butuh keputusan arsitektur
    dulu: dashboard baru terpisah, atau `HomeScreen.kt` di-repurpose?
18. ❌ "Customize view" bottom sheet (layar swipe)
19. ❌ Notifications: split jadi 2 toggle (in-progress + monthly)
20. ❌ Settings > My Account (sign in/up) — SENGAJA ditaruh prioritas
    PALING RENDAH: butuh auth+backend, scope beda kelas dari 19 item lain
    di Fase E, jangan dikerjakan tanpa keputusan produk eksplisit user

### Fase D — Jangkauan pasar (menyamai skala 780rb download Sponge)
11. **Multi-bahasa**: minimal Spanish + Portuguese (BR).
12. **Monetisasi one-time-purchase premium** (bukan subscription) —
    tentukan fitur premium (mis. random mode, unlimited undo, tema
    ekstra) sebelum implementasi billing.
13. **Play Store readiness**: privacy policy URL, Data Safety form,
    screenshot set, short/long description dengan ASO keyword
    ("gallery cleaner", "swipe photos", "declutter").

## 4. Definisi "sukses setara/melampaui Sponge"
- Semua gap Fase A tertutup DAN teruji
- Minimal 1 fitur Fase B live sebelum Sponge merilisnya resmi
- CI hijau konsisten
- Privacy posture: 100% on-device, dinyatakan jujur di Data Safety form

## 5. Catatan proses
- Update roadmap tiap riset kompetitor diulang / gap baru ditemukan.
- Tiap fase dipecah jadi batch ≤10 file (kecuali Atomic Change berjustifikasi).
