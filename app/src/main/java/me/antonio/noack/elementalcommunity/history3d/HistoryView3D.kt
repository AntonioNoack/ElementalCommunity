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
import android.widget.TextView
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.GroupsEtc
import me.antonio.noack.elementalcommunity.GroupsEtc.getCacheEntry
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.history3d.ElementHistoryCache.getElement
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.cubeIndices
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.cubePositions
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.flatIndices
import me.antonio.noack.elementalcommunity.history3d.FloatBuffer.Companion.flatPositions
import me.antonio.noack.elementalcommunity.time.SimpleDate.formatMinutesSince1970
import me.antonio.noack.elementalcommunity.utils.Compact.compacted
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
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

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

    fun init(allManager: AllManager): Boolean {
        this.all = allManager

        val activityManager = all.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val version = configurationInfo.reqGlEsVersion
        val major = version.shr(16)
        val minor = version.and(0xffff)

        if (major < 2) return false

        // val version10x = major * 10 + min(minor, 9)
        // GFXFeatures.supportsShaderStorageBuffers = version10x >= 31
        // GFXFeatures.supportsTextureGather = version10x >= 32

        // GL11.setVersion(major, minor)
        println("OpenGL ES Version: $major.$minor")

        // Request an OpenGL ES 2.0 compatible context.
        setEGLContextClientVersion(major)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY//RENDERMODE_WHEN_DIRTY

        elementBirthView = allManager.findViewById(R.id.elementBirthDates)

        setListeners()
        return true
    }


    lateinit var all: AllManager

    private var frameWidth = 1
    private var frameHeight = 1

    private val fontImage = Texture2D(R.drawable.font, true)
    private val skyboxImage = Texture2D(R.drawable.skybox, false)

    // todo render date and time
    // todo render timeline??? with zoom and scroll??
    // todo date-picker would be nice

    var animationTime = 0.0
    var lastDown = 0L

    var rotY = 0f
    var rotX = -0.5f

    var targetRotY = 0f
    var targetRotX = -0.5f

    var radius = 200f
    val far get() = radius * 2f + 7f * (max - min)

    private var lastTime = 0L

    private var lastMinElementId = 0

    private val cameraMatrix = Matrix4f()
    private val transform = Matrix4f()

    private lateinit var textProgram: TextProgramBase

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        check(SkyboxProgram.create()) { "Failed to create skybox program" }
        textProgram = if (TextProgram.create()) TextProgram else {
            check(TextProgramFallback.create()) { "Failed to create text program & fallback" }
            TextProgramFallback
        }

        check(CubeProgram.create()) { "Failed to create cube program" }
        checkErrors()

        cubePositions.create()
        cubeIndices.create()
        flatPositions.create()
        flatIndices.create()
        checkErrors()

        fontImage.create(all)
        skyboxImage.create(all)
        checkErrors()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.frameWidth = max(width, 1)
        this.frameHeight = max(height, 1)
    }

    override fun onDrawFrame(gl: GL10?) {
        val dt = updateTime()
        skipToTarget(dt)
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

        glDisable(GL_DEPTH_TEST)
        showBirthedElements()

        checkErrors()
    }

    var elementBirthView: TextView? = null
    private var lastShownBirthedElement = ""

    fun showBirthedElements() {
        val elements = lastGoodElements
        if (elements.isEmpty()) return

        val view = elementBirthView ?: return
        val lastAddedName = elements.last().name
        if (lastAddedName == lastShownBirthedElement) return
        lastShownBirthedElement = lastAddedName

        all.runOnUiThread {
            val se = searchedElement
            view.text = elements.indices.joinToString("\n") { i ->
                val element = elements[elements.lastIndex - i]
                val date = formatMinutesSince1970(element.timestampMinutes)
                "${element.name}: $date"
            } + if (se != null) "\n\nSearching '${se.name}': ${lastSearchedIndex}/?" else ""
        }
    }

    private val shownGoodElements = 4
    var currentTimeEstimate = 0.0
    var lastGoodElements: List<Element3D> = emptyList()

    private var autoSpeed = 0f
    private fun updateTime(): Float {
        val time = System.nanoTime()
        val dt = min(abs(time - lastTime) * 1e-9f, 0.1f)
        animationTime += dt

        if (abs(time - lastDown) > 2e9) {
            targetRotY = (targetRotY + 0.3f * dt * autoSpeed)
            autoSpeed += (1f - autoSpeed) * dt
        } else autoSpeed = 0f

        val factor = exp(-20f * dt)
        rotX = mix(targetRotX, rotX, factor)
        rotY = mix(targetRotY, rotY, factor)

        // clamp for precision
        if (rotX < -PI) {
            rotX += 2f * PI.toFloat()
            targetRotX += 2f * PI.toFloat()
        } else if (rotX > PI) {
            rotX -= 2f * PI.toFloat()
            targetRotX -= 2f * PI.toFloat()
        }

        lastTime = time
        return dt
    }

    private fun calculateCameraMatrix() {
        cameraMatrix
            .identity()
            .perspective(
                90f * (PI.toFloat() / 180), frameWidth.toFloat() / frameHeight.toFloat(),
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

        val scale = far * 0.55f
        transform.identity()
            .scale(scale, scale, scale) // large to clear depth

        cameraMatrix.mul(transform, tmpM).fillInto(tmp16)
        glUniformMatrix4fv(shader.transform, 1, false, tmp16)

        skyboxImage.bind(0)

        glDrawElements(GL_TRIANGLES, cubeIndices.size, GL_UNSIGNED_INT, 0)
        checkErrors()
    }

    private fun drawElementCubes() {

        cubePositions.bindAsPosNor(CubeProgram.attrPos, CubeProgram.attrNor)
        cubeIndices.bindAsIndices()

        // sky texture is already bound
        val shader = CubeProgram
        shader.bind()
        checkErrors()

        val vx = cos(-rotX) * sin(rotY)
        val vy = sin(-rotX)
        val vz = cos(-rotX) * cos(rotY)

        cameraPos[0] = vx * radius + center
        cameraPos[1] = vy * radius - radius * 0.5f
        cameraPos[2] = vz * radius + center

        cameraMatrix.fillInto(tmp16)
        glUniformMatrix4fv(shader.transform, 1, false, tmp16)

        val camZ = animationTime * zSpeed
        drawBase(camZ)

        glUniform1f(shader.metallic, 0.5f)

        var maxZ = 0
        val lastGoodElements = ArrayDeque<Element3D>(shownGoodElements)
        var firstHiddenElement = true
        var currentTimeEstimate1 = 0.0
        for (index in lastMinElementId until Int.MAX_VALUE) {
            val curr = getElement(index) ?: break
            val done = drawElementCube(index, curr, camZ)
            maxZ = curr.z

            if (curr.name.isNotEmpty() && curr.z < camZ) {
                if (lastGoodElements.size >= shownGoodElements) {
                    lastGoodElements.removeFirstOrNull()
                }
                lastGoodElements.add(curr)
            }

            if (curr.z >= camZ && firstHiddenElement && lastGoodElements.isNotEmpty()) {
                for (prev in lastGoodElements) {
                    if (curr.timestampMinutes == prev.timestampMinutes) {
                        currentTimeEstimate1 = prev.timestampMinutes.toDouble()
                    } else {
                        firstHiddenElement = false
                        val fraction = (camZ - prev.z) / (curr.z - prev.z)
                        currentTimeEstimate1 = mix(
                            prev.timestampMinutes.toDouble(),
                            curr.timestampMinutes.toDouble(),
                            fraction
                        )
                    }
                }
            }

            if (done) break
        }

        this.lastGoodElements = lastGoodElements
        currentTimeEstimate = currentTimeEstimate1
        val maxT = (maxZ + 5) / zSpeed
        if (maxT + 1 < animationTime) println("Jumping down from $animationTime to $maxT by maxZ")
        animationTime = min(animationTime, maxT)
    }

    private fun drawElementNames() {

        flatPositions.bindAsPositions()
        flatIndices.bindAsIndices()

        val shader = textProgram
        shader.bind()

        // cameraMatrix.fillInto(tmp16) // already done
        glUniformMatrix4fv(shader.transform, 1, false, tmp16)

        fontImage.bind(0)

        val camZ = animationTime * zSpeed
        for (index in lastMinElementId until Int.MAX_VALUE) {
            val done = drawElementName(getElement(index) ?: break, camZ)
            if (done) break
        }
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

    @Suppress("SameParameterValue")
    private fun drawBaseLayer(camZ: Double, z: Int, scale: Float) {
        val sx = 0.5f * (max - min)
        val scaleZ = sx * (1.4f + 0.2f * z + 0.02f * z * z)
        drawCube(
            center, -(camZ.toFloat() + (z * 2 + 2) * scale), center,
            scaleZ, scale, scaleZ,
            0x11111111 * max(15 - z, 1), 0f
        )
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

    private fun drawElementCube(i: Int, element: Element3D, camZ: Double): Boolean {
        if (element.z + tooLow < camZ) {
            lastMinElementId = max(lastMinElementId, i)
            return false
        }

        if (element.name.isEmpty()) return false

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
            if (element.z == element.parentAZ && element.z == element.parentBZ) return false

            val barLength = hypot(dx, dz)
            val rotation = atan2(dx, dz)
            val topLength = abs(element.z - middleZ)
            val legLength0 = abs(middleZ - element.parentAZ)
            val legLength1 = abs(middleZ - element.parentBZ)

            val totalLength = barLength + topLength + max(legLength0, legLength1)
            val animTime = min(totalLength * animTimeX, maxAnimTime)
            if (deltaTime >= animTime) return true // too early

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
            if (topDZ == 0) return false

            val animTime = min(topDZ * animTimeX, maxAnimTime)
            if (deltaTime >= animTime) return true // too early

            val growthTime = 1f - max(deltaTime / animTime, 0f)
            if (growthTime > 0f) {
                val middleZ = mix(minZ.toFloat(), element.z.toFloat(), 0.5f * growthTime)
                drawCube(
                    px, (middleZ - camZ).toFloat(), pz,
                    scaleX, growthTime * topDZ * 0.5f, scaleX, color, 0f
                )
            }
        }

        return false
    }

    private val textPaint = Paint().apply { textSize = 10f }

    private val maxDistSqForText = 500f.pow(2)

    private fun drawElementName(element: Element3D, camZ: Double): Boolean {
        if (element.z + tooLow < camZ) return false
        if (element.name.isEmpty()) return false

        val deltaTime = (camZ - element.z).toFloat()
        if (deltaTime < 0.5f) return true

        // draw name on 4 sides

        val px = element.x.toFloat() - cameraPos[0]
        val py = (element.z - camZ).toFloat() - cameraPos[1]
        val pz = element.y.toFloat() - cameraPos[2]

        if (px * px + py * py + pz * pz > maxDistSqForText) return false

        val colors = GroupsEtc.GroupColors
        val color = colors[clamp(element.groupId, 0, colors.lastIndex)]
        drawText(element.name, color, px, py, pz)
        return false
    }

    private fun drawText(text: String, color: Int, px: Float, py: Float, pz: Float) {
        val entry = getCacheEntry(text, text, 0, 100f, textPaint)
        drawText(color, px, py, pz, entry.lines, entry.textSize * 0.01f)
    }

    private fun drawText(
        color: Int, px: Float, py: Float, pz: Float,
        lines: List<String>, textSize: Float
    ) {
        val li0 = lines.lastIndex * 0.5f
        for (li in lines.indices) {
            val name = lines[li]
            val shader = textProgram

            var ki = -name.length
            val scale = cubeSize * 0.5f * textSize
            val lineDy = scale * 1.6f

            for (i in name.indices) {
                val char = name[i]; ki += 2
                if (char.isWhitespace()) continue

                val code = char.code - 32
                val offsetX = ki * scale * 0.5f

                val xi = code % 10
                val yi = code / 10

                glUniform4f(shader.pos, px, py - (li - li0) * lineDy, pz, offsetX)
                glUniform1f(shader.size, scale)
                glUniform4f(shader.range, 0.05f, -0.05f, xi * 0.1f + 0.05f, yi * 0.1f + 0.05f)

                val textColorI =
                    if (color.r01() * 0.2f + color.g01() * 0.7f + color.b01() * 0.1f > 0.6f) 0f else 1f
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
                        val speed = 10f / max(frameWidth, frameHeight)
                        val dx = (event.x - lastX) * speed
                        val dy = (event.y - lastY) * speed
                        cancelSearchingMotion += hypot(dx, dy)

                        targetRotY -= dx
                        targetRotX = clamp(
                            targetRotX - dy,
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

    private var lastSearchedIndex = 0
    private var searchedElement: Element? = null
    private var cancelSearchingMotion = 0f

    private fun skipToTarget(dt: Float) {
        val te = searchedElement ?: return
        if (cancelSearchingMotion > 1f) {
            all.runOnUiThread { AllManager.toast("Cancelled search", true) }
            searchedElement = null
            cancelSearchingMotion = 0f
            return
        }

        val k = 1000
        animationTime += dt * 10f // sped up ^^
        for (i in lastSearchedIndex until lastSearchedIndex + k) {

            val element = getElement(i) ?: break
            lastSearchedIndex = i + 1
            if (element.name.isEmpty()) continue

            if (compacted(element.name) == te.compacted) {
                // found :)
                animationTime = element.z / zSpeed - 1.0
                println("Found element by skipping, $animationTime")
                searchedElement = null
                return
            } else if (element.z > i - 4) {
                val newT = element.z / zSpeed
                if (newT + 1 < animationTime) println("Jumping down zo $newT by search")
                animationTime = min(newT, animationTime)
            }
        }
    }

    fun skipToElement(element: Element) {
        animationTime = 0.0
        cancelSearchingMotion = 0f
        val quickIndex = ElementHistoryCache.find(element)
        if (quickIndex != null) {
            animationTime = quickIndex.first.z / zSpeed - 1.0
            lastMinElementId = max(quickIndex.second - 100, 0)
            println("Found element by immediately, $animationTime")
        } else {
            animationTime = 0.0
            searchedElement = element
            lastSearchedIndex = 0
            lastMinElementId = 0
        }
    }
}