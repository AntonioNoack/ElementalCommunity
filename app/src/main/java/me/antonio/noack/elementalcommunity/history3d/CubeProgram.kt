package me.antonio.noack.elementalcommunity.history3d

import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import kotlin.math.PI

object CubeProgram : Program(
    "cube", """
        attribute vec3 pos;
        attribute vec3 nor;
        
        varying vec3 position;
        varying vec3 normal;
        
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
            normal = rotY(nor, rotationY);
            gl_Position = transform * vec4(position, 1.0);
        }
    """.trimIndent(), """
        varying vec3 position;
        varying vec3 normal;
        
        uniform vec3 relPos;
        uniform vec3 cubeSize;
        uniform vec4 color;
        uniform float metallic;
        uniform float rotationY;
        uniform sampler2D skyTexture;
        
        vec3 sampleSky(vec3 dir) {
            float uvx = atan(+dir.z, dir.x);
            float uvy = atan(-dir.y, length(dir.xz));
            vec2 uv = vec2(uvx, uvy) * ${0.5 / PI} + 0.5;
            return texture2D(skyTexture, uv).rgb;
        }
        
        vec3 rotY(vec3 pos, float angle) {
            float c = cos(angle), s = sin(angle);
            pos.xz *= mat2(c,s,-s,c);
            return pos;
        }
        
        void main() {
            vec3 diffuse = mix(vec3(0.3,0.4,0.4), vec3(1.0), vec3(normal.x * 0.3 + normal.y * 0.2 + 0.5));
            vec3 viewDir = normalize(position);
            vec3 specular = sampleSky(reflect(normal, viewDir));
            
            vec3 light = mix(diffuse, specular, vec3(metallic));
            gl_FragColor = vec4(light * color.rgb, color.a);
        }
    """.trimIndent()
) {

    var color = -1
    var pos = -1
    var size = -1
    var transform = -1
    var rotation = -1
    var metallic = -1

    var attrPos = -1
    var attrNor = -1

    override fun init() {

        attrPos = glGetAttribLocation(program, "pos")
        attrNor = glGetAttribLocation(program, "nor")

        color = glGetUniformLocation(program, "color")
        pos = glGetUniformLocation(program, "relPos")
        size = glGetUniformLocation(program, "cubeSize")
        transform = glGetUniformLocation(program, "transform")
        rotation = glGetUniformLocation(program, "rotationY")
        metallic = glGetUniformLocation(program, "metallic")

        bind()
        glUniform1i(glGetUniformLocation(program, "skyTexture"), 0)
    }
}