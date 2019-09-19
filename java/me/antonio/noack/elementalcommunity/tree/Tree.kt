package me.antonio.noack.elementalcommunity.tree

import android.util.SparseArray
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

class Tree {

    var multiplierX = 2
    var multiplierY = 2

    var maxElementRadius = 10
    val maxPerRow = maxElementRadius * 2 + 1
    var maxRadiusX = (maxElementRadius * multiplierX).toFloat()
    var top = 0f
    var bottom = 5f

    lateinit var elements: ArrayList<Element>

    val treeMap = SparseArray<Element>()

    fun invalidate() = build()

    fun build(){

        // build the tree of elements
        treeMap.clear()
        var elementCount = 0

        val allRecipes = HashSet<Triple<Element, Element, Element>>()

        for((src, dst) in AllManager.elementByRecipe){
            allRecipes.add(Triple(src.first, src.second, dst))
        }

        elements = ArrayList(max(4, AllManager.unlockeds.sumBy { it.size }))
        val todo = HashSet<Element>(elements.size)

        for(elementRow in AllManager.unlockeds){
            for(element in elementRow){
                elements.add(element)
                element.hasTreeOutput = false
                todo.add(element)
                element.rank = -1
                elementCount++
            }
        }

        for(id in 1 .. 4){
            val element = AllManager.elementById[id]
            element.rank = 0
            todo.remove(element)
        }

        loop@ while(allRecipes.isNotEmpty()){
            for(recipe in allRecipes){
                val (a, b, result) = recipe
                val firstRank = a.rank
                val secondRank = b.rank
                if(firstRank > -1 && secondRank > -1){
                    val newRank = max(firstRank, secondRank)+1
                    if(result.rank < 0 || newRank < result.rank){
                        result.srcA = a
                        result.srcB = b
                        result.rank = max(firstRank, secondRank)+1
                    }
                    allRecipes.remove(recipe)
                    continue@loop
                }
            }
            break
        }

        val maxRank = elements.maxBy { it.rank }?.rank ?: 0

        // this would iterate over all elements, however we want to use all recipes, that are possible to use
        /*loop@ while(allRecipes.isNotEmpty()){
            for((result, recipes) in allRecipes){
                for(recipe in recipes){
                    val firstRank = recipe.first.rank
                    val secondRank = recipe.second.rank
                    if(firstRank > -1 && secondRank > -1){
                        result.srcA = recipe.first
                        result.srcB = recipe.second
                        result.rank = max(firstRank, secondRank)+1
                        maxRank = max(maxRank, result.rank)
                        allRecipes.remove(result)
                        continue@loop
                    }
                }
            }
            break
        }*/

        val byRank = Array(maxRank+2){ ArrayList<Element>() }
        for(element in elements){
            val rank = element.rank
            if(rank > -1){
                element.srcA?.hasTreeOutput = true
                element.srcB?.hasTreeOutput = true
            }
            byRank[if(rank < 0) byRank.lastIndex else rank].add(element)
        }

        for(unsorted in byRank){// primary sorted by group, then by uuid
            unsorted.sortBy { sortScore(it) }
        }

        var positionY = 0
        for(list in byRank){

            // split it into chunks...
            val length = list.size
            val maxFull = length / maxPerRow * maxPerRow
            val rest = length % maxPerRow

            val offset = (maxPerRow - rest) / 2
            val halfOffset = if(rest.and(1) == 0) multiplierX/2 else 0

            for((indexX, element) in list.withIndex()){

                // println("${element.name}: ${element.rank}")

                if(indexX < maxFull){
                    // a full position
                    val rawX = indexX % maxPerRow
                    val positionX = rawX - maxElementRadius

                    element.treeX = positionX * multiplierX
                    element.treeY = positionY * multiplierY

                    if(rawX == maxPerRow-1){
                        positionY++
                    }

                } else {

                    // a offset position

                    val rawX = indexX % maxPerRow
                    val positionX = rawX + offset - maxElementRadius

                    element.treeX = positionX * multiplierX + halfOffset
                    element.treeY = positionY * multiplierY

                }

                treeMap.put(element.treeX.shl(16) or element.treeY.and(0xffff), element)

            }
            positionY++
        }

        bottom = ((positionY - 1) * multiplierY).toFloat()

    }

    fun sortScore(element: Element?): Int {
        if(element == null) return 0
        return  (3 * sortScore(element.srcA) + sortScore(element.srcB)) * 3 + element.uuid
    }

}