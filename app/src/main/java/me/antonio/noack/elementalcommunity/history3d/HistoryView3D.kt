package me.antonio.noack.elementalcommunity.history3d

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20.GL_BACK
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_CULL_FACE
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_FUNC_ADD
import android.opengl.GLES20.GL_LESS
import android.opengl.GLES20.GL_NO_ERROR
import android.opengl.GLES20.GL_ONE
import android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA
import android.opengl.GLES20.GL_SRC_ALPHA
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glBlendEquation
import android.opengl.GLES20.glBlendFuncSeparate
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearDepthf
import android.opengl.GLES20.glCullFace
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glDrawElements
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glGetError
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform3f
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.ScaleGestureDetector
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.GroupsEtc
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.cubeIndices
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.cubePositions
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.flatIndices
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.flatPositions
import java.lang.StrictMath.clamp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// todo plan:
//  we have an API, that gives us the 3d positions for each element,
//  sorted by ID...
//  and we query a range of IDs...
//  and display all until time X,
//  and we add a little animation for when an element is appearing
//  and we should add a minimum distance of 1..., and a maximum distance of 10
//  our RaspberryPi must compute everything
//  one unit = 1h?
//  and it should store the data minimally:
//   index -> (id: u32, x: 0-99, y: 0-99, z: 0-u32)

