package dev.kikugie.postprocess.impl.jsonlang

import dev.kikugie.postprocess.core.PostProcessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

open class JsonLangPlugin : Plugin<Project> {
    override fun apply(target: Project) = PostProcessPlugin.register<JsonLangExtension>(target, "jsonlang")
}