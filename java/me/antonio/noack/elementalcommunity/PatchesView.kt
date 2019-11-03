package me.antonio.noack.elementalcommunity

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.children
import kotlin.math.max

class PatchesView(context: Context, attributeSet: AttributeSet?): ViewGroup(context, attributeSet){

    fun dpToPx(dp: Int): Float {
        val displayMetrics = context!!.resources.displayMetrics
        return (dp * displayMetrics.density) + 0.5f
    }

    fun getMargin() = dpToPx(5).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val margin = getMargin()
        val originalWidth = MeasureSpec.getSize(widthMeasureSpec)

        val smallerMS = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
        for(child in children) {
            child.measure(smallerMS, heightMeasureSpec)
        }

        val y = calcLayout(originalWidth, margin)

        setMeasuredDimension(originalWidth, y)

    }

    fun calcLayout(width: Int, margin: Int): Int {

        var maxHeight = 0
        var x = -margin
        var y = 0
        for(child in children){
            val x1 = x + margin + child.measuredWidth
            if(x1 <= width){
                // add child to line
                x += margin
                child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
                maxHeight = max(maxHeight, child.measuredHeight)
                x = x1
            } else {
                // add child to next line
                y += maxHeight + margin
                x = 0
                child.layout(0, y, child.measuredWidth, y + child.measuredHeight)
                x += child.measuredWidth
                maxHeight = child.measuredHeight
            }
        }

        y += maxHeight

        return y

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val margin = getMargin()
        val originalWidth = r-l

        calcLayout(originalWidth, margin)

    }

}