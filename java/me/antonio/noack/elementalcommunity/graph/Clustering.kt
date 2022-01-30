package me.antonio.noack.elementalcommunity.graph

import androidx.core.math.MathUtils.clamp
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.utils.Maths.mix
import kotlin.math.*
import kotlin.random.Random

object Clustering {

    val limit = 10

    fun cluster(
        elements: List<Element>,
        getConnections: (Element) -> Sequence<Element>,
        numClusters: Int = sqrt(elements.size.toFloat()).toInt()
    ) {

        if (elements.isEmpty()) {
            return
        }

        val maxIdP1 = elements.maxByOrNull { it.uuid }!!.uuid + 1
        val sorted = elements.sortedBy { it.uuid }

        val elementCount = sorted.size

        val random = Random(1234L)
        val elementX = FloatArray(maxIdP1)
        val elementY = FloatArray(maxIdP1)

        // assign all their random id to keep the elements consistent with their position
        for (i in 0 until maxIdP1) {
            elementX[i] = random.nextFloat() * 4f - 2f
            elementY[i] = random.nextFloat() * 4f - 2f
        }

        val elementClusterIndices = IntArray(elementCount)

        val clusterCenterX = FloatArray(numClusters)
        val clusterCenterY = FloatArray(numClusters)
        for (i in 0 until numClusters) {
            val angle = i * 6.2830f / numClusters
            clusterCenterX[i] = cos(angle)
            clusterCenterY[i] = sin(angle)
        }

        val newClusterCenterX = FloatArray(numClusters)
        val newClusterCenterY = FloatArray(numClusters)
        val newClusterCounts = IntArray(numClusters)

        val gridSize = max(5, sqrt(elementCount.toFloat()).toInt())
        val grid = Grid(gridSize, gridSize)

        for (iteration in 0 until 200) {

            val alpha = 0.02f // how much we move towards other elements
            val beta = 0.05f // how much we move towards cluster center
            val gamma = 0.1f // force multiplier, force is proportional to 1/distance³

            // find which cluster each cluster is closest to and average the cluster position
            newClusterCenterX.fill(0f)
            newClusterCenterY.fill(0f)
            newClusterCounts.fill(0)

            // move elements towards their partners
            for (i in 0 until elementCount) {
                val element = sorted[i]
                val j = element.uuid
                var px = elementX[j]
                var py = elementY[j]
                for (other in getConnections(element)) {
                    val oi = other.uuid
                    var ox = elementX[oi]
                    var oy = elementY[oi]
                    px = mix(px, ox, alpha)
                    py = mix(py, oy, alpha)
                    ox = mix(ox, px, alpha)
                    oy = mix(oy, py, alpha)
                    elementX[oi] = ox
                    elementY[oi] = oy
                }
                elementX[j] = px
                elementY[j] = py
            }

            var minX0 = Float.POSITIVE_INFINITY
            var maxX0 = Float.NEGATIVE_INFINITY
            var minY0 = Float.POSITIVE_INFINITY
            var maxY0 = Float.NEGATIVE_INFINITY

            for (i in 0 until elementCount) {
                val element = sorted[i]
                val j = element.uuid
                val px = elementX[j]
                val py = elementY[j]
                minX0 = min(minX0, px)
                minY0 = min(minY0, py)
                maxX0 = max(maxX0, px)
                maxY0 = max(maxY0, py)
            }

            grid.clear()
            grid.setSize(minX0, maxX0, minY0, maxY0)

            for (i in 0 until elementCount) {
                val element = sorted[i]
                val j = element.uuid
                grid.add(elementX[j], elementY[j], element)
            }

            // try to prevent overlaps
            for (i in 0 until elementCount) {
                val element = sorted[i]
                val j = element.uuid
                var px = elementX[j]
                var py = elementY[j]
                grid.forAllNeighbors(px, py, limit) { other ->
                    val k = other.uuid
                    val ox = elementX[k]
                    val oy = elementY[k]
                    // repulsive force
                    val dx = px - ox
                    val dy = py - oy
                    val d2 = dx * dx + dy * dy
                    if (d2 < 1e-16f) {
                        // try to fix it...
                        px = random.nextFloat()
                        py = random.nextFloat()
                    } else {
                        val scale = 1f / d2
                        val sca2 = scale * scale * gamma
                        px += sca2 * dx
                        py += sca2 * dy
                    }
                }
                elementX[j] = px
                elementY[j] = py
            }

            // find the closest clusters to recalculate the cluster center
            for (i in 0 until elementCount) {
                val j = sorted[i].uuid
                val ex = elementX[j]
                val ey = elementY[j]
                var bestDistance = Float.POSITIVE_INFINITY
                var bestCluster = 0
                for (ci in 0 until numClusters) {
                    val cx = clusterCenterX[ci] - ex
                    val cy = clusterCenterY[ci] - ey
                    val distance = cx * cx + cy * cy
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestCluster = ci
                    }
                }
                elementClusterIndices[i] = bestCluster
                newClusterCenterX[bestCluster] += ex
                newClusterCenterY[bestCluster] += ey
                newClusterCounts[bestCluster]++
            }

            // compute the cluster centers
            var minX = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            for (i in 0 until numClusters) {
                val ncc = newClusterCounts[i]
                if (ncc > 0) {
                    val norm = 1f / ncc
                    clusterCenterX[i] = newClusterCenterX[i] * norm
                    clusterCenterY[i] = newClusterCenterY[i] * norm
                } else {// empty cluster, try again
                    clusterCenterX[i] = random.nextFloat()
                    clusterCenterY[i] = random.nextFloat()
                }
                minX = min(minX, clusterCenterX[i])
                maxX = max(maxX, clusterCenterX[i])
                minY = min(minY, clusterCenterY[i])
                maxY = max(maxY, clusterCenterY[i])
            }

            // rescale all positions, so we don't get clustering into a single point
            val rescale = 1f / min(maxX - minX, maxY - minY)
            val centerX = (minX + maxX) * 0.5f
            val centerY = (minY + maxY) * 0.5f
            for (i in 0 until elementCount) {
                val j = sorted[i].uuid
                elementX[j] = (elementX[j] - centerX) * rescale
                elementY[j] = (elementY[j] - centerY) * rescale
            }

            for (i in 0 until numClusters) {
                clusterCenterX[i] = (clusterCenterX[i] - centerX) * rescale
                clusterCenterY[i] = (clusterCenterY[i] - centerY) * rescale
            }

            // move elements towards their cluster
            for (i in 0 until elementCount) {
                val j = sorted[i].uuid
                val eci = elementClusterIndices[i]
                elementX[j] = mix(elementX[j], clusterCenterX[eci], beta)
                elementY[j] = mix(elementY[j], clusterCenterY[eci], beta)
            }
        }

