plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    `java-gradle-plugin`
}

group = "org.astronkt"
version = "0.1.0"

dependencies {
    compileOnly(libs.kotlin.plugin)
    implementation(project(":dclassmacro"))
}

gradlePlugin {
    plugins {
        create("dclassPlugin") {
            id = "org.astronkt.plugin"
            implementationClass = "org.astronkt.plugin.DClassPlugin"
        }
    }
}

publishing {
    repositories {
        add(rootProject.publishing.repositories.named("GitHubPackages").get())
    }
}