plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    `maven-publish`
}

group = "org.astronkt"
version = "0.1.0"

publishing {
    publications {
        create<MavenPublication>("dclassmacro") {
            from(components["java"])
            groupId = "org.astronkt"
            artifactId = "dclassmacro"
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

application {
    mainClass = "org.astronkt.dclassmacro.MainKt"
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.better.parse)
    implementation(project(":library"))
}
