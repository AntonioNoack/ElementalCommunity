package me.antonio.noack.elementalcommunity.utils

import android.view.View
import android.view.ViewGroup
import kotlin.math.min

object MeasureSpecHelper {
    fun getDefaultSize(measureSpec: Int, size: Int, lp: Int): Int {
        val selfSize = View.MeasureSpec.getSize(measureSpec)
        return when (View.MeasureSpec.getMode(measureSpec)) {
            View.MeasureSpec.AT_MOST -> when (lp) {
                ViewGroup.LayoutParams.MATCH_PARENT -> selfSize
                else -> min(selfSize, size) // wrap_content
            }
            View.MeasureSpec.EXACTLY -> selfSize
            else -> size // UNSPECIFIED
        }
    }
}