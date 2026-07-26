package me.antonio.noack.elementalcommunity.history3d

object TextProgramFallback : TextProgramBase(
    TextProgram.vertexSource, """
        varying vec2 uvs;
        
        uniform vec3 textColor;
        uniform vec4 uvRange;
        uniform sampler2D textTexture;
        
        void main() {
            vec2 uv = uvs.xy * uvRange.xy + uvRange.zw;
            vec4 color = texture2D(textTexture, uv);
            
            gl_FragColor = vec4(textColor, color.g);
            if (gl_FragColor.a <= 0.01) discard;
        }
    """.trimIndent()
)