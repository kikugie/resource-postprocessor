# Resource Postprocessor

A library and a collection of plugins for processing resources after the standard `processResources` Gradle task.

### General setup
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://maven.kikugie.dev/releases")
    }
}
```

```groovy
// settings.gradle
pluginManagement {
    repositories {
        maven { url = "https://maven.kikugie.dev/releases" }
    }
}
```

# Bundled plugins
## J52J
*AKA "Json5 to Json"*  
A Gradle plugin that converts `.json5` files into `.json`.

> [!WARNING]
> You can't use `fabric.mod.json5` with this plugin.
> Fabric Loom reads `fabric.mod.json` directly from the source code and doesn't work with the `.json5` format.

### Buildscript setup
```kotlin
// build.gradle[.kts]
plugins {
    id ("dev.kikugie.postprocess") version "2.1-beta.1"
    id ("dev.kikugie.postprocess.j52j")
}

j52j {
    /* Overrides sources processed by the plugin.
    By default, it dynamically adds all registered sources,
    so this is not required unless you want some sources to not be processed.*/
    sources(sourceSets["main"])

    /* Enables indentation in the processed JSON files.
    Due to limitations of Gson, the indent can only be two spaces.*/
    prettyPrint = true
}
```

### File options
File properties are configured in a header comment:
```json5
// this is a header
{
  json: 5
}
```
There are the following parameters that can be put in the header:
- `// no j52j` - Skips processing for the file, keeping it in JSON5 format.
- `// to mcmeta` (or other) - Specifies a different file extension to be used after conversion. The extension must not start with a dot.

