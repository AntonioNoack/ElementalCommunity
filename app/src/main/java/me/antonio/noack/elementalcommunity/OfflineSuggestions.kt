package me.antonio.noack.elementalcommunity

import android.content.SharedPreferences
import me.antonio.noack.elementalcommunity.AllManager.Companion.checkIsUIThread
import me.antonio.noack.elementalcommunity.api.WebServices.tryCaptchaLarge
import me.antonio.noack.elementalcommunity.utils.Compact.compacted
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.min

object OfflineSuggestions {

    data class Ingredients(val compA: Int, val compB: Int)
    data class Recipe(val element: Element, val group: Int)

    fun hasRecipes() = offlineRecipes.isNotEmpty()

    private val offlineElements = ArrayList<Element>()
    private val offlineRecipes = HashMap<Ingredients, Recipe>()

    fun getOfflineRecipe(compA: Element, compB: Element): Element? {
        val ing = Ingredients(compA.uuid, compB.uuid)
        val rec = offlineRecipes[ing] ?: return null
        return rec.element
    }

    fun addOfflineRecipe(
        all: AllManager,
        compA: Element,
        compB: Element,
        name: String,
        group: Int
    ): Element {
        val newElement = createOfflineElement(name, group)
        // add recipe to used-recipe-database
        AllManager.addRecipe(compA, compB, newElement, all, true)
        val ing = Ingredients(compA.uuid, compB.uuid)
        offlineRecipes[ing] = Recipe(newElement, group)
        return newElement
    }

    private fun createOfflineElement(name: String, group: Int): Element {
        checkIsUIThread()
        val compactName = compacted(name)
        val oldValue = offlineElements.firstOrNull { compacted(it.name) == compactName }
        return if (oldValue != null) oldValue
        else {
            val newUUID = (offlineElements.minByOrNull { it.uuid }?.uuid ?: 0) - 1
            // println("Creating new offline element: $newUUID, $name/$compactName")
            val newElement = Element(name, newUUID, group, 0)
            offlineElements.add(newElement)
            newElement
        }
    }

    fun loadOfflineElements(all: AllManager, pref: SharedPreferences) {
        val offlineElements = HashMap<Int, Element>()
        val offlineElements1 = pref.getStringSet("offlineElements", HashSet()) ?: emptySet()
        for (e in offlineElements1) {
            val cai = e.indexOf(';', 1)
            if (cai < 0) continue
            val gri = e.indexOf(';', cai + 2)
            if (gri < 0 || gri + 2 >= e.length) continue
            val id = e.substring(0, cai).toIntOrNull() ?: continue
            val group = e.substring(cai + 1, gri).toIntOrNull() ?: continue
            val name = e.substring(gri + 1)
            offlineElements[id] = createOfflineElement(name, group)
        }
        val offlineRecipes1 = pref.getStringSet("offlineRecipes", HashSet()) ?: emptySet()
        for (e in offlineRecipes1) {
            val cai = e.indexOf(';', 1)
            if (cai < 0) continue
            val cbi = e.indexOf(';', cai + 2)
            if (cbi < 0) continue
            val gri = e.indexOf(';', cbi + 2)
            if (gri < 0 || gri + 2 >= e.length) continue
            val compAi0 = e.substring(0, cai).toIntOrNull() ?: continue
            val compBi0 = e.substring(cai + 1, cbi).toIntOrNull() ?: continue
            val group = e.substring(cbi + 1, gri).toIntOrNull() ?: continue
            val name = e.substring(gri + 1)
            val compAi = min(compAi0, compBi0)
            val compBi = max(compAi0, compBi0)
            val compA = AllManager.elementById[compAi] ?: offlineElements[compAi] ?: continue
            val compB = AllManager.elementById[compBi] ?: offlineElements[compBi] ?: continue
            addOfflineRecipe(all, compA, compB, name, group)
        }
    }

    private fun storeOfflineElements(editor: SharedPreferences.Editor) {
        val usedElements = HashSet<Element>()
        for ((_, result) in offlineRecipes) {
            usedElements.add(result.element)
        }
        editor
            // only keep offlineElements, that are actually used
            .putStringSet("offlineElements", offlineElements
                .filter { it in usedElements }
                .map { e ->
                    "${e.uuid};${e.group};${e.name}"
                }.toSet()
            )
            .putStringSet("offlineRecipes", offlineRecipes.entries.map { (k, v) ->
                "${k.compA};${k.compB};${v.group};${v.element.name}"
            }.toSet())
    }

    fun storeOfflineElements() {
        val edit = AllManager.edit.invoke()
        storeOfflineElements(edit)
        edit.apply()
    }

