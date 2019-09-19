package me.antonio.noack.elementalcommunity

import me.antonio.noack.elementalcommunity.AllManager.Companion.elementById
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementByName
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementsByGroup
import me.antonio.noack.elementalcommunity.AllManager.Companion.invalidate
import me.antonio.noack.elementalcommunity.AllManager.Companion.save
import me.antonio.noack.elementalcommunity.AllManager.Companion.unlockeds

class Element private constructor(var name: String, val uuid: Int, var group: Int){

    var rank = -1

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
        fun get(name: String, uuid: Int, group: Int): Element {
            val old = elementById[uuid]
            return if(old != null){
                old.name = name
                old.lcName = name.toLowerCase()
                if(old.group != group){
                    synchronized(Unit){
                        elementsByGroup[old.group].remove(old)
                        unlockeds[old.group].remove(old)
                        elementsByGroup[group].add(old)
                        unlockeds[group].add(old)
                        old.group = group
                        save()
                    }
                    invalidate()
                }
                old
            } else Element(name, uuid, group)
        }
    }

}