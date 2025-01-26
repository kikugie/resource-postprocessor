package dev.kikugie.postprocess.core

import dev.kikugie.postprocess.api.ResourcePostProcessor
import org.gradle.api.Project

open class PostProcessExtension(private val project: Project) {
    internal val processors: MutableList<ResourcePostProcessor> = mutableListOf()
}