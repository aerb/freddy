package ca.adamerb.freddy

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ParserTests {

    @Test
    fun `Field name longer than buffer`() {
        val longName = (0 .. 10).joinToString(separator = "_") { "long_field_name" }
        val json = readJsonTree(
            JsonTokenReader(
                """{
                    "$longName" : {
                        "a" : "0"
                    }
                }""",
                bufferSize = 10
            )
        )

        val keys = json.asObject().keys
        assertEquals(1, keys.size)
        assertEquals(longName, keys.first())
    }

    @Test
    fun `Test basic usage`() {
        val json = readJsonTree(
            JsonTokenReader(
                """{
                    "a" : {
                        "b" : "0",
                        "c" : "1"
                    }
                }"""
            )
        )

        assertTrue(json["d"]["b"].hasNoValue())
        assertTrue(json["a"] is JsonObject)
        assertEquals(json["a"]["b"].asString(), "0")
        assertEquals(json["a"]["c"].asString(), "1")
        assertEquals(json["a"].asObject().keys, setOf("b", "c"))
    }
}



