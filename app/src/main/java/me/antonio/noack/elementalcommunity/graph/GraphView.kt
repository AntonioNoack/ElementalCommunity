package me.antonio.noack.elementalcommunity.graph

import android.content.Context
import android.util.AttributeSet
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element

class GraphView(ctx: Context, attrs: AttributeSet?) :
    NodeGraphView(ctx, attrs, alwaysValid = true, iterativeTree = true) {

    private val grid = Grid(1, 1)

    override fun buildTreeAsync() {
        val elements = getElements()
        Clustering.cluster(elements, getRecipes(elements))
    }

    override fun buildTreeIteratively() {
        val elements = getElements()
        Clustering.clusterIteratively(elements, grid, getRecipes(elements))
    }

    private fun getElements(): List<Element> {
        val unlocked = AllManager.unlockedElements
        val elements = ArrayList<Element>(unlocked.sumOf { it.size })
        forAllElements { element -> elements.add(element) }
        return elements
    }

    private fun getRecipes(elements: List<Element>): List<Collection<Element>> {
        for (i in elements.indices) {
            elements[i].index = i
        }
        val result = Array(elements.size) { ArrayList<Element>(4) }
        for ((re, recipes) in AllManager.recipesByElement) {
            val rl = result[re.index]
            for ((ae, be) in recipes) {

                val al = result[ae.index]
                if (ae !== re && ae !in rl) {
                    rl.add(ae)
                    al.add(re)
                }

                val bl = result[be.index]
                if (be !== re && ae !== be && be !in rl) {
                    rl.add(be)
                    bl.add(re)
                }

            }
        }
        return result.toList()
    }

}