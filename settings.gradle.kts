pluginManagement {
    repositories {
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
rootProject.name = "astronkt"
include("library")
include("serverapp")
include("explorer")
include("clientapp")
include("dclassmacro")