package me.antonio.noack.elementalcommunity.io

import android.content.SharedPreferences
import java.lang.Exception
import java.lang.RuntimeException

object Saver {

    fun save(preferences: SharedPreferences, ignore: (String) -> Boolean): String {

        var ret = ""

        entries@ for((key, value) in preferences.all){

            if(!ignore(key)){

                try {

                    val escapedName = escape(key)
                    var line = "${escapedName.length};$escapedName"

                    line += when(value){
                        null -> continue@entries
                        is Int -> "i$value"
                        is Long -> "l$value"
                        is Float -> "f$value"
                        is String -> "s${escape(value)}"
                        else -> throw RuntimeException("Unknown type of ${value.javaClass.simpleName}")
                    }

                    ret += line + "\n"

                } catch (e: Exception){
                    e.printStackTrace()
                }

            }

        }

        return ret

    }

    fun escape(string: String) = string.replace("\n", "\\n")

}