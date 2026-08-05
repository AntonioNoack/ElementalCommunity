package me.antonio.noack.elementalcommunity.history3d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.util.AttributeSet
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.math.MathUtils.clamp
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.time.SimpleDate
import me.antonio.noack.elementalcommunity.time.SimpleDate.countMinutesSince1970
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@SuppressLint("ClickableViewAccessibility")
class HistoryTimeChooser(ctx: Context, attributeSet: AttributeSet?) :
    View(ctx, attributeSet) {

    companion object {
        val minTime = countMinutesSince1970(2019, 4, 14, 0, 0).toDouble()
        val maxTime get() = System.currentTimeMillis() / (1000L * 60)
    }

    var renderView: HistoryView3D? = null
    fun init(allManager: AllManager): Boolean {
        this.all = allManager

        renderView = allManager.findViewById(R.id.historyView)
        setListeners()
        invalidate()
        return true
    }

    var deltaTime = (24 * 60).toDouble()
    var centerTime = minTime + deltaTime * 0.5

    val linePaint = Paint()
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = height * 0.4f

        linePaint.color = 0x2CA4EC or 0xff000000.toInt()
        textPaint.color = 0x8fd5ff or 0xff000000.toInt()

        linePaint.style = Style.STROKE
        linePaint.strokeWidth = 1f
        textPaint.style = Style.FILL

        // from left to right draw the dates
        val numX = max(width / (height * 4), 3)
        val sx = numX * 5
        for (i in 0 until sx) {
            val fraction = (i + 0.5f) / sx
            val x = this.width * fraction
            canvas.drawLine(x, height * 0.2f, x, height * 0.8f, linePaint)
        }

        // from left to right draw the dates
        for (i in 0 until numX) {
            val fraction = (i + 0.5f) / numX
            val x = this.width * fraction
            val minutes = centerTime + deltaTime * (fraction - 0.5)
            val date = SimpleDate.formatMinutesSince1970(minutes.toLong())
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
            canvas.drawText(date.toString(), x, height * 0.7f, textPaint)
        }

        val renderView = renderView ?: return

        linePaint.color = -1
        linePaint.strokeWidth = 3f
        var minX = +100f
        var maxX = -100f
        for (element in renderView.lastGoodElements) {
            val relX = ((element.timestampMinutes - centerTime) / deltaTime + 0.5f).toFloat()
            val x = width * relX
            linePaint.color = GroupColors[clamp(element.groupId, 0, GroupColors.lastIndex)]
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
            maxX = max(maxX, relX)
            minX = min(minX, relX)
        }

        val cte = renderView.currentTimeEstimate
        val relX = ((cte - centerTime) / deltaTime + 0.5f).toFloat()
        maxX = max(maxX, relX)

        if (abs(System.nanoTime() - lastDown) > 5e9) {

            centerTime += ((minX + maxX) * 0.5 - 0.5) * deltaTime * 0.03 // factor for a smooth transition
            clampCenterTime()

            val delta = maxX - minX
            if (delta > 0f) {
                deltaTime *= (delta / 0.8).pow(0.03)
                clampDeltaTime()
            }
        }

        invalidate()
    }

    lateinit var all: AllManager

    private var lastDown = 0L

    fun clampCenterTime() {
        centerTime = clamp(
            centerTime,
            minTime + deltaTime * 0.5,
            maxTime - deltaTime * 0.5
        )
    }

    fun clampDeltaTime() {
        deltaTime = clamp(deltaTime, 60.0, 2.0 * 365 * 24 * 60)
    }

    @Suppress("AssignedValueIsNeverRead") // AndroidStudio is retarded
    fun setListeners() {

        val scaleDetector = ScaleGestureDetector(
            context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    deltaTime /= detector.scaleFactor
                    clampDeltaTime()
                    invalidate()
                    return true
                }
            })

        var lastX = 0f
        var disableMove = false

        setOnTouchListener { _, event ->

            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                ACTION_DOWN -> {
                    lastX = event.x
                    disableMove = false
                }

                ACTION_MOVE -> {
                    if (!disableMove && !scaleDetector.isInProgress) {
                        val dx = (event.x - lastX)
                        centerTime -= dx * deltaTime / width
                        clampCenterTime()
                        invalidate()
                    } else disableMove = true

                    lastX = event.x
                    lastDown = System.nanoTime()
                }
            }

            true
        }
    }
}