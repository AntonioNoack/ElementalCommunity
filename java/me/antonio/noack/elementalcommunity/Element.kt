package me.antonio.noack.elementalcommunity

import androidx.core.math.MathUtils.clamp
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementById
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementByName
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementsByGroup
import me.antonio.noack.elementalcommunity.AllManager.Companion.invalidate
import me.antonio.noack.elementalcommunity.AllManager.Companion.save
import me.antonio.noack.elementalcommunity.AllManager.Companion.unlockeds
import me.antonio.noack.elementalcommunity.GroupsEtc.minimumCraftingCount

class Element private constructor(var name: String, val uuid: Int, var group: Int): Comparable<Element> {

    override fun compareTo(other: Element): Int = uuid.compareTo(other.uuid)

    var rank = -1

    /**
     * how often it was created, roughly
     * */
    var craftingCount = -1

    var srcA: Element? = null
    var srcB: Element? = null
    var hasTreeOutput = false

    var treeX = 0
    var treeY = 0

    var lcName = name.toLowerCase()

    init {
        synchronized(Unit){
            elementById.put(uuid, this)
            elementByName[name] = this
            elementsByGroup[group].add(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        return uuid == (other as? Element)?.uuid
    }

    override fun hashCode(): Int {
        return group * 12461 + uuid
    }

    override fun toString(): String {
        return "$name($uuid, $group)"
    }

    companion object {
        fun get(name: String, uuid: Int, group: Int, craftingCount: Int): Element {
            val old = elementById[uuid]
            return if(old != null){
                old.name = name
                old.lcName = name.toLowerCase()
                if(craftingCount >= minimumCraftingCount){ old.craftingCount = craftingCount }
                if(old.group != group){
                    synchronized(Unit){
                        println("group for $name: $group")
                        val newGroup = clamp(group, 0, elementsByGroup.size-1)
                        elementsByGroup[old.group].remove(old)
                        unlockeds[old.group].remove(old)
                        elementsByGroup[newGroup].add(old)
                        unlockeds[newGroup].add(old)
                        old.group = newGroup
                        save()
                    }
                    invalidate()
                }
                old
            } else Element(name, uuid, group)
        }
    }

}