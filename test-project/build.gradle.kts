plugins {
    java
    id("dev.kikugie.postprocess.j52j")
    id("dev.kikugie.postprocess.yamlang")
    id("dev.kikugie.postprocess.jsontree")
}

sourceSets {
    val extra by creating {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
}

yamlang {
    languageDirectory = "lang"
}

jsontree {
    prettyPrint = true
    languageDirectory = "lang"
}