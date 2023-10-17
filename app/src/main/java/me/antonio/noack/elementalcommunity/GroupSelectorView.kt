package me.antonio.noack.elementalcommunity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElementBackground
import me.antonio.noack.elementalcommunity.GroupsEtc.hues
import me.antonio.noack.elementalcommunity.GroupsEtc.saturations

@SuppressLint("ClickableViewAccessibility")
class GroupSelectorView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var selected = -1
    var debugColors = false

    init {

        setOnTouchListener { _, e ->
            when(e.actionMasked){
                MotionEvent.ACTION_UP, MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {

                    val widthPerNode = measuredHeight * 1f / saturations.size
                    val x = (e.x / widthPerNode).toInt()
                    val y = (e.y / widthPerNode).toInt()

                    val newSelected = x * saturations.size + y
                    if(selected != newSelected){
                        selected = newSelected
                        invalidate()
                    }

                    true
                }
                else -> false
            }
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth,  measuredWidth * (saturations.size * saturations.size) / GroupColors.size)
    }

    private val bgPaint = Paint()
    init {
        bgPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        GroupsEtc.tick()

        val widthPerNode = measuredWidth * 1f / hues.size

        for(i in GroupColors.indices){
            val x0 = (i / saturations.size) * widthPerNode
            val y0 = (i % saturations.size) * widthPerNode
            if(debugColors){

                drawElement(canvas, -1, x0, y0, 0f,
                    widthPerNode, true, "$i", i, -1, bgPaint, bgPaint)

            } else {

                if(i == selected){
                    drawElementBackground(canvas, x0, y0, -widthPerNode/6, widthPerNode, true, i, bgPaint)
                } else {
                    drawElementBackground(canvas, x0, y0, 0f, widthPerNode, true, i, bgPaint)
                }

            }
        }


    }

}