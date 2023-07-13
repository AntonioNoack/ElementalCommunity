package me.antonio.noack.elementalcommunity

import android.content.SharedPreferences
import java.lang.Exception

class BetterPreferences(private val pref: SharedPreferences): SharedPreferences {

    override fun contains(key: String?): Boolean = pref.contains(key)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        try {
            return pref.getBoolean(key, defValue)
        } catch (_: Exception){}
        if(!pref.contains(key)) return defValue
        try {
            return pref.getString(key, null)?.toBoolean() ?: defValue
        } catch (_: Exception){}
        try {
            return pref.getInt(key, when(defValue){
                true -> 1
                else -> 0
            }) == 1
        } catch (_: Exception){}
        return defValue
    }

    override fun getInt(key: String?, defValue: Int): Int {
        try {
            return pref.getInt(key, defValue)
        } catch (_: Exception){}
        if(!pref.contains(key)) return defValue
        try {
            return pref.getString(key, null)?.toInt() ?: defValue
        } catch (_: Exception){}
        return defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        try {
            return pref.getFloat(key, defValue)
        } catch (_: Exception){}
        if(!pref.contains(key)) return defValue
        try {
            return pref.getString(key, null)?.toFloat() ?: defValue
        } catch (_: Exception){}
        return defValue
    }

    override fun getString(key: String?, defValue: String?): String? {
        try {
            return pref.getString(key, defValue)
        } catch (_: Exception){}
        if(!pref.contains(key)) return defValue
        try {
            return pref.getInt(key, 0).toString()
        } catch (_: Exception){}
        try {
            return pref.getFloat(key, 0f).toString()
        } catch (_: Exception){}
        try {
            return pref.getLong(key, 0L).toString()
        } catch (_: Exception){}
        try {
            return pref.getBoolean(key, false).toString()
        } catch (_: Exception){}
        return defValue
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        pref.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        pref.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?) =
        pref.getStringSet(key, defValues)

    override fun getAll(): MutableMap<String, *> = pref.all

    override fun edit(): SharedPreferences.Editor = pref.edit()

    override fun getLong(key: String?, defValue: Long): Long {
        try {
            return pref.getLong(key, defValue)
        } catch (_: Exception){}
        if(!pref.contains(key)) return defValue
        try {
            return pref.getInt(key, -1).toLong()
        } catch (_: Exception){}
        try {
            return pref.getString(key, null)?.toLong() ?: defValue
        } catch (_: Exception){}
        return defValue
    }


}