        // todo make sure that no elements overlap

        val scale = 5f * sqrt(elementCount.toFloat())

        // save the computed positions
        for (elementIndex in 0 until elementCount) {
            val element = sorted[elementIndex]
            val i = element.uuid
            element.px = elementX[i] * scale
            element.py = elementY[i] * scale
            println("${element.name} is at ${element.px}, ${element.py}")
        }


    }

    fun clusterIteratively(
        elements: List<Element>,
        grid: Grid,
        getConnections: (Element) -> Sequence<Element>
    ) {

        if (elements.isEmpty()) return

        val elementCount = elements.size
        val sqrt = sqrt(elementCount.toFloat())

        val scale = 5f * sqrt

        val gridSize = max(5, sqrt.toInt())
        grid.resize(gridSize, gridSize)

        for (i in 0 until elementCount) {
            val element = elements[i]
            element.fx = 0f
            element.fy = 0f
            if (element.px == 0f || element.py == 0f ||
                !element.px.isFinite() || !element.py.isFinite()
            ) resetElement(element, scale)
        }

        // move elements towards their partners
        for (i in 0 until elementCount) {
            val element = elements[i]
            for (other in getConnections(element)) {
                addForce(element, other, -1f)
            }
        }

        var minX0 = Float.POSITIVE_INFINITY
        var maxX0 = Float.NEGATIVE_INFINITY
        var minY0 = Float.POSITIVE_INFINITY
        var maxY0 = Float.NEGATIVE_INFINITY

        for (i in 0 until elementCount) {
            val element = elements[i]
            val px = element.px
            val py = element.py
            minX0 = min(minX0, px)
            minY0 = min(minY0, py)
            maxX0 = max(maxX0, px)
            maxY0 = max(maxY0, py)
        }

        grid.setSize(minX0, maxX0, minY0, maxY0)

        for (i in 0 until elementCount) {
            val element = elements[i]
            grid.add(element.px, element.py, element)
        }

        // try to prevent overlaps
        for (i in 0 until elementCount) {
            val element = elements[i]
            grid.forAllNeighbors(element.px, element.py, limit) { other ->
                addForce(element, other, 1.5f)
            }
        }

        // apply force on velocity
        // apply velocity on position
        val dt = 0.1f
        val friction = 0.9f
        val max = 100f
        val min = -max
        for (i in 0 until elementCount) {
            val element = elements[i]
            element.vx += clamp(element.fx, min, max) * dt
            element.vy += clamp(element.fy, min, max) * dt
            element.vx *= friction
            element.vy *= friction
            element.px += clamp(element.vx, min, max) * dt
            element.py += clamp(element.vy, min, max) * dt
        }

        // compute the cluster centers
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (i in 0 until elementCount) {
            val element = elements[i]
            val px = element.px
            val py = element.py
            minX = min(minX, px)
            minY = min(minY, py)
            maxX = max(maxX, px)
            maxY = max(maxY, py)
        }

        // rescale all positions, so we don't get clustering into a single point
        val rescale = scale / min(maxX - minX, maxY - minY)
        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        for (i in 0 until elementCount) {
            val element = elements[i]
            element.px = (element.px - centerX) * rescale
            element.py = (element.py - centerY) * rescale
        }

    }

    private fun resetElement(element: Element, scale: Float) {
        element.px = (Math.random().toFloat() - 0.5f) * scale
        element.py = (Math.random().toFloat() - 0.5f) * scale
    }

    private fun addForce(e1: Element, e2: Element, fx: Float, fy: Float) {
        e1.fx += fx
        e1.fy += fy
        e2.fx -= fx
        e2.fy -= fy
    }

    private fun addForce(e1: Element, e2: Element, strength: Float) {
        val dx = e1.px - e2.px
        val dy = e1.py - e2.py
        val s = strength / sq(dx * dx + dy * dy)
        if (s.isFinite()) addForce(e1, e2, s * dx, s * dy)
    }

    private fun sq(x: Float) = x * x

}