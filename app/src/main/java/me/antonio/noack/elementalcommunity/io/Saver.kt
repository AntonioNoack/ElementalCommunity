package me.antonio.noack.elementalcommunity.io

import android.content.SharedPreferences
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder

object Saver {

    fun save(preferences: SharedPreferences, ignore: (String) -> Boolean): String {

        val all = preferences.all
        val ret = StringBuilder(all.size * 16)

        entries@ for ((key, value) in all) {

            if (!ignore(key)) {

                if (value == null) continue

                try {

                    val escapedName = escape(key)
                    ret.append(escapedName.length)
                    ret.append(';')
                    ret.append(escapedName)
                    when (value) {
                        is Int -> {
                            ret.append('i')
                            ret.append(value.toString())
                        }
                        is Long -> {
                            ret.append('l')
                            ret.append(value.toString())
                        }
                        is Float -> {
                            ret.append('f')
                            ret.append(value.toString())
                        }
                        is String -> {
                            ret.append('s')
                            ret.append(escape(value))
                        }
                        else -> throw RuntimeException("Unknown type of ${value.javaClass.simpleName}")
                    }
                    ret.append('\n')

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }

        return ret.toString()

    }

    fun escape(string: String) = string.replace("\n", "\\n")

}