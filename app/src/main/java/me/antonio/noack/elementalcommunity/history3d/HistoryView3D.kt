package me.antonio.noack.elementalcommunity.history3d

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Paint
import android.opengl.GLES20.GL_BACK
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_CULL_FACE
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_FUNC_ADD
import android.opengl.GLES20.GL_LEQUAL
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
import android.opengl.GLES20.glUniform1f
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
import me.antonio.noack.elementalcommunity.GroupsEtc.getCacheEntry
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.history3d.ElementHistoryCache.getElement
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.cubeIndices
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.cubePositions
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.flatIndices
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.flatPositions
import me.antonio.noack.elementalcommunity.utils.Maths.mix
import java.lang.StrictMath.clamp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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
        private fun Int.a01() = shr(24).and(255) * INV255

        val cubeSize = 15f

        private val animTimeX = 0.3f
        private val maxAnimTime = 30f

        private val charWidth0 = 64

        val min = 0
        val max = 99
        val center = (min + max) * 0.5f

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

    private val fontImage = Texture2D(R.drawable.font)
    private val skyboxImage = Texture2D(R.drawable.skybox)

    // todo render time
    // todo render timeline??? with zoom and scroll??

    var animationTime = 0.0
    var lastDown = 0L

    var rotY = 0f
    var rotX = 0f

    var radius = 200f
    val far get() = radius * 2f + 7f * (max - min)

    private var lastTime = 0L

    private var lastMinElementId = 0

    val cameraMatrix = Matrix4f()
    val transform = Matrix4f()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        SkyboxProgram.create()
        TextProgram.create()
        CubeProgram.create()
        checkErrors()

        cubePositions.create()
        cubeIndices.create()
        flatPositions.create()
        flatIndices.create()
        checkErrors()

        val all = all
        fontImage.create(all)
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

        glEnable(GL_CULL_FACE)
        glDepthFunc(GL_LEQUAL)

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
            rotY = ((rotY + 0.3f * dt * autoSpeed) % (PI * 2)).toFloat()
            autoSpeed += (1f - autoSpeed) * dt.toFloat()
        } else autoSpeed = 0f

        lastTime = time
    }

    private fun calculateCameraMatrix() {
        cameraMatrix
            .identity()
            .perspective(
                90f * (PI.toFloat() / 180), width.toFloat() / height,
                far * 0.01f, far, zZeroToOne = false
            )
            .rotateX(-rotX)
            .rotateY(-rotY)
    }

    private fun drawBackgroundCube() {
        cubePositions.bindAsPositions()
        cubeIndices.bindAsIndices()

        val shader = SkyboxProgram
        shader.bind()
        checkErrors()

        val scale = far * 0.6f
        transform.identity()
            .scale(scale, scale, scale) // large to clear depth

        cameraMatrix.mul(transform, tmpM).fillInto(tmp16)
        glUniformMatrix4fv(shader.transform, 1, false, tmp16)

        skyboxImage.bind(0)

        glDrawElements(GL_TRIANGLES, cubeIndices.size, GL_UNSIGNED_INT, 0)
        checkErrors()
    }

    private fun drawElementCubes() {

        // cube mesh and skybox texture are already bound
        val shader = CubeProgram
        shader.bind()
        checkErrors()

        val vx = cos(-rotX) * sin(rotY)
        val vy = sin(-rotX)
        val vz = cos(-rotX) * cos(rotY)

        cameraPos[0] = vx * radius + center
        cameraPos[1] = vy * radius
        cameraPos[2] = vz * radius + center

        cameraMatrix.fillInto(tmp16)
        glUniformMatrix4fv(shader.transform, 1, false, tmp16)

        val camZ = animationTime * zSpeed
        drawBase(camZ)

        glUniform1f(shader.metallic, 0.5f)

        var maxZ = 0
        for (index in lastMinElementId until Int.MAX_VALUE) {
            val element = getElement(index) ?: break
            drawElementCube(index, element, camZ)
            maxZ = element.z
        }
        animationTime = min(animationTime, (maxZ + 5) / zSpeed)
    }

    private fun drawBase(camZ: Double) {
        glUniform1f(CubeProgram.metallic, 0f)
        glEnable(GL_BLEND)

        val scale = 10f
        for (z in 0 until 16) {
            drawBaseLayer(camZ, z, scale)
        }
        glDisable(GL_BLEND)
    }

    private fun drawBaseLayer(camZ: Double, z: Int, scale: Float) {
        val sx = 0.5f * (max - min)
        val scaleZ = sx * (1.4f + 0.2f * z + 0.02f * z * z)
        drawCube(
            center, -(camZ.toFloat() + (z * 2 + 2) * scale), center,
            scaleZ, scale, scaleZ,
            0x11111111 * max(15 - z, 1), 0f
        )
    }

    private fun drawElementNames() {

        flatPositions.bindAsPositions()
        flatIndices.bindAsIndices()

        val shader = TextProgram
        shader.bind()

        // cameraMatrix.fillInto(tmp16) // already done
        glUniformMatrix4fv(shader.transform, 1, false, tmp16)

        fontImage.bind(0)

        val camZ = animationTime * zSpeed
        for (index in lastMinElementId until Int.MAX_VALUE) {
            drawElementName(getElement(index) ?: break, camZ)
        }
    }

    private val cameraPos = FloatArray(3)

    private fun drawCube(
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Int, rotation: Float
    ) {
        val shader = CubeProgram
        glUniform3f(
            shader.pos,
            px - cameraPos[0],
            py - cameraPos[1],
            pz - cameraPos[2]
        )
        glUniform3f(shader.size, sx, sy, sz)
        glUniform4f(shader.color, color.r01(), color.g01(), color.b01(), color.a01())
        glUniform1f(shader.rotation, rotation)

        glDrawElements(GL_TRIANGLES, cubeIndices.size, GL_UNSIGNED_INT, 0)
    }

    private fun drawElementCube(i: Int, element: Element3D, camZ: Double) {
        if (element.z + tooLow < camZ) {
            lastMinElementId = max(lastMinElementId, i)
            return
        }

        if (element.name.isEmpty()) return

        val colors = GroupsEtc.GroupColors
        val color = colors[clamp(element.groupId, 0, colors.lastIndex)]
        val scale = cubeSize * 0.5f

        val px = element.x.toFloat()
        val py = (element.z - camZ).toFloat()
        val pz = element.y.toFloat()

        val deltaTime = (element.z - camZ).toFloat()
        val scale1 = scale * min(-deltaTime, 1f)
        if (scale1 > 0f) {
            drawCube(px, py, pz, scale1, scale1, scale1, color, 0f)
        }

        val dx = (element.parentAX - element.parentBX).toFloat()
        val dz = (element.parentAY - element.parentBY).toFloat()

        val scaleX = scale * 0.1f

        if (dx != 0f || dz != 0f) {

            val middleZ = (element.z + max(element.parentAZ, element.parentBZ)) * 0.5f
            if (element.z == element.parentAZ && element.z == element.parentBZ) return

            val barLength = hypot(dx, dz)
            val rotation = atan2(dx, dz)
            val topLength = abs(element.z - middleZ)
            val legLength0 = abs(middleZ - element.parentAZ)
            val legLength1 = abs(middleZ - element.parentBZ)

            val totalLength = barLength + topLength + max(legLength0, legLength1)
            val animTime = min(totalLength * animTimeX, maxAnimTime)
            if (deltaTime >= animTime) return // not good enough

            val topAnimTime = animTime * topLength / totalLength
            val topGrowth = 1f - max(deltaTime / topAnimTime, 0f)
            if (topGrowth > 0f) {
                drawCube(// top-middle
                    px, (mix(middleZ, element.z.toFloat(), 0.5f * topGrowth) - camZ).toFloat(), pz,
                    scaleX, topGrowth * topLength * 0.5f, scaleX, color, rotation
                )
            }

            val barAnimTime = animTime * barLength / totalLength
            val barGrowth = 1f - max((deltaTime - topAnimTime) / barAnimTime, 0f)
            if (barGrowth > 0f) {
                if (barGrowth < 1f) {
                    // grow from both sides
                    val mix0 = barGrowth * 0.25f
                    val mix1 = 1f - mix0
                    val lengthZ = barGrowth * barLength * 0.25f + scaleX
                    drawCube( // middle
                        mix(element.parentAX.toFloat(), element.parentBX.toFloat(), mix0),
                        (middleZ - camZ).toFloat(),
                        mix(element.parentAY.toFloat(), element.parentBY.toFloat(), mix0),
                        scaleX, scaleX, lengthZ, color, rotation
                    )
                    drawCube( // middle
                        mix(element.parentAX.toFloat(), element.parentBX.toFloat(), mix1),
                        (middleZ - camZ).toFloat(),
                        mix(element.parentAY.toFloat(), element.parentBY.toFloat(), mix1),
                        scaleX, scaleX, lengthZ, color, rotation
                    )
                } else {
                    drawCube( // middle
                        px, (middleZ - camZ).toFloat(), pz,
                        scaleX, scaleX, barLength * 0.5f + scaleX, color, rotation
                    )
                }
            }

            val legAnimTime0 = animTime * legLength0 / totalLength
            val legGrowth0 = 1f - max((deltaTime - (topAnimTime + barAnimTime)) / legAnimTime0, 0f)
            if (legGrowth0 > 0f) {
                val leg0Z = mix(element.parentAZ.toFloat(), middleZ, legGrowth0 * 0.5f)
                val leg0L = legGrowth0 * abs(middleZ - element.parentAZ) * 0.5f
                drawCube( // left leg
                    element.parentAX.toFloat(),
                    (leg0Z - camZ).toFloat(),
                    element.parentAY.toFloat(),
                    scaleX, leg0L, scaleX, color, rotation
                )
            }

            val legAnimTime1 = animTime * legLength1 / totalLength
            val legGrowth1 = 1f - max((deltaTime - (topAnimTime + barAnimTime)) / legAnimTime1, 0f)
            if (legGrowth1 > 0f) {
                val leg1Z = mix(element.parentBZ.toFloat(), middleZ, legGrowth1 * 0.5f)
                val leg1L = legGrowth1 * abs(middleZ - element.parentBZ) * 0.5f
                drawCube( // right leg
                    element.parentBX.toFloat(),
                    (leg1Z - camZ).toFloat(),
                    element.parentBY.toFloat(),
                    scaleX, leg1L, scaleX, color, rotation
                )
            }

        } else {
            // just a line straight down
            val minZ = min(element.parentAZ, element.parentBZ)
            val topDZ = abs(element.z - minZ)
            if (topDZ <= 0) return

            val animTime = min(topDZ * animTimeX, maxAnimTime)
            if (deltaTime >= animTime) return // not good enough

            val growthTime = 1f - max(deltaTime / animTime, 0f)
            if (growthTime > 0f) {
                val middleZ = mix(minZ.toFloat(), element.z.toFloat(), 0.5f * growthTime)
                drawCube(
                    px, (middleZ - camZ).toFloat(), pz,
                    scaleX, growthTime * topDZ * 0.5f, scaleX, color, rotation
                )
            }
        }

    }

    private val textPaint = Paint().apply { textSize = 10f }

    private fun drawElementName(element: Element3D, camZ: Double) {
        if (element.z + tooLow < camZ) return
        if (element.name.isEmpty()) return

        val deltaTime = (camZ - element.z).toFloat()
        if (deltaTime < 0.5f) return

        // draw name on 4 sides
        val entry = getCacheEntry(element.name, element.name, 0, 100f, textPaint)
        val lines = entry.lines

        val px = element.x.toFloat() - cameraPos[0]
        val py = (element.z - camZ).toFloat() - cameraPos[1]
        val pz = element.y.toFloat() - cameraPos[2]

        val li0 = lines.lastIndex * 0.5f
        for (li in lines.indices) {
            val name = lines[li]
            // todo we need line-breaks like when drawing elements...

            val textSize = entry.textSize * 0.01f
            val shader = TextProgram

            var ki = -(name.length - 1) * charWidth0
            val scale = cubeSize * 0.5f * textSize
            val lineDy = scale * 1.6f

            for (i in name.indices) {
                val char = name[i]
                if (char.isWhitespace()) continue

                val code = char.code - 32
                ki += charWidth0

                val offsetX = ki * scale * 0.008f

                ki += charWidth0

                val xi = code % 10
                val yi = code / 10

                glUniform4f(shader.pos, px, py - (li - li0) * lineDy, pz, offsetX)
                glUniform1f(shader.size, scale)
                glUniform4f(shader.range, 0.05f, -0.05f, xi * 0.1f + 0.05f, yi * 0.1f + 0.05f)

                val colors = GroupsEtc.GroupColors
                val color = colors[clamp(element.groupId, 0, colors.lastIndex)]
                val textColorI =
                    if (color.r01() * 0.2f + color.g01() * 0.7f + color.b01() * 0.1f > 0.3f) 0f else 1f
                glUniform3f(shader.color, textColorI, textColorI, textColorI)

                glDrawElements(GL_TRIANGLES, flatIndices.size, GL_UNSIGNED_INT, 0)
            }
        }
    }

    val zSpeed = 10.0
    val tooLow = 2000f

    @Suppress("AssignedValueIsNeverRead") // AndroidStudio is retarded
    fun setListeners() {

        val scaleDetector = ScaleGestureDetector(
            context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    radius = clamp(radius / detector.scaleFactor, cubeSize * 5f, 5000f)
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
                            0f
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