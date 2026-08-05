package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.glGetUniformLocation

abstract class TextProgramBase(name: String, vertexSource: String, fragmentSource: String) :
    Program(name, vertexSource, fragmentSource) {

    var pos = -1
    var size = -1
    var color = -1
    var range = -1
    var transform = -1

    override fun init() {
        pos = glGetUniformLocation(program, "pos")
        size = glGetUniformLocation(program, "size")
        range = glGetUniformLocation(program, "uvRange")
        color = glGetUniformLocation(program, "textColor")
        transform = glGetUniformLocation(program, "transform")
    }
}