plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    `java-gradle-plugin`
}

dependencies {
    compileOnly(libs.kotlin.plugin)
    implementation(project(":dclassmacro"))
}

group = "org.astronkt"

gradlePlugin {
    plugins {
        create("dclassPlugin") {
            id = "org.astronkt.plugin"
            implementationClass = "org.astronkt.plugin.DClassPlugin"
            version = rootProject.version
        }
    }
}

publishing {
    repositories {
        add(rootProject.publishing.repositories.named("GitHubPackages").get())
    }
}