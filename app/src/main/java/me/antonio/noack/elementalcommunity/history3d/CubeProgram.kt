package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import kotlin.math.PI

object CubeProgram : Program(
    """
        #version 300 es
        
         in vec3 pos;
        out vec3 position;
        
        uniform float rotationY;
        uniform vec3 relPos;
        uniform vec3 cubeSize;
        uniform mat4 transform;
        
        vec3 rotY(vec3 pos, float angle) {
            float c = cos(angle), s = sin(angle);
            pos.xz *= mat2(c,s,-s,c);
            return pos;
        }
        
        void main() {
            position = rotY(pos * cubeSize, rotationY) + relPos;
            gl_Position = transform * vec4(position, 1.0);
        }
    """.trimIndent(), """
        #version 300 es
        
         in vec3 position;
        out vec4 result;
        
        uniform vec3 relPos;
        uniform vec3 cubeSize;
        uniform vec4 color;
        uniform float metallic;
        uniform sampler2D skyTexture;
        
        vec3 sampleSky(vec3 dir, float lod) {
            float uvx = atan(+dir.z, dir.x);
            float uvy = atan(-dir.y, length(dir.xz));
            vec2 uv = vec2(uvx, uvy) * ${0.5 / PI} + 0.5;
            return textureLod(skyTexture, uv, lod).rgb;
        }
        
        void main() {
            vec3 normal = cross(dFdx(position), dFdy(position));
            normal = normalize(normal);
            
            vec3 diffuse = mix(vec3(0.3,0.4,0.4), vec3(1.0), vec3(normal.x * 0.3 + normal.y * 0.2 + 0.5));
            vec3 viewDir = normalize(position);
            vec3 specular = sampleSky(reflect(normal, viewDir), 0.0);
            
            vec3 light = mix(diffuse, specular, vec3(metallic));
            result = vec4(light * color.rgb, color.a);
        }
    """.trimIndent()
) {

    var color = -1
    var pos = -1
    var size = -1
    var transform = -1
    var rotation = -1
    var metallic = -1

    override fun init() {
        color = glGetUniformLocation(program, "color")
        pos = glGetUniformLocation(program, "relPos")
        size = glGetUniformLocation(program, "cubeSize")
        transform = glGetUniformLocation(program, "transform")
        rotation = glGetUniformLocation(program, "rotationY")
        metallic = glGetUniformLocation(program, "metallic")

        glUniform1i(glGetUniformLocation(program, "skyTexture"), 0)
    }
}