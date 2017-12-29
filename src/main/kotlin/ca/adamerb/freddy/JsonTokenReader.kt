package ca.adamerb.freddy
import java.io.Reader

enum class ParseContext {
    TopLevel,
    ObjectStart,
    ExpectingValue,
    Object,
    Array
}

enum class SymbolType {
    StartObject,
    EndObject,
    StartArray,
    FieldName,
    TextValue,
    NumberValue,
    BoolValue,
    NullValue
}

class JsonTokenReader(reader: Reader) {

    private val reader = CharacterReader(reader)
    private var context = ParseContext.TopLevel
    private val stack = ArrayList<ParseContext>()

    private fun pushContext(next: ParseContext) {
        stack += context
        context = next
    }

    fun popContext() {
        context = stack.removeAt(stack.size - 1)
    }

    fun startObject() {
        reader.read().check('{')
        reader.advance()
        pushContext(ParseContext.ObjectStart)
    }

    fun endObject() {
        reader.read().check('}')
        reader.advance()
        popContext()
    }

    fun readString(): String {
        reader.read().check('"')
        val sb = StringBuilder()
        var i = 1
        readStringLoop@ while (true) {
            val point = reader.read(i++, skipWs = false).throwIfEof().toChar()
            when(point) {
                '"' -> break@readStringLoop
                else -> {
                    sb.append(point)
                }
            }
        }
        reader.advanceAll()
        return sb.toString()
    }

    fun readFieldName(): String {
        if(reader.read().throwIfEof().toChar() == ',') {
            reader.advance()
        }
        return readString()
    }

    private fun throwUnexpectedError(point: Char): Nothing {
        throw IllegalStateException("Unexpected char $point in context $context")
    }

    fun peakNext(): SymbolType {

        val point = reader.read().throwIfEof().toChar()

        return when(context) {
            ParseContext.TopLevel -> {
                when(point) {
                    '{' -> SymbolType.StartObject
                    '[' -> SymbolType.StartArray
                    else -> throwUnexpectedError(point)
                }
            }
            ParseContext.ObjectStart -> {
                when(point) {
                    '"' -> SymbolType.FieldName
                    '}' -> SymbolType.EndObject
                    else -> throwUnexpectedError(point)
                }
            }
            ParseContext.Object -> {
                when(point) {
                    ',' -> SymbolType.FieldName
                    '}' -> SymbolType.EndObject
                    else -> throwUnexpectedError(point)
                }
            }
            ParseContext.ExpectingValue -> {
                when(point) {
                    '"' -> SymbolType.TextValue
                    't','f' -> SymbolType.BoolValue
                    '{' -> SymbolType.StartObject
                    '[' -> SymbolType.StartArray
                    'n' -> SymbolType.NullValue
                    in '0'..'9' -> SymbolType.NumberValue
                    else -> throwUnexpectedError(point)
                }
            }
            else -> throw IllegalStateException()
        }

    }

    fun startValue() {
        reader.read().check(':')
        reader.advance()
        pushContext(ParseContext.ExpectingValue)
    }

    fun endValue() {
        val next = reader.read().throwIfEof().toChar()
        if(next == ',') reader.advance()
        popContext()
    }
}



