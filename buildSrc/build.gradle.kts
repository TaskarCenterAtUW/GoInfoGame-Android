repositories {
    google()
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("com.beust:klaxon:5.6")
    implementation("de.westnordost:countryboundaries:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
    implementation("com.esotericsoftware.yamlbeans:yamlbeans:1.17")
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.charleskorn.kaml:kaml:0.102.0")
    implementation("org.jetbrains:markdown:0.7.3")
}

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.2.21"
}
