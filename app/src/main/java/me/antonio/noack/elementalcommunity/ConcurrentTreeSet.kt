package me.antonio.noack.elementalcommunity

import java.util.TreeSet

class ConcurrentTreeSet<V> : Set<V> {

    private val content = TreeSet<V>()

    override val size: Int get() = content.size

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun iterator(): Iterator<V> {
        return synchronized(this) {
            content.toList().iterator()
        }
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return synchronized(this) {
            content.containsAll(elements)
        }
    }

    override fun contains(element: V): Boolean {
        return synchronized(this) {
            content.contains(element)
        }
    }

    fun add(element: V): Boolean {
        return synchronized(this) {
            content.add(element)
        }
    }

    fun addAll(elements: Collection<V>): Boolean {
        return synchronized(this) {
            content.addAll(elements)
        }
    }

    fun clear() {
        synchronized(this) {
            content.clear()
        }
    }

    fun remove(element: V): Boolean {
        return synchronized(this) {
            content.remove(element)
        }
    }

    fun removeAll(elements: Set<V>): Boolean {
        return synchronized(this) {
            content.removeAll(elements)
        }
    }

    fun getOrNull(index: Int): V? {
        return synchronized(this) {
            content.elementAtOrNull(index) // pretty expensive and inefficient
        }
    }

}