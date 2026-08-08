# PROJECT_STATE — GalleryCleaner

## Versi Saat Ini
v3 — Batch3 (Fix compile error dari Batch2: wrong CompositionLocalProvider import + experimental Slider API)

## Status Build Terakhir
Batch1: FAILED (2026-07-27) → syntax error `app/build.gradle.kts:123`. DIPERBAIKI.
Batch2: FAILED (attempt 1, branch main) → 3 error kompilasi Kotlin di komponen baru (lihat `test-result-main-attempt-1.log` dari GitHub Actions artifact — mekanisme log-capture otomatis Batch1 berfungsi sesuai desain: nama file dinamis `test-result-<branch>-attempt-<run_attempt>.log`, terkonfirmasi terisi `main`/`1` sesuai kondisi run yang sebenarnya).
Batch3 (batch ini): fix 3 error kompilasi tsb, belum di-run ulang di CI.

## Root Cause Batch2 Build Failure (dari test-result-main-attempt-1.log)
1. `TactileButton.kt` & `GlassNavigation.kt` — import salah: `androidx.compose.material3.CompositionLocalProvider` (tidak ada). Fungsi ini ada di `androidx.compose.runtime.CompositionLocalProvider`. DIPERBAIKI.
2. `TactileSlider.kt:37` — parameter `thumb` pada M3 `Slider` masih experimental di compose-bom 2024.06.00, perlu `@OptIn(ExperimentalMaterial3Api::class)`. DIPERBAIKI.

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
