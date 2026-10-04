// Batch148 — build Gradle TERPISAH khusus detekt. Sengaja tidak ikut build utama
// (settings.gradle.kts di root hanya meng-include :app) supaya plugin detekt
// TIDAK PERNAH bisa merusak pembuatan APK: kalau resolusi plugin/versi bermasalah,
// yang gagal cuma job analisis statis (non-blocking), bukan build rilis.
//
// Jalankan dari folder ini:  gradle detekt
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "static-analysis"
