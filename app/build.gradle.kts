plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// A matching signing certificate (see signingConfigs below) is only half of
// what makes an update install in place — Android separately refuses to
// install an APK whose versionCode isn't strictly greater than the one
// already installed, treating it as a downgrade/duplicate rather than an
// update. A hardcoded `versionCode = 1` would fail that check on literally
// every build after the first one, no matter how the signing is set up.
// Counting git commits gives a versionCode that's guaranteed to increase
// with every push, with no manual bump-the-number step to forget.
fun gitCommitCount(): Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1 // local build outside a git checkout, or git unavailable — never fail the build over this
}

val appVersionCode = gitCommitCount()

android {
    namespace = "com.example.gallerycleaner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gallerycleaner"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = "1.0.$appVersionCode"
    }

    signingConfigs {
        // Reuses Android's auto-generated debug key for LOCAL builds where
        // no release secrets are present (e.g. building outside CI), so
        // `assembleRelease` still produces an installable APK without
        // needing the real keystore on every machine.
        getByName("debug") {}

        // The real signing identity for CI-built release APKs. Populated
        // from environment variables that GitHub Actions injects from repo
        // secrets (see .github/workflows/build.yml) — the actual keystore
        // and passwords are never committed to this repo.
        //
        // This is the fix for "every update forces an uninstall first":
        // that happens when consecutive release APKs are signed with
        // DIFFERENT certificates, which is exactly what was happening
        // before — signingConfig was pointed at the "debug" key, and
        // Android's debug keystore is auto-generated per-machine/per-CI-run
        // if one doesn't already exist. Every GitHub Actions run starts on
        // a fresh runner with no prior debug.keystore, so every build was
        // silently getting a brand new, different signing key. A real,
        // persistent release key — generated once, stored only as a
        // GitHub secret, reused every build — is what makes signatures
        // match release over release, which is what lets Android treat a
        // new APK as an update to the existing app instead of a conflicting
        // unknown one.
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Only falls back to the debug key when the real release
            // secrets genuinely aren't available. In CI, RELEASE_KEYSTORE_PATH
            // is always set (see workflow), so every APK GitHub Actions
            // produces is signed with the same real, persistent key.
            signingConfig = if (System.getenv("RELEASE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfigs.getByName("debug")
            } else {
                signingConfigs.getByName("release")
            }
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
