pluginManagement {
    includeBuild("..")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/third-party") {
            name = "KikuGie"
        }
    }
}