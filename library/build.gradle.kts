plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "org.astronkt"
version = "0.1.0"

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = "org.astronkt"
            artifactId = "core"
            version = "0.1.0"
        }
    }

    repositories {
        add(rootProject.publishing.repositories.named("GitHubPackages").get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
}