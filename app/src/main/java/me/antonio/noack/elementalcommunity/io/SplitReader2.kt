package me.antonio.noack.elementalcommunity.io

import java.io.InputStream

class SplitReader2(input: InputStream) {

    var input = input
        set(value) {
            field = value
            hasRemaining = true
            hasReachedEndOfBlock = false
        }

    private val builder = StringBuilder()

    var hasRemaining = true
        private set

    var hasReachedEndOfBlock = false
        private set

    fun next() {
        hasReachedEndOfBlock = !hasRemaining
    }

    fun readInt(primary: Char, secondary: Char, default: Int): Int {
        if (hasReachedEndOfBlock) return default
        val pri = primary.code
        val sec = secondary.code
        var i = 0
        while (true) {
            when (val char = input.read()) {
                sec -> return i
                pri, -1 -> {
                    if (char == -1) hasRemaining = false
                    hasReachedEndOfBlock = true
                    return i
                }
                in 48..58 -> i = i * 10 + char - 48
                else -> return readError(pri, default)
            }
        }
    }

    fun readStringNullable(primary: Char, secondary: Char, default: String?): String? {
        if (hasReachedEndOfBlock) return default
        val pri = primary.code
        val sec = secondary.code
        builder.clear()
        while (true) {
            when (val char = input.read()) {
                sec -> return builder.toString()
                pri, -1 -> {
                    if (char == -1) hasRemaining = false
                    hasReachedEndOfBlock = true
                    return builder.toString()
                }
                else -> builder.append(char.toChar())
            }
        }
    }

    fun readString(primary: Char, secondary: Char, default: String): String {
        return readStringNullable(primary, secondary, default)!!
    }

    private fun readError(pri: Int, default: Int): Int {
        while (true) {
            when (val char = input.read()) {
                pri, -1 -> {
                    if (char == -1) hasRemaining = false
                    hasReachedEndOfBlock = true
                    return default
                }
            }
        }
    }

}