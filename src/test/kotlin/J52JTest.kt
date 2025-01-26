import dev.kikugie.postprocess.createGson
import dev.kikugie.postprocess.impl.j52j.J52JExtension
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.quiltmc.parsers.json.MalformedSyntaxException
import kotlin.test.assertEquals

object J52JTest {
    val GSON = createGson(true)
    val SAMPLES = buildList {
        test("standard conversion") {
            input = """
                {
                  // I like commenting!
                  json: 5
                }
            """.trimIndent()
            output = """
                {
                  "json": 5
                }
            """.trimIndent()
        }
        test("conversion error") {
            input = """
                {
                  // I like commenting!
                  json: "5
                }
            """.trimIndent()
            exception = MalformedSyntaxException::class
        }
    }

    @TestFactory
    fun `test transformer`() = SAMPLES.map {
        DynamicTest.dynamicTest(it.name) { check(it) }
    }

    private fun check(entry: TestEntry) {
        if (entry.exception == null) assertEquals(entry.output, J52JExtension.convertJson(entry.input, GSON))
        else assertThrows(entry.exception!!.java) {
            J52JExtension.convertJson(entry.input, GSON)
        }
    }
}