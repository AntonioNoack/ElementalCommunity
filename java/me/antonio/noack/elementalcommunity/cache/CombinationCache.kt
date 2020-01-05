package me.antonio.noack.elementalcommunity.cache

import android.content.SharedPreferences
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.GroupsEtc
import me.antonio.noack.elementalcommunity.api.WebServices
import java.lang.Math.abs

object CombinationCache {

    val ms = 1000000L

    // todo show # of recipes of+from a recipe?
    // todo search for mandala view

    // load split into 21 pieces for network efficiency
    private val hiddenRecipes = Array(GroupsEtc.GroupColors.size + 12){ CacheGroup() }

    fun init(pref: SharedPreferences){
        for(i in 0 until hiddenRecipes.size){
            (pref.getString("cache.$i", null) ?: null)?.apply {
                loadMultipleFromString()
            }
            hiddenRecipes[i].validationDate = pref.getLong("cache.$i.date", hiddenRecipes[i].validationDate)
        }
    }

    fun String.loadMultipleFromString(){
        println("[CCache] loaded ${this.length}: '$this'")
        for(abr in split(';')){
            if(abr.length >= 5){ // 1,2,3
                val abrElements = abr
                    .split(',')
                    .map { it.toIntOrNull() }
                    .map { AllManager.elementById[it ?: 0] }
                if(abrElements.size > 2){
                    var (a,b,r) = abrElements
                    if(a != null && b != null && r != null){
                        if(a.uuid > b.uuid){
                            val t = a
                            a = b
                            b = t
                        }
                        hiddenRecipes[a.group][a, b] = r
                    }
                }
            }
        }
    }

    fun updateInWealth(a: Element, b: Element){
        if(a.uuid > b.uuid) return updateInWealth(b, a)
        hiddenRecipes[a.group].updateInWealth(a, b)
    }

    fun save(edit: SharedPreferences.Editor){
        for((i, group) in hiddenRecipes.withIndex()){
            edit.putString("cache.$i", group.save())
            edit.putLong("cache.$i.date", group.validationDate)
        }
    }

    fun invalidate(){
        for(list in hiddenRecipes){
            list.invalidate()
        }
    }

    fun askRegularly(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit){
        if(a.uuid > b.uuid){
            askRegularly(all, b, a, callback)
        } else {
            hiddenRecipes[a.group].askRegularly(all, a, b, callback)
        }
    }

    fun askInEmergency(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit){
        if(a.uuid > b.uuid){
            askRegularly(all, b, a, callback)
        } else {
            hiddenRecipes[a.group].askInEmergency(all, a, b, callback)
        }
    }

    class CacheGroup {

        var validationDate = 0L
        val data = HashMap<Pair<Element, Element>, Element>()

        operator fun set(a: Element, b: Element, r: Element){
            data[a to b] = r
        }

        fun isValid(date: Long) = abs(date - validationDate) < 3600 * 1000 * ms // 1h

        fun askRegularly(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit){
            val date = System.currentTimeMillis()
            if(isValid(date)){
                // valid -> return
                askDirectly(all, a, b, callback)
            } else {
                // invalid -> ask server
                WebServices.askAllRecipesOfGroup(a.group, {
                    validationDate = date
                    it.loadMultipleFromString()
                    askDirectly(all, a, b, callback)
                    AllManager.save()
                }, {
                    askInEmergency(all, a, b, callback)
                    it.printStackTrace()
                })
            }
        }

        /**
         * doesn't care about validation
         * */
        fun askInEmergency(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit){
            askDirectly(all, a, b, callback)
        }

        fun askDirectly(all: AllManager, a: Element, b: Element, callback: (Element?) -> Unit){
            callback(data[a to b] ?: AllManager.getRecipe(a, b))
        }

        fun save() = data.entries.joinToString(";"){ (ab, r) ->
            val (a,b) = ab
            "${a.uuid},${b.uuid},${r.uuid}"
        }

        fun invalidate(){
            validationDate = 0L
        }

        fun updateInWealth(a: Element, b: Element){
            val date = System.nanoTime()
            if(!isValid(date)){
                val oldDate = validationDate
                validationDate = date // optimistic
                WebServices.askAllRecipesOfGroup(a.group, {
                    it.loadMultipleFromString()
                    AllManager.save()
                }, {
                    validationDate = oldDate // failure
                    it.printStackTrace()
                })
            }// else already valid -> nothing to do
        }
    }

}