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
    // Batch114 (Opsi (b) Stage 2, FINAL): material-icons-extended DICABUT lagi —
    // beda dari percobaan Batch109 (Opsi A, gagal karena masih ada 7 titik pakai
    // Icons.Filled.{Shuffle,Folder,GridView,Sort,Undo,ViewCarousel,ZoomIn} yang
    // genuinely extended-only, CI run231). Kali ini ke-7 titik itu SUDAH dipindah
    // ke vector drawable lokal (app/src/main/res/drawable/ic_*.xml, foundation-nya
    // ditambah Batch112) sebelum baris ini dicabut — HomeScreen.kt/
    // HomeScreenFolderRow.kt/SwipeScreen.kt/SwipeScreenGrid.kt di-update ATOMIK
    // bareng commit yang sama (5 file total, precedent STABILITY WINS Batch77).
    // proguard-rules.pro rule #5 (Batch113 hotfix) SENGAJA dibiarkan (sekarang
    // cuma cover sisa material-icons-core kecil di package yang sama, harmless).
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
}
