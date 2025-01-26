package dev.kikugie.postprocess.impl.jsonlang

import com.google.gson.*
import dev.kikugie.postprocess.api.ResourcePostProcessor
import dev.kikugie.postprocess.createGson
import dev.kikugie.postprocess.putChecked
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.quiltmc.parsers.json.JsonReader
import org.quiltmc.parsers.json.gson.GsonReader
import java.io.File

open class JsonLangExtension(@Transient override val project: Project? = null) : ResourcePostProcessor {
    internal companion object {
        fun createProperties(extension: JsonLangExtension) = JsonProperties(
            extension.allowListEntries,
            extension.allowDuplicateKeys,
        )

        fun convertAnyJson(input: String, gson: Gson, properties: JsonProperties): String {
            val reader = JsonReader.json5(input)
            val element = JsonParser.parseReader(GsonReader(reader))
            check(element is JsonObject) { "Parsed JSON5 is not a map." }

            val result = mutableMapOf<String, Any>()
            flatten(element, result, "", properties)

            return gson.toJson(result)
        }

        fun flatten(element: JsonObject, result: MutableMap<String, Any>, prefix: String, properties: JsonProperties) {
            for ((key, value) in element.entrySet()) {
                val fullKey = when {
                    prefix.isEmpty() -> key
                    key == "." -> prefix
                    else -> "$prefix.$key"
                }
                when(value) {
                    is JsonPrimitive -> result.putChecked(fullKey, value.asString, properties.allowDuplicateKeys)
                    is JsonObject -> flatten(value, result, fullKey, properties)
                    is JsonNull -> error("Null entries are not allowed (key '$fullKey')")
                    is JsonArray -> if (properties.allowListEntries) result.putChecked(fullKey, value, properties.allowDuplicateKeys)
                    else error("List entries are not allowed without the 'preserveLists' (key '$fullKey')")
                    else -> error("Unknown type: ${value::class.qualifiedName} (key '$fullKey')")
                }
            }
        }
    }
    internal data class JsonProperties(
        val allowListEntries: Boolean,
        val allowDuplicateKeys: Boolean
    )

    override val name: String = "jsonlang"
    override val display: String = "JsonLangExtension"
    override var sources: Set<SourceSet>? = null

    /**Path to the language file directory starting from `src/{any}/resources/`. When `null`, no files will be processed.*/
    var languageDirectory: String? = null
    /**Enables indentation in the resulting JSON file. Due to [Gson] limitations, the indent can only be two spaces.*/
    var prettyPrint: Boolean = false
    /**Allows lists in language entries for [owo-lib rich translations](https://docs.wispforest.io/owo/rich-translations/) compatibility.*/
    var allowListEntries: Boolean = false

    /**
     * Allows duplicated keys in the JSON[5] files and resulting JSON.
     * When a duplicate is found, it will replace the previous value.
     *
     * ```json5
     * modid: {
     *   keys:
     *     foo: "Discarded value"
     *   "keys.foo": "New value"
     * }
     * ```
     */
    var allowDuplicateKeys: Boolean = false

    override fun relocate(file: File): File? = when {
        languageDirectory == null -> null
        file.extension != "json" && file.extension != "json5" -> null
        !file.parentFile.invariantSeparatorsPath.endsWith(languageDirectory!!) -> null
        else -> file.resolveSibling("${file.nameWithoutExtension}.json")
    }

    override fun convert(input: String): String =
        convertAnyJson(input, createGson(prettyPrint), createProperties(this))
}