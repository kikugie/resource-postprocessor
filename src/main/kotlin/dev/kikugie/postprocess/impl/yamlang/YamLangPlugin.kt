package dev.kikugie.postprocess.impl.yamlang

import dev.kikugie.postprocess.core.PostProcessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

open class YamLangPlugin : Plugin<Project> {
    override fun apply(target: Project) = PostProcessPlugin.register<YamLangExtension>(target, "yamlang")
}