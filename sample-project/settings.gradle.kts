dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

    repositories {
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sample-project"
includeBuild("../") {
    name = "astronkt"
}