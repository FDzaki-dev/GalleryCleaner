# CHANGELOG

## v1_Batch1 — 2026-08-09
- Fix: `app/build.gradle.kts` missing closing brace `}` di blok validasi `RELEASE_KEY_PASSWORD` (baris 58-62) → menyebabkan `Expecting '}'` di line 123 dan build CI gagal total.
- Add: `.github/workflows/build.yml` step build sekarang menyimpan output ke `test-result-<branch>-attempt-<run_attempt>.log` dan upload otomatis sebagai GitHub Actions artifact jika build gagal (`if: failure()`).
