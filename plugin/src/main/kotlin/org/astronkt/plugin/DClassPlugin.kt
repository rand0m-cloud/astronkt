@file:Suppress("unused")

package org.astronkt.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import java.io.File

class DClassPluginException(errorMsg: String, cause: Throwable? = null) : Throwable(errorMsg, cause)

class DClassPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dClassPluginConfig", DClassPluginConfig::class.java, project)
        val outputDir = "${project.layout.buildDirectory.asFile.get()}/generated/dclassbindings/kotlin"

        project.tasks.register("generateDClassBindings") {
            it.inputs.files(extension.files)
            it.outputs.dir(outputDir)

            it.doFirst {
                File(outputDir).apply {
                    deleteRecursively()
                    mkdirs()
                }
            }

            it.doLast {
                val dClassFiles = extension.files.get()
                if (dClassFiles.isEmpty()) {
                    throw DClassPluginException("No DClass Files were specified in the build script")
                }

                val err = runCatching {
                    org.astronkt.dclassmacro.main((dClassFiles + outputDir).toTypedArray())
                }.exceptionOrNull()

                if (err != null) {
                    throw DClassPluginException(
                        "Failed to generate DClass bindings: ${err.message}",
                        err
                    ).fillInStackTrace()
                }
            }
        }

        project.tasks.named("build") {
            it.dependsOn("generateDClassBindings")
        }

        project.afterEvaluate {
            val kotlinExtension =
                project.extensions.findByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
            kotlinExtension?.sourceSets?.getByName("main")?.kotlin?.srcDir(outputDir)
                ?: error("could not add generate source to main kotlin source set")
        }
    }
}

open class DClassPluginConfig(project: Project) {
    @get:Input
    val files: ListProperty<String> = project.objects.listProperty(String::class.java)
}