plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("org.astronkt.plugin") version "0.1"
}

application {
    mainClass = "org.astronkt.sample.MainKt"
}

dClassPluginConfig {
    files = listOf("game.dc")
}

dependencies {
    implementation("org.astronkt:core:0.1")
}