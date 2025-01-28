plugins {
    idea
    java
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.0"
}

group = "dev.kikugie"
version = "2.1-beta.3"

repositories {
    mavenCentral()
    maven("https://maven.kikugie.dev/third-party") {
        name = "KikuGie"
    }
}

dependencies {
    implementation("org.snakeyaml:snakeyaml-engine:2.9")
    implementation("org.quiltmc.parsers:gson:0.2.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(16)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

publishing {
    repositories {
        maven {
            name = "kikugieMaven"
            url = uri("https://maven.kikugie.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create("basic", BasicAuthentication::class)
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "postprocess"
            version = project.version.toString()
            artifact(tasks.getByName("jar"))
        }
    }
}

gradlePlugin {
    website = "https://github.com/kikugie/resource-postprocessor"
    vcsUrl = "https://github.com/kikugie/resource-postprocessor"

    plugins {
        create("postprocess") {
            id = "dev.kikugie.postprocess"
            displayName = "Resource Postprocessor"
            description = "Core functionality for resource postprocessing."
            implementationClass = "dev.kikugie.postprocess.core.PostProcessPlugin"
        }
        create("j52j") {
            id = "dev.kikugie.postprocess.j52j"
            displayName = "J52J"
            description = "Json5 to Json resource processing plugin"
            implementationClass = "dev.kikugie.postprocess.impl.j52j.J52JPlugin"
        }
        create("yamlang") {
            id = "dev.kikugie.postprocess.yamlang"
            displayName = "YAMLang"
            description = "Plugin for converting nested YAML language files to plain JSON for Minecraft mods."
            implementationClass = "dev.kikugie.postprocess.impl.yamlang.YamLangPlugin"
        }
        create("jsontree") {
            id = "dev.kikugie.postprocess.jsonlang"
            displayName = "JSONTLang"
            description = "Plugin for converting nested JSON or JSON5 language files to plain JSON for Minecraft mods."
            implementationClass = "dev.kikugie.postprocess.impl.jsonlang.JsonLangPlugin"
        }
    }
}