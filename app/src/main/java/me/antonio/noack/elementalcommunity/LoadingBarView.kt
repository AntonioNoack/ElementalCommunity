package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonio.noack.elementalcommunity.utils.Maths.fract
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class LoadingBarView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {

    val paint = Paint()

    var lastTime = System.nanoTime()

    override fun draw(canvas: Canvas?) {

        super.draw(canvas)
        canvas ?: return

        val width = width
        val height = height

        val thisTime = System.nanoTime()
        val deltaTime = min(0.1, (thisTime - lastTime) * 1e-9)
        lastTime = thisTime

        val lastProgress = progress

        val done = BasicOperations.done.get()
        val todo = BasicOperations.todo.get()

        val target = if (todo == done) done + 0.1 else done + 0.5
        progress = max(progress, target - 5f)

        val speed = if (todo == done) 1.0 else 0.1 * (target - progress)
        progress += (target - progress) * deltaTime * speed
        progress = min(progress, todo.toDouble())

        paint.color = 0x2CA4EC or 0xff000000.toInt()
        val actualWidth = (fract(progress) * width).toInt()

        if (todo != done) {
            // show stripes while loading
            // animate them
            val animation = (thisTime shr 19).and(1023) / 1023f
            val stripeLength = height * 2
            val stripeSpace = height + 0
            val stripeOffset = stripeLength + stripeSpace
            val startX = ((1f - animation) * stripeOffset).toInt()
            for (dx in startX until actualWidth step stripeOffset) {
                canvas.drawRect(x + dx, y, x + min(actualWidth, dx + height), y + height, paint)
            }
        } else {
            canvas.drawRect(x, y, x + actualWidth, y + height, paint)
        }

        if (progress != lastProgress || todo != done) invalidate()
        else {
            thread {
                Thread.sleep(200)
                postInvalidate()
            }
        }

    }

    companion object {
        // one instance should be working only at a single time
        var progress = 0.0
    }

}