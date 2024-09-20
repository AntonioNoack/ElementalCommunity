package me.antonio.noack.elementalcommunity

import androidx.core.math.MathUtils.clamp
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementById
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementByName
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementsByGroup
import me.antonio.noack.elementalcommunity.AllManager.Companion.invalidate
import me.antonio.noack.elementalcommunity.AllManager.Companion.saveElement2
import me.antonio.noack.elementalcommunity.AllManager.Companion.unlockedElements
import me.antonio.noack.elementalcommunity.GroupsEtc.minimumCraftingCount
import me.antonio.noack.elementalcommunity.utils.Compact
import kotlin.math.min

class Element constructor(
    var name: String,
    val uuid: Int,
    var group: Int,
    /**
     * how often it was created, roughly
     * */
    var craftingCount: Int
) : Comparable<Element> {

    override fun compareTo(other: Element): Int {
        val y = startingNumber.compareTo(other.startingNumber)
        if (y != 0) return y
        val x = hashLong.compareTo(other.hashLong)
        if (x != 0) return x
        return compacted.compareTo(other.compacted)
    }

    var rank = -1

    var srcA: Element? = null
    var srcB: Element? = null
    var hasTreeOutput = false

    // tree position
    var tx = 0f
    var ty = 0f

    // graph position
    var px = 0f
    var py = 0f

    var vx = 0f
    var vy = 0f

    var fx = 0f
    var fy = 0f

    var index = 0
    var lastSwitch = -1

    var compacted = Compact.compacted(name)
    var hashLong = calcHashLong()
    var startingNumber = calcStartingNumber()
    var createdDate = 0

    fun calcStartingNumber(): Long {

        var num = 0L
        var i = 0

        val isNegative = if (compacted.startsWith("-")) {
            i++
            true
        } else {
            false
        }

        val limit = if (isNegative) 20 else 19

        number@ while (i < compacted.length) {
            if (i == limit) return if (isNegative) Long.MIN_VALUE else Long.MAX_VALUE
            when (compacted[i]) {
                in '0'..'9' -> {
                    num = num * 10 + compacted[i].code - '0'.code
                }
                else -> break@number
            }
            i++
        }

        return if (isNegative) -num else num
    }

    fun calcHashLong(): Long {
        val lcName = compacted
        var x = 0L
        for (i in 0 until min(9, lcName.length)) {
            x = x.shl(7) or lcName[i].code.toLong()
        }
        for (i in min(9, lcName.length) until 9) {
            x = x.shl(7)
        }
        return x
    }

    init {
        elementById[uuid] = this
        elementByName[name] = this
        elementByName[compacted] = this
        elementsByGroup.getOrNull(group)?.add(this)
    }

    override fun equals(other: Any?): Boolean {
        return uuid == (other as? Element)?.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return "$name($uuid, $group)"
    }

    companion object {
        fun get(name: String, uuid: Int, group: Int, craftingCount: Int, save: Boolean): Element {
            val element = elementById[uuid]
            return if (element != null) {
                var needsSave = false
                if (element.name != name) {
                    element.name = name
                    element.compacted = Compact.compacted(name)
                    element.startingNumber = element.calcStartingNumber()
                    element.hashLong = element.calcHashLong()
                    needsSave = true
                }
                if (craftingCount >= minimumCraftingCount && element.craftingCount != craftingCount) {
                    element.craftingCount = craftingCount
                    needsSave = true
                }
                if (element.group != group) {
                    // println("group for $name: $group")
                    val newGroup = clamp(group, 0, elementsByGroup.size - 1)
                    elementsByGroup[element.group].remove(element)
                    unlockedElements[element.group].remove(element)
                    elementsByGroup[newGroup].add(element)
                    unlockedElements[newGroup].add(element)
                    element.group = newGroup
                    // there is the issue:
                    // saving is done inefficiently
                    needsSave = true
                    invalidate()
                }
                if (save && needsSave) {
                    saveElement2(element)
                }
                element
            } else Element(name, uuid, group, craftingCount)
        }
    }

}