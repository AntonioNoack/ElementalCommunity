package me.antonio.noack.webdroid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.View
import androidx.core.math.MathUtils.clamp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@SuppressLint("ClickableViewAccessibility")
class CaptchaView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {

    companion object {
        private val invalidChallenge = Challenge(ArrayList(), emptyList(), 100, 0.0, "")
        private const val BLACK = (0xff shl 24)
    }

    var challenge: Challenge = invalidChallenge

    private val slots get() = challenge.slots
    private val angles get() = challenge.angles
    private val da get() = challenge.da
    private val num get() = challenge.num

    private val hasChallenge
        get() = challenge != invalidChallenge &&
                challenge.angles.isNotEmpty()

    fun markReady(challenge: Challenge) {
        this.challenge = challenge
        sign = 0
        lastTime = 0L
        timeSum = 0.0
        lastSign = 0
        isSolved = false
        sequence.clear()
        invalidate()
    }

    private var pointX0 = 0f
    private var pointX1 = 0f
    private var pointRadius = 0f
    private var pointY = 0f
    var isSolved = false

    private var sign = 0
    private var lastTime = 0L

    private val numAngles get() = angles.size

    val sequence = ArrayList<Press>()

    init {
        setOnTouchListener { _, e ->

            sign = 0
            when (e.action) {
                ACTION_MOVE, ACTION_DOWN -> {
                    for (i in 0 until e.pointerCount) {
                        val x = e.getX(i) //- this.x
                        val y = e.getY(i) //- this.y
                        if (sq(x - pointX0) + sq(y - pointY) < sq(pointRadius)) {
                            sign--
                        } else if (sq(x - pointX1) + sq(y - pointY) < sq(pointRadius)) {
                            sign++
                        }
                    }
                }
            }
            sign = clamp(sign, -1, +1)
            invalidate()
            true
        }
    }

    private fun sq(x: Float): Float {
        return x * x
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        var numAngles = numAngles
        if (numAngles < 1) numAngles = 3 // avoid division by zero

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width / numAngles, height)

