plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

group = "org.astronkt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
}