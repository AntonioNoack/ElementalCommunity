package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.utils.MeasureSpecHelper
import kotlin.math.max
import kotlin.math.min

class OneElement(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    var element: Element? = null
    var alphaOverride = 255

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fittingSize = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        val idealSize = max(150, fittingSize)
        val lp = layoutParams
        setMeasuredDimension(
            MeasureSpecHelper.getDefaultSize(widthMeasureSpec, idealSize, lp.width),
            MeasureSpecHelper.getDefaultSize(heightMeasureSpec, idealSize, lp.height)
        )
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
        val w = width.toFloat()
        val h = height.toFloat()
        val size = min(w, h)
        bgPaint.alpha = alphaOverride
        textPaint.alpha = alphaOverride
        drawElement(
            canvas, -1, (w - size) * 0.5f, (h - size) * 0.5f, 0f, size, true,
            candidate?.name ?: "???", candidate?.group ?: 15, -1,
            bgPaint, textPaint
        )
    }

}