package dev.kikugie.postprocess.impl.yamlang

import com.google.gson.Gson
import dev.kikugie.postprocess.api.ResourcePostProcessor
import dev.kikugie.postprocess.createGson
import dev.kikugie.postprocess.putChecked
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.schema.CoreSchema
import org.snakeyaml.engine.v2.schema.FailsafeSchema
import org.snakeyaml.engine.v2.schema.JsonSchema
import org.snakeyaml.engine.v2.schema.Schema
import java.io.File

open class YamLangExtension(@Transient override val project: Project? = null) : ResourcePostProcessor {
    internal companion object {
        fun createProperties(extension: YamLangExtension) = YamlProperties(
            extension.allowListEntries,
            extension.allowDuplicateKeys,
            extension.allowRecursiveKeys,
            getSchema(extension.yamlSchema)
        )

        fun getSchema(name: String) = when(name) {
            "core" -> CoreSchema()
            "json" -> JsonSchema()
            "failsafe" -> FailsafeSchema()
            else -> error("Unknown YAML schema: $name")
        }

        fun convertYaml(input: String, gson: Gson, properties: YamlProperties): String {
            val settings = LoadSettings.builder().apply {
                setSchema(properties.schema)
                if (properties.allowRecursiveKeys) setAllowRecursiveKeys(true)
                if (properties.allowDuplicateKeys) setAllowDuplicateKeys(true)
            }.build()
            val yaml: Any = Load(settings).loadFromString(input)
            check(yaml is Map<*, *>) { "Parsed YAML is not a map."}

            val map: MutableMap<String, Any> = mutableMapOf()
            flatten(yaml, map, "", properties)
            return gson.toJson(map)
        }

        fun flatten(element: Map<*, *>, result: MutableMap<String, Any>, prefix: String, properties: YamlProperties) {
            for ((key, value) in element) {
                check(key is String) { "Null key at $prefix" }
                val fullKey = when {
                    prefix.isEmpty() -> key.toString()
                    key == "." -> prefix
                    else -> "$prefix.$key"
                }
                when (value) {
                    is String, Int, Boolean -> result.putChecked(fullKey, value, properties.allowDuplicateKeys)
                    is Map<*, *> -> flatten(value, result, fullKey, properties)
                    null -> error("Null entries are not allowed (key '$fullKey')")
                    is List<*> -> if (properties.allowListEntries) result.putChecked(fullKey, value, properties.allowDuplicateKeys)
                    else error("List entries are not allowed without the 'preserveLists' (key '$fullKey')")

                    else -> error("Unknown type: ${value::class.qualifiedName} (key '$fullKey')")
                }
            }
        }
    }
    internal data class YamlProperties(
        val allowListEntries: Boolean,
        val allowDuplicateKeys: Boolean,
        val allowRecursiveKeys: Boolean,
        val schema: Schema
    )

    override val name: String = "yamlang"
    override val display: String = "YAMLangExtension"
    override var sources: Set<SourceSet>? = null

    /**Enables indentation in the resulting JSON file. Due to [Gson] limitations, the indent can only be two spaces.*/
    var prettyPrint: Boolean = false
    /**Allows lists in language entries for [owo-lib rich translations](https://docs.wispforest.io/owo/rich-translations/) compatibility.*/
    var allowListEntries: Boolean = false
    /**
     * Path to the language file directory starting from `src/{any}/resources/`.
     * When `null`, no files will be processed.
     */
    var languageDirectory: String? = null

    /**
     * Allows duplicated keys in the YAML files and resulting JSON.
     * When a duplicate is found, it will replace the previous value.
     *
     * ```yaml
     * modid:
     *  keys:
     *   foo: Discarded value
     *  keys.foo: New value
     * ```
     */
    var allowDuplicateKeys: Boolean = false
    /**
     * Allows key anchors to be used in the source YAML files.
     * The keys should not form a recursive dependency,
     * which will cause an error.
     *
     * ```yaml
     * modid:
     *  title: &title My Mod
     *  settings:
     *    title: *title
     * ```
     */
    var allowRecursiveKeys: Boolean = false

    /**
     * YAML schema used for processing files.
     * Options:
     * - ["core"](https://yaml.org/spec/1.2.2/#103-core-schema) (default)
     * - ["json"](https://yaml.org/spec/1.2.2/#102-json-schema)
     * - ["failsafe"](https://yaml.org/spec/1.2.2/#101-failsafe-schema)
     */
    var yamlSchema: String = "core"
        set(value) = when (value) {
            "core" -> field = "core"
            "json" -> field = "json"
            "failsafe" -> field = "failsafe"
            else -> error("Unknown YAML schema: $value")
        }

    override fun relocate(file: File): File? = when {
        languageDirectory == null -> null
        file.extension != "yml" && file.extension != "yaml" -> null
        !file.parentFile.invariantSeparatorsPath.endsWith(languageDirectory!!) -> null
        else -> file.resolveSibling("${file.nameWithoutExtension}.json")
    }

    override fun convert(input: String): String =
        convertYaml(input, createGson(prettyPrint), createProperties(this))
}