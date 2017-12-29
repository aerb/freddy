package ca.adamerb.freddy

import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val test = """
{
    "a" : {
        "b" : "0",
        "c" : "1"
    }
}
"""

class ParserTests {

    @Test
    fun `Test basic usage`() {
        val reader = JsonTokenReader(ByteArrayInputStream(test.toByteArray()).bufferedReader())
        val e = readObject(reader)

        assertTrue(e["d"]["b"].hasNoValue())
        assertTrue(e["a"] is JsonObject)
        assertEquals(e["a"]["b"].asString(), "0")
        assertEquals(e["a"]["c"].asString(), "1")
        assertEquals(e["a"].asObject().keys, setOf("b", "c"))
    }
}



