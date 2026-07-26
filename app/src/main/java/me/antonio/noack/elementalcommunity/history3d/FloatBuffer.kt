package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_STATIC_DRAW
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBufferData
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glGenBuffers
import android.opengl.GLES20.glVertexAttribPointer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FloatBuffer(values: FloatArray) {

    companion object {
        val tmp = IntArray(1)

        val flatPositions = FloatBuffer(
            floatArrayOf(
                -1f, -1f, 0f,
                +1f, -1f, 0f,
                -1f, +1f, 0f,
                +1f, +1f, 0f,

                -1f, -1f, 1f,
                +1f, -1f, 1f,
                -1f, +1f, 1f,
                +1f, +1f, 1f,

                -1f, -1f, 2f,
                +1f, -1f, 2f,
                -1f, +1f, 2f,
                +1f, +1f, 2f,

                -1f, -1f, 3f,
                +1f, -1f, 3f,
                -1f, +1f, 3f,
                +1f, +1f, 3f,
            )
        )

        val flatIndices = IntBuffer(
            intArrayOf(
                0, 1, 3, 0, 3, 2,
                4, 5, 7, 4, 7, 6,
                8, 9, 11, 8, 11, 10,
                12, 13, 15, 12, 15, 14,
            )
        )

        val cubePositions = FloatBuffer(
            floatArrayOf(
                -1f, -1f, -1f,
                1f, -1f, -1f,
                -1f, 1f, -1f,
                1f, 1f, -1f,

                -1f, -1f, +1f,
                1f, -1f, +1f,
                -1f, 1f, +1f,
                1f, 1f, +1f,
            )
        )

        val cubeIndices = IntBuffer(
            intArrayOf(
                0, 3, 1, 0, 2, 3, // +1, +2
                4, 5, 7, 6, 4, 7, // all +4

                0, 1, 5, 4, 0, 5, // +1, +4
                2, 7, 3, 2, 6, 7, // all +2

                0, 6, 2, 0, 4, 6, // +2, +4
                1, 3, 7, 5, 1, 7, // all +1
            )
        )

    }

    val size = values.size

    var pointer = -1

    private val nativeBuffer by lazy {
        ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(values); flip()
            }
    }

    fun create() {
        tmp[0] = -1
        glGenBuffers(1, tmp, 0)
        pointer = tmp[0]
        glBindBuffer(GL_ARRAY_BUFFER, pointer)
        glBufferData(GL_ARRAY_BUFFER, nativeBuffer.capacity() * 4, nativeBuffer, GL_STATIC_DRAW)
    }

    fun bindAsPositions() {
        check(pointer >= 0)
        glBindBuffer(GL_ARRAY_BUFFER, pointer)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 12, 0)
        glEnableVertexAttribArray(0)
    }

}