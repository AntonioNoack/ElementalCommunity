package me.antonio.noack.elementalcommunity.utils

object Maths {

    fun fract(x: Float): Float {
        return x - Math.floor(x.toDouble()).toFloat()
    }

    fun mix(a: Float, b: Float, f: Float): Float {
        return (1-f)*a+b*f
    }

}