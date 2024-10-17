pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "astronkt"
include("library")
include("explorer")
include("dclassmacro")
include("plugin")
