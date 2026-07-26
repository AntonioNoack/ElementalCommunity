package me.antonio.noack.elementalcommunity.history3d

import android.graphics.BitmapFactory
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR
import android.opengl.GLES20.GL_REPEAT
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glGenerateMipmap
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLUtils
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.tmp

class Texture2D(val resource: Int, val mipmap: Boolean) {

    private var pointer = -1

    fun create(all: AllManager) {
        val options = BitmapFactory.Options()
        options.inScaled = false // Prevent Android from scaling the image

        val bitmap = BitmapFactory.decodeResource(
            all.getResources(),
            resource,
            options
        )

        glGenTextures(1, tmp, 0)
        glBindTexture(GL_TEXTURE_2D, tmp[0])

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

        if (mipmap) {
            glGenerateMipmap(GL_TEXTURE_2D)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        bitmap.recycle()

        pointer = tmp[0]
    }

    fun bind(slot: Int) {
        if (false) glActiveTexture(GL_TEXTURE0 + slot)
        glBindTexture(GL_TEXTURE_2D, pointer)
    }
}
