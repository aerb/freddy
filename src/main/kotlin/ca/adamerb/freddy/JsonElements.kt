@file:Suppress("MemberVisibilityCanPrivate")

package ca.adamerb.freddy
import java.math.BigDecimal

interface JsonElement
object JsonNull: JsonElement
data class JsonString(val value: String): JsonElement

data class JsonNumber(
    val decimal: BigDecimal
) : Number(), JsonElement {
    override fun toByte(): Byte = decimal.toByte()
    override fun toChar(): Char = decimal.toChar()
    override fun toDouble(): Double = decimal.toDouble()
    override fun toLong(): Long = decimal.toLong()
    override fun toShort(): Short = decimal.toShort()
    override fun toInt(): Int = decimal.toInt()
    override fun toFloat(): Float = decimal.toFloat()
}

data class JsonBool(val value: String): JsonElement
data class JsonObject(
    private val map: LinkedHashMap<String, JsonElement>
) : JsonElement, Map<String, JsonElement> by map
data class JsonArray(
    private val list: List<JsonElement>
) : JsonElement, List<JsonElement> by list
fun JsonElement?.hasValue(): Boolean = this != null && this !is JsonNull
fun JsonElement?.hasNoValue(): Boolean = this == null || this is JsonNull
fun JsonElement?.asString(): String = (this as JsonString).value
fun JsonElement?.toInt(): Int = (this as JsonNumber).toInt()
fun JsonElement?.asBool(): Boolean = (this as JsonBool).value.toBoolean()
fun JsonElement?.asObject(): Map<String, JsonElement> = this as JsonObject
fun JsonElement?.asList(): List<JsonElement> = this as JsonArray
operator fun JsonElement?.get(key: String): JsonElement? =
    if(this == null || this is JsonNull) null
    else (this as JsonObject)[key]
operator fun JsonElement?.get(index: Int): JsonElement? =
    if(this == null || this is JsonNull) null
    else (this as JsonArray).getOrNull(index)