package me.antonio.noack.elementalcommunity

import java.util.*
import kotlin.math.floor

class NoiseMap2D {

    val random = Random()

    fun getNoise(x: Float, y: Float): Float {
        val floorY = floor(y)
        val fractY = y - floorY
        val indexY = floorY.toInt()
        return mix(getNoise(x, indexY), getNoise(x, indexY+1), fractY)
    }

    fun getNoise(x: Float, y: Int): Float {
        val floorX = floor(x)
        val fractX = x - floorX
        val indexX = floorX.toInt()
        return mix(getNoise(indexX, y), getNoise(indexX+1, y), fractX)
    }

    fun mix(x: Float, y: Float, f: Float): Float {
        return (1f-f) * x + y * f
    }

    fun getNoise(x: Int, y: Int): Float {
        random.setSeed(x + 651231 * y.toLong())
        return random.nextFloat()
    }

}