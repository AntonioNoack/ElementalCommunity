package me.antonio.noack.elementalcommunity.graph

import androidx.core.math.MathUtils.clamp
import me.antonio.noack.elementalcommunity.Element
import kotlin.math.max
import kotlin.math.min

class Grid(private var sizeX: Int, private var sizeY: Int) {

    private var lists = Array(sizeX * sizeY) { ArrayList<Element>() }

    fun resize(sizeX: Int, sizeY: Int) {
        val size = sizeX * sizeY
        if (size != this.sizeX * this.sizeY) {
            lists = Array(sizeX * sizeY) { ArrayList() }
        } else clear()
        this.sizeX = sizeX
        this.sizeY = sizeY
    }

    private fun getIndex(x: Float, sizeX: Int) = clamp((x * sizeX).toInt(), 0, sizeX - 1)

    fun add(x: Float, y: Float, element: Element) {
        val ix = getIndex((x - ox) * sx, sizeX)
        val iy = getIndex((y - oy) * sy, sizeY)
        lists[ix + iy * sizeX].add(element)
    }

    fun forNeighbor(x: Int, y: Int, runnable: (Element) -> Unit): Int {
        val i = x + y * sizeX
        val list = lists[i]
        for (element in list) {
            runnable(element)
        }
        return list.size
    }

    fun forAllNeighbors(xf: Float, yf: Float, limit: Int, runnable: (Element) -> Unit) {
        val sizeX = sizeX
        val sizeY = sizeY
        val ix = getIndex((xf - ox) * sx, sizeX)
        val iy = getIndex((yf - oy) * sy, sizeY)
        var remaining = limit - forNeighbor(ix, iy, runnable)
        // spiral, until we have found enough neighbors
        for (r in 1 until max(sizeX, sizeY)) {
            if (remaining <= 0) return
            if (iy + r < sizeY) for (x in max(ix - r, 0) until min(ix + r, sizeX)) {
                remaining -= forNeighbor(x, iy + r, runnable)
            }
            if (ix + r < sizeX) for (dy in min(iy + r, sizeY - 1) downTo max(iy + -r + 1, 0)) {
                remaining -= forNeighbor(ix + r, dy, runnable)
            }
            if (iy - r >= 0) for (dx in min(ix + r, sizeX - 1) downTo max(ix - r + 1, 0)) {
                remaining -= forNeighbor(dx, iy - r, runnable)
            }
            if (ix - r >= 0) for (dy in max(iy - r, 0) until min(iy + r, sizeY)) {
                remaining -= forNeighbor(ix - r, dy, runnable)
            }
        }
    }

    var ox = 0f
    var oy = 0f
    var sx = 1f
    var sy = 1f

    fun setSize(minX: Float, maxX: Float, minY: Float, maxY: Float) {
        ox = minX
        oy = minY
        sx = 1f / (maxX - minX)
        sy = 1f / (maxY - minY)
    }

    fun clear() {
        lists.forEach {
            it.clear()
        }
    }

}