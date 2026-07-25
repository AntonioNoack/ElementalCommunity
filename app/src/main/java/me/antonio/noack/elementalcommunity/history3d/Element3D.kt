package me.antonio.noack.elementalcommunity.history3d

data class Element3D(
    val name: String, val groupId: Int,
    val x: Int, val y: Int, val z: Int,
    val parentAX: Int, val parentAY: Int, val parentAZ: Int,
    val parentBX: Int, val parentBY: Int, val parentBZ: Int,
) {
    constructor(name: String, groupId: Int, x: Int, y: Int, z: Int) :
            this(name, groupId, x, y, z, x, y, z, x, y, z)
}