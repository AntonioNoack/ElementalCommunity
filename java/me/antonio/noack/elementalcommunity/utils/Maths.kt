package me.antonio.noack.elementalcommunity.utils

import kotlin.math.floor

object Maths {

    fun fract(x: Double): Double {
        return x - floor(x)
    }

    fun fract(x: Float): Float {
        return x - floor(x.toDouble()).toFloat()
    }

    fun mix(a: Float, b: Float, f: Float): Float {
        return (1-f)*a+b*f
    }

    fun random() = Math.random()

}