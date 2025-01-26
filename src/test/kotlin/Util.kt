import kotlin.reflect.KClass

class TestEntry {
    lateinit var name: String
    lateinit var input: String
    var output: String? = null
    var exception: KClass<out Throwable>? = null
}

inline fun MutableList<TestEntry>.test(name: String, block: TestEntry.() -> Unit) {
    add(TestEntry().apply { this.name = name }.apply(block))
}