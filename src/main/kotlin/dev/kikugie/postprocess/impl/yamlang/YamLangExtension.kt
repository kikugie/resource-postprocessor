package dev.kikugie.postprocess.impl.yamlang

import com.google.gson.Gson
import dev.kikugie.postprocess.api.ResourcePostProcessor
import dev.kikugie.postprocess.createGson
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.collections.set

open class YamLangExtension(@Transient override val project: Project? = null) : ResourcePostProcessor {
    internal companion object {
        fun convertYaml(input: String, gson: Gson, preserveLists: Boolean): String {
            val yaml: Map<*, *> = Yaml().load(input)
            val map: MutableMap<String, Any> = mutableMapOf()
            flatten(yaml, map, "", preserveLists)
            return gson.toJson(map)
        }

        fun flatten(element: Map<*, *>, result: MutableMap<String, Any>, prefix: String, preserveLists: Boolean) {
            for ((key, value) in element) {
                check(key is String) { "Null key at $prefix" }
                val fullKey = when {
                    prefix.isEmpty() -> key.toString()
                    key == "." -> prefix
                    else -> "$prefix.$key"
                }
                when (value) {
                    is String, Int, Boolean -> result[fullKey] = value.toString()
                    is Map<*, *> -> flatten(value, result, fullKey, preserveLists)
                    null -> error("Null entries are not allowed (key '$fullKey')")
                    is List<*> -> if (preserveLists) result[fullKey] = value
                    else error("List entries are not allowed without the 'preserveLists' (key '$fullKey')")

                    else -> error("Unknown type: ${value::class.qualifiedName} (key '$fullKey')")
                }
            }
        }
    }

    override val name: String = "yamlang"
    override val display: String = "YAMLangExtension"
    override var sources: Set<SourceSet>? = null

    var prettyPrint: Boolean = false
    var preserveLists: Boolean = false
    var languageDirectory: String? = null

    override fun relocate(file: File): File? = when {
        languageDirectory == null -> null
        file.extension != "yml" && file.extension != "yaml" -> null
        !file.parentFile.invariantSeparatorsPath.endsWith(languageDirectory!!) -> null
        else -> file.resolveSibling("${file.nameWithoutExtension}.json")
    }

    override fun convert(input: String): String =
        convertYaml(input, createGson(prettyPrint), preserveLists)

}