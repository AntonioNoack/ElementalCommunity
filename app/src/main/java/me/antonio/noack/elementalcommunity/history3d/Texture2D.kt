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
import androidx.core.graphics.get
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.tmp
import kotlin.math.max

class Texture2D(val resource: Int) {

    private var pointer = -1

    fun create(all: AllManager, charWidth: IntArray? = null) {
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
        glGenerateMipmap(GL_TEXTURE_2D)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        if (charWidth != null) {
            val nx = 10
            val ny = 10
            val minAllowedWidth = bitmap.width / nx / 4
            charWidth.fill(minAllowedWidth)
            for (yi in 0 until ny) {
                val y0 = ((yi + 0.25f) * bitmap.height / ny).toInt()
                val y1 = ((yi + 0.75f) * bitmap.height / ny).toInt()
                for (xi in 0 until nx) {
                    var maxFoundWidth = minAllowedWidth
                    for (y in y0 until y1) {
                        var x0 = xi * bitmap.width / nx
                        var x1 = (xi + 1) * bitmap.width / nx
                        while (x0 < x1 && bitmap[x0, y].shr(8).and(255) < 100) x0++
                        while (x0 < x1 && bitmap[x0, y].shr(8).and(255) < 100) x1--
                        if (x0 < x1) {
                            maxFoundWidth = max(maxFoundWidth, x1 - x0)
                        }
                    }
                    charWidth[xi + yi * ny] = maxFoundWidth
                    // println("CharWidth[${32 + xi + yi * ny}] = $maxFoundWidth")
                }
            }
        }

        bitmap.recycle()

        pointer = tmp[0]
    }

    fun bind(slot: Int) {
        if (false) glActiveTexture(GL_TEXTURE0 + slot)
        glBindTexture(GL_TEXTURE_2D, pointer)
    }
}
