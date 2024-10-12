plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

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