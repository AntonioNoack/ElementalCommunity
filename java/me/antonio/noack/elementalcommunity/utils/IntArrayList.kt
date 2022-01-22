package me.antonio.noack.elementalcommunity.utils

class IntArrayList(capacity: Int = 16) {

    var size = 0
    var data = IntArray(capacity)

    fun add(i: Int) {
        if (size >= data.size) {
            val data = IntArray(data.size * 2)
            System.arraycopy(this.data, 0, data, 0, size)
            this.data = data
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