package me.antonio.noack.elementalcommunity.io

class SplitReader(
    val format: List<ElementType>,
    primary: Char,
    secondary: Char,
    input: String
) {

    var input = input
        set(value) {
            field = value
            hasRemaining = true
        }

    private var index = 0
    private fun readChar(): Int {
        return if (index in input.indices) input[index++].toInt() else -1
    }

    private val primary = primary.toInt()
    private val secondary = secondary.toInt()

    private val ints = IntArray(format.lastIndexOf(ElementType.INT) + 1)
    private val strings = Array(format.lastIndexOf(ElementType.STRING) + 1) { "" }

    private val builder = StringBuilder()

    fun getInt(index: Int) = ints[index]
    fun getString(index: Int) = strings[index]

    var hasRemaining = true
        private set

    /**
     * reads values, and returns number of read properties
     * */
    fun read(): Int {
        // read until primary or -1
        parts@ for (formatIndex in format.indices) {
            when (format[formatIndex]) {
                ElementType.INT -> {
                    var i = 0
                    while (true) {
                        when (val char = readChar()) {
                            primary, -1 -> {
                                if (char == -1) hasRemaining = false
                                ints[formatIndex] = i
                                return formatIndex + 1
                            }
                            secondary -> {
                                ints[formatIndex] = i
                                continue@parts
                            }
                            in 48..58 -> {
                                i = i * 10 + char - 48
                            }
                            else -> return readError()
                        }
                    }
                }
                ElementType.STRING -> {
                    builder.clear()
                    while (true) {
                        when (val char = readChar()) {
                            primary, -1 -> {
                                if (char == -1) hasRemaining = false
                                strings[formatIndex] = builder.toString()
                                return formatIndex + 1
                            }
                            secondary -> {
                                strings[formatIndex] = builder.toString()
                                continue@parts
                            }
                            else -> {
                                builder.append(char.toChar())
                            }
                        }
                    }
                }
            }
        }
        while (true) {
            when (val char = readChar()) {
                primary, -1 -> {
                    if (char == -1) hasRemaining = false
                    return format.size
                }
            }
        }
    }

    private fun readError(): Int {
        while (true) {
            when (val char = readChar()) {
                primary, -1 -> {
                    if (char == -1) hasRemaining = false
                    return -1
                }
            }
        }
    }
}