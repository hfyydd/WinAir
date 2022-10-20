import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
}

group = "me.winair"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven ("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.github.serezhka:java-airplay-lib:1.0.5")
    implementation("com.github.serezhka:java-airplay-server:1.0.5")
    implementation("com.github.serezhka:fdk-aac-jni:1.0.0")
    implementation("net.java.dev.jna:jna:5.11.0")
    implementation("net.java.dev.jna:jna-platform:5.11.0")
    implementation("org.freedesktop.gstreamer:gst1-java-core:1.4.0")
    implementation("org.freedesktop.gstreamer:gst1-java-swing:0.9.0")
    implementation("com.formdev:flatlaf:2.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "WinAir"
            packageVersion = "1.0.0"
        }
    }
}