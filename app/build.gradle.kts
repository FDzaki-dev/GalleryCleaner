plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun gitCommitCount(): Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

val appVersionCode = gitCommitCount()

android {
    namespace = "com.example.gallerycleaner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gallerycleaner"
        minSdk = 24
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
            isMinifyEnabled = false
            isShrinkResources = false

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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
