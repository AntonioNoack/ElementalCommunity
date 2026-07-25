package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER
import android.opengl.GLES20.GL_STATIC_DRAW
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBufferData
import android.opengl.GLES20.glGenBuffers
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.tmp
import java.nio.ByteBuffer
import java.nio.ByteOrder

class IntBuffer(values: IntArray) {

    val size = values.size

    private val nativeBuffer by lazy {
        ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer().apply {
                put(values); flip()
            }
    }

    private var pointer = -1

    fun create() {
        tmp[0] = -1
        glGenBuffers(1, tmp, 0)
        pointer = tmp[0]
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, pointer)
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            nativeBuffer.capacity() * 4,
            nativeBuffer,
            GL_STATIC_DRAW
        )
    }

    fun bindAsIndices() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, pointer)
    }

}