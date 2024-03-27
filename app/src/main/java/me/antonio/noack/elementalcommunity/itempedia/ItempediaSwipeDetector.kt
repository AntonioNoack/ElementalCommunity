package me.antonio.noack.elementalcommunity.itempedia

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import me.antonio.noack.elementalcommunity.AllManager
import kotlin.math.abs
import kotlin.math.sign

/**
 * swipe listener: when swiping left/right, go left/right one page
 * */
class ItempediaSwipeDetector(val manager: AllManager) : RecyclerView.OnItemTouchListener {

    private val gestureDetector =
        GestureDetector(manager, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                e1 ?: return false
                val diffX = e2.x - e1.x
                return if (abs(diffX) > 100 &&
                    abs(velocityX) > 100 &&
                    abs(velocityX) > abs(velocityY)
                ) {
                    manager.deltaItempediaPage(-sign(diffX).toInt())
                    true
                } else false
            }
        })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        gestureDetector.onTouchEvent(e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // ignore for now
    }

}