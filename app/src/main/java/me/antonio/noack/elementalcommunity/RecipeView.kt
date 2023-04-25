package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.api.WebServices
import java.lang.StrictMath.pow
import java.util.*
import kotlin.math.max
import kotlin.math.sin

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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

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

        /*bgPaint.color = bgPaint.color and 0x50ffffff.toInt()
        canvas.drawRect(0f, 0f, width, widthPerNode, bgPaint)
        */
        /*textPaint.color = 0xff000000.toInt()
        textPaint.textSize = widthPerNode*.21f
        val dy2 = (textPaint.ascent() + textPaint.descent())/2
        canvas.drawText("${timeString(candidate?.dt ?: (pow(2.0, 1.0 + 20.0 * random.nextDouble())).toInt())} ago ${if((candidate?.w ?: 0) > 0) "liked" else "disliked"}",
            widthPerNode*relativeWidth/2,
            widthPerNode + 1.5f * dy2, textPaint)*/

        if(overlay.shr(24).and(255) != 0){
            bgPaint.color = overlay
            canvas.drawRect(0f, 0f, width, measuredHeight.toFloat(), bgPaint)
        }

    }

    fun darken(rgb: Int): Int {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        return 0xff000000.toInt() or ((r / 2) shl 16) or ((g / 2) shl 8) or (b/2)
    }


    fun timeString(sec: Int): String {
        return if(sec < 30){
            sec.toString()+"s"
        } else if(sec < 180){
            ((sec+5)/10*10).toString()+"s"
        } else {
            val min = (sec+30) / 60
            if(min < 30){
                min.toString()+"min"
            } else if(min < 180){
                ((min+5)/10*10).toString()+"min"
            } else {
                val hour = (min+30) / 60
                if(hour < 72){
                    hour.toString()+"h"
                } else {
                    val days = (hour + 12) / 24
                    days.toString()+"d"
                }
            }
        }
    }

}