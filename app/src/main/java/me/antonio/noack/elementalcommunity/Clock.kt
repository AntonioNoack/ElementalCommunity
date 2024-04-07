package me.antonio.noack.elementalcommunity

class Clock {
    private var t0 = System.nanoTime()
    private val t00 = t0
    fun stop(name: String) {
        val t1 = System.nanoTime()
        println("$name took ${(t1 - t0) / 1e6} ms")
        t0 = t1
    }

    fun total(name: String) {
        val t1 = System.nanoTime()
        println("$name took ${(t1 - t00) / 1e6} ms")
    }
}