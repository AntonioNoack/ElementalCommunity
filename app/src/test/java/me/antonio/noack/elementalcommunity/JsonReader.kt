package me.antonio.noack.elementalcommunity

import java.io.EOFException
import java.io.InputStream

class JsonReader(val data: InputStream) {

    constructor(data: ByteArray) : this(data.inputStream())
    constructor(data: String) : this(data.toByteArray())

    var index = 0
    var tmpChar = 0.toChar()

    fun close() {
        data.close()
    }

    fun next(): Char {
        if (tmpChar != 0.toChar()) {
            val v = tmpChar
            tmpChar = 0.toChar()
            return v
        }
        val next = data.read()
        if (next < 0) throw EOFException()
        return next.toChar()
    }

    fun skipSpace(): Char {
        return when (val next = next()) {
            '\r', '\n', '\t', ' ' -> skipSpace()
            else -> next
        }
    }

    fun putBack(char: Char) {
        tmpChar = char
    }

    fun readString(): String {
        val str = StringBuilder()
        while (true) {
            when (val next0 = next()) {
                '\\' -> {
                    when (val next1 = next()) {
                        '\\' -> str.append('\\')
                        'r' -> str.append('\r')
                        'n' -> str.append('\n')
                        't' -> str.append('\t')
                        '"' -> str.append('"')
                        '\'' -> str.append('\'')
                        'f' -> str.append(12.toChar())
                        'b' -> str.append('\b')
                        'u' -> {
                            val a = next()
                            val b = next()
                            val c = next()
                            val d = next()
                            if (!isHex(a) || !isHex(b) || !isHex(c) || !isHex(d))
                                throw RuntimeException("Expected 4 hex characters after \\u")
                            str.append((toHex(a, b).shl(8) + toHex(c, d)).toChar())
                        }
                        else -> throw RuntimeException("Unknown escape sequence \\$next1")
                    }
                }
                '"' -> return str.toString()
                else -> {
                    str.append(next0)
                }
            }
        }
    }

    private fun isHex(c: Char): Boolean {
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    private fun toHex(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c.code - 48
            in 'a'..'f' -> c.code - 'a'.code + 10
            else -> c.code - 'A'.code + 10
        }
    }

    private fun toHex(c: Char, d: Char): Int {
        return toHex(c) * 4 + toHex(d)
    }

    fun skipString() {
        while (true) {
            when (next()) {
                '\\' -> {
                    when (val next1 = next()) {
                        '\\', 'r', 'n', 't', '"', '\'', 'f', 'b' -> {}
                        'u' -> {
                            assert(isHex(next()), "expected hex")
                            assert(isHex(next()), "expected hex")
                            assert(isHex(next()), "expected hex")
                            assert(isHex(next()), "expected hex")
                        }
                        else -> throw RuntimeException("Unknown escape sequence \\$next1")
                    }
                }
                '"' -> return
                else -> {}
            }
        }
    }

