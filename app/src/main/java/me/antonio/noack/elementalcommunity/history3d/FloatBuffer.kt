package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_STATIC_DRAW
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBufferData
import android.opengl.GLES20.glDisableVertexAttribArray
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glGenBuffers
import android.opengl.GLES20.glVertexAttribPointer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FloatBuffer(values: FloatArray, val stride: Int) {

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
            ), 12
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
                // -Z
                -1f, -1f, -1f, 0f, 0f, -1f,
                +1f, -1f, -1f, 0f, 0f, -1f,
                -1f, +1f, -1f, 0f, 0f, -1f,
                +1f, +1f, -1f, 0f, 0f, -1f,

                // +Z
                -1f, -1f, 1f, 0f, 0f, 1f,
                +1f, -1f, 1f, 0f, 0f, 1f,
                -1f, +1f, 1f, 0f, 0f, 1f,
                +1f, +1f, 1f, 0f, 0f, 1f,

                // -Y
                -1f, -1f, -1f, 0f, -1f, 0f,
                +1f, -1f, -1f, 0f, -1f, 0f,
                -1f, -1f, +1f, 0f, -1f, 0f,
                +1f, -1f, +1f, 0f, -1f, 0f,

                // +Y
                -1f, 1f, -1f, 0f, 1f, 0f,
                +1f, 1f, -1f, 0f, 1f, 0f,
                -1f, 1f, +1f, 0f, 1f, 0f,
                +1f, 1f, +1f, 0f, 1f, 0f,

                // -X
                -1f, -1f, -1f, -1f, 0f, 0f,
                -1f, +1f, -1f, -1f, 0f, 0f,
                -1f, -1f, +1f, -1f, 0f, 0f,
                -1f, +1f, +1f, -1f, 0f, 0f,

                // +X
                1f, -1f, -1f, 1f, 0f, 0f,
                1f, +1f, -1f, 1f, 0f, 0f,
                1f, -1f, +1f, 1f, 0f, 0f,
                1f, +1f, +1f, 1f, 0f, 0f,
            ), 24
        )

        val cubeIndices = IntBuffer(
            intArrayOf(
                0, 3, 1, 0, 2, 3, // -Z
                4, 5, 7, 6, 4, 7, // +Z
                8, 9, 11, 8, 11,  10,// -Y
                12, 15, 13, 12, 14, 15, // +Y
                16, 19, 17, 16, 18, 19, // -X
                20, 21, 23, 22, 20, 23, // +X
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
        bindAsPosNor(0, -1)
    }

    fun bindAsPosNor(pos: Int, nor: Int) {
        check(pointer >= 0)
        glBindBuffer(GL_ARRAY_BUFFER, pointer)
        if (pos >= 0) {
            glVertexAttribPointer(pos, 3, GL_FLOAT, false, stride, 0)
            glEnableVertexAttribArray(pos)
        }
        if (nor >= 0) {
            val offset = if (stride > 12) 12 else 0
            glVertexAttribPointer(nor, 3, GL_FLOAT, false, stride, offset)
            glEnableVertexAttribArray(nor)
        }
        if (pos != 0 && nor != 0) glDisableVertexAttribArray(0)
        if (pos != 1 && nor != 1) glDisableVertexAttribArray(1)
    }

}