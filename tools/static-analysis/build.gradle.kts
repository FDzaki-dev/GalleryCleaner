import io.gitlab.arturbosch.detekt.Detekt

// detekt 1.23.6 dipilih karena dikompilasi dengan Kotlin 1.9.23 / Gradle 8.7 /
// JDK 17 — pasangan paling dekat dengan proyek ini (Kotlin 1.9.24, AGP 8.5.0,
// JDK 17). Versi 1.23.7+ dikompilasi dengan Kotlin 2.x, tidak dipakai.
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

// Folder ini berada di <root>/tools/static-analysis, jadi root repositori = ../..
val repoRoot: File = rootDir.resolve("../..").canonicalFile

detekt {
    // Timpa bawaan detekt, bukan menggantikannya (lihat config/detekt/detekt.yml).
    buildUponDefaultConfig = true
    config.setFrom(repoRoot.resolve("config/detekt/detekt.yml"))
    source.setFrom(repoRoot.resolve("app/src/main/java"))

    // NON-BLOCKING: temuan dilaporkan, tidak pernah menggagalkan task/build.
    ignoreFailures = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
    }
}
