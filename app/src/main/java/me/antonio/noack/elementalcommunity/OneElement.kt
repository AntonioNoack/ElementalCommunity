package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import kotlin.math.min

class OneElement(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var element: Element? = null

    var theWidth = 350f

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

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    init {textPaint.textAlign = Paint.Align.CENTER }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        GroupsEtc.tick()

        val candidate = element
        val width = measuredWidth * 1f
        drawElement(canvas, -1,0f, 0f, 0f, width, true, candidate?.name ?: "???", candidate?.group ?: 15, -1, bgPaint, textPaint)

    }

}