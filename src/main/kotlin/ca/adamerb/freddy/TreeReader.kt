package ca.adamerb.freddy

private fun readJsonValue(reader: JsonTokenReader): JsonElement {
    return when(reader.peakNext()) {
        SymbolType.StartObject -> readJsonObject(reader)
        SymbolType.TextValue -> JsonString(reader.readString())
        else -> throw IllegalStateException("Unexpected symbol ${reader.peakNext()}")
    }
}

private fun readJsonObject(reader: JsonTokenReader): JsonObject {
    val map = LinkedHashMap<String, JsonElement>()
    reader.startObject()
    objectLoop@ while (true) {
        when (reader.peakNext()) {
             SymbolType.FieldName -> {
                 val field = reader.readFieldName()
                 reader.startValue()
                 map[field] = readJsonValue(reader)
                 reader.endValue()
             }
            SymbolType.EndObject -> break@objectLoop
            else -> throw IllegalStateException("Unexpected symbol ${reader.peakNext()}")
        }
    }
    reader.endObject()
    return JsonObject(map)
}

fun readJsonTree(reader: JsonTokenReader): JsonElement {
    val json = readJsonValue(reader)
    check(json is JsonObject || json is JsonArray)
    return json
}