package me.antonio.noack.elementalcommunity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.math.MathUtils.clamp

object GroupsEtc {

    val GroupColors: IntArray
    val GroupSizes: IntArray

    private fun brightness(color: Int): Float {
        val r = (color shr 16) and 255
        val g = (color shr 8) and 255
        val b = color and 255
        return (0.299f*r + 0.587f*g + 0.114f*b)/255f
    }

    val hues = intArrayOf(0, 31, 51, 110, 178, 208, 233, 277, 304, -1).map { x -> x * 1f }
    val saturations = intArrayOf(22, 60, 85).map { x -> x / 100f }

    init {

        // Helligkeitskorrektur :D

        val hue0 = 288
        // val values = intArrayOf(81, 82, 69, 37).map { x -> x / 100f }
        val values = intArrayOf(100, 100, 85, 70).map { x -> x / 100f }
        val brightness0 = FloatArray(saturations.size)

        GroupColors = IntArray(hues.size * saturations.size)
        GroupSizes = IntArray(GroupColors.size)

        val tempArray = FloatArray(3){ hue0 * 1f }
        for(i in 0 until saturations.size){
            tempArray[0] = hue0 * 1f
            tempArray[1] = saturations[i]
            tempArray[2] = values[i]
            brightness0[i] = brightness(Color.HSVToColor(tempArray))
        }

        for(i in 0 until hues.size){
            tempArray[0] = hues[i]
            if(tempArray[0] < 0){
                val id = i * saturations.size
                GroupColors[id + 0] = 0xffdddddd.toInt()
                GroupColors[id + 1] = 0xffbbbbbb.toInt()
                GroupColors[id + 2] = 0xff888888.toInt()
            } else {
                for(j in 0 until saturations.size){
                    val id = i * saturations.size + j
                    tempArray[1] = saturations[j]
                    tempArray[2] = 1f - j * j * .33f * .33f + j * .33f * .25f
                    // tempArray[2] = MathUtils.clamp(values[j] * brightness0[j] / brightness(Color.HSVToColor(tempArray)), 0f, 1f)
                    GroupColors[id] = Color.HSVToColor(tempArray)
                }
            }
        }
    }

    private val fRect = RectF()

    fun getMargin(widthPerNode: Float): Float = widthPerNode * 0.02f

    fun drawElement(canvas: Canvas, x0: Float, y0: Float, widthPerNode: Float, margin: Boolean, element: Element, bgPaint: Paint, textPaint: Paint){
        drawElement(canvas, x0, y0, widthPerNode, margin, element.name, element.group, bgPaint, textPaint)
    }

    fun drawElement(canvas: Canvas, x0: Float, y0: Float, widthPerNode: Float, margin: Boolean, name: String, group: Int, bgPaint: Paint, textPaint: Paint){

        drawElementRaw(canvas, x0, y0, widthPerNode, margin, group, bgPaint)

        // todo split long text into multiple sections...
        val width0 = textPaint.measureText(name)
        textPaint.color = if(brightness(bgPaint.color) > 0.3f) 0xff000000.toInt() else -1
        textPaint.textSize = clamp(textPaint.textSize * widthPerNode * 0.8f / width0, widthPerNode*0.02f, widthPerNode*0.35f)
        val textDy = (widthPerNode - (textPaint.ascent() + textPaint.descent()))/2
        canvas.drawText(name, x0 + widthPerNode/2, y0 + textDy, textPaint)

    }

    fun drawElementRaw(canvas: Canvas, x0: Float, y0: Float, widthPerNode: Float, margin: Boolean, group: Int, bgPaint: Paint){
        drawElementRaw(canvas, x0, y0, widthPerNode, if(margin) getMargin(widthPerNode) else 0f, group, bgPaint)
    }

    fun drawElementRaw(canvas: Canvas, x0: Float, y0: Float, widthPerNode: Float, margin: Float, group: Int, bgPaint: Paint){

        bgPaint.color = if(group < 0) 0xff000000.toInt() else GroupColors[group]
        val roundness = widthPerNode * 0.1f
        drawRoundRect(canvas,
            x0 + margin,
            y0 + margin,
            x0 + widthPerNode - margin * 2,
            y0 + widthPerNode - margin * 2,
            roundness, roundness, bgPaint)

    }

    private fun drawRoundRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, bgPaint: Paint){
        fRect.left = left
        fRect.top = top
        fRect.right = right
        fRect.bottom = bottom
        canvas.drawRoundRect(fRect, rx, ry, bgPaint)
    }

}