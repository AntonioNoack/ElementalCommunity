package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.GroupsEtc.getMargin
import kotlin.math.min

class OneElement(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var element: Element? = null

    var theWidth = 350f

    lateinit var onLiked: () -> Unit
    lateinit var onDisliked: () -> Unit

    var mx = 0f
    var my = 0f

    val bottom = 1.3f

    var touchesLike = false
    var touchesDislike = false

    init {

        val gestureDetector = GestureDetector(context, object: GestureDetector.OnGestureListener {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean = false
            override fun onDown(e: MotionEvent?): Boolean = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = false
            override fun onLongPress(e: MotionEvent?) = Unit
            override fun onShowPress(e: MotionEvent?){}
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return if(e != null){

                    calculateTouches(e)

                    if(touchesLike) onLiked()
                    if(touchesDislike) onDisliked()

                    mx = 0f
                    my = 0f

                    invalidate()

                    true
                } else return false
            }
        })

        setOnTouchListener { _, e ->
            calculateTouches(e)
            invalidate()
            gestureDetector.onTouchEvent(e)
        }

    }

    fun calculateTouches(e: MotionEvent){

        mx = e.x / measuredWidth
        my = e.y / measuredWidth

        touchesLike = my > 1f && mx < 0.5f
        touchesDislike = !touchesLike && my > 1f && mx > 0.5f

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        when(mode){
            MeasureSpec.EXACTLY -> {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                theWidth = measuredWidth.toFloat()
            }
            MeasureSpec.UNSPECIFIED -> {

            }
            MeasureSpec.AT_MOST -> {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                theWidth = min(measuredWidth.toFloat(), theWidth)
            }
        }
        setMeasuredDimension(theWidth.toInt(), theWidth.toInt())
    }

    private val path = Path()

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    init {textPaint.textAlign = Paint.Align.CENTER }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        val candidate = element
        val width = measuredWidth * 1f
        drawElement(canvas, 0f, 0f, 0f, width, true, candidate?.name ?: "???", candidate?.group ?: 15, bgPaint, textPaint)

    }

    fun darken(rgb: Int): Int {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        return 0xff000000.toInt() or ((r / 2) shl 16) or ((g / 2) shl 8) or (b/2)
    }

}