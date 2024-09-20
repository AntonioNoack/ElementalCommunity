package me.antonio.noack.elementalcommunity.cache

import android.content.SharedPreferences
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.GroupsEtc
import me.antonio.noack.elementalcommunity.OfflineSuggestions
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.io.ElementType
import me.antonio.noack.elementalcommunity.io.SplitReader
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object CombinationCache {

    // todo show # of recipes of+from a recipe?
    // todo search for mandala view

    // load split into 21 pieces for network efficiency
    private val hiddenRecipes = Array(GroupsEtc.GroupColors.size + 12) { CacheGroup() }

    fun init(pref: SharedPreferences) {
        thread(name = "CombinationCache.init") {
            for (i in hiddenRecipes.indices) {
                val str = pref.getString("cache.$i", null)
                if (str != null) loadMultipleFromString(str)
                hiddenRecipes[i].validationDate =
                    pref.getLong("cache.$i.date", hiddenRecipes[i].validationDate)
            }
        }
    }

    private fun loadMultipleFromString(str: String) {
        val reader = SplitReader(
            listOf(ElementType.INT, ElementType.INT, ElementType.INT),
            ';', ',', str
        )
        while (reader.hasRemaining) {
            if (reader.read() >= 3) {
                var a = AllManager.elementById[reader.getInt(0)]
                var b = AllManager.elementById[reader.getInt(1)]
                val r = AllManager.elementById[reader.getInt(2)]
                if (a != null && b != null && r != null) {
                    if (a.uuid > b.uuid) {
                        val t = a
                        a = b
                        b = t
                    }
                    hiddenRecipes[a.group][a, b] = r
                }
            }
        }
    }

    fun updateInWealth(a: Element, b: Element) {
        if (a.uuid > b.uuid) return updateInWealth(b, a)
        hiddenRecipes[a.group].updateInWealth(a)
    }

    fun save(edit: SharedPreferences.Editor) {
        for (i in hiddenRecipes.indices) {
            saveGroup(edit, i)
        }
    }

    fun saveGroup(edit: SharedPreferences.Editor, i: Int) {
        val group = hiddenRecipes.getOrNull(i) ?: return
        edit.putString("cache.$i", group.save())
        edit.putLong("cache.$i.date", group.validationDate)
    }

    fun invalidate(edit: SharedPreferences.Editor) {
        for (i in hiddenRecipes.indices) {
            val group = hiddenRecipes[i]
            group.invalidate()
            edit.putLong("cache.$i.date", group.validationDate)
        }
    }

    fun askRegularly(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit) {
        if (a.uuid > b.uuid) {
            askRegularly(all, b, a, callback)
        } else {
            hiddenRecipes[a.group].askRegularly(all, a, b, callback)
        }
    }

    fun askInEmergency(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit) {
        if (a.uuid > b.uuid) {
            askInEmergency(all, b, a, callback)
        } else {
            hiddenRecipes[a.group].askInEmergency(a, b, callback)
        }
    }

    class CacheGroup {

        var validationDate = 0L
        private val data = HashMap<Long, Element>()

        fun key(a: Element, b: Element): Long {
            return min(a.uuid, b.uuid).toLong().shl(32) + max(a.uuid, b.uuid)
        }

        operator fun set(a: Element, b: Element, r: Element) {
            if (a.name == "Earth" && b.name == "Air") {
                println("CombinationCache[$a,$b] = $r")
            }
            synchronized(data) {
                data[key(a, b)] = r
            }
        }

        private fun isValid(date: Long): Boolean =
            data.isNotEmpty() && abs(date - validationDate) < 3_600_000 // 1h

        fun askRegularly(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit) {
            val date = System.currentTimeMillis()
            if (isValid(date)) {
                println("cache is still valid")
                // valid -> return
                askDirectly(a, b, callback)
            } else {
                println("cache has become invalid")
                // invalid -> ask server
                WebServices.askAllRecipesOfGroup(a.group, {
                    validationDate = date
                    loadMultipleFromString(it)
                    askDirectly(a, b, callback)
                    val edit = all.pref.edit()
                    saveGroup(edit, a.group)
                    edit.apply()
                }, {
                    askInEmergency(a, b, callback)
                    it.printStackTrace()
                })
            }
        }

        /**
         * doesn't care about validation
         * */
        fun askInEmergency(a: Element, b: Element, callback: (Element?) -> Unit) {
            println("asking cache in emergency")
            askDirectly(a, b, callback)
        }

        private fun askDirectly(a: Element, b: Element, callback: (Element?) -> Unit) {
            if (a.uuid > b.uuid) return askDirectly(b, a, callback)
            val suggestion = OfflineSuggestions.getOfflineRecipe(a, b)
            println(
                "asking cache directly, " +
                        "$suggestion, " +
                        "${synchronized(data) { data[key(a, b)] }}, " +
                        "${AllManager.getRecipe(a, b)}"
            )
            val recipe = suggestion
                ?: synchronized(data) { data[key(a, b)] }
                ?: AllManager.getRecipe(a, b)
            callback(recipe)
        }

        fun save(): String {
            val builder = StringBuilder()
            synchronized(data) {
                for ((ab, r) in data) {
                    if (builder.isNotEmpty()) builder.append(';')
                    val a = ab.ushr(32).toInt()
                    val b = ab.toInt()
                    builder.append(a).append(',').append(b).append(',').append(r)
                }
            }
            return builder.toString()
        }

        fun invalidate() {
            validationDate = 0L
        }

        fun updateInWealth(a: Element) {
            val date = System.nanoTime()
            if (!isValid(date)) {
                val oldDate = validationDate
                validationDate = date // optimistic
                WebServices.askAllRecipesOfGroup(a.group, {
                    loadMultipleFromString(it)
                    val edit = AllManager.edit()
                    saveGroup(edit, a.group)
                    edit.apply()
                }, {
                    validationDate = oldDate // failure
                    it.printStackTrace()
                })
            }// else already valid -> nothing to do
        }
    }

}