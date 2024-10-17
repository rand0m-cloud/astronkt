import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = group as String
            artifactId = "core"
            version = rootProject.version as String
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
}

publishing {
    publications.withType<MavenPublication>().all {
        @Suppress("UNCHECKED_CAST") val template =
            rootProject.extraProperties["PomTemplate"] as MavenPublication.(String, String) -> Unit
        template("astronkt-core", "A Kotlin library for interacting with an Astron server cluster")
    }
}
