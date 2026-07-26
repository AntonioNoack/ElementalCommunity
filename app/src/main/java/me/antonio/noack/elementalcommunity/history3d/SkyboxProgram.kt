package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.glGetUniformLocation
import kotlin.math.PI

object SkyboxProgram : Program(
    """
        attribute vec3 pos;
        varying vec3 position;
        
        uniform mat4 transform;
        
        void main() {
            gl_Position = transform * vec4(pos,1.0);
            position = pos;
        }
    """.trimIndent(), """
        varying vec3 position;
        
        uniform sampler2D skyTexture;
        
        void main() {
            float uvx = atan(+position.z, position.x);
            float uvy = atan(-position.y, length(position.xz));
            vec2 uv = vec2(uvx, uvy) * ${0.5 / PI} + 0.5;
            gl_FragColor = texture2D(skyTexture, uv);
        }
    """.trimIndent()
) {
    var transform = -1
    override fun init() {
        transform = glGetUniformLocation(program, "transform")
    }
}