package ca.adamerb.freddy
import java.io.EOFException


fun readNumber(reader: CharacterReader): String {
    val sb = StringBuilder()
    while (true) {
        val point = reader.read(skipWs = false).toChar()
        when {
            point in '0'..'9' -> sb.append(point)
            point == ',' || point.isWhitespace() -> {
                return sb.toString()
            }
            else -> throw IllegalStateException("point $point")
        }
    }
}

fun Int.throwIfEof(): Int {
    if(this == -1) throw EOFException()
    return this
}

fun Int.check(char: Char) {
    throwIfEof()
    if(char != toChar()) throw IllegalStateException("Expected '$char'. Got '${toChar()}'.")
}

fun Char.check(char: Char) {
    if(char != toChar()) throw IllegalStateException("Expected $char. Got ${toChar()}")
}