plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "org.astronkt"
version = "0.1.1"

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rand0m-cloud/astronkt")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: ""
                password = project.findProperty("gpr.token") as String? ?: ""
            }
        }
        mavenLocal()
    }
}