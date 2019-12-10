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
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.api.web.Candidate
import kotlin.math.min

class CandidateView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var candidate: Candidate? = null

    var theWidth = 350f

    lateinit var onLiked: () -> Unit
    lateinit var onDisliked: () -> Unit

    var mx = 0f
    var my = 0f

    val bottom = 1.3f

    var touchesLike = false
    var touchesDislike = false

    init {

        MotionEvent.AXIS_VSCROLL

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

                    true
                } else return false
            }
        })

        setOnTouchListener { _, e ->
            if(e.y > measuredWidth){// only the bottom is touchable
                calculateTouches(e)
                gestureDetector.onTouchEvent(e)
            } else false
        }

    }

    fun calculateTouches(e: MotionEvent){

        mx = e.x / measuredWidth
        my = e.y / measuredWidth

        val newTouchesLike = my > 1f && mx < 0.5f
        val newTouchesDislike = !touchesLike && my > 1f && mx > 0.5f

        if(touchesLike != newTouchesLike || touchesDislike != newTouchesDislike){
            touchesLike = newTouchesLike
            touchesDislike = newTouchesDislike
            invalidate()
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        minimumWidth = theWidth.toInt()
        minimumHeight = (theWidth * bottom).toInt()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, (measuredHeight/bottom).toInt())
        setMeasuredDimension(size, (size * bottom).toInt())
    }

    private val path = Path()

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    init {textPaint.textAlign = Paint.Align.CENTER }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        GroupsEtc.tick()

        val candidate = candidate
        val width = measuredWidth * 1f
        drawElement(canvas, -1, 0f, 0f, 0f, width, true, candidate?.name ?: "???", candidate?.group ?: 15, -1, bgPaint, textPaint)

        val margin = getMargin(width)
        val shift = width * 0.03f

        canvas.translate(0f, -margin)

        bgPaint.style = Paint.Style.FILL
        val color = bgPaint.color
        val dark = darken(color)

        bgPaint.color = if(touchesLike) dark else color

        path.reset()
        path.moveTo(margin, width)
        path.lineTo(width/2 - margin/2, width)
        path.lineTo(width/2 - 2*shift - margin/2, width*bottom)
        path.lineTo(margin, width*bottom)
        canvas.drawPath(path, bgPaint)

        bgPaint.color = if(touchesDislike) dark else color

        path.reset()
        path.moveTo(width-margin, width)
        path.lineTo(width/2 + margin/2, width)
        path.lineTo(width/2 - 2*shift + margin/2, width*bottom)
        path.lineTo(width-margin, width*bottom)
        canvas.drawPath(path, bgPaint)

        textPaint.textSize = (bottom-1f)*width*.5f
        textPaint.color = 0xff000000.toInt()

        val dy = (textPaint.ascent() + textPaint.descent())/2
        canvas.drawText("Like", width/4, width*(1f+bottom)/2-dy, textPaint)
        canvas.drawText("Dislike", width*3/4, width*(1f+bottom)/2-dy, textPaint)

    }

    fun darken(rgb: Int): Int {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        return 0xff000000.toInt() or ((r / 2) shl 16) or ((g / 2) shl 8) or (b/2)
    }

}