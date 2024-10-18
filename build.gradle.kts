import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jreleaser.model.Active
import org.jreleaser.model.Signing

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
    alias(libs.plugins.jreleaser)

}

group = "io.github.rand0m-cloud.astronkt"
version = "0.1.3"

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = rootProject.group
    version = rootProject.version

    publishing {
        repositories {
            maven {
                url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
            }
            mavenLocal()
        }
    }


    java {
        withJavadocJar()
        withSourcesJar()
    }
}

jreleaser {
    signing {
        active = Active.ALWAYS
        armored = true
        mode = Signing.Mode.COMMAND
        command {
            keyName = "67330A37C2F9CF7EFAA3FC88AD494F4E8D2BE55B"
        }
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

val usePomTemplate: MavenPublication.(String, String) -> Unit = { publicName, publicDescription ->
    pom {
        name = publicName
        description = publicDescription
        url = "https://github.com/rand0m-cloud/astronkt"
        organization {
            name = "io.github.rand0m-cloud.astronkt"
            url = "https://github.com/rand0m-cloud/astronkt"
        }

        developers {
            developer {
                name = "Abby Bryant"
                email = "rand0m-cloud@outlook.com"
                url = "https://github.com/rand0m-cloud"
            }
        }

        issueManagement {
            system = "github"
            url = "https://github.com/rand0m-cloud/astronkt"
        }

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
            license {
                name = "The MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "repo"
            }
        }

        scm {
            url = "https://github.com/rand0m-cloud/astronkt"
            connection = "scm:git:git://github.com/rand0m-cloud/astronkt"
            developerConnection = "scm:git:ssh://github.com/rand0m-cloud/astronkt.git"
        }
    }
}
extraProperties.set("PomTemplate", usePomTemplate)
