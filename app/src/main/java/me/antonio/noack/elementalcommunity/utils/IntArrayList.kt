package me.antonio.noack.elementalcommunity.utils

class IntArrayList(capacity: Int = 16) {

    var size = 0
    var data = IntArray(capacity)

    fun add(i: Int) {
        if (size >= data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size++] = i
    }

    fun addPair(i: Int, j: Int) {
        add(i)
        add(j)
    }

    inline fun forEachPair(run: (Int, Int) -> Unit) {
        for (i in 0 until size step 2) {
            run(data[i], data[i + 1])
        }
    }

}