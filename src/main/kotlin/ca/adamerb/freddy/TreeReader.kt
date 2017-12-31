package ca.adamerb.freddy

import ca.adamerb.freddy.SymbolType.*

private fun readJsonValue(reader: JsonTokenizer): JsonElement {
    return when(reader.peakNext()) {
        StartObject -> readJsonObject(reader)
        StartArray -> readJsonArray(reader)
        TextValue -> JsonString(reader.readString())
        NumberValue -> reader.readNumber()
        BoolValue -> JsonBool(reader.readBoolean())
        else -> throw IllegalStateException("Unexpected symbol ${reader.peakNext()}")
    }
}

private fun readJsonArray(reader: JsonTokenizer): JsonArray {
    val list = ArrayList<JsonElement>()
    reader.startArray()
    arrayLoop@ while (true) {
        if (reader.peakNext() != EndArray) {
            reader.startValue()
            list += readJsonValue(reader)
            reader.endValue()
        } else {
            break@arrayLoop
        }
    }
    reader.endArray()
    return JsonArray(list)
}

private fun readJsonObject(reader: JsonTokenizer): JsonObject {
    val map = LinkedHashMap<String, JsonElement>()
    reader.startObject()
    objectLoop@ while (true) {
        when (reader.peakNext()) {
             FieldName -> {
                val field = reader.readFieldName()
                reader.startValue()
                map[field] = readJsonValue(reader)
                reader.endValue()
             }
            EndObject -> break@objectLoop
            else -> throw IllegalStateException("Unexpected symbol ${reader.peakNext()}")
        }
    }
    reader.endObject()
    return JsonObject(map)
}

fun readJsonTree(reader: JsonTokenizer): JsonElement {
    val json = readJsonValue(reader)
    check(json is JsonObject || json is JsonArray)
    return json
}