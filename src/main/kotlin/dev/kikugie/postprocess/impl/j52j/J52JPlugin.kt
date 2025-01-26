package dev.kikugie.postprocess.impl.j52j

import dev.kikugie.postprocess.core.PostProcessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

open class J52JPlugin : Plugin<Project> {
    override fun apply(target: Project) = PostProcessPlugin.register<J52JExtension>(target, "j52j")
}