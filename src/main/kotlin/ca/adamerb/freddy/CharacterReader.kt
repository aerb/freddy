@file:Suppress("NOTHING_TO_INLINE")
package ca.adamerb.freddy
import java.io.Reader


inline fun Int.isWhiteSpace(): Boolean = Character.isWhitespace(this)

class CharacterReader(private val reader: Reader) {

    private val consumed = ArrayList<Int>()

    private fun readNextPoint(skipWs: Boolean): Int {
        if(!skipWs) {
            return reader.read()
        } else {
            while (true) {
                val next = reader.read()
                if(next.isWhiteSpace()) continue
                else return next
            }
        }
    }

    fun read(index: Int = 0, skipWs: Boolean = true): Int {
        while(index >= consumed.size) {
            consumed += readNextPoint(skipWs = skipWs)
        }
        return consumed[index]
    }

    fun advance() {
        consumed.removeAt(0)
    }

    fun advanceAll() {
        consumed.clear()
    }
}