    fun readNumber(): String {
        var str = ""
        var isFirst = true
        while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                in '0'..'9', '+', '-', '.', 'e', 'E' -> {
                    str += next
                }
                '_' -> {}
                '"' -> {
                    if (str.isEmpty()) return readString()
                    else throw RuntimeException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next
                    return str
                }
            }
            isFirst = false
        }
    }

    fun skipNumber(readOpeningChar: Boolean = true) {
        var isEmpty = readOpeningChar
        var isFirst = readOpeningChar
        while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                in '0'..'9', '+', '-', '.', 'e', 'E' -> isEmpty = false
                '_' -> {}
                '"' -> {
                    if (isEmpty) {
                        skipString()
                        return
                    } else throw RuntimeException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next
                    return
                }
            }
            isFirst = false
        }
    }

    fun readObject(
        readOpeningBracket: Boolean = true,
        filter: ((String) -> Boolean)? = null
    ): JsonObject {
        if (readOpeningBracket) assert(skipSpace(), '{')
        var next = skipSpace()
        val obj = JsonObject()
        while (true) {
            when (next) {
                '}' -> return obj
                '"' -> {
                    val name = readString()
                    assert(skipSpace(), ':')
                    if (filter == null || filter(name)) {
                        obj[name] = readSomething(skipSpace(), filter)
                    } else skipSomething(skipSpace())
                    next = skipSpace()
                }
                ',' -> {
                    next = skipSpace()
                }
                else -> assert(next, '}', '"')
            }
        }
    }

    fun skipObject(readOpeningBracket: Boolean = true) {
        if (readOpeningBracket) assert(skipSpace(), '{')
        var next = skipSpace()
        while (true) {
            when (next) {
                '}' -> return
                '"' -> {
                    skipString() // name
                    assert(skipSpace(), ':')
                    skipSomething(skipSpace())
                    next = skipSpace()
                }
                ',' -> next = skipSpace()
                else -> assert(next, '}', '"')
            }
        }
    }

    fun readSomething(next: Char, filter: ((String) -> Boolean)? = null): Any? {
        return when (next) {
            in '0'..'9', '.', '+', '-' -> {
                putBack(next)
                readNumber()
            }
            '"' -> readString()
            '[' -> readArray(false)
            '{' -> readObject(false, filter)
            't', 'T' -> {
                assert(next(), 'r', 'R')
                assert(next(), 'u', 'U')
                assert(next(), 'e', 'E')
                true
            }
            'f', 'F' -> {
                assert(next(), 'a', 'A')
                assert(next(), 'l', 'L')
                assert(next(), 's', 'S')
                assert(next(), 'e', 'E')
                false
            }
            'n', 'N' -> {
                assert(next(), 'u', 'U')
                assert(next(), 'l', 'L')
                assert(next(), 'l', 'L')
                null
            }
            else -> throw RuntimeException("Expected value, got $next")
        }
    }

    fun skipSomething(next: Char) {
        when (next) {
            in '0'..'9', '.', '+', '-' -> skipNumber(false)
            '"' -> skipString()
            '[' -> skipArray(false)
            '{' -> skipObject(false)
            // should we throw errors while skipping?
            't', 'T' -> {
                assert(next(), 'r', 'R')
                assert(next(), 'u', 'U')
                assert(next(), 'e', 'E')
            }
            'f', 'F' -> {
                assert(next(), 'a', 'A')
                assert(next(), 'l', 'L')
                assert(next(), 's', 'S')
                assert(next(), 'e', 'E')
            }
            'n', 'N' -> {
                assert(next(), 'u', 'U')
                assert(next(), 'l', 'L')
                assert(next(), 'l', 'L')
            }
            else -> throw RuntimeException("Expected value, got $next")
        }
    }

    fun readArray(readOpeningBracket: Boolean = true): JsonArray {
        if (readOpeningBracket) assert(skipSpace(), '[')
        var next = skipSpace()
        val obj = JsonArray()
        while (true) {
            when (next) {
                ']' -> return obj
                ',' -> {}
                else -> obj.add(readSomething(next))
            }
            next = skipSpace()
        }
    }

    fun skipArray(readOpeningBracket: Boolean = true) {
        if (readOpeningBracket) assert(skipSpace(), '[')
        var next = skipSpace()
        while (true) {
            when (next) {
                ']' -> return
                ',' -> {}
                else -> skipSomething(next)
            }
            next = skipSpace()
        }
    }

    // Java/Kotlin's defaults assert only works with arguments
    // we want ours to always work
    // we can't really put it elsewhere without prefix, because Kotlin will use the wrong import...
    fun assert(i: Char, c1: Char, c2: Char) {
        if (i != c1 && i != c2) throw RuntimeException("Expected $c1 or $c2, but got $i")
    }

    fun assert(i: Char, c: Char) {
        if (i != c) throw RuntimeException("Expected $c, but got $i")
    }

    fun assert(b: Boolean, msg: String) {
        if (!b) throw RuntimeException(msg)
    }

}