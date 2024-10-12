plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("org.astronkt.plugin") version "0.1.1"
}

group = "org.astronkt"

application {
    mainClass = "org.astronkt.sample.MainKt"
}

dClassPluginConfig {
    files = listOf("game.dc")
}

dependencies {
    // NOTE: This is using subproject coordinates
    implementation("astronkt:library")
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
}