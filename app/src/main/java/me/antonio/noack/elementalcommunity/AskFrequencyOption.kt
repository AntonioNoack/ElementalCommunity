package me.antonio.noack.elementalcommunity

import android.content.SharedPreferences
import java.util.*

enum class AskFrequencyOption(val displayName: String, val chance: Float, val uuid: Int) {
    ALWAYS("always", 1f, 0),
    SOMETIMES("sometimes", 0.3f, 10),
    RARELY("rarely", 0.03f, 20),
    NEVER("never", 0f, 30);

    fun store(pref: SharedPreferences) = pref.edit().putInt("askFrequency", this.uuid).apply()

    fun isTrue() = Random().nextFloat() < chance

    companion object {
        operator fun get(index: Int) = when (index) {
            SOMETIMES.uuid -> SOMETIMES
            RARELY.uuid -> RARELY
            NEVER.uuid -> NEVER
            else -> ALWAYS
        }

        operator fun get(pref: SharedPreferences) = get(pref.getInt("askFrequency", ALWAYS.uuid))
    }
}