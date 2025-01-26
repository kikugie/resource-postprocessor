package dev.kikugie.postprocess.core

import dev.kikugie.postprocess.api.ResourcePostProcessor
import dev.kikugie.postprocess.postProcessTask
import dev.kikugie.postprocess.sourceSets
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findPlugin
import org.gradle.kotlin.dsl.register

open class PostProcessPlugin : Plugin<Project> {
    companion object {
        @JvmStatic inline fun <reified T : ResourcePostProcessor> register(project: Project, name: String) =
            register(project, name, T::class.java)

        @JvmStatic fun register(project: Project, name: String, cls: Class<out ResourcePostProcessor>) = with(project.plugins) {
            val extension = project.extensions.create(name, cls, project)
            findPlugin(PostProcessPlugin::class)?.registerProcessor(project, extension)
                ?: apply(PostProcessPlugin::class).registerProcessor(project, extension)
        }
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create<PostProcessExtension>("postprocess", target)
        configureTaskManagement(target, extension)
    }

    private fun registerProcessor(project: Project, processor: ResourcePostProcessor) {
        project.extensions.getByType(PostProcessExtension::class.java).processors += processor
        project.tasks.filterIsInstance<PostProcessTask>().forEach { it.processors.add(processor) }
    }

    private fun configureTaskManagement(project: Project, extension: PostProcessExtension) = project.sourceSets?.apply {
        all { createPostProcessingTask(this, project, extension) }
        whenObjectRemoved { project.tasks.findByName(postProcessTask)?.apply { enabled = false } }
    }

    private fun createPostProcessingTask(source: SourceSet, project: Project, extension: PostProcessExtension) = source.run {
        val dependency = project.tasks.findByName(processResourcesTaskName) ?: return@run null
        project.tasks.findByName(postProcessTask)?.let { it.enabled = true; return@run null }
        project.tasks.register<PostProcessTask>(postProcessTask) {
            processors.set(extension.processors.toMutableList())
            source(this@run)
            dependsOn(dependency)
        }.also {
            dependency.finalizedBy(it)
        }
    }
}