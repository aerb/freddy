package ca.adamerb.freddy

fun Char.check(char: Char) {
    kotlin.check(char == this) { "Expected '$char'. Got '${this}'." }
}