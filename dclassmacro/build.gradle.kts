plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "org.astronkt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass = "org.astronkt.dclassmacro.MainKt"
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.better.parse)
    implementation(project(":library"))
}
