package me.antonio.noack.elementalcommunity.io

class SplitReader2 {

    var input = ""
        set(value) {
            field = value
            index = 0
            hasReachedEndOfBlock = false
        }

    private var index = 0

    private val builder = StringBuilder()

    val hasRemaining get() = index < input.length

    var hasReachedEndOfBlock = false
        private set

    fun next() {
        hasReachedEndOfBlock = !hasRemaining
    }

    private fun read(): Int {
        return if (index < input.length) input[index++].code
        else -1
    }

    fun readInt(primary: Char, secondary: Char, default: Int): Int {
        if (hasReachedEndOfBlock) return default
        val pri = primary.code
        val sec = secondary.code
        var i = 0
        while (true) {
            when (val char = read()) {
                sec -> return i
                pri, -1 -> {
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
            when (val char = read()) {
                sec -> return builder.toString()
                pri, -1 -> {
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
            when (read()) {
                pri, -1 -> {
                    hasReachedEndOfBlock = true
                    return default
                }
            }
        }
    }

}