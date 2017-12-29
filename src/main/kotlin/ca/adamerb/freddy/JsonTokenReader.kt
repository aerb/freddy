package ca.adamerb.freddy

import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.Reader
import java.util.*

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

private const val DEFAULT_BUFFER_SIZE = 1024

class JsonTokenReader(
    private val reader: Reader,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
) {

    constructor(json: String, bufferSize: Int = DEFAULT_BUFFER_SIZE):
        this(ByteArrayInputStream(json.toByteArray()).reader(), bufferSize)

    private val buffer = CharArray(bufferSize)
    private var bI: Int = bufferSize

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
        pushContext(ParseContext.ObjectStart)
    }

    fun endObject() {
        nextNonWs()
        buffer[bI++].check('}')
        popContext()
    }

    private val stringBuilder = StringBuilder()
    fun readString(): String {
        nextNonWs()
        buffer[bI++].check('"')

        var sI = bI
        readStringLoop@ while (true) {
            if(bI >= buffer.size) {
                val len = bI - sI
                if(len > 0) {
                    stringBuilder.append(buffer, sI, len)
                }
                fillBuffer()
                sI = 0
            }

            val point = buffer[bI++]
            when (point) {
                '"' -> break@readStringLoop
            }
        }
        return if(stringBuilder.isNotEmpty()) {
            val len = bI - sI - 1
            if(len > 0) {
                stringBuilder.append(buffer, sI, len)
            }
            val string = stringBuilder.toString()
            stringBuilder.setLength(0)
            string
        } else {
            val eI = bI - 1
            buffer.copyOfRange(sI, eI).let { String(it) }
        }
    }

    fun readFieldName(): String {
        return readString()
    }

    private fun throwUnexpectedPoint(point: Char): Nothing {
        throw IllegalStateException("Unexpected char $point in context $context")
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
            ParseContext.ObjectStart -> {
                when (point) {
                    '"' -> SymbolType.FieldName
                    '}' -> SymbolType.EndObject
                    else -> throwUnexpectedPoint(point)
                }
            }
            ParseContext.Object -> {
                when (point) {
                    ',' -> SymbolType.FieldName
                    '}' -> SymbolType.EndObject
                    else -> throwUnexpectedPoint(point)
                }
            }
            ParseContext.ExpectingValue -> {
                when (point) {
                    '"' -> SymbolType.TextValue
                    't', 'f' -> SymbolType.BoolValue
                    '{' -> SymbolType.StartObject
                    '[' -> SymbolType.StartArray
                    'n' -> SymbolType.NullValue
                    in '0'..'9' -> SymbolType.NumberValue
                    else -> throwUnexpectedPoint(point)
                }
            }
            else -> throw IllegalStateException()
        }

    }

    fun startValue() {
        nextNonWs()
        buffer[bI++].check(':')
        pushContext(ParseContext.ExpectingValue)
    }

    fun endValue() {
        nextNonWs()
        if (buffer[bI] == ',') bI++
        popContext()
    }
}