@SuppressLint("ClickableViewAccessibility")
class HistoryView3D(ctx: Context, attributeSet: AttributeSet?) :
    GLSurfaceView(ctx, attributeSet), GLSurfaceView.Renderer {

    companion object {
        private val tmp16 = run {
            ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }

        private val tmpM = Matrix4f()

        fun checkErrors() {
            val error = glGetError()
            if (error != GL_NO_ERROR) {
                RuntimeException("OpenGL Error: $error")
                    .printStackTrace()
            }
        }

        private const val INV255 = 1f / 255f
        private fun Int.r01() = shr(16).and(255) * INV255
        private fun Int.g01() = shr(8).and(255) * INV255
        private fun Int.b01() = and(255) * INV255

        private val cubeSize = 30f
        private val charWidth = IntArray(100)
    }

    fun init(allManager: AllManager) {
        this.all = allManager

        val activityManager = all.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val version = configurationInfo.reqGlEsVersion
        val major = version.shr(16)
        val minor = version.and(0xffff)

        // val version10x = major * 10 + min(minor, 9)
        // GFXFeatures.supportsShaderStorageBuffers = version10x >= 31
        // GFXFeatures.supportsTextureGather = version10x >= 32

        // GL11.setVersion(major, minor)
        println("OpenGL ES Version: $major.$minor")

        // Request an OpenGL ES 2.0 compatible context.
        setEGLContextClientVersion(major)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY//RENDERMODE_WHEN_DIRTY

        setListeners()
    }


    lateinit var all: AllManager

    private var width = 1
    private var height = 1

    private val min = -99
    private val max = 99
    private val center = (min + max) * 0.5f

    private val testElements = listOf(
        Element3D("Earth", 5, min, max, 0),
        Element3D("Air", 12, min, min, 0),
        Element3D("Water", 20, max, min, 0),
        Element3D("Fire", 4, max, max, 0),
    )

    private val fontImage = Texture2D(R.drawable.font)
    private val skyboxImage = Texture2D(R.drawable.skybox)

    private val skyboxProgram = Program(
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
    )

    private var skyboxTransform = -1
    private var cubeTransform = -1
    private var cubeColor = -1
    private var cubeRelPos = -1

    private val cubeProgram = Program(
        """
        attribute vec3 pos;
        varying vec3 position;
        
        uniform mat4 transform;
        
        void main() {
            gl_Position = transform * vec4(pos,1.0);
            position = pos;
        }
    """.trimIndent(), """
        #extension GL_OES_standard_derivatives : require
        
        varying vec3 position;
        
        uniform vec3 color;
        uniform vec3 relPos;
        uniform sampler2D skyTexture;
        
        vec3 sampleSky(vec3 dir) {
            float uvx = atan(+dir.z, dir.x);
            float uvy = atan(-dir.y, length(dir.xz));
            vec2 uv = vec2(uvx, uvy) * ${0.5 / PI} + 0.5;
            return texture2D(skyTexture, uv).rgb;
        }
        
        void main() {
            vec3 normal = cross(dFdx(position), dFdy(position));
            normal = normalize(normal);
            vec3 diffuse = sampleSky(normal);
            vec3 viewDir = normalize(relPos + position * $cubeSize);
            vec3 specular = sampleSky(reflect(normal, viewDir));
            
            vec3 absPos = abs(position);
            float metallic = 
                (
                absPos.x > 0.5 ? max(absPos.y,absPos.z) :
                absPos.y > 0.5 ? max(absPos.x,absPos.z) :
                                 max(absPos.x,absPos.y)
                ) > 0.8 ? 0.9 : 0.1;
            
            vec3 light = mix(diffuse, specular, vec3(metallic));
            gl_FragColor = vec4(color * light, 1.0);
        }
    """.trimIndent()
    )

    private var textTransform = -1
    private var textUVRange = -1
    private var textColor = -1

    private val textProgram = Program(
        """
        attribute vec3 pos;
        varying vec3 position;
        
        uniform mat4 transform;
        
        void main() {
            gl_Position = transform * vec4(pos,1.0);
            position = pos;
        }
    """.trimIndent(), """
        #extension GL_OES_standard_derivatives : require
        varying vec3 position;
        
        uniform vec3 textColor;
        uniform vec4 uvRange;
        uniform sampler2D textTexture;
        
        void main() {
            vec3 normal = cross(dFdx(position), dFdy(position));
            normal = normalize(normal);
            vec2 uv = position.xy * uvRange.xy + uvRange.zw;
            vec4 color = texture2D(textTexture, uv);
            
            gl_FragColor = vec4(textColor, color.g);
            float gradient = abs(dFdx(gl_FragColor.a)) + abs(dFdy(gl_FragColor.a)) + 0.1;
            gl_FragColor.a = 1.0 - (0.5-gl_FragColor.a) / gradient;
            if (gl_FragColor.a <= 0.001) discard;
        }
    """.trimIndent()
    )

    // todo render time
    // todo render timeline??? with zoom and scroll??

    fun getElement(index: Int): Element3D? {
        // todo cache, and will it using web-requests
        return testElements.getOrNull(index)
    }

    var animationTime = 0.0
    var lastDown = 0L

    var rotY = 0f
    var rotX = 0f

    var far = 1000f
    var radius = 500f

    private var lastTime = 0L

    private var lastMinElementId = 0

    val cameraMatrix = Matrix4f()
    val transform = Matrix4f()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // todo create all programs and textures:
        //  - background program
        //  - flat color program incl. cube animation
        //  - background texture
        //  - font texture
        skyboxProgram.create()
        textProgram.create()
        cubeProgram.create()
        checkErrors()

        cubePositions.create()
        cubeIndices.create()
        flatPositions.create()
        flatIndices.create()
        checkErrors()

        skyboxTransform = glGetUniformLocation(skyboxProgram.program, "transform")
        textTransform = glGetUniformLocation(textProgram.program, "transform")
        textUVRange = glGetUniformLocation(textProgram.program, "uvRange")
        textColor = glGetUniformLocation(textProgram.program, "textColor")
        cubeTransform = glGetUniformLocation(cubeProgram.program, "transform")
        cubeColor = glGetUniformLocation(cubeProgram.program, "color")
        cubeRelPos = glGetUniformLocation(cubeProgram.program, "relPos")
        println("cube-transform, flat-transform: $skyboxTransform, $textTransform")

        checkErrors()

        val all = all
        fontImage.create(all, charWidth)
        skyboxImage.create(all)
        checkErrors()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = max(width, 1)
        this.height = max(height, 1)
    }

    override fun onDrawFrame(gl: GL10?) {
        updateTime()
        calculateCameraMatrix()

        glDisable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        drawBackgroundCube() // depth is written without test

        cameraMatrix
            .translate(-cameraPos[0], -cameraPos[1], -cameraPos[2])

        glEnable(GL_CULL_FACE)
        glDepthFunc(GL_LESS)

        glClearDepthf(1f)
        glClear(GL_DEPTH_BUFFER_BIT)
        glEnable(GL_DEPTH_TEST)

        drawElementCubes()

        glEnable(GL_BLEND)
        glBlendFuncSeparate(
            GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
            GL_ONE, GL_ONE_MINUS_SRC_ALPHA
        )
        glBlendEquation(GL_FUNC_ADD)
        drawElementNames()

        checkErrors()
    }

    private var autoSpeed = 0f
    private fun updateTime() {
        val time = System.nanoTime()
        val dt = min(abs(time - lastTime) * 1e-9, 0.1)
        animationTime += dt

        if (abs(time - lastDown) > 2e9) {
            rotY = ((rotY + dt * autoSpeed) % (PI * 2)).toFloat()
            autoSpeed += (1f - autoSpeed) * dt.toFloat()
        } else autoSpeed = 0f

        lastTime = time
    }

    private fun calculateCameraMatrix() {
        cameraMatrix
            .identity()
            .perspective(
                130f * (PI.toFloat() / 180), width.toFloat() / height,
                far * 0.01f, far, zZeroToOne = false
            )
            .rotateX(-rotX)
            .rotateY(-rotY)
    }

    private fun drawBackgroundCube() {
        cubePositions.bindAsPositions()
        cubeIndices.bindAsIndices()

        val shader = skyboxProgram
        shader.bind()
        checkErrors()

        val scale = far * 0.6f
        transform.identity()
            .scale(scale, scale, scale) // large to clear depth

        cameraMatrix.mul(transform, tmpM).fillInto(tmp16)
        glUniformMatrix4fv(skyboxTransform, 1, false, tmp16)

        skyboxImage.bind(0)

        glDrawElements(GL_TRIANGLES, cubeIndices.size, GL_UNSIGNED_INT, 0)
        checkErrors()
    }

    private fun drawElementCubes() {

        // cube mesh and skybox texture are already bound
        val shader = cubeProgram
        shader.bind()
        checkErrors()

        val vx = -cos(-rotX) * sin(rotY)
        val vy = -sin(-rotX)
        val vz = -cos(-rotX) * cos(rotY)

        cameraPos[0] = center - vx * radius
        cameraPos[1] = -vy * radius
        cameraPos[2] = center - vz * radius

        val camZ = animationTime * zSpeed
        for (index in lastMinElementId until Int.MAX_VALUE) {
            drawElementCube(index, getElement(index) ?: break, camZ)
        }
    }

    private fun drawElementNames() {

        flatPositions.bindAsPositions()
        flatIndices.bindAsIndices()

        textProgram.bind()
        fontImage.bind(0)

        val camZ = animationTime * zSpeed
        for (index in lastMinElementId until Int.MAX_VALUE) {
            drawElementName(getElement(index) ?: break, camZ)
        }
    }

    private val cameraPos = FloatArray(3)

    private fun drawElementCube(i: Int, element: Element3D, camZ: Double) {
        if (element.z + tooLow < camZ) {
            lastMinElementId = max(lastMinElementId, i)
            return
        }

        val scale = cubeSize * 0.5f
        val dx = element.x.toFloat()
        val dy = (element.z - camZ).toFloat()
        val dz = element.y.toFloat()
        transform.identity()
            .translate(dx, dy, dz)
            .scale(scale, scale, scale) // large to clear depth

        if (cubeRelPos != -1) glUniform3f(
            cubeRelPos,
            dx - cameraPos[0],
            dy - cameraPos[1],
            dz - cameraPos[2]
        )

        cameraMatrix.mul(transform, tmpM).fillInto(tmp16)
        glUniformMatrix4fv(cubeTransform, 1, false, tmp16)

        val colors = GroupsEtc.GroupColors
        val color = colors[clamp(element.groupId, 0, colors.lastIndex)]
        glUniform3f(cubeColor, color.r01(), color.g01(), color.b01())

        glDrawElements(GL_TRIANGLES, cubeIndices.size, GL_UNSIGNED_INT, 0)
        checkErrors()

        // todo draw connection from parent cubes
    }

    private fun drawElementName(element: Element3D, camZ: Double) {
        if (element.z + tooLow < camZ) return

        // draw name on 4 sides (todo only 2 visible -> only on those)
        val name = element.name

        val dx = element.x.toFloat()
        val dy = (element.z - camZ).toFloat()
        val dz = element.y.toFloat()

        // todo we need line-breaks like when drawing elements...

        var ki0 = 0f
        for (i in name.indices) {
            val char = name[i]
            val code = char.code - 32
            ki0 += charWidth[code]
        }

        val textSize = 70f / ki0

        val offset = 0.503f * cubeSize
        for (k in 0..3) {
            var ki = -ki0
            for (i in name.indices) {
                val char = name[i]
                if (char.isWhitespace()) continue

                val code = char.code - 32
                val cw = charWidth[code]
                ki += cw

                val scale = cubeSize * 0.5f * textSize
                transform.identity()
                    .translate(dx, dy, dz)
                    .rotateY(k * (PI * 0.5).toFloat())
                    .translate(ki * 0.004f * textSize * cubeSize, 0f, offset)
                    .scale(scale, scale, scale) // large to clear depth

                ki += cw

                val xi = code % 10
                val yi = code / 10

                cameraMatrix.mul(transform, tmpM).fillInto(tmp16)
                glUniformMatrix4fv(textTransform, 1, false, tmp16)
                glUniform4f(textUVRange, 0.025f, -0.025f, xi * 0.1f + 0.05f, yi * 0.1f + 0.05f)


                val colors = GroupsEtc.GroupColors
                val color = colors[clamp(element.groupId, 0, colors.lastIndex)]
                val textColorI = if (color.r01() + color.g01() + color.b01() > 1f) 0f else 1f
                glUniform3f(textColor, textColorI, textColorI, textColorI)

                glDrawElements(GL_TRIANGLES, flatIndices.size, GL_UNSIGNED_INT, 0)
                checkErrors()
            }
        }
    }

    val zSpeed = 0.0
    val tooLow = 20

    fun setListeners() {

        val scaleDetector = ScaleGestureDetector(
            context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    radius = clamp(radius / detector.scaleFactor, cubeSize * 2f, 2000f)
                    far = radius * 2f + (max - min)
                    invalidate()
                    return true
                }
            })

        var lastX = 0f
        var lastY = 0f
        var disableMove = false

        setOnTouchListener { _, event ->

            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    disableMove = false
                }

                ACTION_MOVE -> {
                    if (!disableMove && !scaleDetector.isInProgress) {
                        val speed = 10f / max(width, height)
                        val dx = (event.x - lastX) * speed
                        val dy = (event.y - lastY) * speed

                        rotY = (rotY + dx) % (PI * 2).toFloat()
                        rotX = clamp(
                            rotX + dy,
                            (-PI / 2).toFloat(),
                            (PI / 2).toFloat()
                        )

                        invalidate()
                    } else disableMove = true

                    lastX = event.x
                    lastY = event.y
                    lastDown = System.nanoTime()
                }
            }

            true
        }
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        return if (x < min) min else if (x > max) max else x
    }
}