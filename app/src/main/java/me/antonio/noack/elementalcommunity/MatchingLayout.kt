package me.antonio.noack.elementalcommunity

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import kotlin.math.max

/**
 * has issues with static sized margins -> only for views with relative margins or we need to create an advanced version, that is a linear layout itself
 * */
class MatchingLayout(ctx: Context, attributeSet: AttributeSet?): FrameLayout(ctx, attributeSet) {

    var childLeft = 0
    var childTop = 0

    var realWidth = 0
    var realHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val child = children.firstOrNull()
        if(child == null){
            setMeasuredDimension(0, 0)
            return
        }

        val marginLeft = child.marginLeft()
        val marginTop = child.marginTop()
        val marginRight = child.marginRight()
        val marginBottom = child.marginBottom()

        measureChildWithMargins(child,
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST), 0,
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST), 0)

        // todo get maximum size
        val maxSizeX = when(MeasureSpec.getMode(widthMeasureSpec)){
            MeasureSpec.EXACTLY -> max(0, MeasureSpec.getSize(widthMeasureSpec) - (paddingLeft + paddingRight + marginLeft + marginRight))
            MeasureSpec.AT_MOST -> max(0, MeasureSpec.getSize(widthMeasureSpec) - (paddingLeft + paddingRight + marginLeft + marginRight))
            MeasureSpec.UNSPECIFIED -> 1024 * 16
            else -> 0
        }

        val maxSizeY = when(MeasureSpec.getMode(heightMeasureSpec)){
            MeasureSpec.EXACTLY -> max(0, MeasureSpec.getSize(heightMeasureSpec) - (paddingTop + paddingBottom + marginTop + marginBottom))
            MeasureSpec.AT_MOST -> max(0, MeasureSpec.getSize(heightMeasureSpec) - (paddingTop + paddingBottom + marginTop + marginBottom))
            MeasureSpec.UNSPECIFIED -> 1024 * 16
            else -> 0
        }

        if(maxSizeX == 0 || maxSizeY == 0){
            child.layout(0, 0, 0, 0)
            setMeasuredDimension(0, 0)
            return
        }

        val width = child.measuredWidth
        val height = child.measuredHeight

        if(width * maxSizeY > height * maxSizeX){
            // top & bottom have space

            realWidth = maxSizeX
            realHeight = height * realWidth / width

            childLeft = 0
            childTop = (maxSizeY - realHeight)/2

        }  else {
            // left & right have space

            realHeight = maxSizeY
            realWidth = width * realHeight / height

            childTop = 0
            childLeft = (maxSizeX - realWidth)/2

        }

        childLeft += paddingLeft + marginLeft
        childTop += paddingTop + marginTop

        child.measure(MeasureSpec.makeMeasureSpec(realWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(realHeight, MeasureSpec.EXACTLY))

        // child.layout(childLeft, childTop, childLeft + realWidth, childTop + realHeight)

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        children.firstOrNull()?.layout(childLeft, childTop, childLeft + realWidth, childTop + realHeight)
        // throw RuntimeException("$childLeft, $childTop, $right, $bottom, $realWidth, $realHeight")
    }

    fun View.marginLeft() = (this.layoutParams as? MarginLayoutParams)?.leftMargin ?: 0
    fun View.marginTop() = (this.layoutParams as? MarginLayoutParams)?.topMargin ?: 0
    fun View.marginRight() = (this.layoutParams as? MarginLayoutParams)?.rightMargin ?: 0
    fun View.marginBottom() = (this.layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0

}