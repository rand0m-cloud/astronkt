dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}