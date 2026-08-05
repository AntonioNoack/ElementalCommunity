package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.GL_COMPILE_STATUS
import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_LINK_STATUS
import android.opengl.GLES20.GL_TRUE
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glGetProgramInfoLog
import android.opengl.GLES20.glGetProgramiv
import android.opengl.GLES20.glGetShaderInfoLog
import android.opengl.GLES20.glGetShaderiv
import android.opengl.GLES20.glLinkProgram
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES20.glUseProgram
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.tmp

abstract class Program(val name: String, val vertexSource: String, val fragmentSource: String) {

    companion object {
        private const val INVALID = -1
        private const val PREFIX = "" +
                "precision mediump float;\n" +
                "precision mediump int;\n"

        val errorLog = StringBuilder()
    }

    var program = INVALID

    fun createShader(variantName: String, variantId: Int, source: String): Int {
        val shader = glCreateShader(variantId)
        glShaderSource(shader, PREFIX + source)
        glCompileShader(shader)
        glGetShaderiv(shader, GL_COMPILE_STATUS, tmp, 0)

        if (tmp[0] != GL_TRUE) {
            val msg = glGetShaderInfoLog(shader)
            synchronized(errorLog) {
                errorLog.append(name).append(":").append(variantName).append("\n")
                    .append(msg.trim()).append('\n')
            }
            return INVALID
        }
        return shader
    }

    fun create(): Boolean {
        this.program = INVALID
        val program = glCreateProgram()
        val vertexShader = createShader("vs", GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = createShader("fs", GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == INVALID || fragmentShader == INVALID) {
            return false
        }

        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)
        this.program = program

        glGetProgramiv(program, GL_LINK_STATUS, tmp, 0)
        if (tmp[0] != GL_TRUE) {
            val msg = glGetProgramInfoLog(program)
            synchronized(errorLog) {
                errorLog.append(name).append(':').append("pr").append('\n')
                    .append(msg.trim()).append('\n')
            }
            return false
        }

        init()

        return true
    }

    abstract fun init()

    fun bind() {
        glUseProgram(program)
    }
}