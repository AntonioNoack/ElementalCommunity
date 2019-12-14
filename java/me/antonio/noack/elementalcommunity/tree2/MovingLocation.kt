package me.antonio.noack.elementalcommunity.tree2

import me.antonio.noack.elementalcommunity.utils.Maths.mix
import kotlin.math.min

class MovingLocation(var targetX: Float, var targetY: Float, var targetZ: Float,
                     var sourceX: Float, var sourceY: Float, var sourceZ: Float){

    constructor(): this(0f, 0f, 1f, 0f, 0f, 0f)

    constructor(targetX: Float, targetY: Float): this(targetX, targetY, 1f, targetX, targetY, 0f)

    fun getX(time: Float) = mix(sourceX, targetX, min(1f, time))
    fun getY(time: Float) = mix(sourceY, targetY, min(1f, time))
    fun getZ(time: Float) = mix(sourceZ, targetZ, min(1f, time))

    val lenSq
        get() = targetX*targetX + targetY*targetY

    operator fun timesAssign(s: MovingLocation){
        sourceX = s.targetX
        sourceY = s.targetY
        sourceZ = s.targetZ
    }

}