    fun uploadValues(all: AllManager, onSuccess: (Int, Int) -> Unit, onError: (Exception) -> Unit) {
        // try to upload everything
        val maxNumRecipes = 250 - 2 // -2 for potentially required extra ingredient data
        val offlineIdToElement = offlineElements.associateBy { it.uuid }
        val entries = ArrayList(offlineRecipes.entries)
        entries.removeAll { (k, _) ->
            ((k.compA <= 0 && k.compA !in offlineIdToElement) ||
                    (k.compB <= 0 && k.compB !in offlineIdToElement))
        }
        entries.sortBy { (k, _) -> // prefer recipes with existing components
            (if (k.compA < 0) 1 else 0) + (if (k.compB < 0) 1 else 0)
        }
        if (entries.isEmpty()) {
            onSuccess(0, 0)
        } else {

            while (entries.size > maxNumRecipes) entries.removeLast()

            // all ingredients need to be included in the query!
            val knownElements = HashMap<Int, Element>()
            for (i in 0 until min(maxNumRecipes, entries.size)) {

                val (ingredients, result) = entries[i]
                fun register(comp: Int) {
                    val compI = offlineIdToElement[comp]!!
                    // best solution: find recipe, which has that solution and is available
                    val bestSolutionIndex = entries.indexOfFirst { (k, v) ->
                        v.element == compI &&
                                (k.compA > 0 || k.compA in knownElements) &&
                                (k.compB > 0 || k.compB in knownElements)
                    }
                    if (bestSolutionIndex >= 0) {
                        val bestSolution = entries.removeAt(bestSolutionIndex)
                        entries.add(i, bestSolution)
                    } else {
                        // otherwise, add pseudo recipe
                        entries.add(i, object : MutableMap.MutableEntry<Ingredients, Recipe> {
                            override val key = Ingredients(0, 0)
                            override val value = Recipe(compI, compI.group)
                            override fun setValue(newValue: Recipe): Recipe {
                                throw NotImplementedError()
                            }
                        })
                    }
                    knownElements[comp] = compI
                }

                // new element becomes known
                val id = result.element.uuid
                knownElements[id] = result.element

                if (ingredients.compA < 0 && ingredients.compA !in knownElements) {
                    register(ingredients.compA)
                }
                if (ingredients.compB < 0 && ingredients.compB !in knownElements) {
                    register(ingredients.compB)
                }
            }

            while (entries.size > maxNumRecipes) entries.removeLast()

            val usedByRecipes = HashSet<Int>(entries.size * 2)
            for ((ingredients, _) in entries) {
                if (ingredients.compA < 0) usedByRecipes.add(ingredients.compA)
                if (ingredients.compB < 0) usedByRecipes.add(ingredients.compB)
            }

            val rin = entries.joinToString(";") { (_, v) ->
                URLEncoder.encode(v.element.name, "UTF-8")
            }
            val ri = entries.joinToString(";") { (_, v) ->
                val id = v.element.uuid
                if (id > 0 || v.element.uuid !in usedByRecipes) "" else "${-id}"
            }
            val gi = entries.joinToString(";") { (_, v) -> v.group.toString() }
            val bi = entries.joinToString(";") { (k, _) -> k.compB.toString() }
            val ai = entries.joinToString(";") { (k, _) -> k.compA.toString() }
            val data = "ai=$ai&bi=$bi&gi=$gi&ri=$ri&rin=$rin"
            tryCaptchaLarge(
                all, data, "u=${AllManager.customUUID}", { response ->
                    var numChanges = 0
                    val lines = response.split('\n')
                    for (li in lines.indices) {
                        val line = lines[li]
                        if (!line.startsWith('#') && line.length >= 12) {
                            val parts = line.split(':')
                            if (parts.size >= 6) {
                                val a = parts[0].toIntOrNull() ?: continue
                                val b = parts[1].toIntOrNull() ?: continue
                                val r = parts[2].toIntOrNull() ?: continue
                                val g = parts[3].toIntOrNull() ?: continue
                                val c = parts[4].toIntOrNull() ?: continue
                                val n = parts[5]
                                val re = Element.get(n, r, g, c, true)
                                val entry = entries[li]
                                offlineElements.remove(entry.value.element) // O(n²) :/
                                offlineRecipes.remove(entry.key)
                                // add recipe to used recipe database
                                if (a != 0 && b != 0) {
                                    val ra = AllManager.elementById[a] ?: offlineIdToElement[a]
                                    val rb = AllManager.elementById[b] ?: offlineIdToElement[b]
                                    if (ra != null && rb != null)
                                        AllManager.addRecipe(ra, rb, re, all, true)
                                }
                                numChanges++
                            }
                        }
                    }
                    if (numChanges > 0) {
                        storeOfflineElements()
                    }
                    onSuccess(numChanges, offlineRecipes.size)
                }, onError
            )
        }
    }

}