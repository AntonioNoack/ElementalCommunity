package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElementRaw
import me.antonio.noack.elementalcommunity.GroupsEtc.hues
import me.antonio.noack.elementalcommunity.GroupsEtc.saturations

class Colors(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var selected = -1

    init {

        setOnTouchListener { _, e ->

            val widthPerNode = measuredHeight * 1f / saturations.size
            val x = (e.x / widthPerNode).toInt()
            val y = (e.y / widthPerNode).toInt()

            selected = x * saturations.size + y

            invalidate()

            true

        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth,  measuredWidth * (saturations.size * saturations.size) / GroupColors.size)
    }

    private val bgPaint = Paint()

    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
        if(canvas == null) return

        val widthPerNode = measuredWidth * 1f / hues.size

        for(i in 0 until GroupColors.size){
            val x0 = (i / saturations.size) * widthPerNode
            val y0 = (i % saturations.size) * widthPerNode
            if(i == selected){
                drawElementRaw(canvas, x0+widthPerNode/6, y0+widthPerNode/6, widthPerNode*2/3, true, i, bgPaint)
            } else {
                drawElementRaw(canvas, x0, y0, widthPerNode, true, i, bgPaint)
            }
        }

    }

}