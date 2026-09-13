plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Batch83 — Versioning Lock fix (AUDIT_GAP.md #15, PROJECT_STATE.md "ATURAN
// PERMANEN SESI" #2): versionCode/versionName WAJIB dari GITHUB_RUN_NUMBER.
// Function lama di sini pakai `git rev-list --count HEAD` (total commit
// count) — angka BEDA dari $GITHUB_RUN_NUMBER yang dipakai build.yml untuk
// APK filename & GitHub Release tag (Batch43 cuma nyamain 2 hal ITU, tidak
// pernah nyentuh versionCode Gradle asli di sini). Akibatnya versionCode
// yang ke-bake ke dalam APK (yang dibaca PackageManager/BuildConfig) selalu
// beda dari angka yang tertulis di nama filenya sendiri. GITHUB_RUN_NUMBER
// sudah otomatis ada di environment tiap step Actions (proses Gradle
// mewarisi env shell runner), 0 perubahan workflow diperlukan buat
// nyediain variabel ini. Fallback ke 1 HANYA untuk build lokal (Termux/
// Android Studio) di luar CI, di mana GITHUB_RUN_NUMBER memang tidak ada.
fun ciVersionCode(): Int =
    System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

val appVersionCode = ciVersionCode()

android {
    namespace = "com.example.gallerycleaner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gallerycleaner"
        // Batch105 — instruksi eksplisit user: naikkan ke scope asli project
        // ("WAJIB Kotlin+Compose murni, minSdk=31"), gap sejak dulu cuma
        // di-flag (Batch64/Batch85), sengaja gak disentuh sampai user
        // approve karena mempersempit device support. Efek samping: seluruh
        // percabangan Build.VERSION.SDK_INT untuk API24-30 (MoveHelper,
        // ImageCompressor, MediaDataSource, dst.) jadi structurally dead
        // code (selalu ambil jalur >=30/>=31), begitu juga 1 baris manifest
        // WRITE_EXTERNAL_STORAGE (maxSdkVersion=28) — TIDAK dihapus batch
        // ini (cleanup terpisah, di luar scope "naikkan minSdk", butuh izin
        // eksplisit lagi), cuma dead tapi harmless, 0 risiko compile/regresi.
        minSdk = 31
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "1.0.$appVersionCode"
    }

    signingConfigs {
        getByName("debug") {}

        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("RELEASE_KEY_ALIAS")
            val keyPasswordValue = System.getenv("RELEASE_KEY_PASSWORD")

            if (keystorePath.isNullOrBlank()) {
                throw GradleException(
                    "RELEASE_KEYSTORE_PATH is missing. " +
                    "Release APK signing is mandatory."
                )
            }

            if (keystorePassword.isNullOrBlank()) {
                throw GradleException(
                    "RELEASE_KEYSTORE_PASSWORD is missing."
                )
            }

            if (keyAliasValue.isNullOrBlank()) {
                throw GradleException(
                    "RELEASE_KEY_ALIAS is missing."
                )
            }

            if (keyPasswordValue.isNullOrBlank()) {
                throw GradleException(
                    "RELEASE_KEY_PASSWORD is missing."
                )
            }

            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Batch114 (Opsi (b) Stage 2): material-icons-extended sempat DICABUT —
    // beda dari percobaan Batch109 (Opsi A, gagal karena masih ada 7 titik pakai
    // Icons.Filled.{Shuffle,Folder,GridView,Sort,Undo,ViewCarousel,ZoomIn} yang
    // genuinely extended-only, CI run231). Ke-7 titik itu dipindah ke vector
    // drawable lokal (app/src/main/res/drawable/ic_*.xml, foundation Batch112)
    // sebelum baris ini dicabut — HomeScreen.kt/HomeScreenFolderRow.kt/
    // SwipeScreen.kt/SwipeScreenGrid.kt di-update ATOMIK bareng commit yang sama
    // (5 file total, precedent STABILITY WINS Batch77).
    // [Batch128 KOREKSI, bukan lagi final] Dependency DIKEMBALIKAN LAGI — user
    // upload log CI gagal (run247, compileReleaseKotlin): Batch127 (custom video
    // controller) pakai Icons.Filled.{Pause,Replay10,Forward10}, ketiganya
    // genuinely -extended-only (confirmed dari log: Unresolved reference persis
    // 3 nama ini, 11 icon -core lain di file yang sama — Close/PlayArrow — 0
    // error). Pola mitigasi sama persis Batch109→110 (revert 1 baris, tercepat
    // & teraman). proguard-rules.pro rule#5 (Batch113) SUDAH cover ini (blanket
    // keep utk SELURUH package androidx.compose.material.icons.filled, 0
    // perubahan proguard diperlukan). App-size gap yang ditutup Batch108-114
    // REOPEN untuk 3 icon baru ini (dependency ~2000+ icon balik lagi, rule#5
    // sendiri yang bikin R8 gak bisa shrink package filled). Migrasi 3 icon ini
    // ke vector drawable lokal (pola sama Batch112/114) adalah opsi lanjutan
    // buat nutup app-size lagi — BELUM dieksekusi batch ini: Replay10/Forward10
    // icon compound (arc panah + digit "10" dibakar ke pathData), beda kelas
    // risiko dari 7 icon shape-murni yang sudah dimigrasi Batch112 (fidelity
    // reproduksi dari memori jauh lebih tidak pasti) — nunggu instruksi eksplisit
    // user kalau mau lanjut migrasi ketiganya.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    // Batch40 (Audit Gap P0 #1): video frame thumbnails. Registered once in
    // GalleryCleanerApp's shared ImageLoader — every screen that already
    // renders a MediaItem via MediaPreview.kt gets working video thumbnails
    // for free, no per-screen changes needed.
    implementation("io.coil-kt:coil-video:2.6.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // Batch44 (Audit Gap P1 #5): efficient incremental-append for
    // progressive gallery loading (MainActivity's `allMedia`). Pinned to
    // 0.3.8, NOT the newest release — 0.4.0+ requires Kotlin >=2.1.20,
    // this project is on Kotlin 1.9.24 (see build.gradle.kts), so a newer
    // version would fail dependency resolution. 0.3.8 only requires
    // Kotlin >=1.9.21, safely under this project's version.
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    // Batch39 (Audit Gap P0 #4): real BiometricPrompt for App Lock,
    // replacing the deprecated KeyguardManager.createConfirmDeviceCredentialIntent().
    implementation("androidx.biometric:biometric:1.1.0")
    // Batch49: in-app update (UpdateChecker + ApkDownloader). Brings Okio
    // transitively, used for true chunk-by-chunk APK streaming instead of
    // loading the whole response body into RAM (project rule "Release
    // Downloader (Anti-OOM)").
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Batch121 (user bug report): the app has scanned + shown video
    // thumbnails since Batch40 (Audit Gap P0 #1, coil-video above), but
    // tapping a video to inspect/fullscreen it only ever decoded ONE
    // still frame via Coil's VideoFrameDecoder — there was never an
    // actual player behind it, which read as "can't play/inspect it at
    // all". media3-exoplayer + media3-ui add real playback (controls,
    // seek) to SwipeScreenCard.kt's FullscreenViewer, for video items
    // only — photo/GIF preview is untouched.
    // Pinned to 1.4.1 (Aug 27, 2024, verified via
    // mvnrepository.com/artifact/androidx.media3/media3-exoplayer — a
    // real released version, not the newest available) instead of the
    // current latest stable (1.11.0, per
    // developer.android.com/jetpack/androidx/releases/media3): that
    // changelog's own "Common library" entry for 1.11.0 says it upgraded
    // its internal Kotlin toolchain from 2.0.20 to 2.2.0, and this
    // project is pinned to Kotlin 1.9.24 (root build.gradle.kts,
    // protected file, out of scope to bump for this task) — same class
    // of risk already hit once in this project with
    // kotlinx-collections-immutable (Batch44, pinned to 0.3.8 for the
    // identical reason: a newer release needed a newer Kotlin than this
    // project has). 1.4.1 predates that Kotlin bump by roughly two
    // years and sits in the same era as this project's other pinned
    // build tooling (Kotlin 1.9.24, compose-bom 2024.06.00, AGP 8.5.0).
    // No new proguard-rules.pro entry: Media3/ExoPlayer artifacts ship
    // their own consumer ProGuard rules bundled in the AAR (unlike Coil,
    // which needs this file's existing manual -keep rules because it
    // discovers decoders via reflection) — recorded here as the
    // reasoning, not re-verified against an actual R8 build in this
    // sandbox (same "belum tervalidasi compiler asli" caveat as every
    // other dependency change in this project).
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
