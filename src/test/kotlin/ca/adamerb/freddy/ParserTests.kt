package ca.adamerb.freddy

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ParserTests {
    
    @Test
    fun `Field name longer than buffer`() {
        val longName = (0 .. 10).joinToString(separator = "_") { "long_field_name" }
        val json = readJsonTree(
            JsonTokenizer(
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
    fun `Test array`() {
        val json = readJsonTree(JsonTokenizer("""["a", "b", "c"]"""))
        assertTrue(json is JsonArray)
        assertEquals(json.asList().map { it.asString() }, listOf("a", "b", "c"))
        assertEquals(json[0].asString(), "a")
    }

    @Test
    fun `Test escaped strings`() {
        val strings = readJsonTree(JsonTokenizer(
            """["a\"", "\\b", "c\n"]"""
        )).asList().map { it.asString() }
        assertEquals(strings, listOf("a\"", "\\b", "c\n"))
    }

    @Test
    fun `Test unicode strings`() {

        val strings = readJsonTree(JsonTokenizer("""["\u2022","\uD834\uDD1E"]"""))
            .asList()
            .map { it.asString() }
        assertEquals(strings, listOf("•", "𝄞"))
    }

    @Test
    fun `Test integers`() {
        readJsonTree(JsonTokenizer("""[0, 1, 2]"""))
            .asList()
            .map { it.toInt() }
            .forEachIndexed { index, value ->
                assertEquals(index, value)
            }
    }

    @Test
    fun `Test numbers`() {
        val nums = readJsonTree(JsonTokenizer("""[0.0, 1E4, 1E-1]"""))
            .asList()
            .map { it.toInt() }
        assertEquals(listOf(0, 1E4.toInt(), 1E-1.toInt()), nums)
    }

    @Test
    fun `Test bool`() {
        val json = readJsonTree(JsonTokenizer("""{
            "true": true,
            "false": false
        }""")).asObject()

        assertEquals(true, json["true"].asBool())
        assertEquals(false, json["false"].asBool())
    }

    @Test
    fun `Test basic usage`() {
        val json = readJsonTree(
            JsonTokenizer(
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
        assertEquals("0", json["a"]["b"].asString())
        assertEquals("1", json["a"]["c"].asString())
        assertEquals(setOf("b", "c"), json["a"].asObject().keys)
    }
}