        setMeasuredDimension((size * numAngles), (size * 1.5).toInt())
    }

    private val paint = Paint()

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val paint = paint
        if (!hasChallenge) {
            val cx = width * 0.5f
            val cy = height * 0.5f
            paint.color = BLACK
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = cy * 0.2f
            canvas.drawText("Loading...", cx, cy, paint)
            return
        }

        if (sign != 0) invalidate()

        val angles = angles
        val slots = slots
        val lineThickness = max(height * 0.01f, 1f)
        val size = min(width / numAngles, height)

        val signalColor = 0x00ff00 or BLACK
        val textColor = BLACK

        val time = System.nanoTime()
        val dt = min((time - lastTime) * 1e-9, 0.1)
        lastTime = time

        var allSolved = true
        for (i in angles.indices) {
            val cx = (i + 0.5f) * size
            val cy = 0.5f * size

            val radius = size * 0.42f
            val solved = isSolved(angles[i], slots[i], num)
            if (!solved) allSolved = false

            paint.strokeWidth = lineThickness
            paint.style = Paint.Style.STROKE
            paint.color = if (solved) signalColor else textColor
            canvas.drawCircle(cx, cy, radius, paint)

            fun drawPin(angle: Float, r0: Float, r1: Float, thickness: Float, color: Int) {
                val c = cos(angle)
                val s = sin(angle)
                paint.color = color
                paint.strokeWidth = lineThickness * thickness
                paint.style = Paint.Style.STROKE
                canvas.drawLine(
                    cx + s * r0, cy - c * r0,
                    cx + s * r1, cy - c * r1,
                    paint
                )
            }

            for (j in 0 until num) {
                val angle = angles[i].toFloat() + j * (Math.PI * 2).toFloat() / num
                val color = if (j == 0) signalColor else textColor
                val isMajor = j % 5 == 0
                val r0 = radius * 0.9f
                val r1 = radius * (if (isMajor) 0.7f else 0.8f)
                drawPin(angle, r0, r1, if (isMajor) 1f else 0.5f, color)
            }
            drawPin(
                slots[i] * (Math.PI * 2).toFloat() / num,
                radius * 1.04f, radius * 1.15f, 1.3f, signalColor
            )
        }
        isSolved = allSolved

        val pressColor = 0x006096 or BLACK
        val gray = 0x1178B4 or BLACK
        val pressColor2 = 0x333333 or BLACK
        val gray3 = 0x1178B4 or BLACK
        val path = path
        for (i in 0 until 2) {
            val cx = size * angles.size * 0.5f + size * 0.75f * (i - 0.5f)
            val cy = size * 1.25f
            val radius = size * 0.25f - 1.5f

            if (i == 0) {
                pointX0 = cx
                pointY = cy
                pointRadius = radius
            } else {
                pointX1 = cx
            }

            val thisSign = if (i == 0) -1 else +1
            val pressed = thisSign == this.sign
            if (pressed) {
                if (sequence.isNotEmpty() && thisSign == lastSign) {
                    sequence.last().dt += dt
                } else {
                    sequence.add(Press(thisSign, dt))
                }
                rotate(dt, thisSign)
            }

            paint.strokeWidth = lineThickness
            paint.color = if (pressed) pressColor else gray
            for (j in 0 until 3) {
                canvas.drawCircle(cx, cy, radius * (1f - j * 0.1f), paint)
            }

            paint.color = pressColor2
            canvas.drawCircle(cx, cy, radius * (1f - 5 * 0.1f), paint)

            paint.color = if (pressed) signalColor else gray3

            val r0 = radius * 0.45f
            paint.style = Paint.Style.FILL
            path.reset()
            for (j in 0 until 3) {
                val a0 = i * Math.PI.toFloat() + j * Math.PI.toFloat() * 2f / 3f
                val x = cx - r0 * cos(a0)
                val y = cy + r0 * sin(a0)
                if (j == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
        }

        if (this.sign == 0) {
            rotate(dt, 0)
        }
    }

    private val path = Path()

    private fun isSolved(angle: Double, slot: Int, num: Int): Boolean {
        val pos = angle * num / (Math.PI * 2) - slot
        return pos.roundToInt() % num == 0
    }

    private fun timeToPress(time: Double): Double {
        return (time + 0.2f * cos(time)) * time
    }

    private fun angleCompare(a: Double, b: Double): Int {
        var delta = (a - b) % (Math.PI * 2)
        if (delta < 0) delta += Math.PI * 2
        return if (delta > Math.PI) -1 else +1
    }

    private var lastSign = 0
    private var timeSum = 0.0
    private fun rotate(dt: Double, sign: Int) {
        if (sign != lastSign) {
            timeSum = 0.0
            lastSign = sign
        }
        if (sign == 0) return
        val deltaAngle = timeToPress(timeSum + dt) - timeToPress(timeSum)
        timeSum += dt
        if (sign > 0) flipAngles()
        var remainder = deltaAngle
        while (remainder > 0f) {
            rotateImpl(min(remainder, 1.0))
            remainder--
        }
        if (sign > 0) flipAngles()
    }

    private fun flipAngles() {
        val angles = angles
        for (i in angles.indices) {
            angles[i] = -angles[i]
        }
    }

    private fun rotateImpl(deltaAngle: Double) {
        val angles = angles
        var lastAngle0 = angles[0]
        var lastAngle1 = lastAngle0 - deltaAngle
        angles[0] = lastAngle1

        for (i in 1 until angles.size) {
            if (angleCompare(lastAngle0 - da, angles[i]) > 0 &&
                angleCompare(lastAngle1 - da, angles[i]) < 0
            ) {
                lastAngle0 = angles[i]
                lastAngle1 -= da
                angles[i] = lastAngle1
            } else break
        }
    }
}