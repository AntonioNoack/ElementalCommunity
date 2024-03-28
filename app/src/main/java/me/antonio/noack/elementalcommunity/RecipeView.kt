package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement

class RecipeView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var aName = ""
    var aGroup = 0

    var bName = ""
    var bGroup = 0

    var rName = ""
    var rGroup = 0

    var overlay = 0

    private val relativeWidth = 4f

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (width / relativeWidth).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        GroupsEtc.tick()

        val width = measuredWidth * 1f
        val widthPerNode = width / relativeWidth
        textPaint.textAlign = Paint.Align.CENTER

        drawElement(canvas, -1, 0f, 0f, 0f, widthPerNode, true, aName, aGroup, -1, bgPaint, textPaint)
        drawElement(canvas, -1, widthPerNode*(relativeWidth-1f)/2, 0f, 0f, widthPerNode, true, bName, bGroup, -1, bgPaint, textPaint)
        drawElement(canvas, -1, widthPerNode*(relativeWidth-1f), 0f, 0f, widthPerNode, true, rName, rGroup, -1, bgPaint, textPaint)

        textPaint.textSize = widthPerNode*.5f
        textPaint.color = 0xff777777.toInt()

        val dy = (textPaint.ascent() + textPaint.descent())/2
        canvas.drawText("+", widthPerNode*((relativeWidth-1f)/4 + 0.5f), widthPerNode/2-dy, textPaint)
        canvas.drawText("->", widthPerNode*((relativeWidth-1f)*3/4 + 0.5f), widthPerNode/2-dy, textPaint)

        if(overlay.shr(24).and(255) != 0){
            bgPaint.color = overlay
            canvas.drawRect(0f, 0f, width, measuredHeight.toFloat(), bgPaint)
        }

    }

}