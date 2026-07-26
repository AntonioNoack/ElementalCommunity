package me.antonio.noack.elementalcommunity.history3d

data class Element3D(
    val name: String, val groupId: Int, val z: Int,
    val parentAX: Int, val parentAY: Int, val parentAZ: Int,
    val parentBX: Int, val parentBY: Int, val parentBZ: Int,
    val timestampMinutes: Int,
) {

    val x get() = (parentAX + parentBX).shr(1)
    val y get() = (parentAY + parentBY).shr(1)

    constructor(name: String, groupId: Int, x: Int, y: Int, z: Int) :
            this(name, groupId, z, x, y, z, x, y, z, 0)
}