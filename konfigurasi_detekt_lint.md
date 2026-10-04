# Konfigurasi detekt dan lintDebug (Batch148)

Acuan utama validasi ketat untuk Snaply. Sifatnya **non-blocking**: temuan hanya dilaporkan dan tidak pernah menggagalkan build atau rilis.

## Berkas

| Berkas | Fungsi |
|---|---|
| `config/detekt/detekt.yml` | Aturan detekt 1.23.6, menimpa bawaan detekt (`buildUponDefaultConfig = true`) |
| `tools/static-analysis/` | Build Gradle terpisah yang menjalankan detekt; tidak menyentuh build APK |
| `app/lint.xml` | Severity Android Lint untuk `lintDebug` |
| `app/build.gradle.kts` (blok `lint {}`) | `abortOnError = false`, laporan HTML/XML/SARIF |
| `.github/workflows/build.yml` (job `static-analysis`) | Menjalankan keduanya di CI, mengunggah laporan sebagai artifact |

## Fokus aturan

Hanya potensi bug logika dan mekanisme yang dapat diamati:

- **potential-bugs**: null-safety (`!!`, cast), locale implisit, kode tak terjangkau.
- **exceptions**: pengecualian yang tertelan, `catch` terlalu umum, `printStackTrace`.
- **coroutines**: `GlobalScope`, `Thread.sleep`, `runCatching` yang menelan pembatalan.
- **empty-blocks** dan **performance**: bawaan detekt.

Dimatikan: `comments`, `complexity`, `naming`, `style`, karena fungsi `@Composable` menghasilkan ratusan temuan palsu. `InjectDispatcher` juga dimatikan karena aturan proyek mewajibkan `Dispatchers.IO`/`Default` dipakai langsung.

Lint: `NewApi`, `MissingPermission`, `WrongThread`, `DefaultLocale`, `UnspecifiedImmutableFlag`, `NotificationPermission`, dan state Compose dinaikkan ke `error`; `Typos` dimatikan (kamus Inggris).

## Cara menjalankan

```
# detekt (dari folder tools/static-analysis)
gradle detekt

# lint (dari root; butuh Android SDK)
gradle lintDebug
```

Laporan:

- detekt: `tools/static-analysis/build/reports/detekt/detekt.{html,xml,txt,sarif}`
- lint: `app/build/reports/lint-results-debug.{html,xml,sarif}`

Di CI, ringkasan jumlah temuan tampil di Job Summary dan laporan lengkap ada di artifact **`Snaply_static-analysis-reports_run<N>`**.

### Penanda proyek

Nama repositori adalah `GalleryCleaner`, sedangkan aplikasinya Snaply, dan proyek lain memiliki artifact serupa. Agar tidak tertukar:

- nama artifact selalu diawali `Snaply_` dan memuat nomor run;
- isi ZIP: `SNAPLY.txt` (proyek, repositori, run, branch, commit, waktu, jumlah temuan), `detekt/`, `lint/`, `log/`;
- judul Job Summary dan nama job juga memuat "Snaply".

## Catatan

- Proyek tidak memiliki Gradle wrapper (`gradlew`) dan Termux tidak memiliki Android SDK, sehingga pre-commit hook lokal tidak dapat menjalankan `detekt lintDebug`. Gerbang validasinya adalah job CI.
- `abortOnError = false` juga berlaku untuk `lintVitalRelease` pada `assembleRelease`: lint "fatal" kini hanya dilaporkan, tidak lagi menggagalkan build rilis.
- Belum pernah dijalankan: sandbox pengembangan tidak memiliki Gradle, SDK, maupun jaringan. Eksekusi pertama terjadi di CI.
