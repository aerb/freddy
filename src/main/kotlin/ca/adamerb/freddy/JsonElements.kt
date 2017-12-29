package ca.adamerb.freddy

interface JsonElement
object JsonNull: JsonElement
data class JsonString(val value: String): JsonElement
data class JsonObject(
    private val map: LinkedHashMap<String, JsonElement>
) : JsonElement, Map<String, JsonElement> by map
data class JsonArray(
    private val list: List<JsonElement>
) : JsonElement, List<JsonElement> by list
fun JsonElement?.hasValue(): Boolean = this != null && this !is JsonNull
fun JsonElement?.hasNoValue(): Boolean = this == null || this is JsonNull
fun JsonElement?.asString(): String = (this as JsonString).value
fun JsonElement?.asObject(): Map<String, JsonElement> = this as JsonObject
operator fun JsonElement?.get(key: String): JsonElement? =
    if(this == null || this is JsonNull) null
    else (this as JsonObject)[key]