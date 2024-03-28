package me.antonio.noack.elementalcommunity.mandala

import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import kotlin.math.*

// todo reload news on swipe down

class Mandala(val element: Element, oldTree: Mandala?){

    val toThisElement = ArrayList<Recipe>()
    val fromThisElement = ArrayList<Recipe>()

    val toThisSet = HashSet<Recipe>()
    val fromThisSet = HashSet<Recipe>()

    var outherSize = 1f
    var innerSize = 1f

    fun draw(time: Float,
             drawElement: (element: Element, x: Float, y: Float, alpha: Float, size: Float) -> Unit,
             drawLine: (x0: Float, y0: Float, x1: Float, y1: Float, alpha: Float) -> Unit){
        toThisElement.forEach { r ->
            r.apply {
                val ax = al.getX(time)
                val ay = al.getY(time)
                val bx = bl.getX(time)
                val by = bl.getY(time)
                val rx = rl.getX(time)
                val ry = rl.getY(time)
                val cx = (ax+bx+rx) * 0.33f
                val cy = (ay+by+ry) * 0.33f
                val alpha = al.getZ(time)
                drawLine(ax, ay, cx, cy, alpha)
                drawLine(bx, by, cx, cy, alpha)
                drawLine(rx, ry, cx, cy, alpha)
            }
        }
        fromThisElement.forEach {r ->
            r.apply {
                val ax = al.getX(time)
                val ay = al.getY(time)
                val bx = bl.getX(time)
                val by = bl.getY(time)
                val rx = rl.getX(time)
                val ry = rl.getY(time)
                val cx = (ax+bx+rx) * 0.33f
                val cy = (ay+by+ry) * 0.33f
                val alpha = al.getZ(time)
                drawLine(ax, ay, cx, cy, alpha)
                drawLine(bx, by, cx, cy, alpha)
                drawLine(rx, ry, cx, cy, alpha)
            }
        }
        toThisElement.forEach { r ->
            r.apply {
                val ax = al.getX(time)
                val ay = al.getY(time)
                val bx = bl.getX(time)
                val by = bl.getY(time)
                val rx = rl.getX(time)
                val ry = rl.getY(time)
                drawElement(a, ax, ay, al.getZ(time), outherSize)
                if(a != b) drawElement(b, bx, by, bl.getZ(time), outherSize)
                if(abs(rx)+abs(ry) > 0.01f) drawElement(r.r, rx, ry, rl.getZ(time), 1f)
            }
        }
        drawElement(element, 0f, 0f, 1f, 2f - min(1f, time))
        fromThisElement.forEach {r ->
            r.apply {
                val ax = al.getX(time)
                val ay = al.getY(time)
                val bx = bl.getX(time)
                val by = bl.getY(time)
                val rx = rl.getX(time)
                val ry = rl.getY(time)
                if(abs(ax)+abs(ay) > 0.01f) drawElement(a, ax, ay, al.getZ(time), innerSize)
                if(abs(bx)+abs(by) > 0.01f) drawElement(b, bx, by, bl.getZ(time), innerSize)
                drawElement(r.r, rx, ry, rl.getZ(time), outherSize)
            }
        }
    }

    // todo change element in view if clicked, or got result

    init {

        // collect all recipes for & from this element
        AllManager.elementByRecipe.entries.forEach { (ab, r) ->
            val (a, b) = ab
            if(r == element){
                toThisElement.add(Recipe(a, b, r))
            }
            if(a == element || b == element){
                fromThisElement.add(Recipe(a, b, r))
            }
        }

        toThisSet.addAll(toThisElement)
        fromThisSet.addAll(fromThisElement)

        // calculate locations, and move the elements
        /*for(r in toThisElement){
            r.rl.targetX = 0f
            r.rl.sourceX = 0f
        }*/

        val toCount = toThisElement.sumBy { r -> if(r.a == r.b) 1 else 2 }
        val fromCount = fromThisElement.size

        val sumCount = toCount + fromCount + 3
        val toArea = 2f * toCount.toFloat() / sumCount
        val fromArea = 2f * fromCount.toFloat() / sumCount

        val scale = min(1f, 20f / sumCount)
        outherSize = scale
        innerSize = scale

        val toDifHalf = (toCount-1) * 0.5f
        val toDif = (toArea * PI/max(1,toCount-1)).toFloat()
        val fromDifHalf = (fromCount-1) * 0.5f
        val fromDif = (fromArea * PI/max(1, fromCount-1)).toFloat()

        val many = sumCount > 20

        val outher = 1f
        val inner0 = if(many) 0.75f else 0.667f
        val inner1 = if(many) 0.60f else inner0
        val inner = listOf(inner0, inner1)

        var ctr = 0
        toThisElement.forEach { r ->
            r.al = getLocation(ctr++ - toDifHalf, toDif, outher, false)
            if(r.a != r.b){
                r.bl = getLocation(ctr++ - toDifHalf, toDif, outher, false)
            }
        }

        ctr = 0
        fromThisElement.forEach { r ->
            if(r.a != element) r.al = getLocation(ctr - fromDifHalf, fromDif, inner[ctr and 1], true)
            else r.bl = getLocation(ctr - fromDifHalf, fromDif, inner[ctr and 1], true)
            r.rl = getLocation(ctr - fromDifHalf, fromDif, outher, true)
            ctr++
        }

        if(oldTree != null){// move old recipes :)
            oldTree.fromThisSet.filter { oldRecipe ->
                if(toThisSet.contains(oldRecipe)) {
                    val newRecipe = toThisElement[toThisElement.indexOf(oldRecipe)]
                    newRecipe.al *= oldRecipe.al
                    newRecipe.bl *= oldRecipe.bl
                    newRecipe.rl *= oldRecipe.rl
                    false
                } else true
            }.forEach {
                it.decay()
                //toThisElement.add(it)
            }
            oldTree.toThisSet.filter { oldRecipe ->
                if(fromThisSet.contains(oldRecipe)) {
                    val newRecipe = fromThisElement[fromThisElement.indexOf(oldRecipe)]
                    newRecipe.al *= oldRecipe.al
                    newRecipe.bl *= oldRecipe.bl
                    newRecipe.rl *= oldRecipe.rl
                    false
                } else true
            }.forEach {
                it.decay()
                //fromThisElement.add(it)
            }
        }


    }


    fun getLocation(i: Float, dif: Float, amplitude: Float, isBottom: Boolean): MovingLocation {
        val angle = i * dif + if(isBottom) 0f else PI.toFloat()
        val cos = cos(angle) * amplitude
        val sin = sin(angle) * amplitude
        return MovingLocation(sin, cos)
    }




}