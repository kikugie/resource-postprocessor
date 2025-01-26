package dev.kikugie.postprocess.api

import dev.kikugie.postprocess.postProcessTask
import dev.kikugie.postprocess.sourceSets
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.util.PatternFilterable
import java.io.File

/**
 * Enables the core post-processing plugin to efficiently transform resources.
 * Usage:
 * ```kt
 * // PostProcessPlugin.register() handles extension registration and applies the main plugin if needed.
 * open class MyPlugin : Plugin<Project> {
 *     override fun apply(target: Project) {
 *         PostProcessPlugin.register<MyExtension>(target, "MyExtensionName")
 *     }
 * }
 *
 * // You will need [project] to update task states, but it cannot be stored with Gradle configuration cache.
 * open class MyExtension(@Transient private val project: Project? = null) : ResourcePostProcessor {
 *      ...
 * }
 * ```
 */
interface ResourcePostProcessor {
    /**Short lowercase identifier for this instance, written in kebab-case.*/
    val name: String
    /**Extension name used in report titles.*/
    val display: String
    /**Project used to update tasks to the provided sources.
     * Expect to be `null` at the task runtime stage.
     * Also recommended to annotate the implementation with `@`[Transient].*/
    val project: Project?
    /**Source sets used by the extension. When `null` any source is allowed.*/
    var sources: Set<SourceSet>?

    /**Provides the matching output for the given [file] or `null` if the file shouldn't be processed.*/
    fun relocate(file: File): File?
    /**Performs the conversion for the file contents.*/
    fun convert(input: String): String
    /**Assigns [sources] and updates corresponding tasks.*/
    fun sources(vararg src: SourceSet) = sources(src.toList())
    /**Assigns [sources] and updates corresponding tasks.*/
    fun sources(src: Iterable<SourceSet>) {
        requireNotNull(project) { "Cannot set sources on extension without a project"}
        sources = src.toSet()
        project!!.sourceSets?.forEach {
            project!!.tasks.findByName(it.postProcessTask)?.enabled = it in sources!!
        }
    }
}