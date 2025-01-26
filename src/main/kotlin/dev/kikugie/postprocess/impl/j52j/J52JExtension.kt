package dev.kikugie.postprocess.impl.j52j

import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.kikugie.postprocess.api.ResourcePostProcessor
import dev.kikugie.postprocess.createGson
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.quiltmc.parsers.json.JsonReader
import org.quiltmc.parsers.json.gson.GsonReader
import java.io.File

open class J52JExtension(@Transient override val project: Project? = null) : ResourcePostProcessor {
    internal data class J52JFileProperties(val skip: Boolean, val format: String)
    internal companion object {
        val DEFAULT = J52JFileProperties(false, "json")
        val PATTERN = Regex("to (\\S+)")

        fun convertJson(input: String, gson: Gson): String {
            val reader = JsonReader.json5(input)
            val element = JsonParser.parseReader(GsonReader(reader))
            return gson.toJson(element)
        }

        fun parseProperties(file: File): J52JFileProperties = file.useLines { lines ->
            val first = lines.find { it.trimStart().startsWith("//") } ?: return@useLines DEFAULT
            val skip = "no j52j" in first
            val target = PATTERN.find(first)?.groupValues?.getOrNull(1) ?: "json"
            J52JFileProperties(skip, target)
        }
    }

    override val name: String = "j52j"
    override val display: String = "J52JExtension"
    override var sources: Set<SourceSet>? = null

    /**Enables indentation in the resulting JSON file. Due to [Gson] limitations, the indent can only be two spaces.*/
    var prettyPrint: Boolean = false

    override fun convert(input: String): String = convertJson(input, createGson(prettyPrint))

    override fun relocate(file: File): File? {
        if (file.extension != "json5") return null
        if (!file.exists()) return file.resolveSibling("${file.nameWithoutExtension}.json")
        val properties = parseProperties(file)
        return if (properties.skip) null
        else file.resolveSibling("${file.nameWithoutExtension}.${properties.format}")
    }
}