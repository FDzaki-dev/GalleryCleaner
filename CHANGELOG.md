# CHANGELOG

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
