plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = "org.astronkt"
            artifactId = "core"
            version = rootProject.version as String
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