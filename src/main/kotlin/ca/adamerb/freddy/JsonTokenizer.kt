package ca.adamerb.freddy

import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.Reader
import java.math.BigDecimal
import java.util.*

enum class ParseContext {
    TopLevel,
    ExpectingValue,
    Object,
    Array
}

enum class SymbolType {
    StartObject,
    EndObject,
    StartArray,
    EndArray,
    FieldName,
    TextValue,
    NumberValue,
    BoolValue,
    NullValue
}

private inline fun Char.check(char: Char) {
    kotlin.check(char == this) { "Expected '$char'. Got '${this}'." }
}

private const val DEFAULT_BUFFER_SIZE = 1024

class JsonTokenizer(
    private val reader: Reader,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {
    constructor(json: String, bufferSize: Int = DEFAULT_BUFFER_SIZE) :
        this(ByteArrayInputStream(json.toByteArray()).reader(), bufferSize)

    private val buffer = CharArray(bufferSize)
    private var bI: Int = bufferSize

    private var valueBuffer = CharArray(bufferSize)
    private var vI: Int = -1

    private var context = ParseContext.TopLevel
    private val stack = Stack<ParseContext>()

    private fun pushContext(next: ParseContext) {
        stack.push(context)
        context = next
    }

    private fun popContext() {
        context = stack.pop()
    }

    private fun fillBuffer(throwOnEof: Boolean = true) {
        if (bI >= buffer.size) {
            bI = 0
            if(reader.read(buffer) == -1 && throwOnEof) {
                throw EOFException("Unexpected EOF in context $context")
            }
        }
    }

    private fun nextNonWs() {
        while (true) {
            fillBuffer()
            if (buffer[bI].isWhitespace()) {
                bI++
            } else {
                return
            }
        }
    }

    fun startObject() {
        nextNonWs()
        buffer[bI++].check('{')
        pushContext(ParseContext.Object)
    }

    fun startArray() {
        nextNonWs()
        buffer[bI++].check('[')
        pushContext(ParseContext.Array)
    }

    fun endArray() {
        nextNonWs()
        buffer[bI++].check(']')
        popContext()
    }

    fun endObject() {
        nextNonWs()
        buffer[bI++].check('}')
        popContext()
    }

    private fun escapeCharacter(point: Char): Char {
        return when(point) {
            '"' -> '"'
            '\\' -> '\\'
            'n' -> '\n'
            't' -> '\t'
            else -> throw IllegalStateException("Unknown escape type \\$point.")
        }
    }

    private fun appendToValueBuffer(char: Char) {
        vI ++
        if(vI >= valueBuffer.size) {
            valueBuffer = valueBuffer.copyOf(valueBuffer.size + bufferSize)
        }
        valueBuffer[vI] = char
    }

    private val unicodeBuffer = CharArray(4)
    private fun readUnicodePoint(): Char {
        for(i in 0..3) {
            bI ++
            fillBuffer()
            unicodeBuffer[i] = buffer[bI]
        }
        return String(unicodeBuffer).toInt(16).toChar()
    }

    private inline fun readPrimitive(readUntil: (point: Char, escaped: Boolean) -> Boolean): String {
        readPrimitiveLoop@ while (true) {
            fillBuffer()
            val point = buffer[bI]
            val needsEscaped = point == '\\'
            val escaped = if(needsEscaped) {
                bI ++
                fillBuffer()
                when(buffer[bI]) {
                    '"' -> '"'
                    '\\' -> '\\'
                    'n' -> '\n'
                    't' -> '\t'
                    'u' -> readUnicodePoint()
                    else -> throw IllegalStateException("Unknown escape type \\$point.")
                }
            } else point

            if(readUntil(escaped, needsEscaped)) {
                break@readPrimitiveLoop
            } else {
                appendToValueBuffer(escaped)
            }
            bI ++
        }

        val len = vI + 1
        vI = -1
        return String(valueBuffer, 0, len)
    }

//    private fun readComplexNumber(): Number {
//        fun readInt(allowSign: Boolean = true): String {
//            while (true) {
//                fillBuffer()
//                val next = buffer[bI]
//                if(next == '-') {
//                    if(vI == -1 && allowSign) {
//                        appendToValueBuffer(next)
//                        bI ++
//                    }
//                    else throwUnexpectedPoint(next)
//                } else if(next.isDigit()) {
//                    appendToValueBuffer(next)
//                    bI ++
//                } else {
//                    break
//                }
//            }
//            val len = vI + 1
//            vI = -1
//            return String(valueBuffer, 0, len)
//        }
//        val integer = readInt()
//        var next = buffer[bI]
//        val fraction = if(next == '.') { bI++; readInt(allowSign = false) } else null
//        next = buffer[bI]
//        val exp = if(next == 'e' || next == 'E') { bI++; readInt() } else null
//        return JsonNumber(integer, fraction, exp).toString().toBigDecimal()
//    }

    fun readString(): String {
        nextNonWs()
        buffer[bI++].check('"')
        val string = readPrimitive(
            readUntil = { p, escaped -> p == '"' && !escaped }
        )
        buffer[bI++].check('"')
        return string
    }

    fun readNumber(): JsonNumber {
        nextNonWs()
        readNumberChars@ while (true) {
            fillBuffer()
            val point = buffer[bI]
            if(point.isJsonDigit()) {
                appendToValueBuffer(point)
                bI ++
            } else {
                break@readNumberChars
            }
        }
        val len = vI + 1
        vI = -1
        return JsonNumber(BigDecimal(valueBuffer, 0, len))
    }

    fun readBoolean(): String {
        nextNonWs()
        return readPrimitive(
            readUntil = { p, _ -> !p.isLetter() }
        ).toLowerCase().also {
            check(it == "true" || it == "false")
        }
    }

    fun readFieldName(): String {
        return readString()
    }

    private fun throwUnexpectedPoint(point: Char): Nothing {
        throw IllegalStateException("Unexpected char $point in context $context")
    }

    private fun Char.isJsonDigit(): Boolean =
        when(this) {
            '-', '+',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '.', 'e', 'E' -> true
            else -> false
        }

    private fun peakValue(point: Char): SymbolType? {
        return when (point) {
            '"' -> SymbolType.TextValue
            't', 'f' -> SymbolType.BoolValue
            '{' -> SymbolType.StartObject
            '[' -> SymbolType.StartArray
            'n' -> SymbolType.NullValue
            '-', '1', '2', '3', '4', '5', '6', '7', '8', '9' ->
                SymbolType.NumberValue
            '+', '0' -> // these are not valid starting numeric values in json, but lets accept for now.
                SymbolType.NumberValue
            else -> null
        }
    }

    fun peakNext(): SymbolType {
        nextNonWs()

        val point = buffer[bI]
        return when (context) {
            ParseContext.TopLevel -> {
                when (point) {
                    '{' -> SymbolType.StartObject
                    '[' -> SymbolType.StartArray
                    else -> throwUnexpectedPoint(point)
                }
            }
            ParseContext.Object -> {
                when (point) {
                    '"' -> SymbolType.FieldName
                    '}' -> SymbolType.EndObject
                    else -> throwUnexpectedPoint(point)
                }
            }
            ParseContext.Array -> {
                peakValue(point) ?:
                    if(point == ']') SymbolType.EndArray
                    else throwUnexpectedPoint(point)
            }
            ParseContext.ExpectingValue -> {
                peakValue(point) ?: throwUnexpectedPoint(point)
            }
            else -> throw IllegalStateException()
        }
    }

    fun startValue() {
        nextNonWs()
        if(buffer[bI] == ':') bI++
        pushContext(ParseContext.ExpectingValue)
    }

    fun endValue() {
        nextNonWs()
        if (buffer[bI] == ',') bI++
        popContext()
    }
}



