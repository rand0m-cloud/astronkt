plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("io.github.rand0m-cloud.astronkt.plugin")
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
    implementation("io.github.rand0m-cloud.astronkt:library")
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
}