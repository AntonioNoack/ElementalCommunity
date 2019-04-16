package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.api.WebServices
import java.util.*

class NewsView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var news = arrayOfNulls<WebServices.News?>(10)

    private val relativeWidth = 4f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth,  measuredHeight)
    }

    private val random = Random(System.nanoTime())

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        val size = GroupColors.size


        val sideWays = measuredWidth > measuredHeight
        val width = measuredWidth * 1f
        val widthPerNode = width / if(sideWays) 2f * relativeWidth else relativeWidth

        for(candidate in news){

            val store = canvas.save()

            if(sideWays) canvas.translate((width-relativeWidth*widthPerNode)/2 + (random.nextFloat()-.5f)*width, (random.nextFloat()-.5f)*width)
            canvas.rotate((random.nextFloat()-.5f) * 50, width/2, widthPerNode/2)

            drawElement(canvas, 0f, 0f, widthPerNode, true, candidate?.a?.name ?: "App", candidate?.a?.group ?: random.nextInt(size), bgPaint, textPaint)
            drawElement(canvas, widthPerNode*(relativeWidth-1f)/2, 0f, widthPerNode, true, candidate?.b?.name ?: "No WLAN", candidate?.b?.group ?: random.nextInt(size), bgPaint, textPaint)
            drawElement(canvas, widthPerNode*(relativeWidth-1f), 0f, widthPerNode, true, candidate?.result ?: "No Game", candidate?.resultGroup ?: random.nextInt(size), bgPaint, textPaint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = widthPerNode*.5f
            textPaint.color = 0xff777777.toInt()

            val dy = (textPaint.ascent() + textPaint.descent())/2
            canvas.drawText("+", widthPerNode*((relativeWidth-1f)/4 + 0.5f), widthPerNode/2-dy, textPaint)
            canvas.drawText("->", widthPerNode*((relativeWidth-1f)*3/4 + 0.5f), widthPerNode/2-dy, textPaint)

            /*bgPaint.color = bgPaint.color and 0x50ffffff.toInt()
            canvas.drawRect(0f, 0f, width, widthPerNode, bgPaint)
            */
            textPaint.color = 0xff000000.toInt()
            textPaint.textSize = widthPerNode*.21f
            val dy2 = (textPaint.ascent() + textPaint.descent())/2
            canvas.drawText("${timeString(candidate?.dt ?: (Math.pow(2.0, 1.0 + 20.0 * random.nextDouble())).toInt())} ago ${if((candidate?.w ?: 0) > 0) "liked" else "disliked"}",
                widthPerNode*relativeWidth/2,
                widthPerNode + 1.5f * dy2, textPaint)

            canvas.restoreToCount(store)
            if(!sideWays){
                canvas.translate(0f, widthPerNode * if(sideWays) 0.42f else 0.85f)
            }

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