package me.antonio.noack.elementalcommunity.history3d

import me.antonio.noack.elementalcommunity.history3d.HistoryView3D.Companion.cubeSize
import kotlin.math.PI

object TextProgram : TextProgramBase(
    """
        attribute vec3 position0;
        varying vec2 uvs;
        
        uniform vec4 pos;
        uniform float size;
        
        uniform mat4 transform;
        
        void main() {
            uvs = position0.xy;
            float r = position0.z * ${PI / 2};
            float u = uvs.x * size + pos.w;
            float v = uvs.y * size;
            gl_Position = transform * vec4(
                pos.x + u * cos(r) - sin(r) * ${0.51f * cubeSize}, 
                pos.y + v,
                pos.z + u * sin(r) + cos(r) * ${0.51f * cubeSize},
                1.0);
        }
    """.trimIndent(), """
        #extension GL_OES_standard_derivatives : require
        varying vec2 uvs;
        
        uniform vec3 textColor;
        uniform vec4 uvRange;
        uniform sampler2D textTexture;
        
        void main() {
            vec2 uv = uvs.xy * uvRange.xy + uvRange.zw;
            vec4 color = texture2D(textTexture, uv);
            
            gl_FragColor = vec4(textColor, color.g);
            float gradient = abs(dFdx(gl_FragColor.a)) + abs(dFdy(gl_FragColor.a)) + 0.1;
            gl_FragColor.a = 1.0 - (0.5-gl_FragColor.a) / gradient;
            if (gl_FragColor.a <= 0.001) discard;
        }
    """.trimIndent()
)