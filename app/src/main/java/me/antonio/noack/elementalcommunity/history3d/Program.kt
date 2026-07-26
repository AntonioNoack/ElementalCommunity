package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_LINK_STATUS
import android.opengl.GLES20.GL_TRUE
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glGetProgramiv
import android.opengl.GLES20.glLinkProgram
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES20.glUseProgram
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.tmp

abstract class Program(val vertexSource: String, val fragmentSource: String) {

    var program = -1

    fun create(): Boolean {
        this.program = -1
        val program = glCreateProgram()
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, vertexSource)
        glCompileShader(vertexShader)

        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, fragmentSource)
        glCompileShader(fragmentShader)

        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)
        this.program = program

        glGetProgramiv(program, GL_LINK_STATUS, tmp, 0)
        val isOK = tmp[0] == GL_TRUE
        if (!isOK) return false

        init()

        return true
    }

    abstract fun init()

    fun bind() {
        glUseProgram(program)
    }
}