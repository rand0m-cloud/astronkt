import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

publishing {
    publications {
        create<MavenPublication>("dclassmacro") {
            from(components["java"])
            groupId = group as String
            artifactId = "dclassmacro"
            version = rootProject.version as String
        }
    }
}

application {
    mainClass = "org.astronkt.dclassmacro.MainKt"
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.better.parse)
    implementation(project(":library"))
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(kotlin("test"))

}

publishing {
    publications.withType<MavenPublication>().all {
        @Suppress("UNCHECKED_CAST") val template =
            rootProject.extraProperties["PomTemplate"] as MavenPublication.(String, String) -> Unit
        template("astronkt-dclassmacro", "An app for generating AstronKt bindings from DClass files")
    }
}

