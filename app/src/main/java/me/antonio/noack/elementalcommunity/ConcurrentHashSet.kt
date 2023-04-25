package me.antonio.noack.elementalcommunity

import java.util.concurrent.ConcurrentHashMap

class ConcurrentHashSet<V> : ConcurrentHashMap<V, Unit> {

    constructor() : super()
    constructor(capacity: Int) : super(capacity)

    fun put(element: V) {
        this[element] = Unit
    }

    fun addAll(elements: Collection<V>) {
        for (e in elements) {
            put(e!!, Unit)
        }
    }

}