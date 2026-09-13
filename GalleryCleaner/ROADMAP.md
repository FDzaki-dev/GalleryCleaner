# ROADMAP — Snaply vs "Sponge - Gallery Cleaner"

## 🔗 Rilis Terbaru
APK signed terbaru: **https://github.com/FDzaki-dev/GalleryCleaner/releases/latest**

## Status Ringkas (terbaru)
✅ Fase A (gap fungsional inti) selesai 4/4 — Batch20. ✅ Fase B (AI on-device) selesai 3/3 — Batch25. ⏳ Fase C (reliability/visual) & Fase D (jangkauan pasar) belum dimulai — lihat `PROJECT_STATE.md` → "Belum Dikerjakan" untuk detail terkini.

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
