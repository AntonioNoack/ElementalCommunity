package me.antonio.noack.elementalcommunity.io

import android.content.SharedPreferences
import java.lang.Exception

object Loader {

    fun load(ctx: String, preferences: SharedPreferences, overrideEverything: Boolean){

        val edit = preferences.edit()
        if(overrideEverything) edit.clear()

        val entries = ctx.split('\n')
        for(entry in entries){

            if(entry.isBlank()) break

            try {

                // {NameLength};{Name}{ValueType}{Value}
                val commaIndex = entry.indexOf(';')
                if(commaIndex < 0) continue
                val keyNameLength = entry.substring(0, commaIndex).toInt()
                val keyName = unescape(entry.substring(commaIndex+1, commaIndex+1+keyNameLength))
                val valueType = entry[commaIndex+1+keyNameLength]
                val rawValue = entry.substring(commaIndex+1+keyNameLength+1)

                loadObject(rawValue, valueType, keyName, edit)

            } catch (e: Exception){
                e.printStackTrace()
            }

        }

        edit.apply()


    }

    fun loadObject(rawValue: String, type: Char, key: String, edit: SharedPreferences.Editor){

        when(type){
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