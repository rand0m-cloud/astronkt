import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

dependencies {
    compileOnly(libs.kotlin.plugin)
    implementation(project(":dclassmacro"))
}


gradlePlugin {
    plugins {
        create("dclassPlugin") {
            id = "${group as String}.plugin"
            implementationClass = "org.astronkt.plugin.DClassPlugin"
            version = rootProject.version
        }
    }
}

publishing {
    publications.withType<MavenPublication>().all {
        @Suppress("UNCHECKED_CAST") val template =
            rootProject.extraProperties["PomTemplate"] as MavenPublication.(String, String) -> Unit
        template("astronkt-plugin", "A Gradle plugin for generating AstronKt bindings for DClass files")
    }
}