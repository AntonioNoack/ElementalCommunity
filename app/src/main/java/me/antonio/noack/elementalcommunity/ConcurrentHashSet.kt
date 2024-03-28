package me.antonio.noack.elementalcommunity

import java.util.concurrent.ConcurrentHashMap

class ConcurrentHashSet<V>(capacity: Int) : ConcurrentHashMap<V, Unit>(capacity) {

    fun put(element: V) {
        this[element] = Unit
    }

    fun addAll(elements: Collection<V>) {
        for (e in elements) {
            put(e!!, Unit)
        }
    }

}