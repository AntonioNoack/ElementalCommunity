package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import java.lang.Math.abs

class ColorPicker(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet){

    fun setResolutionPerField(resolution: Int){
        this.resolution = -abs(resolution)
    }

    fun setResolutionAbsolute(resolution: Int){
        this.resolution = abs(resolution)
    }

    var resolution = -10

    val ratio = 1.3f

    val slider = ProgressBar(ctx)

    val color3f = FloatArray(3)

    val bgPaint = Paint()
    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        slider.max = 360
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val specModeY = MeasureSpec.getMode(heightMeasureSpec)
        if(specModeY == MeasureSpec.EXACTLY){
            val height = measuredHeight
            setMeasuredDimension((height / ratio).toInt(), height)
        } else {
            val width = measuredWidth
            setMeasuredDimension(width, (width * ratio).toInt())
        }

        slider.measure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((measuredWidth * (ratio - 1f)).toInt(), MeasureSpec.AT_MOST))
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if(canvas == null) return

        // draw the color field

        val width = measuredWidth * 1f
        val numFields = if(resolution < 0) (width / resolution).toInt() else resolution

        val hue = slider.progress * 360f / slider.max
        color3f[0] = hue

        var lastX = 0f
        for(x in 0 until numFields){
            var lastY = 0f
            val nextX = (x+1f)/numFields
            for(y in 0 until numFields){
                val nextY = (y+1f)/numFields
                bgPaint.color = Color.HSVToColor(color3f)
                canvas.drawRect(lastX, lastY, nextX, nextY, bgPaint)
                lastY = nextY
            }
            lastX = nextX
        }

        canvas.translate(0f, width)
        slider.draw(canvas)

    }

}