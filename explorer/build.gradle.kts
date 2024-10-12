plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "org.astronkt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass = "org.astronkt.explorer.MainKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.jar {
    dependsOn(project(":library").tasks.jar)
    manifest {
        attributes["Main-Class"] = application.mainClass
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(project(":library"))
}
