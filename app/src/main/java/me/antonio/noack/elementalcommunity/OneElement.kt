package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import kotlin.math.min

class OneElement(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    var element: Element? = null
    var alphaOverride = 255

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var calcWidth = 350
        when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                calcWidth = measuredWidth
            }
            MeasureSpec.UNSPECIFIED -> {}
            MeasureSpec.AT_MOST -> {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                calcWidth = min(measuredWidth, calcWidth)
            }
        }
        setMeasuredDimension(calcWidth, calcWidth)
    }

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        GroupsEtc.tick()

        val candidate = element
        val width = measuredWidth * 1f
        bgPaint.alpha = alphaOverride
        textPaint.alpha = alphaOverride
        drawElement(
            canvas, -1, 0f, 0f, 0f, width, true,
            candidate?.name ?: "???", candidate?.group ?: 15, -1,
            bgPaint, textPaint
        )

    }

}