## YAMLang
Plugin for converting nested YAML language files to plain JSON for Minecraft mods.
Based on [Fallen Breath's yamlang](https://github.com/Fallen-Breath/yamlang),
with fixes to the task caches and Gradle configuration cache compatibility.

Using YAML for language files provides several advantages:
- Less syntactic sugar, such as quotes and manual `\n` line breaks.
- Convenient multi-line and wrapped string syntax (see https://yaml-multiline.info/).
- Single-line comment support.
- Shorter key definitions, occupying less screen space.
- Foldable translation groups in modern editors.

### Functionality
This plugin converts nested YAML files of similar format:
```yaml
modid:
  # Mod title
  .: My Mod
  item:
    super_stick: Super Stick
    evil_potato: Evil Potato
  tooltip.item:
    super_stick: Hits reaaaaally hard!
    evil_potato: |-
      What disastrous plans may it have? 
      You will never know!
```
To the JSON file compatible with Minecraft translation style:
```json
{
  "modid": "My Mod",
  "modid.item.super_stick": "Super Stick",
  "modid.item.evil_potato": "Evil Potato",
  "modid.tooltip.item.super_stick": "Hits reaaaaally hard!",
  "modid.tooltip.item.evil_potato": "What disastrous plans may it have?\nYou will never know!"
}
```

### Buildscript setup
```kotlin
// build.gradle[.kts]
plugins {
    id ("dev.kikugie.postprocess") version "2.1-beta.1"
    id ("dev.kikugie.postprocess.yamlang")
}

yamlang {
    /* Overrides sources processed by the plugin.
    By default, it dynamically adds all registered sources,
    so this is not required unless you want some sources to not be processed.*/
    sources(sourceSets["main"])

    // Path to the language file directory starting from `src/{any}/resources/`. When `null`, no files will be processed.
    languageDirectory = "assets/${modId}/lang"

    /* Enables indentation in the processed JSON files.
    Due to limitations of Gson, the indent can only be two spaces.*/
    prettyPrint = true

    // Allows lists in language entries for https://docs.wispforest.io/owo/rich-translations/ compatibility.
    allowListEntries = true
    
    // Allows duplicate keys in both YAML and resulting JSON to override previous values.
    allowDuplicateKeys = true
    
    // Allows using value anchors in source files. See https://support.atlassian.com/bitbucket-cloud/docs/yaml-anchors/
    allowRecursiveKeys = true
    
    /* Selects a built-in schema for the YAML parser.
    Must be either "core", "json" or "failsafe".
    See https://yaml.org/spec/1.2.2/#chapter-10-recommended-schemas
    or ask ChatGPT about it if reading a wall of text is hard.*/
    yamlSchema = "failsafe"
}
```

## JSONLang
Plugin for converting nested JSON or JSON5 language files to plain JSON.
The configuration is largely similar to the YAML described above.

Language files written in JSON5 have a few differences to YAML and JSON formats:
- Supports full [JSON5 spec](https://spec.json5.org/).
- Can be preprocessed by [Stonecutter](https://stonecutter.kikugie.dev/stonecutter/guide/comments).
- Syntax is stricter than YAML, as well as easier to parse with third-party tools.
- Limited duplicate key support when compared to YAML.
    ```json5
    {
      // Not allowed
      key: "old val",
      key: "new val",
      
      // Allowed
      keys: {
        key: "old val"
      },
      "keys.key": "new val"
    }
    ```

### Functionality
This plugin converts nested JSON files of similar format:
```json
{
  "modid": {
    ".": "My mod",
    "item": {
      "super_stick": "Super Stick",
      "evil_potato": "Evil Potato"
    },
    "tooltip.item": {
      "super_stick": "Hits reaaaaally hard!",
      "evil_potato": "What disastrous plans may it have?\nYou will never know!"
    }
  }
}
```
To the JSON file compatible with Minecraft translation style:
```json
{
  "modid": "My Mod",
  "modid.item.super_stick": "Super Stick",
  "modid.item.evil_potato": "Evil Potato",
  "modid.tooltip.item.super_stick": "Hits reaaaaally hard!",
  "modid.tooltip.item.evil_potato": "What disastrous plans may it have?\nYou will never know!"
}
```

### Buildscript setup
```kotlin
// build.gradle[.kts]
plugins {
    id ("dev.kikugie.postprocess") version "2.1-beta.1"
    id ("dev.kikugie.postprocess.jsonlang")
}

jsonlang {
    /* Overrides sources processed by the plugin.
    By default, it dynamically adds all registered sources,
    so this is not required unless you want some sources to not be processed.*/
    sources(sourceSets["main"])

    // Path to the language file directory starting from `src/{any}/resources/`. When `null`, no files will be processed.
    languageDirectory = "assets/${modId}/lang"

    /* Enables indentation in the processed JSON files.
    Due to limitations of Gson, the indent can only be two spaces.*/
    prettyPrint = true

    // Allows lists in language entries for https://docs.wispforest.io/owo/rich-translations/ compatibility.
    allowListEntries = true
    
    // Allows duplicate keys in both YAML and resulting JSON to override previous values.
    allowDuplicateKeys = true
}
```

# Custom processors
Additional logic can be added by third-party or convention plugins to the resource processing.
Registering a processor through the plugin allows it to mitigate most of the order-dependency issues,
as well as improve execution speed and error reports.

### Implementation
A custom processor must implement [ResourcePostProcessor](https://github.com/stonecutter-versioning/resource-postprocessor/blob/master/src/main/kotlin/dev/kikugie/postprocess/api/ResourcePostProcessor.kt) interface. 
Refer to the KDoc comments for implementation details.

The processor must have a constructor that accepts a transient Gradle `Project` instance.
```java
public class MyResourceProcessor implements ResourcePostProcessor {
    transient private final Project project;
    
    public MyResourceProcessor(@Nullable Project project) {
        this.project = project;
    }
    
    // The rest of the implementation
}
```

The processor will be registered as an extension, allowing it to be configured as shown in plugins above.
Processors have to be registered in the plugin entrypoint.
This will automatically apply the base plugin if needed.
```java
void apply(Project target) {
    PostProcessPlugin.register(target, "myplugin", MyResourceProcessor.class);
}
```
In this example extension named `myplugin` will be created and can be accessed with
```kotlin
// build.gradle.kts
myplugin {
    // ...
}
```
