package me.antonio.noack.elementalcommunity.graph

import android.content.Context
import android.util.AttributeSet
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element

class GraphView(ctx: Context, attrs: AttributeSet) :
    NodeGraphView(ctx, attrs, alwaysValid = true, iterativeTree = true) {

    private val grid = Grid(1, 1)

    override fun buildTreeAsync() {
        Clustering.cluster(getElements(), ::getRecipes)
    }

    override fun buildTreeIteratively() {
        Clustering.clusterIteratively(getElements(), grid, ::getRecipes)
    }

    private fun getElements(): List<Element> {
        val unlocked = AllManager.unlockedElements
        val elements = ArrayList<Element>(unlocked.sumOf { it.size })
        forAllElements { element -> elements.add(element) }
        return elements
    }

    private fun getRecipes(element: Element): Sequence<Element> {
        return sequence {
            val recipes = AllManager.recipesByElement[element]
            if (recipes != null) {
                for (recipe in recipes) {
                    yield(recipe.first)
                    yield(recipe.second)
                }
            }
        }
    }

}