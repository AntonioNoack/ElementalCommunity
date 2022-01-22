package me.antonio.noack.elementalcommunity.io

import android.content.SharedPreferences
import java.lang.Exception
import java.util.*

object Loader {

    fun load(ctx: String, preferences: SharedPreferences, overrideEverything: Boolean) {

        val edit = preferences.edit()
        if (overrideEverything) edit.clear()

        val entries = ctx.split('\n')
        for (entry in entries) {

            if (entry.isBlank()) break

            try {

                // {NameLength};{Name}{ValueType}{Value}
                val commaIndex = entry.indexOf(';')
                if (commaIndex < 0) continue
                val keyNameLength = entry.substring(0, commaIndex).toInt()
                val i2 = commaIndex + 1 + keyNameLength
                val keyName = unescape(entry.substring(commaIndex + 1, i2))
                val valueType = entry[commaIndex + 1 + keyNameLength]
                val rawValue = entry.substring(commaIndex + 1 + keyNameLength + 1)

                if (keyName == "unlocked" && !overrideEverything) {

                    val set = TreeSet<Int>()
                    // edit.putString("unlocked", unlockedIds.joinToString(","))

                    val oldEntries = preferences.getString("unlocked", "")!!.split(',')
                    for (id in oldEntries) {
                        val id2 = id.trim()
                        val value = id2.toIntOrNull()
                        if (value != null) {
                            set.add(value)
                        }
                    }

                    val newEntries = unescape(rawValue).split(',')
                    for (id in newEntries) {
                        val id2 = id.trim()
                        val value = id2.toIntOrNull()
                        if (value != null) {
                            set.add(value)
                        }
                    }

                    if (set.isNotEmpty()) {

                        edit.putString("unlocked", set.joinToString(","))

                    }

                } else {
                    loadObject(rawValue, valueType, keyName, edit)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        edit.apply()


    }

    fun loadObject(rawValue: String, type: Char, key: String, edit: SharedPreferences.Editor) {

        when (type) {
            'i' -> {
                edit.putInt(key, rawValue.toInt())
            }
            'l' -> {
                edit.putLong(key, rawValue.toLong())
            }
            'b' -> {
                edit.putBoolean(key, rawValue.toBoolean())
            }
            'f' -> {
                edit.putFloat(key, rawValue.toFloat())
            }
            's' -> {// string
                edit.putString(key, unescape(rawValue))
            }
            'S' -> {// string set
                // not yet implemented
            }
            else -> println("Unknown type $type")
        }
    }

    fun unescape(string: String) = string.replace("\\n", "\n")

}