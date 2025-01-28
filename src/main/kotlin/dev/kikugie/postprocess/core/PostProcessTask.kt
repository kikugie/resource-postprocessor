package dev.kikugie.postprocess.core

import dev.kikugie.postprocess.api.ResourcePostProcessor
import org.gradle.api.file.FileTree
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class PostProcessTask : SourceTask() {
    @get:Internal
    abstract val files: Property<FileTree>

    @get:Inject
    abstract val problems: Problems

    @get:Input
    abstract val processors: ListProperty<ResourcePostProcessor>

    init {
        processors.convention(mutableListOf())
    }

    fun source(src: SourceSet) {
        if (processors.get().all { it.sources != null && src !in it.sources!! })
            return
        src.resources.sourceDirectories.forEach(::source)
        src.output.resourcesDir?.asTree()?.let {
            files.set(files.orNull?.plus(it) ?: it)
        }
    }

    @TaskAction
    fun run() {
        val exception = RuntimeException("[PostProcessor] One or more files failed to be processed. See messages above for more information")
        for (file in files.get()) processFile(file).onFailure {
            exception.addSuppressed(it)
        }

        if (exception.suppressed.isNotEmpty()) throw problems.reporter.throwing {
            PostProcessorGroup.child {
                name = "postprocessor-composite"
                display = "Post Processor Task Error"
            }.applyToSpec(this)
            withException(exception)
        }
    }

    private fun processFile(file: File) = runCatching {
        var finalized = file
        val usedProcessors = mutableSetOf<ResourcePostProcessor>()
        while (true) usedProcessors += processors.get().find {
            if (it in usedProcessors) return@find false
            val dest = it.runCatching { relocate(finalized) }.getOrReport(problems) {e ->
                applyReportID(it)
                withException(e)
                contextualLabel("An exception occurred while relocating $file")
                logger.error("[PostProcessor] Error while relocating $file with ${it.display}", e)
            } ?: return@find false
            finalized = dest
            true
        } ?: break
        if (usedProcessors.isEmpty()) return@runCatching

        var result = file.readText()
        for (processor in usedProcessors) result = runCatching {
            processor.convert(result)
        }.getOrReport(problems) {
            applyReportID(processor)
            withException(it)
            contextualLabel("An exception occurred while $file was processed by ${processor.display}")
            logger.error("[PostProcessor] Error while processing $file with ${processor.display}", it)
        }

        finalized.runCatching {
            writeText(result)
            if (file != this) file.delete()
            this
        }.getOrReport(problems) {
            applyReportID("io-error", "IO")
            withException(it)
            logger.error("[PostProcessor] Error while finalizing file changes", it)
        }
    }

    private fun ProblemSpec.applyReportID(type: String, title: String) {
        PostProcessorGroup.child {
            name = "postprocessor-files"
            display = "File Processing Reports"
        }.child {
            name = "postprocessor-$type"
            display = "$title Error"
        }.applyToSpec(this)
    }

    private fun ProblemSpec.applyReportID(processor: ResourcePostProcessor) {
        PostProcessorGroup.child {
            name = "postprocessor-${processor.name}"
            display = "${processor::class.simpleName} Reports"
        }.child {
            name = "postprocessor-processor-error"
            display = "${processor::class.simpleName} Error"
        }.applyToSpec(this)
    }

    private fun File.asTree() = project.objects.fileTree().from(this)
}