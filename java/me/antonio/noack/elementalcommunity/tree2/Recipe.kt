package me.antonio.noack.elementalcommunity.tree2

import me.antonio.noack.elementalcommunity.Element

/**
 * always from ingredient to result
 * */
class Recipe(var a: Element,
             var b: Element,
             val r: Element,
             var al: MovingLocation,
             var bl: MovingLocation,
             var rl: MovingLocation): Comparable<Recipe> {

    constructor(a: Element, b: Element, r: Element): this(a, b, r, MovingLocation(), MovingLocation(), MovingLocation())

    init {
        if(a.uuid > b.uuid){
            val t = a
            a = b
            b = t
        }
    }

    val hash = a.uuid.shl(22) or b.uuid.shl(11) or r.uuid
    override fun compareTo(other: Recipe): Int {
        val x0 = hash.compareTo(other.hash)
        if(x0 != 0) return x0
        val x1 = a.uuid.compareTo(other.a.uuid)
        if(x1 != 0) return x1
        val x2 = b.uuid.compareTo(other.b.uuid)
        if(x2 != 0) return x2
        return r.uuid.compareTo(other.r.uuid)
    }

    override fun equals(other: Any?): Boolean {
        return other is Recipe && other.a == a && other.b == b && other.r == r
    }

    fun decay(){
        al.sourceZ = 1f
        bl.sourceZ = 1f
        rl.sourceZ = 1f
        al.targetZ = 0f
        bl.targetZ = 0f
        rl.targetZ = 0f
    }

    override fun hashCode(): Int = hash

}