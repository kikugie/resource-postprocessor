package dev.kikugie.postprocess.impl.jsonlang

import com.google.gson.*
import dev.kikugie.postprocess.api.ResourcePostProcessor
import dev.kikugie.postprocess.createGson
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.quiltmc.parsers.json.JsonReader
import org.quiltmc.parsers.json.gson.GsonReader
import java.io.File

open class JsonLangExtension(@Transient override val project: Project? = null) : ResourcePostProcessor {
    internal companion object {
        fun convertAnyJson(input: String, gson: Gson, preserveLists: Boolean): String {
            val reader = JsonReader.json5(input)
            val element = JsonParser.parseReader(GsonReader(reader))
            check(element is JsonObject) { "Parsed JSON5 is not a map." }

            val result = mutableMapOf<String, Any>()
            flatten(element, result, "", preserveLists)

            return gson.toJson(result)
        }

        fun flatten(element: JsonObject, result: MutableMap<String, Any>, prefix: String, preserveLists: Boolean) {
            for ((key, value) in element.entrySet()) {
                val fullKey = when {
                    prefix.isEmpty() -> key
                    key == "." -> prefix
                    else -> "$prefix.$key"
                }
                when(value) {
                    is JsonPrimitive -> result[fullKey] = value.asString
                    is JsonObject -> flatten(value, result, fullKey, preserveLists)
                    is JsonNull -> error("Null entries are not allowed (key '$fullKey')")
                    is JsonArray -> if (preserveLists) result[fullKey] = value
                    else error("List entries are not allowed without the 'preserveLists' (key '$fullKey')")
                    else -> error("Unknown type: ${value::class.qualifiedName} (key '$fullKey')")
                }
            }
        }
    }

    override val name: String = "jsonlang"
    override val display: String = "JsonLangExtension"
    override var sources: Set<SourceSet>? = null

    var prettyPrint: Boolean = false
    var preserveLists: Boolean = false
    var languageDirectory: String? = null

    override fun relocate(file: File): File? = when {
        languageDirectory == null -> null
        file.extension != "json" && file.extension != "json5" -> null
        !file.parentFile.invariantSeparatorsPath.endsWith(languageDirectory!!) -> null
        else -> file.resolveSibling("${file.nameWithoutExtension}.json")
    }

    override fun convert(input: String): String =
        convertAnyJson(input, createGson(prettyPrint), preserveLists)
}