package ca.adamerb.freddy

fun readObject(reader: JsonTokenReader): JsonObject {
    val map = LinkedHashMap<String, JsonElement>()
    reader.startObject()
    objectLoop@ while (true) {
        when (reader.peakNext()) {
             SymbolType.FieldName -> {
                 val field = reader.readFieldName()
                 reader.startValue()
                 map[field] = when(reader.peakNext()) {
                     SymbolType.StartObject -> readObject(reader)
                     SymbolType.TextValue -> JsonString(reader.readString())
                     else -> throw IllegalStateException("Unexpected symbol ${reader.peakNext()}")
                 }
                 reader.endValue()
             }
            SymbolType.EndObject -> {
                break@objectLoop
            }
            else -> throw IllegalStateException("Unexpected symbol ${reader.peakNext()}")
        }
    }
    reader.endObject()
    return JsonObject(map)
}