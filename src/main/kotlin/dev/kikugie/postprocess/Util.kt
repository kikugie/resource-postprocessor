package dev.kikugie.postprocess

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

val Project.sourceSets: SourceSetContainer? get() = project.findProperty("sourceSets") as? SourceSetContainer
val SourceSet.postProcessTask: String get() = "postProcess${name.replaceFirstChar(Char::uppercaseChar)}Resources"

fun createGson(prettyPrint: Boolean): Gson = GsonBuilder()
    .disableHtmlEscaping()
    .apply { if (prettyPrint) setPrettyPrinting() }
    .create()