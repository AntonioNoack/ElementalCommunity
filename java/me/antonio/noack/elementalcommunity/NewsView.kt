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

class NewsView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    val noise = NoiseMap2D()
    var news = ArrayList<WebServices.News>(10)

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

        GroupsEtc.tick()

        val size = GroupColors.size

        val width = measuredWidth * 1f
        val widthPerNode = width / relativeWidth

        val time = sin(System.nanoTime() * 1e-10).toFloat()

        for(i in 0 until max(10, news.size)){

            val candidate = news.getOrNull(i)

            val store = canvas.save()

            canvas.rotate((noise.getNoise(time, i)-.5f) * 50, width/2, widthPerNode/2)

            drawElement(canvas, 0f, 0f, 0f, widthPerNode, true, candidate?.a?.name ?: "App", candidate?.a?.group ?: (noise.getNoise(time, 78+i*100).times(size).toInt()), bgPaint, textPaint)
            drawElement(canvas, widthPerNode*(relativeWidth-1f)/2, 0f, 0f, widthPerNode, true, candidate?.b?.name ?: "No WLAN", candidate?.b?.group ?: (noise.getNoise(time, 156+i*1020).times(size).toInt()), bgPaint, textPaint)
            drawElement(canvas, widthPerNode*(relativeWidth-1f), 0f, 0f, widthPerNode, true, candidate?.result ?: "No Game", candidate?.resultGroup ?: (noise.getNoise(time, 23+i*950).times(size).toInt()), bgPaint, textPaint)

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
            canvas.drawText("${timeString(candidate?.dt ?: (pow(2.0, 1.0 + 20.0 * random.nextDouble())).toInt())} ago ${if((candidate?.w ?: 0) > 0) "liked" else "disliked"}",
                widthPerNode*relativeWidth/2,
                widthPerNode + 1.5f * dy2, textPaint)

            canvas.restoreToCount(store)
            canvas.translate(0f, widthPerNode * 0.85f)

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