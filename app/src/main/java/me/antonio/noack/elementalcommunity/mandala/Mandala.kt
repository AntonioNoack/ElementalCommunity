package me.antonio.noack.elementalcommunity.mandala

import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// todo reload news on swipe down

class Mandala(val element: Element, oldTree: Mandala?) {

    val toThisElement = ArrayList<Recipe>()
    val fromThisElement = ArrayList<Recipe>()

    private val toThisSet = HashSet<Recipe>()
    private val fromThisSet = HashSet<Recipe>()

    private var outerSize = 1f
    private var innerSize = 1f

    fun draw(
        time: Float,
        drawElement: (element: Element, x: Float, y: Float, alpha: Float, size: Float) -> Unit,
        drawLine: (x0: Float, y0: Float, x1: Float, y1: Float, alpha: Float) -> Unit
    ) {
        for (ri in toThisElement.indices) {
            val r = toThisElement[ri]
            val al = r.al
            val bl = r.bl
            val rl = r.rl
            val ax = al.getX(time)
            val ay = al.getY(time)
            val bx = bl.getX(time)
            val by = bl.getY(time)
            val rx = rl.getX(time)
            val ry = rl.getY(time)
            val cx = (ax + bx + rx) * 0.33f
            val cy = (ay + by + ry) * 0.33f
            val alpha = al.getZ(time)
            drawLine(ax, ay, cx, cy, alpha)
            drawLine(bx, by, cx, cy, alpha)
            drawLine(rx, ry, cx, cy, alpha)

        }
        for (ri in fromThisElement.indices) {
            val r = fromThisElement[ri]
            val al = r.al
            val bl = r.bl
            val rl = r.rl
            val ax = al.getX(time)
            val ay = al.getY(time)
            val bx = bl.getX(time)
            val by = bl.getY(time)
            val rx = rl.getX(time)
            val ry = rl.getY(time)
            val cx = (ax + bx + rx) * 0.33f
            val cy = (ay + by + ry) * 0.33f
            val alpha = al.getZ(time)
            drawLine(ax, ay, cx, cy, alpha)
            drawLine(bx, by, cx, cy, alpha)
            drawLine(rx, ry, cx, cy, alpha)
        }
        for (ri in toThisElement.indices) {
            val r = toThisElement[ri]
            val al = r.al
            val bl = r.bl
            val rl = r.rl
            val ax = al.getX(time)
            val ay = al.getY(time)
            val bx = bl.getX(time)
            val by = bl.getY(time)
            val rx = rl.getX(time)
            val ry = rl.getY(time)
            drawElement(r.a, ax, ay, al.getZ(time), outerSize)
            if (r.a != r.b) drawElement(r.b, bx, by, bl.getZ(time), outerSize)
            if (abs(rx) + abs(ry) > 0.01f) drawElement(r.r, rx, ry, rl.getZ(time), 1f)
        }
        drawElement(element, 0f, 0f, 1f, 2f - min(1f, time))
        for (ri in fromThisElement.indices) {
            val r = fromThisElement[ri]
            val al = r.al
            val bl = r.bl
            val rl = r.rl
            val ax = al.getX(time)
            val ay = al.getY(time)
            val bx = bl.getX(time)
            val by = bl.getY(time)
            val rx = rl.getX(time)
            val ry = rl.getY(time)
            if (abs(ax) + abs(ay) > 0.01f) drawElement(r.a, ax, ay, al.getZ(time), innerSize)
            if (abs(bx) + abs(by) > 0.01f) drawElement(r.b, bx, by, bl.getZ(time), innerSize)
            drawElement(r.r, rx, ry, rl.getZ(time), outerSize)
        }
    }

    // todo change element in view if clicked, or got result

    init {

        // collect all recipes for & from this element
        for ((ab, r) in AllManager.elementByRecipe) {
            val (a, b) = ab
            if (r == element) {
                toThisElement.add(Recipe(a, b, r))
            }
            if (a == element || b == element) {
                fromThisElement.add(Recipe(a, b, r))
            }
        }

        toThisSet.addAll(toThisElement)
        fromThisSet.addAll(fromThisElement)

        // calculate locations, and move the elements

        val toCount = toThisElement.sumOf { r -> if (r.a == r.b) 1L else 2L }
        val fromCount = fromThisElement.size

        val sumCount = toCount + fromCount + 3
        val toArea = 2f * toCount.toFloat() / sumCount
        val fromArea = 2f * fromCount.toFloat() / sumCount

        val scale = min(1f, 20f / sumCount)
        outerSize = scale
        innerSize = scale

        val toDifHalf = (toCount - 1) * 0.5f
        val toDif = (toArea * PI / max(1, toCount - 1)).toFloat()
        val fromDifHalf = (fromCount - 1) * 0.5f
        val fromDif = (fromArea * PI / max(1, fromCount - 1)).toFloat()

        val many = sumCount > 20

        val outher = 1f
        val inner0 = if (many) 0.75f else 0.667f
        val inner1 = if (many) 0.60f else inner0
        val inner = listOf(inner0, inner1)

        var ctr = 0
        toThisElement.forEach { r ->
            r.al = getLocation(ctr++ - toDifHalf, toDif, outher, false)
            if (r.a != r.b) {
                r.bl = getLocation(ctr++ - toDifHalf, toDif, outher, false)
            }
        }

        ctr = 0
        fromThisElement.forEach { r ->
            if (r.a != element) r.al =
                getLocation(ctr - fromDifHalf, fromDif, inner[ctr and 1], true)
            else r.bl = getLocation(ctr - fromDifHalf, fromDif, inner[ctr and 1], true)
            r.rl = getLocation(ctr - fromDifHalf, fromDif, outher, true)
            ctr++
        }

        if (oldTree != null) {// move old recipes :)
            oldTree.fromThisSet.filter { oldRecipe ->
                if (toThisSet.contains(oldRecipe)) {
                    val newRecipe = toThisElement[toThisElement.indexOf(oldRecipe)]
                    newRecipe.al *= oldRecipe.al
                    newRecipe.bl *= oldRecipe.bl
                    newRecipe.rl *= oldRecipe.rl
                    false
                } else true
            }.forEach {
                it.decay()
            }
            oldTree.toThisSet.filter { oldRecipe ->
                if (fromThisSet.contains(oldRecipe)) {
                    val newRecipe = fromThisElement[fromThisElement.indexOf(oldRecipe)]
                    newRecipe.al *= oldRecipe.al
                    newRecipe.bl *= oldRecipe.bl
                    newRecipe.rl *= oldRecipe.rl
                    false
                } else true
            }.forEach {
                it.decay()
            }
        }
    }

    private fun getLocation(
        i: Float,
        dif: Float,
        amplitude: Float,
        isBottom: Boolean
    ): MovingLocation {
        val angle = i * dif + if (isBottom) 0f else PI.toFloat()
        val cos = cos(angle) * amplitude
        val sin = sin(angle) * amplitude
        return MovingLocation(sin, cos)
    }

}